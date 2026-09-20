package villager_voices;

/**
 * A reference to one catalogue line for one event, opaque outside {@code common}: {@code id} is
 * whatever unique key the eventual {@link LineCatalogue} implementation assigns each entry (VV-3
 * proposes the sound id, e.g. {@code "villager_voices:reaction.trade_completed.1"},
 * docs/spec/domains/reaction-lines.md §2) and is used only for equality, so {@link LineSelector}
 * can exclude the line that played last for a villager/event pair
 * (docs/spec/domains/reaction.md REACTION-REQ-005). Carries {@code event} alongside so a
 * {@link LineSink} does not need the event passed to it separately.
 */
public record LineRef(VillagerReactionEvent event, String id) {
}
