package villager_voices.catalogue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The loaded line catalogue: every reaction event's eligible {@link Line}s, keyed by event id
 * string (e.g. {@code "trade_completed"}, docs/spec/domains/reaction.md §3's own event names)
 * rather than any loader-specific event enum. VV-2 defines a {@code ReactionEvent} enum and a
 * {@code LineCatalogue} interface for its selection logic; that type is not on this branch, so this
 * class deliberately keys by the event's plain id string instead — the merge maps a
 * {@code ReactionEvent} constant to this key (its name, lower-cased, matching reaction.md §3)
 * rather than this class importing VV-2's type.
 */
public final class Catalogue {

    private final Map<String, List<Line>> linesByEvent;

    private Catalogue(Map<String, List<Line>> linesByEvent) {
        this.linesByEvent = linesByEvent;
    }

    /** The eligible lines for {@code eventId}, or an empty list if no file has been loaded for it. */
    public List<Line> linesFor(String eventId) {
        return linesByEvent.getOrDefault(eventId, List.of());
    }

    /** Every event id with at least one loaded line. */
    public Set<String> events() {
        return linesByEvent.keySet();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Accumulates one {@link Catalogue} from a sequence of loaded files, one per event. Loading a
     * file for an event id already loaded replaces its lines entirely — the file-level
     * datapack-override semantics of {@code REACTION-REQ-011}: the later {@link #load} call for
     * that event id wins outright, and nothing from the earlier file survives (this is not a merge
     * of the two files' lines). Calling this in datapack-priority order, lowest first, and loading a
     * later-priority pack's file last, reproduces vanilla's own per-id override behaviour without
     * this class needing to know anything about packs itself.
     */
    public static final class Builder {

        private final Map<String, List<Line>> linesByEvent = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Parses and stores {@code json} as {@code eventId}'s catalogue, replacing any prior
         * {@link #load} call for the same {@code eventId}.
         *
         * @throws CatalogueLoadException per {@link CatalogueCodec#parseEventFile}
         */
        public Builder load(String eventId, String json, Predicate<String> soundExists) {
            linesByEvent.put(eventId, CatalogueCodec.parseEventFile(eventId, json, soundExists));
            return this;
        }

        public Catalogue build() {
            return new Catalogue(Map.copyOf(linesByEvent));
        }
    }
}
