import info.bitrich.xchangestream.bitstamp.BitstampStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        String className = BitstampStreamingExchange.class.getName();
        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(className);
        // Connect to the Exchange WebSocket API. Blocking wait for the connection
        exchange.connect().blockingAwait();
        // Subscribe order book data with the reference to the subscription
        CountDownLatch latch = new CountDownLatch(1);

        // TODO: Subscribe: do something more useful, like saving to a DB instead of just LOG.info

        // For some exchanges (like bitfinex, binance) "USD" is "USDT", change accordingly
        CurrencyPair pair = CurrencyPair.BTC_USD;

        // Subscribe to order book
        Disposable obSubscription = exchange.getStreamingMarketDataService()
                .getOrderBook(pair)
                .subscribe(orderBook -> {
                    LOG.info(orderBook.toString());
                });

        // Subscribe to live trades
        Disposable tradeSubscription = exchange.getStreamingMarketDataService()
                .getTrades(pair)
                .subscribe(trade -> {
                    LOG.info(trade.toString());
                });

        latch.await();

        // Unsubscribe from trades
        tradeSubscription.dispose();

        // Unsubscribe from order book
        obSubscription.dispose();

        // Disconnect from exchange (non-blocking)
        exchange.disconnect()
                .subscribe(() -> LOG.info("Disconnected from the " + "Exchange"));
    }
}
