package villager_voices;

import java.util.Set;
import java.util.UUID;

/**
 * Shows a selected line to whichever players should receive it. <b>Not implemented in this
 * ticket (VV-2)</b>: this interface exists so {@link VillagerEventBus} has something to hand the
 * selected line to; VV-7 implements it (the {@code DisplayQueue} in {@code common} plus its
 * fabric shim) after this ticket merges.
 *
 * <p>{@code playerIds} is the candidate-recipient set the publisher supplied on the
 * {@link VillagerReactionSignal} (its {@code nearbyPlayerIds}), already filtered by the
 * per-player server-wide rate limit (docs/spec/domains/reaction.md REACTION-REQ-008). Narrowing
 * it further to players actually within the line's sound event's hearing range
 * (docs/spec/domains/display.md DISPLAY-REQ-002) is the sink's concern, not this ticket's. May be
 * empty, e.g. a game test with no player online, or every candidate rate-limited.
 */
public interface LineSink {

    /** Shows {@code line} for {@code villagerId} to {@code playerIds}. */
    void show(UUID villagerId, Set<UUID> playerIds, LineRef line);
}
