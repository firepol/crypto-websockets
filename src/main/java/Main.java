import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

@CommandLine.Command(name = "cryptows", mixinStandardHelpOptions = true, version = "0.1 alpha")
public class Main implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    @Parameters(arity = "1", paramLabel = "DIR", defaultValue = "./data",
            description = "DIR containing pairs to subscribe to: filename = exchange name, content: base,quote e.g. BTC,USD")
    private String pairsDir;

    public void run() {
        HashMap<String, PairsCollection> exchangePairs = new HashMap<>();
        try (Stream<Path> paths = Files.walk(Paths.get(pairsDir))) {
            paths.filter(Files::isRegularFile)
                .forEach(filePath-> {
                    PairsCollection pairs = new PairsCollection(filePath.toString());
                    String fileName = filePath.getFileName().toString();
                    String exchangeName = fileName.split("\\.")[0];
                    exchangePairs.put(exchangeName, pairs);
                });
            ExchangeManager.processWebsockets(exchangePairs);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        CommandLine.run(new Main(), args);
    }
}
