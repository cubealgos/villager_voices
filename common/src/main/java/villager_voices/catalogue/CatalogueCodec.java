package villager_voices.catalogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes one {@code data/villager_voices/reaction/<event>.json} file's text into its {@link Line}s
 * (docs/spec/domains/reaction-lines.md §2). Field names, confirmed by this ticket (VV-3) exactly as
 * §2 already showed: a top-level JSON object with a {@code "lines"} array, each entry an object
 * with a {@code "subtitle"} string, a {@code "sound"} string, and (VV-18) an optional {@code
 * "grunt"} string -- a namespaced vanilla villager {@code SoundEvent} id, shape-validated here only;
 * whether it actually names a registered event is the loader's own check, same as for {@code sound}
 * (docs/spec/domains/audio.md {@code AUDIO-REQ-007}).
 */
public final class CatalogueCodec {

    /** {@code villager_voices:reaction.<event>.<n>} (`LINES-REQ-003`), {@code n} a positive integer. */
    private static final Pattern SOUND_ID = Pattern.compile("^villager_voices:reaction\\.([a-z_]+)\\.([1-9][0-9]*)$");

    /**
     * A generic {@code namespace:path} id shape (Minecraft's own {@code Identifier} character
     * classes), used only for {@code grunt} -- unlike {@code sound}, a grunt is never required to
     * name this mod's own namespace or follow the {@code reaction.<event>.<n>} convention, since it
     * names a vanilla event instead (e.g. {@code minecraft:entity.villager.trade}).
     */
    private static final Pattern NAMESPACED_ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    private CatalogueCodec() {
    }

    /**
     * Parses {@code eventId}'s file text into its lines, in file order.
     *
     * @param eventId the event this file is for, e.g. {@code "trade_completed"} — every line's
     *     sound id must name this same event (LINES-REQ-003)
     * @param json the file's raw text
     * @param soundExists tests whether a sound id names an already-registered {@code SoundEvent};
     *     an entry naming an id this rejects is rejected with a {@link CatalogueLoadException}
     *     naming that id, never silently dropped (`REACTION-REQ-012`, `REACTION-FAIL-003`)
     * @return an immutable, non-empty list of lines
     * @throws CatalogueLoadException if the JSON is malformed, the shape doesn't match §2, the file
     *     names no lines at all, a sound id doesn't follow {@code villager_voices:reaction.<event>.<n>}
     *     or names a different event than {@code eventId}, {@code soundExists} rejects an id, or a
     *     present {@code grunt} is blank or not a valid namespaced id (VV-18)
     */
    public static List<Line> parseEventFile(String eventId, String json, Predicate<String> soundExists) {
        String fileLabel = "data/villager_voices/reaction/" + eventId + ".json";
        Object root;
        try {
            root = MiniJson.parse(json);
        } catch (MiniJson.JsonSyntaxException e) {
            throw new CatalogueLoadException(fileLabel + " is not valid JSON: " + e.getMessage());
        }
        if (!(root instanceof Map<?, ?> obj)) {
            throw new CatalogueLoadException(fileLabel + ": expected a JSON object at the top level");
        }
        Object linesRaw = obj.get("lines");
        if (!(linesRaw instanceof List<?> list)) {
            throw new CatalogueLoadException(fileLabel + ": expected a \"lines\" array");
        }

        List<Line> lines = new ArrayList<>();
        int index = 0;
        for (Object entry : list) {
            index++;
            if (!(entry instanceof Map<?, ?> lineObj)) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + " is not a JSON object");
            }
            Object subtitleRaw = lineObj.get("subtitle");
            Object soundRaw = lineObj.get("sound");
            if (!(subtitleRaw instanceof String subtitle) || subtitle.isBlank()) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + " is missing a non-blank \"subtitle\"");
            }
            if (!(soundRaw instanceof String soundId) || soundId.isBlank()) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + " is missing a non-blank \"sound\"");
            }

            Matcher matcher = SOUND_ID.matcher(soundId);
            if (!matcher.matches()) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + "'s sound id \"" + soundId
                    + "\" does not follow villager_voices:reaction.<event>.<n>");
            }
            String soundEvent = matcher.group(1);
            if (!soundEvent.equals(eventId)) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + "'s sound id \"" + soundId
                    + "\" names event \"" + soundEvent + "\", not \"" + eventId + "\"");
            }
            if (!soundExists.test(soundId)) {
                throw new CatalogueLoadException(fileLabel + ": entry " + index + " names sound id \"" + soundId
                    + "\", which is not a registered SoundEvent");
            }

            Object gruntRaw = lineObj.get("grunt");
            String grunt = null;
            if (gruntRaw != null) {
                if (!(gruntRaw instanceof String g) || g.isBlank()) {
                    throw new CatalogueLoadException(fileLabel + ": entry " + index
                        + "'s \"grunt\" must be a non-blank string if present");
                }
                if (!NAMESPACED_ID.matcher(g).matches()) {
                    throw new CatalogueLoadException(fileLabel + ": entry " + index + "'s grunt id \"" + g
                        + "\" is not a valid namespaced id");
                }
                grunt = g;
            }

            lines.add(new Line(subtitle, soundId, grunt));
        }

        if (lines.isEmpty()) {
            throw new CatalogueLoadException(fileLabel + ": an event's catalogue must have at least one line");
        }
        return List.copyOf(lines);
    }
}
