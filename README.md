# Cryptows

Subscribe to crypto exchanges websockets. A project based on [xchange-stream](https://github.com/bitrich-info/xchange-stream).

## Configuration

Create a `data` folder and place files in it, a file per exchange, each file should be named by the exchange, lowercase, e.g.:

- bitfinex
- bitstamp
- binance

Inside each file, add the pairs of your interest, comma separated as follows: `base,quote`.

E.g.:

```
BTC,USD
ETH,USD
ETH,BTC
EOS,BTC
```

You can put a `#` in front of a pair, to comment (and ignore) it.

- `base`: an uppercase string code of base fiat or crypto currency. E.g. BTC
- `quote`: an uppercase string code of quoted fiat or crypto currency. E.g. USD

For the pair BTC/USD, BTC is the base, which is the cryptocurrency you are buying or selling; USD is the quote, which is the fiat or cryptocurrency you are using to buy/sell the base.

## Usage

`cryptows` is a command line application and it expects as first parameter the folder containing the pairs, as explained above. If you omit it, `data` will be used by default.