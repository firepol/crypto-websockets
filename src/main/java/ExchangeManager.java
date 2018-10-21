import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.ColumnArg;
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
                    for(int i=0; i<3; i++) {
                        LimitOrder ask = orderBook.getAsks().get(i);

                        QueryBuilder<OrderBook, Integer> queryBuilder = orderBookDao.queryBuilder();
                        queryBuilder.where().eq(OrderBook.EXCHANGE_ID_FIELD_NAME, exchangeName).and()
                            .eq(OrderBook.SORT_FIELD_NAME, i + 1);
                        List<OrderBook> dbAsks = queryBuilder.query();

                        OrderBook dbAsk;
                        if (dbAsks.isEmpty()) {
                            dbAsk = new OrderBook(exchangeName, pair.base.toString(), pair.counter.toString(), "ask",
                                    ask.getLimitPrice(), ask.getOriginalAmount(), i + 1, orderBook.getTimeStamp());
                        } else {
                            dbAsk = dbAsks.get(0);
                            dbAsk.price = ask.getLimitPrice();
                            dbAsk.volume = ask.getOriginalAmount();
                            dbAsk.modified = orderBook.getTimeStamp();
                        }

                        orderBookDao.createOrUpdate(dbAsk);
                    }
                });
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

        // if you need to create the table
        // TODO : create only if not existing
//        TableUtils.createTable(connectionSource, OrderBook.class);
    }
}
