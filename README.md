# Cryptows

A Java program to subscribe to cryptocurrency exchanges websockets and save order books in real time. A project based on [xchange-stream](https://github.com/bitrich-info/xchange-stream) and using [OrmLite](http://ormlite.com/) for the database persistency.

## Supported exchanges

Theoretically all exchanges supported by **xchange-stream** should be supported by this program. However I've tested this only with the following exchanges:

- binance
- bitfinex
- bitstamp
- coinbasepro
- hitbtc

### Add support to other exchanges

To add support to your favorite exchange, you must:
 
- Add its dependency in the `pom.xml` file (see how I added the others).
- Rebuild the project and try it out. If you are lucky it will just work.
- Some exchanges need a `ProductSubscribtion` to be set before connecting to the websocket. If you get an exception about missing ProductSubscribtion, simply add the name of the exchange in the `ExchangeManager` class, in the `NEED_PRODUCT_REGISTRATION` array. Then rebuild and try again.

## Supported databases

I added the necessary code to make this work with:

- SQLite
- PostgreSQL

### Add support to other databases

Refer to the [OrmLite documentation](http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite.html) to see examples for other databases and feel free to submit a PR to support your favorite database.

## Configuration

Create a `data` folder and place files in it, a file per exchange, each file should be named by the exchange, lowercase, e.g.:

- bitfinex.txt
- bitstamp.txt
- binance.txt
- coinbasepro.txt
- hitbtc.txt

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

## Building

Open the project with **Intellij IDEA** > Build > Build Artifacts... > you will find the resulting `cryptows.jar` in the `bin` folder.

## Usage

`cryptows` is a command line application and it expects these parameters:

- `--dir=PATHTODIR` Directory containing pairs to subscribe to. Each file is the exchange name. (e.g. `--dir=./pairs`)
- `--orders=5` Amount of orders to save in the DB, per pair/side (_default: 1_)
- `--dbUrl=DATABASEURL` Path to the database, by default (SQLite): `./cryptows.db`, if you want to use **PostgreSQL**, provide a jdbc connection string, e.g. `jdbc:postgresql://localhost:5432/cryptows` 
- `--dbUsername=USERNAME` Database username
- `--dbPassword=PASSWORD` Database password

### Examples

Browse to the bin folder (where the jar file was built)
```
cd bin
```

Get help showing supported parameters:
```
java -jar cryptows.jar -h
```

Run using the default SQLite database, this will create a `cryptows.db` file in the bin folder
```
java -jar cryptows.jar --dir=./data
```

Run using a PostgreSQL database (you must create the database first): 
```
java -jar cryptows.jar --dir=./data --dbUrl=jdbc:postgresql://localhost:5432/cryptows --dbUsername=USERNAME --dbPassword=PASSWORD
```

## Database

Simple query to check the `order_book` table:
```
select * from order_book order by base, quote, exchange_id, side
```

## Resources

- [ORMLite JDBC examples](http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_7.html#Examples)
- [Example ORMLite: SimpleMain](https://github.com/j256/ormlite-jdbc/blob/master/src/test/java/com/j256/ormlite/examples/simple/SimpleMain.java)
- [How to build a jar properly](https://stackoverflow.com/questions/1082580/how-to-build-jars-from-intellij-properly#answer-45303637)

## Contributing

I'm no java expert, so if you find that this program can be optimized, your contributions are very welcome. Just submit a PR and I'll check it out.

### Contributing rules

- Submit a separate PR for each change.
- A PR can contain several commits, but my favorite PRs contain atomic (single) commits: a single commit is easier to review than many commits.
- If you plan contributing more changes at the same time, try to separate them. At best create a different branch for each PR, so you are sure that they are independent from each other. Like this they are easier to review and can be merged faster.

## License

[MIT License](LICENSE)