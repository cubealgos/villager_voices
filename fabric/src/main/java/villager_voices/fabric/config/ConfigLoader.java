package villager_voices.fabric.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.config.Config;
import villager_voices.config.ConfigCodec;
import villager_voices.fabric.VillagerVoicesFabric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads {@code config/villager_voices.json} from the game's config directory on server start,
 * writing the shipped defaults there the first time it is missing (docs/spec/contracts/data-contract.md
 * {@code DATA-REQ-002}). Takes the config directory itself, not a
 * {@code net.fabricmc.loader.api.FabricLoader} reference, so it stays trivially unit-testable
 * against a plain temp directory ({@code fabric/src/test}) without booting a game instance.
 */
public final class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);

    /** The config file's own name inside the game's config directory. */
    public static final String FILE_NAME = "villager_voices.json";

    private ConfigLoader() {
    }

    /**
     * @param configDir the game's config directory (e.g. {@code FabricLoader.getInstance().getConfigDir()})
     * @return the parsed config, or {@link Config#defaults()} written to disk for next time if the
     *     file did not exist yet, or {@link Config#defaults()} alone (the file is left untouched)
     *     if the file exists but could not be read at all ({@code DATA-REQ-004})
     */
    public static Config loadOrCreateDefault(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        if (Files.notExists(file)) {
            Config defaults = Config.defaults();
            writeQuietly(file, defaults);
            return defaults;
        }
        try {
            return ConfigCodec.parse(Files.readString(file));
        } catch (IOException e) {
            LOGGER.warn("villager_voices: failed to read {}, using the shipped defaults for this session: {}",
                    file, e.getMessage());
            return Config.defaults();
        }
    }

    private static void writeQuietly(Path file, Config config) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, ConfigCodec.serialize(config));
            LOGGER.info("villager_voices: wrote the default config to {}", file);
        } catch (IOException e) {
            LOGGER.warn("villager_voices: could not write the default config to {}: {}", file, e.getMessage());
        }
    }
}
