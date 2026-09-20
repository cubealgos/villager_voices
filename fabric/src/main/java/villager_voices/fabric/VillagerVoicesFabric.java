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

    /**
     * The mod's single {@link VillagerEventBus}, wired with every discovered
     * {@link VillagerEventSource} in {@link #onInitialize}. Exposed statically because the native
     * Fabric API events each source registers against are process-wide listeners, not scoped to
     * one world — a game test (fabric/src/gametest) needs this same instance to subscribe a test
     * consumer to, since a fresh bus of its own would never see events dispatched through the
     * sources actually wired at mod init (docs/spec/operations/testing.md).
     */
    public static final VillagerEventBus BUS = new VillagerEventBus();

    @Override
    public void onInitialize() {
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(BUS);
        }
        LOGGER.info("Wait, they talk now? ready");
    }
}
