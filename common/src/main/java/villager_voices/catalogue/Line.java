package villager_voices.catalogue;

/**
 * One catalogue entry: the subtitle text a player sees, the id of the {@code SoundEvent} it plays,
 * e.g. {@code villager_voices:reaction.trade_completed.1}, an optional vanilla villager
 * {@code grunt} sound event id played first (docs/spec/domains/reaction-lines.md §2,
 * {@code LINES-REQ-003}; docs/spec/domains/audio.md {@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}),
 * and an optional {@code spoken} override (VV-11 round six, {@code AUDIO-DEC-006} amendment).
 * {@code text} deliberately duplicates the lang file's own
 * {@code subtitles.villager_voices.reaction.<event>.<n>} string, so a datapack override can be read
 * without cross-referencing the lang file (`REACTION-DEC-001`).
 *
 * <p>{@code grunt}, when present, is a namespaced id (e.g. {@code minecraft:entity.villager.trade})
 * shape-validated by {@link CatalogueCodec} only -- this class itself only rejects a blank one.
 * Whether the id actually names a registered {@code SoundEvent} is the loader's own check, same as
 * for {@code soundId} ({@code fabric}'s {@code CatalogueReloadListener}), except a grunt that
 * doesn't resolve is never fatal: the line simply plays without one.
 *
 * <p>{@code spoken}, when present, is the text the voice pipeline's generator feeds the TTS engine
 * instead of {@code text} itself, for a line whose subtitle spelling is expressive rather than
 * plainly pronounceable (e.g. "Zzz." or a mid-word interruption) -- `tools/voices/render.py`'s
 * `derive_input_text` is the only reader; a line without one falls back to a general normaliser,
 * not {@code text} verbatim unmodified (`AUDIO-DEC-004`'s "the subtitle itself" now means "the
 * subtitle, normalised for speech" rather than a literal byte-for-byte match). No loader consumes
 * this field -- it exists only for the build-time generator to read from the same catalogue file the
 * game itself loads, never touching gameplay code (`AUDIO-REQ-005`).
 */
public record Line(String text, String soundId, String grunt, String spoken) {

    public Line {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("a line's subtitle text must not be blank");
        }
        if (soundId == null || soundId.isBlank()) {
            throw new IllegalArgumentException("a line's sound id must not be blank");
        }
        if (grunt != null && grunt.isBlank()) {
            throw new IllegalArgumentException("a line's grunt, if present, must not be blank");
        }
        if (spoken != null && spoken.isBlank()) {
            throw new IllegalArgumentException("a line's spoken override, if present, must not be blank");
        }
    }

    /** No grunt, no spoken override (VV-3's original shape, before VV-18/VV-11 round six added them). */
    public Line(String text, String soundId) {
        this(text, soundId, null, null);
    }

    /** No spoken override (VV-18's shape, before VV-11 round six added the field). */
    public Line(String text, String soundId, String grunt) {
        this(text, soundId, grunt, null);
    }
}
