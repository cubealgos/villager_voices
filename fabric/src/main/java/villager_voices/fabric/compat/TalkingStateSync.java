package villager_voices.fabric.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import villager_voices.compat.TalkingState;
import villager_voices.config.Config;

import java.util.Set;
import java.util.UUID;

/**
 * The server-authoritative half of VV-12's talking-state sync (docs/spec/domains/compat.md
 * {@code COMPAT-REQ-002}): marks {@link #talkingState()}'s own {@link TalkingState} when a line
 * plays and sends {@link TalkingPayload} to every candidate player still in the display's own
 * hearing range — the same {@code display.hearingRangeBlocks} filter
 * {@code villager_voices.fabric.sound.FabricLineSink} already applies to the action-bar text,
 * applied again here independently, since the talking-state visual cue is not gated behind
 * {@code display.actionBar} (a player with the action bar off should still see mouth movement).
 *
 * <p>Called from {@code FabricLineSink#show} once a line's sound has actually started playing —
 * this class's own job stops at "mark and sync," never how or whether a line is selected.
 */
public final class TalkingStateSync {

    private final TalkingState talkingState = new TalkingState();
    private final TalkingPayloadSender sender;

    /** Production wiring: sends for real, via Fabric's own S2C networking API. */
    public TalkingStateSync() {
        this(ServerPlayNetworking::send);
    }

    /** @param sender the seam a test substitutes to capture sent payloads (see its own Javadoc). */
    public TalkingStateSync(TalkingPayloadSender sender) {
        this.sender = sender;
    }

    /** The server's own authoritative instance — queryable directly by a test, never by a client. */
    public TalkingState talkingState() {
        return talkingState;
    }

    /**
     * Marks {@code villagerId} talking through
     * {@code now + config.talkingDurationTicks() + extraTicks} and sends {@link TalkingPayload} to
     * every player in {@code playerIds} within hearing range of {@code pos} in {@code level}.
     *
     * @param playerIds the sink's own candidate set (already server-wide-rate-limited, VV-2's
     *     {@code LineSink} contract) — narrowed here to hearing range, independent of the
     *     action-bar's own {@code display.actionBar}-gated narrowing
     * @param now the current server tick ({@code MinecraftServer#getTickCount()}), the same clock
     *     {@code FabricLineSink}'s own caller already has
     * @param extraTicks additional ticks to extend the mark by, beyond
     *     {@code config.talkingDurationTicks()} — VV-18's own grunt delay
     *     ({@code villager_voices.fabric.sound.ReactionSoundPlayer#delayTicksFor}), so the talking
     *     state spans grunt plus line rather than expiring mid-grunt; {@code 0} for a line with no
     *     grunt, unchanged from before this ticket
     */
    public void markTalking(MinecraftServer server, ServerLevel level, UUID villagerId, Vec3 pos,
            Set<UUID> playerIds, Config config, long now, long extraTicks) {
        long durationTicks = config.talkingDurationTicks() + extraTicks;
        talkingState.startTalking(villagerId, now + durationTicks);

        TalkingPayload payload = new TalkingPayload(villagerId, (int) durationTicks);
        double rangeSq = config.hearingRangeBlocks() * config.hearingRangeBlocks();
        for (UUID playerId : playerIds) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || player.level() != level) {
                continue;
            }
            if (player.position().distanceToSqr(pos) > rangeSq) {
                continue;
            }
            sender.send(player, payload);
        }
    }
}
