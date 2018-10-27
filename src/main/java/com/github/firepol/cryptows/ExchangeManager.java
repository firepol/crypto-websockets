package com.github.firepol.cryptows;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ExchangeManager {
    private final static Logger LOG = LoggerFactory.getLogger(ExchangeManager.class);

    private static String DATABASE_URL = "jdbc:sqlite:cryptows.db";
    private static String USERNAME;
    private static String PASSWORD;
    private static Integer ORDERS_TO_SAVE;
    // The following exchanges need a product registration, see processWebsockets method
    private static String[] NEED_PRODUCT_REGISTRATION = new String[]{"GDAX", "binance"};

    private Dao<OrderBook, Integer> orderBookDao;

    public ExchangeManager(Integer orders, String dbUrl, String dbUsername, String dbPassword) {
        ORDERS_TO_SAVE = orders;
        USERNAME = dbUsername;
        PASSWORD = dbPassword;

        if (!dbUrl.startsWith("jdbc")) {
            DATABASE_URL = String.format("jdbc:sqlite:%s", dbUrl);
        } else {
            DATABASE_URL = dbUrl;
        }
    }

    public void processWebsockets(HashMap<String, PairsCollection> pairsByExchange) throws Exception {
        ConnectionSource connectionSource = GetConnectionSource();
        setupDatabase(connectionSource);

        CountDownLatch latch = new CountDownLatch(1);
        pairsByExchange.forEach((exchangeName, pairsCollection)->{
            if (Arrays.asList(NEED_PRODUCT_REGISTRATION).contains(exchangeName)) {
                pairsCollection.pairs.forEach(pair->{
                    StreamingExchange exchange = getStreamingExchange(exchangeName);
                    ProductSubscription productSubscription = ProductSubscription.create()
                        .addOrderbook(pair)
                        .build();
                    exchange.connect(productSubscription).blockingAwait();
                    subscribeOrderBook(exchange, pair);
                });
            } else {
                StreamingExchange exchange = getStreamingExchange(exchangeName);
                exchange.connect().blockingAwait();
                pairsCollection.pairs.forEach(pair->subscribeOrderBook(exchange, pair));
            }
        });

        latch.await();

        closeConnection(connectionSource);
    }

    private ConnectionSource GetConnectionSource() throws SQLException {
        if (DATABASE_URL.startsWith("jdbc:sqlite")) {
            return new JdbcConnectionSource(DATABASE_URL);
        }
        return new DataSourceConnectionSource(createDataSource(), DATABASE_URL);
    }

    private static DataSource createDataSource() {
        // https://gist.github.com/abyrd/0e942f633939b55c7c09ee398cde6c81
        // ConnectionFactory can handle null username and password (for local host-based authentication)
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(DATABASE_URL, USERNAME, PASSWORD);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        GenericObjectPool connectionPool = new GenericObjectPool(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        // Disabling auto-commit on the connection factory confuses ORMLite, so we leave it on.
        // In any case ORMLite will create transactions for batch operations.
        return new PoolingDataSource(connectionPool);
    }

    private static void closeConnection(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Exception ex) {
            LOG.error("Error closing.");
        }
    }

    private StreamingExchange getStreamingExchange(String exchangeName) {
        String className = String.format("info.bitrich.xchangestream.%s.%sStreamingExchange",
                exchangeName.toLowerCase(), StringUtils.capitalize(exchangeName));
        return StreamingExchangeFactory.INSTANCE.createExchange(className);
    }

    private Disposable subscribeOrderBook(StreamingExchange exchange, CurrencyPair pair) {
        String exchangeName = exchange.getExchangeSpecification().getExchangeName().toLowerCase();
        return exchange.getStreamingMarketDataService()
                .getOrderBook(pair)
                .subscribe(orderBook -> {
                    LOG.debug(orderBook.toString());
                    handleOrderBook(exchangeName, orderBook);
                });
    }

    private Disposable subscribeTrades(StreamingExchange exchange, CurrencyPair pair) {
        return exchange.getStreamingMarketDataService()
                .getTrades(pair)
                .subscribe(trade -> {
                    LOG.debug(trade.toString());
                });
    }

    private void handleOrderBook(String exchangeName, org.knowm.xchange.dto.marketdata.OrderBook orderBook) throws java.sql.SQLException {
        for(int i = 0; i< ORDERS_TO_SAVE; i++) {
            Date timestamp = orderBook.getTimeStamp() != null ? orderBook.getTimeStamp() : new Date();
            handleOrderBookOrder(exchangeName, orderBook.getAsks().get(i), "ask", i + 1, timestamp);
            handleOrderBookOrder(exchangeName, orderBook.getBids().get(i), "bid", i + 1, timestamp);
        }
    }

    private void handleOrderBookOrder(String exchangeName, LimitOrder order, String side, int sort, Date date) throws java.sql.SQLException {
        OrderBook dbOrder = getDbOrder(exchangeName, side, order.getCurrencyPair(), sort);
        createOrUpdateOrder(dbOrder, order, exchangeName, side, sort, date);
    }

    private OrderBook getDbOrder(String exchangeName, String side, CurrencyPair pair, int sort) throws java.sql.SQLException {
        OrderBook result = null;
        QueryBuilder<OrderBook, Integer> queryBuilder = orderBookDao.queryBuilder();
        queryBuilder.where()
                .eq(OrderBook.EXCHANGE_ID_FIELD_NAME, exchangeName)
                .and()
                .eq(OrderBook.BASE_FIELD_NAME, pair.base.toString())
                .and()
                .eq(OrderBook.QUOTE_FIELD_NAME, pair.counter.toString())
                .and()
                .eq(OrderBook.SORT_FIELD_NAME, sort)
                .and()
                .eq(OrderBook.SIDE_FIELD_NAME, side);

        List<OrderBook> dbOrderBooks = queryBuilder.query();
        if (dbOrderBooks.size() > 0) {
            result = dbOrderBooks.get(0);
        }
        return result;
    }

    private void createOrUpdateOrder(OrderBook dbOrder, LimitOrder order, String exchangeName, String side, int sort, Date date) throws java.sql.SQLException {
        CurrencyPair pair = order.getCurrencyPair();
        if (dbOrder == null) {
            dbOrder = new OrderBook(exchangeName, side, pair.base.toString(), pair.counter.toString(),
                    order.getLimitPrice(), order.getOriginalAmount(), sort, date);
        } else {
            dbOrder.price = order.getLimitPrice();
            dbOrder.volume = order.getOriginalAmount();
            dbOrder.modified = date;
        }
        orderBookDao.createOrUpdate(dbOrder);
    }

    private void setupDatabase(ConnectionSource connectionSource) throws Exception {
        orderBookDao = DaoManager.createDao(connectionSource, OrderBook.class);
        try {
            TableUtils.createTableIfNotExists(connectionSource, OrderBook.class);
        } catch (SQLException e) {
            // The create sequence is a known error on postgres and can be ignored
            if (!e.getMessage().startsWith("SQL statement failed: CREATE SEQUENCE")) {
                throw e;
            }
        }
    }
}
