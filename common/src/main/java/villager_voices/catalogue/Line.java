package villager_voices.catalogue;

/**
 * One catalogue entry: the subtitle text a player sees and the id of the {@code SoundEvent} it
 * plays, e.g. {@code villager_voices:reaction.trade_completed.1}
 * (docs/spec/domains/reaction-lines.md §2, `LINES-REQ-003`). {@code text} deliberately duplicates
 * the lang file's own {@code subtitles.villager_voices.reaction.<event>.<n>} string, so a datapack
 * override can be read without cross-referencing the lang file (`REACTION-DEC-001`).
 */
public record Line(String text, String soundId) {

    public Line {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("a line's subtitle text must not be blank");
        }
        if (soundId == null || soundId.isBlank()) {
            throw new IllegalArgumentException("a line's sound id must not be blank");
        }
    }
}
