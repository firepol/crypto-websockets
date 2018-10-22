package com.github.firepol.cryptows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

@CommandLine.Command(name = "cryptows", mixinStandardHelpOptions = true, version = "0.1 beta")
public class Main implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    @CommandLine.Option(names = {"--dir"}, defaultValue = "./data",
            description = "Directory containing pairs to subscribe to. Each file is the exchange name." +
                    "Content of each file, one pair per line: base,quote e.g. BTC,USD")
    private String pairsDir;

    @CommandLine.Option(names = {"--db"}, defaultValue = "cryptows.db",
            description = "Path to the database, by default (SQLite): ./cryptows.db")
    private String dbPath;

    public void run() {
        HashMap<String, PairsCollection> exchangePairs = new HashMap<>();
        ExchangeManager manager = new ExchangeManager(dbPath);
        try (Stream<Path> paths = Files.walk(Paths.get(pairsDir))) {
            paths.filter(Files::isRegularFile)
                .forEach(filePath-> {
                    PairsCollection pairs = new PairsCollection(filePath.toString());
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
