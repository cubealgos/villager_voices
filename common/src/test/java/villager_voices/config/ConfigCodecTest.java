package villager_voices.config;

import org.junit.jupiter.api.Test;
import villager_voices.ReactionRules;
import villager_voices.display.DisplayQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docs/spec/operations/testing.md: "the config-file default/clamp behaviour" -- {@link
 * ConfigCodec}'s parse/serialise round trip, its per-field degrade-to-default behaviour on
 * malformed input ({@code DATA-REQ-004}), and {@link Config#defaults()}'s equality with VV-2's and
 * VV-7's own hardcoded constants (this ticket's own requirement, VV-8's Approach section).
 */
final class ConfigCodecTest {

    @Test
    void defaultsMatchVv2AndVv7sOwnHardcodedConstants() {
        Config defaults = Config.defaults();
        assertEquals(ReactionRules.DEFAULT_PER_EVENT_COOLDOWN_TICKS, defaults.perVillagerPerEventCooldownTicks());
        assertEquals(ReactionRules.DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS, defaults.perVillagerGlobalCooldownTicks());
        assertEquals(ReactionRules.DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS, defaults.serverRatePerPlayerTicks());
        assertEquals(DisplayQueue.DEFAULT_MIN_HOLD_TICKS, defaults.displayQueueMinHoldTicks());
    }

    @Test
    void serializeThenParseRoundTripsExactly() {
        Config original = new Config("1.0", 111, 22, 3, false, true, 44, 0.75, 12.5,
                new Config.CategoryMutes(true, false, true, false), 55);

        Config roundTripped = ConfigCodec.parse(ConfigCodec.serialize(original));

        assertEquals(original, roundTripped);
    }

    @Test
    void defaultConfigRoundTrips() {
        assertEquals(Config.defaults(), ConfigCodec.parse(ConfigCodec.serialize(Config.defaults())));
    }

    @Test
    void malformedJsonDegradesWhollyToDefaults() {
        assertEquals(Config.defaults(), ConfigCodec.parse("{ not valid json"));
        assertEquals(Config.defaults(), ConfigCodec.parse(""));
    }

    @Test
    void aJsonValueThatIsNotAnObjectDegradesToDefaults() {
        assertEquals(Config.defaults(), ConfigCodec.parse("[1, 2, 3]"));
        assertEquals(Config.defaults(), ConfigCodec.parse("\"just a string\""));
    }

    @Test
    void oneMissingFieldFallsBackAloneWithoutAffectingItsSiblings() {
        String json = """
                {
                  "cooldowns": { "perVillagerPerEvent": 500 },
                  "display": { "actionBar": false }
                }
                """;

        Config parsed = ConfigCodec.parse(json);

        assertEquals(500L, parsed.perVillagerPerEventCooldownTicks());
        assertFalse(parsed.displayActionBar());
        // Every field this JSON didn't mention keeps its own shipped default.
        assertEquals(Config.DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS, parsed.perVillagerGlobalCooldownTicks());
        assertEquals(Config.DEFAULT_DISPLAY_SUBTITLE_HINT, parsed.displaySubtitleHint());
        assertEquals(Config.DEFAULT_HEARING_RANGE_BLOCKS, parsed.hearingRangeBlocks());
    }

    @Test
    void aWrongTypedFieldFallsBackToItsDefaultInsteadOfThrowing() {
        String json = """
                { "cooldowns": { "perVillagerPerEvent": "not a number" } }
                """;

        Config parsed = ConfigCodec.parse(json);

        assertEquals(Config.DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS, parsed.perVillagerPerEventCooldownTicks());
    }

    @Test
    void aNegativeCooldownInTheFileIsClampedToItsDefaultByConfigItself() {
        String json = """
                { "cooldowns": { "perVillagerPerEvent": -5 } }
                """;

        Config parsed = ConfigCodec.parse(json);

        assertEquals(Config.DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS, parsed.perVillagerPerEventCooldownTicks());
    }

    @Test
    void categoryMutesRoundTripPerCategory() {
        String json = """
                {
                  "categories": {
                    "trade": { "muted": true },
                    "raid": { "muted": true }
                  }
                }
                """;

        Config.CategoryMutes mutes = ConfigCodec.parse(json).categoryMutes();

        assertTrue(mutes.trade());
        assertFalse(mutes.combat());
        assertFalse(mutes.social());
        assertTrue(mutes.raid());
    }

    @Test
    void blankSchemaVersionResetsToTheCurrentOne() {
        Config config = new Config("", 0, 0, 0, true, true, 0, 1, 1, Config.CategoryMutes.NONE_MUTED, 0);
        assertEquals(Config.SCHEMA_VERSION, config.schemaVersion());
    }

    @Test
    void talkingDurationTicksRoundTripsAndClampsLikeEveryOtherTickField() {
        String json = """
                { "compat": { "talkingDurationTicks": 77 } }
                """;

        assertEquals(77L, ConfigCodec.parse(json).talkingDurationTicks());
        assertEquals(Config.DEFAULT_TALKING_DURATION_TICKS,
                ConfigCodec.parse("{}").talkingDurationTicks());
    }

    @Test
    void aNegativeTalkingDurationInTheFileIsClampedToItsDefaultByConfigItself() {
        String json = """
                { "compat": { "talkingDurationTicks": -1 } }
                """;

        assertEquals(Config.DEFAULT_TALKING_DURATION_TICKS, ConfigCodec.parse(json).talkingDurationTicks());
    }
}
