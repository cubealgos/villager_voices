package villager_voices;

import java.util.UUID;

/**
 * One detected event, as reported by a loader adapter. {@code villagerId} identifies the
 * triggering villager for the cooldown/silence rules in docs/spec/domains/reaction.md §3; those
 * rules are not implemented in this bootstrap ticket (VV-1) and land with the event wiring itself.
 */
public record VillagerReactionSignal(UUID villagerId, VillagerReactionEvent event) {
}
