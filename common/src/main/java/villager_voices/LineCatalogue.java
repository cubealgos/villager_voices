package villager_voices;

import java.util.List;

/**
 * Supplies the eligible lines for one event. <b>Not implemented in this ticket (VV-2)</b>: this
 * interface exists so {@link VillagerEventBus} and {@link LineSelector} have something to compile
 * and unit-test against; VV-3 (the line catalogue codec, {@code villager_voices.catalogue} or
 * wherever that ticket's spec-reading lands it) implements it after this ticket merges, decoding
 * {@code data/villager_voices/reaction/<event>.json} per docs/spec/domains/reaction-lines.md §2
 * and applying datapack overrides per REACTION-REQ-011.
 */
public interface LineCatalogue {

    /**
     * Every eligible line for {@code event}, in no particular order — {@link LineSelector} does
     * the random pick. May be empty (an event with no eligible lines simply never reacts), never
     * {@code null}.
     */
    List<LineRef> linesFor(VillagerReactionEvent event);
}
