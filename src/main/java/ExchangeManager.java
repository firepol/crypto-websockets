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

    public static void processWebsockets(HashMap<String, PairsCollection> pairsCollection) throws InterruptedException {

        HashMap<String, StreamingExchange> streamingExchanges = new HashMap<>();
        pairsCollection.forEach((key, value)->{
            StreamingExchange exchange = getStreamingExchange(key);
            exchange.connect().blockingAwait();
            streamingExchanges.put(key, exchange);
        });

        CountDownLatch latch = new CountDownLatch(1);

        pairsCollection.forEach((key, value)->{
            StreamingExchange exchange = streamingExchanges.get(key);
            //value.pairs.forEach(pair->subscribeTrades(exchange, pair));
            value.pairs.forEach(pair->subscribeOrderBook(exchange, pair));
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
