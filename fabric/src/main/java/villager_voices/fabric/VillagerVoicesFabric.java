package villager_voices.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.display.ActionBarDisplay;

/**
 * The mod's server-and-common entrypoint. Wires every {@link VillagerEventSource} discovered on
 * the classpath into a {@link VillagerEventBus}; the sources themselves land with the event
 * wiring tickets, not this bootstrap (docs/spec/domains/reaction.md).
 *
 * <p>Also owns {@link #DISPLAY_QUEUE}, the per-player action-bar queue
 * (docs/spec/domains/display.md), and registers its {@link ActionBarDisplay} tick-driven push.
 * {@code DISPLAY_QUEUE} is this ticket's (VV-7) plumbing point for whoever enqueues a selected
 * line next — expected to be the reaction system's adapter from VV-2's {@code LineSink}, once
 * that package merges (`ARCH-DEC-001`: {@code common} never imports {@code reaction}'s types, so
 * the adapter lives here in {@code fabric} or in a wiring class, not in {@code display} itself).
 */
public final class VillagerVoicesFabric implements ModInitializer {
    public static final String MOD_ID = "villager_voices";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** The per-player action-bar queue (docs/spec/domains/display.md §3 "The per-player queue"). */
    public static final DisplayQueue DISPLAY_QUEUE = new DisplayQueue();

    @Override
    public void onInitialize() {
        VillagerEventBus bus = new VillagerEventBus();
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(bus);
        }
        new ActionBarDisplay(DISPLAY_QUEUE).register();
        LOGGER.info("Wait, they talk now? ready");
    }
}
