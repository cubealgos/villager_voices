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
     * The bus every discovered {@link VillagerEventSource} was wired into at mod init (VV-4).
     * Exposed, not just a local variable, for two reasons: game tests
     * (fabric/src/gametest, docs/spec/operations/testing.md's "Game tests" row) need to
     * {@link VillagerEventBus#subscribe} a test collector onto the exact bus the mixins and native
     * listeners publish into — those hooks are compiled-in and always publish through this one
     * instance, so a test cannot substitute its own bus; and VV-7/VV-8 will attach the real
     * {@code LineSink}/catalogue/clock/roll here once they land.
     */
    public static VillagerEventBus BUS;

    @Override
    public void onInitialize() {
        BUS = new VillagerEventBus();
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(BUS);
        }
        LOGGER.info("Wait, they talk now? ready");
    }
}
