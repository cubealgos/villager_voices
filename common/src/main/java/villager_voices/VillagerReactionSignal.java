package villager_voices;

import java.util.Set;
import java.util.UUID;

/**
 * One detected event, as reported by a loader adapter. {@code villagerId} identifies the
 * triggering villager for the cooldown/selection/silence rules in docs/spec/domains/reaction.md
 * §3 ({@link ReactionRules}, {@link LineSelector}, VV-2).
 *
 * <p>{@code villagerAsleep}/{@code villagerBaby} are the villager's own state at the moment of
 * detection: the loader adapter reads {@code LivingEntity.isSleeping()}/its age the same tick it
 * reports the signal, since {@code common} has no entity of its own to query
 * (docs/spec/04-architecture.md ARCH-DEC-001). {@code nearbyPlayerIds} is the loader adapter's
 * candidate-recipient list, filtered against the per-player rate limit by
 * {@link VillagerEventBus} before it ever reaches a {@link LineSink} (REACTION-REQ-008).
 *
 * <p>The two-argument constructor (VV-1's original shape) defaults both flags to {@code false}
 * and the recipient set to empty, for a caller — or a test — that does not care about silence or
 * recipients.
 */
public record VillagerReactionSignal(
        UUID villagerId,
        VillagerReactionEvent event,
        boolean villagerAsleep,
        boolean villagerBaby,
        Set<UUID> nearbyPlayerIds) {

    public VillagerReactionSignal(UUID villagerId, VillagerReactionEvent event) {
        this(villagerId, event, false, false, Set.of());
    }
}
