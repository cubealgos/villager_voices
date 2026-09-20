package villager_voices.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;

/**
 * The mod's server-and-common entrypoint. Wires every {@link VillagerEventSource} discovered on
 * the classpath into a {@link VillagerEventBus}; the sources themselves land with the event
 * wiring tickets, not this bootstrap (docs/spec/domains/reaction.md).
 */
public final class VillagerVoicesFabric implements ModInitializer {
    public static final String MOD_ID = "villager_voices";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        VillagerEventBus bus = new VillagerEventBus();
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(bus);
        }
        LOGGER.info("Wait, they talk now? ready");
    }
}
