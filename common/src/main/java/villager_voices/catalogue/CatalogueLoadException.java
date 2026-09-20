package villager_voices.catalogue;

/**
 * Thrown when a catalogue file fails to load: malformed JSON, a shape that doesn't match §2, a
 * sound id that doesn't follow {@code villager_voices:reaction.<event>.<n>}, or a sound id naming a
 * {@code SoundEvent} that isn't registered. The message always names the offending file and, where
 * relevant, the offending id — a rejection is reported, never silently dropped or allowed to crash
 * the load with an unrelated exception (docs/spec/domains/reaction.md `REACTION-REQ-012`,
 * `REACTION-FAIL-003`).
 */
public final class CatalogueLoadException extends RuntimeException {

    public CatalogueLoadException(String message) {
        super(message);
    }
}
