package villager_voices.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.ReactionRules;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.config.Config;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.catalogue.CatalogueReloadListener;
import villager_voices.fabric.catalogue.FabricLineCatalogue;
import villager_voices.fabric.compat.TalkingPayload;
import villager_voices.fabric.compat.TalkingStateSync;
import villager_voices.fabric.config.ConfigLoader;
import villager_voices.fabric.debug.DebugCommand;
import villager_voices.fabric.display.ActionBarDisplay;
import villager_voices.fabric.sound.FabricLineSink;
import villager_voices.fabric.sound.SoundRegistration;
import villager_voices.fabric.sound.TickScheduler;

/**
 * The mod's server-and-common entrypoint. Registers the 64 reaction {@code SoundEvent}s
 * ({@link SoundRegistration}, VV-8), the line catalogue's {@link CatalogueReloadListener}
 * (VV-3, docs/spec/domains/reaction-lines.md), and {@link TalkingPayload}'s own
 * {@code CustomPacketPayload} type (VV-12, docs/spec/domains/compat.md {@code COMPAT-REQ-002}) at
 * mod init — all three must exist before any world or datapack loads, or (for the payload) before
 * any player connects. Everything that needs a live server (the loaded config, the actual
 * {@link VillagerEventBus}, the {@link DisplayQueue} sized from it, and every
 * {@link VillagerEventSource}'s registration against that bus) is built once
 * {@link ServerLifecycleEvents#SERVER_STARTED} fires, in {@link #onServerStarted}
 * (docs/spec/contracts/data-contract.md: the config file is "read on server start"). The
 * development-only {@code /villager_voices debug} command (VV-13) registers at mod init, same as
 * before this ticket — it builds its own throwaway {@link VillagerEventBus} per invocation
 * ({@code DebugCommand}'s own Javadoc) and only ever reads {@link #displayQueue()}, so it needs no
 * server-started timing of its own.
 *
 * <p>{@link #displayQueue()} and {@link #eventBus()} expose whichever instance is currently live —
 * the empty pre-server defaults constructed at class init below before {@link #onServerStarted}
 * first runs, the config-sized ones after. Every publisher into {@link #eventBus()} (the mixins,
 * via each {@code villager_voices.fabric.events} class's own static {@code bus} field; the poll in
 * {@code PolledEvents}) reaches it only through whatever bus {@link VillagerEventSource#register}
 * was called with — so a mixin firing before {@code SERVER_STARTED} publishes into that empty
 * pre-server bus (a real, valid instance with no catalogue/sink/clock/roll configured yet, never
 * {@code null}) rather than NPEing; {@code VillagerEventBus#publish} on such an unconfigured bus
 * simply fans the signal out to subscribers and skips the reaction pipeline (its own documented
 * no-argument-constructor behaviour). {@code TradeAndSocialEvents.publish} additionally guards its
 * own static {@code bus} field against still being {@code null} at that point, for the same
 * before-server-started window.
 *
 * <p>A world restart within one client session (leaving a singleplayer world and starting or
 * loading another) re-fires {@code SERVER_STARTED} and rebuilds both, including re-running every
 * {@link VillagerEventSource#register}; VV-4/5/6's own event-hook registrations should stay
 * idempotent-safe against that, or unregister on {@code SERVER_STOPPING}, once that matters in
 * practice (recorded in VV-8's own Findings).
 *
 * <p>{@link #tickScheduler()} (VV-18) is drained every {@code ServerTickEvents.END_SERVER_TICK} and
 * cleared on {@code SERVER_STOPPING}, so a grunt-delayed line-sound task never fires against a level
 * that has since been unloaded.
 */
public final class VillagerVoicesFabric implements ModInitializer {
    public static final String MOD_ID = "villager_voices";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile DisplayQueue displayQueue = new DisplayQueue();
    private static volatile VillagerEventBus eventBus = new VillagerEventBus();

    /**
     * The mod's one grunt-then-line delay scheduler (VV-18, docs/spec/domains/audio.md
     * {@code AUDIO-REQ-007}) — a single long-lived instance, unlike {@link #displayQueue} and
     * {@link #eventBus} it is never rebuilt on {@code SERVER_STARTED} (it carries no config-sized
     * state of its own), only drained every tick and cleared on {@code SERVER_STOPPING} below.
     */
    private static final TickScheduler tickScheduler = new TickScheduler();

    /** The currently live per-player action-bar queue (docs/spec/domains/display.md §3). */
    public static DisplayQueue displayQueue() {
        return displayQueue;
    }

    /** The currently live reaction pipeline (docs/spec/domains/reaction.md). */
    public static VillagerEventBus eventBus() {
        return eventBus;
    }

    /** The mod's one grunt-then-line delay scheduler (VV-18). */
    public static TickScheduler tickScheduler() {
        return tickScheduler;
    }

    @Override
    public void onInitialize() {
        SoundRegistration.registerAll();

        PayloadTypeRegistry.clientboundPlay().register(TalkingPayload.TYPE, TalkingPayload.STREAM_CODEC);

        ResourceLoader.get(PackType.SERVER_DATA)
            .registerReloadListener(CatalogueReloadListener.id(), new CatalogueReloadListener());

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        ServerTickEvents.END_SERVER_TICK.register(server -> tickScheduler.drain(server.getTickCount()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> tickScheduler.clear());

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            DebugCommand.register();
        }

        LOGGER.info("Wait, they talk now? ready");
    }

    /**
     * Loads {@code config/villager_voices.json} (writing shipped defaults if it is missing), then
     * builds the config-sized {@link DisplayQueue}, {@link ReactionRules}, {@link FabricLineSink},
     * and {@link VillagerEventBus} — VV-2's and VV-7's own hardcoded defaults, now
     * operator-overridable (docs/spec/contracts/data-contract.md {@code DATA-REQ-002}) — and
     * registers every discovered {@link VillagerEventSource} against the new bus.
     */
    private void onServerStarted(MinecraftServer server) {
        Config config = ConfigLoader.loadOrCreateDefault(FabricLoader.getInstance().getConfigDir());

        DisplayQueue queue = new DisplayQueue(config.displayQueueMinHoldTicks(), DisplayQueue.DEFAULT_CAP_PER_PLAYER);
        displayQueue = queue;
        new ActionBarDisplay(queue).register();

        ReactionRules rules = new ReactionRules(
            config.perVillagerPerEventCooldownTicks(),
            config.perVillagerGlobalCooldownTicks(),
            config.serverRatePerPlayerTicks());
        FabricLineSink sink = new FabricLineSink(server, config, queue, new TalkingStateSync());
        VillagerEventBus bus = new VillagerEventBus(new FabricLineCatalogue(), sink,
            server::getTickCount, server.overworld().getRandom()::nextInt, rules);
        eventBus = bus;

        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(bus);
        }

        LOGGER.info("villager_voices: reaction pipeline configured from {}", ConfigLoader.FILE_NAME);
    }
}
