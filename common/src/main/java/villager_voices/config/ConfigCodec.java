package villager_voices.config;

import villager_voices.catalogue.MiniJson;

import java.util.Map;

/**
 * Parses and serialises {@code config/villager_voices.json} (docs/spec/contracts/data-contract.md
 * {@code DATA-REQ-002}) on top of {@link MiniJson}, the same hand-written reader
 * {@code villager_voices.catalogue} already carries -- a JSON5/TOML library was considered and
 * skipped for the same reason VV-3 skipped one for the catalogue files: nothing was already on the
 * classpath, and this shape is small and fixed (heimathafen's own standing preference for fewer
 * third parties).
 *
 * <p>{@link #parse} never throws: malformed JSON degrades to {@link Config#defaults()} wholesale,
 * and a single missing or wrong-typed field degrades to just that field's own default, per
 * {@code DATA-REQ-004} -- {@link Config}'s own compact constructor is the second line of defence,
 * clamping any in-range-type-but-out-of-range value (e.g. a negative cooldown) the same way.
 */
public final class ConfigCodec {

    private ConfigCodec() {
    }

    /**
     * @param json the config file's raw text
     * @return the parsed {@link Config}, or {@link Config#defaults()} if {@code json} is not a
     *     valid JSON object at all; any individual field that is missing or the wrong JSON type
     *     falls back to its own shipped default rather than failing the whole parse
     */
    public static Config parse(String json) {
        Map<String, Object> root = asObject(tryParse(json));
        Map<String, Object> cooldowns = objectField(root, "cooldowns");
        Map<String, Object> display = objectField(root, "display");
        Map<String, Object> categories = objectField(root, "categories");

        return new Config(
                stringField(root, "schemaVersion", Config.SCHEMA_VERSION),
                longField(cooldowns, "perVillagerPerEvent", Config.DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS),
                longField(cooldowns, "perVillagerGlobal", Config.DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS),
                longField(cooldowns, "serverRatePerPlayer", Config.DEFAULT_SERVER_RATE_PER_PLAYER_TICKS),
                boolField(display, "actionBar", Config.DEFAULT_DISPLAY_ACTION_BAR),
                boolField(display, "subtitleHint", Config.DEFAULT_DISPLAY_SUBTITLE_HINT),
                longField(display, "queueMinHoldTicks", Config.DEFAULT_DISPLAY_QUEUE_MIN_HOLD_TICKS),
                doubleField(display, "masterVolume", Config.DEFAULT_DISPLAY_MASTER_VOLUME),
                doubleField(display, "hearingRangeBlocks", Config.DEFAULT_HEARING_RANGE_BLOCKS),
                new Config.CategoryMutes(
                        mutedField(categories, "trade"),
                        mutedField(categories, "combat"),
                        mutedField(categories, "social"),
                        mutedField(categories, "raid")));
    }

    /** The canonical on-disk shape: nested {@code cooldowns}/{@code display}/{@code categories} objects. */
    public static String serialize(Config config) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"schemaVersion\": ").append(jsonString(config.schemaVersion())).append(",\n");
        out.append("  \"cooldowns\": {\n");
        out.append("    \"perVillagerPerEvent\": ").append(config.perVillagerPerEventCooldownTicks()).append(",\n");
        out.append("    \"perVillagerGlobal\": ").append(config.perVillagerGlobalCooldownTicks()).append(",\n");
        out.append("    \"serverRatePerPlayer\": ").append(config.serverRatePerPlayerTicks()).append("\n");
        out.append("  },\n");
        out.append("  \"display\": {\n");
        out.append("    \"actionBar\": ").append(config.displayActionBar()).append(",\n");
        out.append("    \"subtitleHint\": ").append(config.displaySubtitleHint()).append(",\n");
        out.append("    \"queueMinHoldTicks\": ").append(config.displayQueueMinHoldTicks()).append(",\n");
        out.append("    \"masterVolume\": ").append(config.displayMasterVolume()).append(",\n");
        out.append("    \"hearingRangeBlocks\": ").append(config.hearingRangeBlocks()).append("\n");
        out.append("  },\n");
        out.append("  \"categories\": {\n");
        out.append("    \"trade\": { \"muted\": ").append(config.categoryMutes().trade()).append(" },\n");
        out.append("    \"combat\": { \"muted\": ").append(config.categoryMutes().combat()).append(" },\n");
        out.append("    \"social\": { \"muted\": ").append(config.categoryMutes().social()).append(" },\n");
        out.append("    \"raid\": { \"muted\": ").append(config.categoryMutes().raid()).append(" }\n");
        out.append("  }\n");
        out.append("}\n");
        return out.toString();
    }

    private static Object tryParse(String json) {
        try {
            return MiniJson.parse(json);
        } catch (RuntimeException e) {
            return null; // MiniJson.JsonSyntaxException, or json itself was null: whole file is unusable.
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static Map<String, Object> objectField(Map<String, Object> obj, String key) {
        return asObject(obj.get(key));
    }

    private static boolean mutedField(Map<String, Object> categories, String category) {
        return boolField(objectField(categories, category), "muted", false);
    }

    private static String stringField(Map<String, Object> obj, String key, String fallback) {
        Object value = obj.get(key);
        return value instanceof String s ? s : fallback;
    }

    private static long longField(Map<String, Object> obj, String key, long fallback) {
        Object value = obj.get(key);
        return value instanceof Double d ? d.longValue() : fallback;
    }

    private static double doubleField(Map<String, Object> obj, String key, double fallback) {
        Object value = obj.get(key);
        return value instanceof Double d ? d : fallback;
    }

    private static boolean boolField(Map<String, Object> obj, String key, boolean fallback) {
        Object value = obj.get(key);
        return value instanceof Boolean b ? b : fallback;
    }

    private static String jsonString(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
