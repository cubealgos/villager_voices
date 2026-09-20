package villager_voices.catalogue;

/**
 * One catalogue entry: the subtitle text a player sees, the id of the {@code SoundEvent} it plays,
 * e.g. {@code villager_voices:reaction.trade_completed.1}, and an optional vanilla villager
 * {@code grunt} sound event id played first (docs/spec/domains/reaction-lines.md §2,
 * {@code LINES-REQ-003}; docs/spec/domains/audio.md {@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}).
 * {@code text} deliberately duplicates the lang file's own
 * {@code subtitles.villager_voices.reaction.<event>.<n>} string, so a datapack override can be read
 * without cross-referencing the lang file (`REACTION-DEC-001`).
 *
 * <p>{@code grunt}, when present, is a namespaced id (e.g. {@code minecraft:entity.villager.trade})
 * shape-validated by {@link CatalogueCodec} only -- this class itself only rejects a blank one.
 * Whether the id actually names a registered {@code SoundEvent} is the loader's own check, same as
 * for {@code soundId} ({@code fabric}'s {@code CatalogueReloadListener}), except a grunt that
 * doesn't resolve is never fatal: the line simply plays without one.
 */
public record Line(String text, String soundId, String grunt) {

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
    }

    /** No grunt (VV-3's original shape, before VV-18 added the optional field). */
    public Line(String text, String soundId) {
        this(text, soundId, null);
    }
}
