import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class ExchangeManager {
    private final static Logger LOG = LoggerFactory.getLogger(ExchangeManager.class);

    public static void processWebsockets(HashMap<String, PairsCollection> pairsByExchange) throws InterruptedException {

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

    private static StreamingExchange getStreamingExchange(String exchangeName) {
        String className = String.format("info.bitrich.xchangestream.%s.%sStreamingExchange",
                exchangeName, StringUtils.capitalize(exchangeName));
        return StreamingExchangeFactory.INSTANCE.createExchange(className);
    }

    private static Disposable subscribeOrderBook(StreamingExchange exchange, CurrencyPair pair) {
        return exchange.getStreamingMarketDataService()
                .getOrderBook(pair)
                .subscribe(orderBook -> {
                    LOG.info(orderBook.toString());
                });
    }

    private static Disposable subscribeTrades(StreamingExchange exchange, CurrencyPair pair) {
        return exchange.getStreamingMarketDataService()
                .getTrades(pair)
                .subscribe(trade -> {
                    LOG.info(trade.toString());
                });
    }
}
