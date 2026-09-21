package villager_voices.catalogue;

/**
 * One catalogue entry: the subtitle text a player sees, the id of the {@code SoundEvent} it plays,
 * e.g. {@code villager_voices:reaction.trade_completed.1}, an optional vanilla villager
 * {@code grunt} sound event id played first (docs/spec/domains/reaction-lines.md §2,
 * {@code LINES-REQ-003}; docs/spec/domains/audio.md {@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}),
 * an optional {@code spoken} override (VV-11 round six, {@code AUDIO-DEC-006} amendment), and an
 * optional {@code mood} (VV-11 round ten, {@code LINES-DEC-002}). {@code text} deliberately
 * duplicates the lang file's own {@code subtitles.villager_voices.reaction.<event>.<n>} string, so
 * a datapack override can be read without cross-referencing the lang file (`REACTION-DEC-001`).
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
 *
 * <p>{@code mood}, when present, names one of the six emotion classes {@code docs/spec/domains/
 * reaction-lines.md}'s {@code LINES-DEC-002} defines over the 16 events -- {@code calm},
 * {@code pleased}, {@code annoyed}, {@code hurt}, {@code alarmed}, {@code gentle} (short for
 * "gentle/pleading") -- one per event, so every line in the same event's catalogue file carries the
 * same value ({@link CatalogueCodec} rejects two lines in one file naming different moods). Like
 * {@code spoken}, no loader ever reads this field; it exists so {@code tools/voices/render.py}'s
 * {@code --batch} mode can pick each line's post-processing reference/settings automatically instead
 * of the whole 64-line batch sharing one mood-blind timbre (VV-11 round ten, Kevin: "they always
 * sound surprised; it is not conveying the correct emotions for everything yet").
 */
public record Line(String text, String soundId, String grunt, String spoken, String mood) {

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
        if (mood != null && mood.isBlank()) {
            throw new IllegalArgumentException("a line's mood, if present, must not be blank");
        }
    }

    /** No grunt, no spoken override, no mood (VV-3's original shape, before VV-18/VV-11 added them). */
    public Line(String text, String soundId) {
        this(text, soundId, null, null, null);
    }

    /** No spoken override, no mood (VV-18's shape, before VV-11 round six/ten added them). */
    public Line(String text, String soundId, String grunt) {
        this(text, soundId, grunt, null, null);
    }

    /** No mood (VV-11 round six's shape, before round ten added it). */
    public Line(String text, String soundId, String grunt, String spoken) {
        this(text, soundId, grunt, spoken, null);
    }
}
