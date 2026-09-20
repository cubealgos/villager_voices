package villager_voices.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.catalogue.CatalogueReloadListener;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.display.ActionBarDisplay;

/**
 * The mod's server-and-common entrypoint. Wires every {@link VillagerEventSource} discovered on
 * the classpath into a {@link VillagerEventBus}; the sources themselves land with the event
 * wiring tickets, not this bootstrap (docs/spec/domains/reaction.md). Also registers the line
 * catalogue's {@link CatalogueReloadListener} (VV-3, docs/spec/domains/reaction-lines.md), so it
 * loads once at server start and again on every {@code /reload}.
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

    /**
     * The mod's single {@link VillagerEventBus}, wired with every discovered
     * {@link VillagerEventSource} in {@link #onInitialize}. Exposed statically because the native
     * Fabric API events each source registers against are process-wide listeners, not scoped to
     * one world — a game test (fabric/src/gametest) needs this same instance to subscribe a test
     * consumer to, since a fresh bus of its own would never see events dispatched through the
     * sources actually wired at mod init (docs/spec/operations/testing.md).
     */
    public static final VillagerEventBus BUS = new VillagerEventBus();
    /** The per-player action-bar queue (docs/spec/domains/display.md §3 "The per-player queue"). */
    public static final DisplayQueue DISPLAY_QUEUE = new DisplayQueue();

    @Override
    public void onInitialize() {
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(BUS);
        }

        ResourceLoader.get(PackType.SERVER_DATA)
            .registerReloadListener(CatalogueReloadListener.id(), new CatalogueReloadListener());

        new ActionBarDisplay(DISPLAY_QUEUE).register();
        LOGGER.info("Wait, they talk now? ready");
    }
}
