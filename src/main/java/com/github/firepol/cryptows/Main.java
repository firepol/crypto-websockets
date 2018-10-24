package com.github.firepol.cryptows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@CommandLine.Command(name = "cryptows", mixinStandardHelpOptions = true, version = "0.2 beta")
public class Main implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    @CommandLine.Option(names = {"--dir"}, required = true,
            description = "Directory containing pairs to subscribe to. Each file is the exchange name." +
                    "Content of each file, one pair per line: base,quote e.g. BTC,USD")
    private String pairsDir;

    @CommandLine.Option(names = {"--orders"}, defaultValue = "1",
            description = "Amount of orders to save in the DB, per pair/side.")
    private Integer orders;

    @CommandLine.Option(names = {"--dbUrl"}, defaultValue = "cryptows.db",
            description = "Path to the database, by default (SQLite): ./cryptows.db")
    private String dbUrl;

    @CommandLine.Option(names = {"--dbUsername"},
            description = "Database Username")
    private String dbUsername;

    @CommandLine.Option(names = {"--dbPassword"},
            description = "Database Password")
    private String dbPassword;

    public void run() {
        HashMap<String, PairsCollection> exchangePairs = new HashMap<>();
        ExchangeManager manager = new ExchangeManager(orders, dbUrl, dbUsername, dbPassword);
        try {
            if (!Files.exists(Paths.get(pairsDir))) {
                LOG.info("The directory you provided does not exist, please double check.");
                return;
            }
            Files.list(Paths.get(pairsDir))
                    .filter(Files::isRegularFile)
                    .forEach(filePath-> {
                        PairsCollection pairs = new PairsCollection(filePath.toFile());
                        String fileName = filePath.getFileName().toString();
                        String exchangeName = fileName.split("\\.")[0];
                        exchangePairs.put(exchangeName, pairs);
                    });
            manager.processWebsockets(exchangePairs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new Main(), args);
    }
}
