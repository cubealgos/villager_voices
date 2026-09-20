package villager_voices.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Two claims about the source tree, checked against the files themselves: the mod opens no socket
 * of its own (COMP-REQ-001: no networking type is referenced outside Minecraft's own packet API),
 * and no source or resource file names the reference products this mod is deliberately never
 * compared to (COMP-REQ-002, docs/spec/decisions/DEC-009-positioning.md).
 */
final class SourceSurfaceTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Pattern NETWORKING = Pattern.compile(
        "java\\.net\\.|java\\.nio\\.channels\\.|HttpClient|Socket|URLConnection|HttpURLConnection");
    private static final Pattern REFERENCE_PRODUCT = Pattern.compile(
        "villager news|element animation|bedrock", Pattern.CASE_INSENSITIVE);

    @Test
    void noNetworkingTypeIsReferencedByTheMod() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            Matcher m = NETWORKING.matcher(Files.readString(file));
            if (m.find()) {
                offenders.add(file + " mentions " + m.group());
            }
        }
        assertTrue(offenders.isEmpty(), "COMP-REQ-001: no outbound networking, but " + offenders);
    }

    @Test
    void noSourceOrResourceFileNamesTheReferenceProducts() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : allTextFiles()) {
            String text = Files.readString(file);
            if (REFERENCE_PRODUCT.matcher(text).find()) {
                offenders.add(file.toString());
            }
        }
        assertTrue(offenders.isEmpty(), "COMP-REQ-002: no reference product named, but " + offenders);
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> walk = Files.walk(MAIN)) {
            return walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static List<Path> allTextFiles() throws IOException {
        try (Stream<Path> mainWalk = Files.walk(MAIN); Stream<Path> resourcesWalk = Files.walk(RESOURCES)) {
            List<Path> files = new ArrayList<>();
            mainWalk.filter(Files::isRegularFile).forEach(files::add);
            resourcesWalk.filter(Files::isRegularFile).forEach(files::add);
            return files;
        }
    }
}
