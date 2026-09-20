package villager_voices.display;

/**
 * One line selected for display (docs/spec/domains/display.md §3 "Display format",
 * {@code DISPLAY-REQ-004}).
 *
 * <p>This is VV-7's own input shape for the display channel, not the reaction system's selected-
 * line type. Once VV-2's {@code reaction} package merges its {@code LineSink} interface, the
 * reaction system is expected to adapt a selected line into a {@code DisplayLine} before calling
 * {@link DisplayQueue#enqueue}; no {@code reaction}-package type is imported here, or ever, by this
 * loader-free module (ARCH-DEC-001).
 *
 * @param speakerLabel the label {@link DisplayFormat} prefixes the line with — the villager's
 *     profession display text, or its own name as a fallback label for a villager with no
 *     profession-appropriate name; already resolved to display-ready text by whichever caller
 *     builds this record (that resolution needs a Minecraft registry/translation lookup this
 *     module cannot make, ARCH-DEC-001). {@code null} or blank falls back to
 *     {@code "Villager: <line>"} ({@code DISPLAY-REQ-004}).
 * @param text the line's own text as shown on the action bar, or a subtitle-key reference once
 *     the catalogue (VV-3) supplies one instead of literal text
 * @param soundId the id of the {@code SoundEvent} the line plays, once registered (VV-8) — carried
 *     here so a single {@code DisplayLine} can drive both the action-bar push and, later, the
 *     sound broadcast whose effective radius determines hearing range ({@code DISPLAY-REQ-002}).
 *     Not read by anything in this ticket.
 */
public record DisplayLine(String speakerLabel, String text, String soundId) {
}
