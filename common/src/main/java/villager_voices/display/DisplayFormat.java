package villager_voices.display;

/**
 * The action-bar text format ({@code DISPLAY-REQ-004}, docs/spec/domains/display.md §3 "Display
 * format"): {@code "<profession>: <line>"}, falling back to {@code "Villager: <line>"} for a
 * villager with no profession. Pure string formatting only, on already-resolved text — a
 * {@link DisplayLine}'s {@code speakerLabel} is the caller-supplied value passed as
 * {@code speakerLabel} here.
 */
public final class DisplayFormat {

    private static final String NO_PROFESSION_LABEL = "Villager";

    private DisplayFormat() {
    }

    /**
     * @param speakerLabel the villager's profession display text (or other speaker label), or
     *     {@code null}/blank for a villager with no profession (nitwit, unemployed)
     * @param line the line's own text
     * @return {@code "<speakerLabel>: <line>"}, or {@code "Villager: <line>"} when
     *     {@code speakerLabel} is {@code null} or blank
     */
    public static String format(String speakerLabel, String line) {
        String label = (speakerLabel == null || speakerLabel.isBlank())
            ? NO_PROFESSION_LABEL
            : speakerLabel;
        return label + ": " + line;
    }
}
