import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ExchangeManager {
    private final static Logger LOG = LoggerFactory.getLogger(ExchangeManager.class);

    private final static String DATABASE_URL = "jdbc:sqlite:cryptows.db";

    public Dao<OrderBook, Integer> orderBookDao;

    public void processWebsockets(HashMap<String, PairsCollection> pairsByExchange) throws Exception {
        ConnectionSource connectionSource = new JdbcConnectionSource(DATABASE_URL);
        setupDatabase(connectionSource);

        CountDownLatch latch = new CountDownLatch(1);
        pairsByExchange.forEach((exchangeName, pairsCollection)->{
            if (exchangeName.equals("binance")) {
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
    }

    private StreamingExchange getStreamingExchange(String exchangeName) {
        String className = String.format("info.bitrich.xchangestream.%s.%sStreamingExchange",
                exchangeName, StringUtils.capitalize(exchangeName));
        return StreamingExchangeFactory.INSTANCE.createExchange(className);
    }

    private Disposable subscribeOrderBook(StreamingExchange exchange, CurrencyPair pair) {
        String exchangeName = exchange.getExchangeSpecification().getExchangeName().toLowerCase();
        return exchange.getStreamingMarketDataService()
                .getOrderBook(pair)
                .subscribe(orderBook -> {
                    LOG.info(orderBook.toString());
                    handleOrderBook(exchangeName, orderBook);
                });
    }

    private void handleOrderBook(String exchangeName, org.knowm.xchange.dto.marketdata.OrderBook orderBook) throws java.sql.SQLException {
        for(int i=0; i<3; i++) {
            Date timestamp = orderBook.getTimeStamp();
            handleOrderBookPosition(exchangeName, "ask", orderBook.getAsks().get(i), timestamp, i + 1);
            handleOrderBookPosition(exchangeName, "bid", orderBook.getBids().get(i), timestamp, i + 1);
        }
    }

    private void handleOrderBookPosition(String exchangeName, String side, LimitOrder order, Date date, int sort) throws java.sql.SQLException {
        OrderBook dbOrder = getDbOrder(exchangeName, side, order.getCurrencyPair(), sort);
        createOrUpdateOrder(dbOrder, order, exchangeName, side, date, sort);
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

    private void createOrUpdateOrder(OrderBook dbOrder, LimitOrder order, String exchangeName, String side, Date date, int sort) throws java.sql.SQLException {
        CurrencyPair pair = order.getCurrencyPair();
        if (dbOrder == null) {
            dbOrder = new OrderBook(exchangeName, pair.base.toString(), pair.counter.toString(), side,
                    order.getLimitPrice(), order.getOriginalAmount(), sort, date);
        } else {
            dbOrder.price = order.getLimitPrice();
            dbOrder.volume = order.getOriginalAmount();
            dbOrder.modified = date;
        }
        orderBookDao.createOrUpdate(dbOrder);
    }

    public Disposable subscribeTrades(StreamingExchange exchange, CurrencyPair pair) {
        return exchange.getStreamingMarketDataService()
                .getTrades(pair)
                .subscribe(trade -> {
                    LOG.info(trade.toString());
                });
    }

    private void setupDatabase(ConnectionSource connectionSource) throws Exception {
        orderBookDao = DaoManager.createDao(connectionSource, OrderBook.class);
        TableUtils.createTableIfNotExists(connectionSource, OrderBook.class);
    }
}
