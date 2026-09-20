package villager_voices.fabric.display;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import villager_voices.display.DisplayFormat;
import villager_voices.display.DisplayLine;
import villager_voices.display.DisplayQueue;

/**
 * The fabric-side push for {@code common}'s {@link DisplayQueue} ({@code DISPLAY-REQ-001}): every
 * server tick, drains newly advanced lines and pushes each to its player's action bar via
 * {@link ServerPlayer#sendOverlayMessage(Component)} — the 26.2 method confirmed by {@code javap
 * -p} against {@code minecraft-merged-deobf-26.2.jar}: {@code ServerPlayer} declares {@code public
 * void sendOverlayMessage(Component)} (renamed from 1.21.1's {@code Player#displayClientMessage
 * (Component, boolean)}), matching docs/spec/04-architecture.md's loader adapter table and
 * docs/spec/domains/display.md §3's per-version table.
 *
 * <p>Registered once, from {@link villager_voices.fabric.VillagerVoicesFabric#onInitialize()}.
 */
public final class ActionBarDisplay {

    /**
     * Placeholder hearing radius in blocks, standing in for {@code DISPLAY-REQ-002}'s "the line's
     * own sound event's effective broadcast radius" — no real {@code SoundEvent} exists yet to
     * read a radius from (VV-8 registers them; this ticket's Findings record the dependency).
     * Vanilla's own {@code sounds.json} default/cap for {@code attenuation_distance} is 16 blocks
     * (research: {@code villager-events-sounds-and-emf-compat.md} §B1), reused here so the
     * placeholder already matches the eventual default in the common case.
     */
    public static final double PLACEHOLDER_HEARING_RADIUS_BLOCKS = 16.0;

    private final DisplayQueue queue;
    private final OverlayPusher pusher;

    public ActionBarDisplay(DisplayQueue queue) {
        this(queue, ActionBarDisplay::sendOverlayMessage);
    }

    /**
     * Test seam: routes each advanced line's resolved text through {@code pusher} instead of a
     * live {@link ServerPlayer#sendOverlayMessage} call. Package-visible for
     * {@code fabric/src/gametest}'s {@code DisplayQueueGameTest} (docs/spec/operations/testing.md).
     */
    ActionBarDisplay(DisplayQueue queue, OverlayPusher pusher) {
        this.queue = queue;
        this.pusher = pusher;
    }

    /** The queue this instance drains — the point future code (VV-2's adapter) enqueues into. */
    public DisplayQueue queue() {
        return queue;
    }

    /** Registers the server-tick-driven drain against {@code ServerTickEvents.END_SERVER_TICK}. */
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    /**
     * Drains {@link #queue} against {@code server}'s own tick count and pushes each newly advanced
     * line to its player, when that player is currently online. Package-visible so the game test
     * can call it directly against a real {@link MinecraftServer}, rather than through Fabric's
     * global {@code ServerTickEvents} bus (avoiding cross-test listener accumulation on that
     * static, JVM-wide event).
     */
    void onEndServerTick(MinecraftServer server) {
        Map<UUID, DisplayLine> pushed = queue.tick(server.getTickCount());
        if (pushed.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, DisplayLine> entry : pushed.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            DisplayLine line = entry.getValue();
            pusher.push(player, DisplayFormat.format(line.speakerLabel(), line.text()));
        }
    }

    /**
     * Every {@link ServerPlayer} within {@link #PLACEHOLDER_HEARING_RADIUS_BLOCKS} blocks of
     * {@code pos} ({@code DISPLAY-REQ-002}), via Fabric API's own {@code PlayerLookup.around}.
     */
    public static Collection<ServerPlayer> playersInHearingRange(ServerLevel level, Vec3 pos) {
        return PlayerLookup.around(level, pos, PLACEHOLDER_HEARING_RADIUS_BLOCKS);
    }

    private static void sendOverlayMessage(ServerPlayer player, String text) {
        player.sendOverlayMessage(Component.literal(text));
    }

    /** The one hop into networking that {@link #onEndServerTick} delegates to; see the test seam note above. */
    @FunctionalInterface
    interface OverlayPusher {
        void push(ServerPlayer player, String text);
    }
}
