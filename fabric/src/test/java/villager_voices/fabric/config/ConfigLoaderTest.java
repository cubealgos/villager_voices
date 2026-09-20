package villager_voices.fabric.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import villager_voices.config.Config;
import villager_voices.config.ConfigCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "the config file is written on first start with defaults" (this ticket's own test list), and the
 * read side of docs/spec/contracts/data-contract.md {@code DATA-REQ-002}/{@code DATA-REQ-004} —
 * all plain file I/O against a JUnit temp directory, no game instance needed.
 */
class ConfigLoaderTest {

    @Test
    void firstRunWritesTheShippedDefaultsAndReturnsThem(@TempDir Path configDir) {
        Path file = configDir.resolve(ConfigLoader.FILE_NAME);
        assertFalse(Files.exists(file), "precondition: no config file yet");

        Config loaded = ConfigLoader.loadOrCreateDefault(configDir);

        assertEquals(Config.defaults(), loaded);
        assertTrue(Files.exists(file), "the default config must be written on first run");
    }

    @Test
    void aSecondLoadReadsBackExactlyWhatTheFirstRunWrote(@TempDir Path configDir) {
        ConfigLoader.loadOrCreateDefault(configDir);

        Config reloaded = ConfigLoader.loadOrCreateDefault(configDir);

        assertEquals(Config.defaults(), reloaded);
    }

    @Test
    void aCustomisedFileOnDiskIsHonouredInsteadOfBeingOverwritten(@TempDir Path configDir) throws IOException {
        Config custom = new Config("1.0", 999, 1, 1, false, true, 5, 0.5, 8.0, Config.CategoryMutes.NONE_MUTED);
        Files.writeString(configDir.resolve(ConfigLoader.FILE_NAME), ConfigCodec.serialize(custom));

        Config loaded = ConfigLoader.loadOrCreateDefault(configDir);

        assertEquals(custom, loaded);
    }

    @Test
    void aMalformedFileDegradesToDefaultsAndIsLeftUntouchedOnDisk(@TempDir Path configDir) throws IOException {
        Path file = configDir.resolve(ConfigLoader.FILE_NAME);
        Files.writeString(file, "{ this is not valid json");

        Config loaded = ConfigLoader.loadOrCreateDefault(configDir);

        assertEquals(Config.defaults(), loaded, "DATA-REQ-004: a malformed file degrades to defaults");
        assertEquals("{ this is not valid json", Files.readString(file),
            "a malformed operator file is never silently rewritten");
    }
}
