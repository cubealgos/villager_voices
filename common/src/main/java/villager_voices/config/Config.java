package villager_voices.config;

import villager_voices.ReactionRules;
import villager_voices.display.DisplayQueue;

/**
 * The one server-operator-facing config file this mod writes
 * (docs/spec/contracts/data-contract.md {@code DATA-REQ-002}): the three cooldown/rate-limit
 * windows VV-2's {@link ReactionRules} otherwise hardcodes, VV-7's {@link DisplayQueue} minimum
 * hold time, the alpha's hearing-range override, the per-category mute toggles, and the two
 * display on/off toggles named in docs/spec/domains/display.md §3's "Config surface" table.
 *
 * <p>Correct by construction: the compact constructor clamps every out-of-range value to its own
 * shipped default rather than throwing, so a {@code Config} built from a malformed file can never
 * itself be invalid ({@code DATA-REQ-004}) — {@link ConfigCodec} only needs to get each field's
 * <em>type</em> right; this constructor is the one place range validity is enforced, for both the
 * parsed-from-disk path and any other caller (a test, a future in-game command) that builds one
 * directly.
 *
 * @param schemaVersion the config file's own schema version, {@value #SCHEMA_VERSION} at 1.0
 *     (docs/spec/contracts/data-contract.md "Versioning"); blank or {@code null} resets to
 *     {@value #SCHEMA_VERSION}
 * @param perVillagerPerEventCooldownTicks {@code cooldowns.perVillagerPerEvent}
 *     (docs/spec/domains/reaction.md §3); feeds {@link ReactionRules}'s own constructor unchanged
 * @param perVillagerGlobalCooldownTicks {@code cooldowns.perVillagerGlobal}
 * @param serverRatePerPlayerTicks {@code cooldowns.serverRatePerPlayer}
 * @param displayActionBar {@code display.actionBar} — the action-bar channel on/off
 *     ({@code DISPLAY-REQ-005}), independent of {@code displaySubtitleHint}
 * @param displaySubtitleHint {@code display.subtitleHint} — this mod's own subtitle-hint toggle;
 *     carried and round-tripped by this ticket (VV-8) but not yet wired into any behaviour, since
 *     nothing downstream reads it yet (recorded in this ticket's Findings)
 * @param displayQueueMinHoldTicks {@code display.queueMinHoldTicks}; feeds
 *     {@link DisplayQueue}'s own constructor unchanged
 * @param displayMasterVolume {@code display.masterVolume} — a multiplier on every line's played
 *     volume, independent of the player's own sound sliders
 * @param hearingRangeBlocks {@code display.hearingRangeBlocks} — an alpha-only override standing
 *     in for {@code DISPLAY-REQ-002}'s eventual "the line's own sound event's effective broadcast
 *     radius" (not yet wired, since no per-sound radius is read anywhere yet; recorded in this
 *     ticket's Findings), defaulting to the same 16 blocks
 *     {@code ActionBarDisplay.PLACEHOLDER_HEARING_RADIUS_BLOCKS} already used
 * @param categoryMutes {@code categories.<trade|combat|social|raid>.muted}; carried and
 *     round-tripped by this ticket but not yet wired into {@link villager_voices.VillagerEventBus}
 *     filtering (recorded in this ticket's Findings — no constructor seam exists for it yet
 *     without changing {@link ReactionRules}' or the event bus's own selection logic)
 */
public record Config(
        String schemaVersion,
        long perVillagerPerEventCooldownTicks,
        long perVillagerGlobalCooldownTicks,
        long serverRatePerPlayerTicks,
        boolean displayActionBar,
        boolean displaySubtitleHint,
        long displayQueueMinHoldTicks,
        double displayMasterVolume,
        double hearingRangeBlocks,
        CategoryMutes categoryMutes) {

    /** The config file's own schema version at 1.0 (docs/spec/contracts/data-contract.md). */
    public static final String SCHEMA_VERSION = "1.0";

    /** Mirrors {@link ReactionRules#DEFAULT_PER_EVENT_COOLDOWN_TICKS} -- one source of truth. */
    public static final long DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS = ReactionRules.DEFAULT_PER_EVENT_COOLDOWN_TICKS;
    /** Mirrors {@link ReactionRules#DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS}. */
    public static final long DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS = ReactionRules.DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS;
    /** Mirrors {@link ReactionRules#DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS}. */
    public static final long DEFAULT_SERVER_RATE_PER_PLAYER_TICKS = ReactionRules.DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS;
    /** Mirrors {@link DisplayQueue#DEFAULT_MIN_HOLD_TICKS}. */
    public static final long DEFAULT_DISPLAY_QUEUE_MIN_HOLD_TICKS = DisplayQueue.DEFAULT_MIN_HOLD_TICKS;
    public static final boolean DEFAULT_DISPLAY_ACTION_BAR = true;
    public static final boolean DEFAULT_DISPLAY_SUBTITLE_HINT = true;
    public static final double DEFAULT_DISPLAY_MASTER_VOLUME = 1.0;
    /** Matches {@code ActionBarDisplay.PLACEHOLDER_HEARING_RADIUS_BLOCKS} (VV-7). */
    public static final double DEFAULT_HEARING_RANGE_BLOCKS = 16.0;

    public Config {
        if (schemaVersion == null || schemaVersion.isBlank()) {
            schemaVersion = SCHEMA_VERSION;
        }
        if (perVillagerPerEventCooldownTicks < 0) {
            perVillagerPerEventCooldownTicks = DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS;
        }
        if (perVillagerGlobalCooldownTicks < 0) {
            perVillagerGlobalCooldownTicks = DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS;
        }
        if (serverRatePerPlayerTicks < 0) {
            serverRatePerPlayerTicks = DEFAULT_SERVER_RATE_PER_PLAYER_TICKS;
        }
        if (displayQueueMinHoldTicks < 0) {
            displayQueueMinHoldTicks = DEFAULT_DISPLAY_QUEUE_MIN_HOLD_TICKS;
        }
        if (!(displayMasterVolume >= 0)) { // catches NaN too
            displayMasterVolume = DEFAULT_DISPLAY_MASTER_VOLUME;
        }
        if (!(hearingRangeBlocks >= 0)) {
            hearingRangeBlocks = DEFAULT_HEARING_RANGE_BLOCKS;
        }
        if (categoryMutes == null) {
            categoryMutes = CategoryMutes.NONE_MUTED;
        }
    }

    /** The shipped defaults -- VV-2's and VV-7's current hardcoded constants, unchanged. */
    public static Config defaults() {
        return new Config(
                SCHEMA_VERSION,
                DEFAULT_PER_VILLAGER_PER_EVENT_COOLDOWN_TICKS,
                DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS,
                DEFAULT_SERVER_RATE_PER_PLAYER_TICKS,
                DEFAULT_DISPLAY_ACTION_BAR,
                DEFAULT_DISPLAY_SUBTITLE_HINT,
                DEFAULT_DISPLAY_QUEUE_MIN_HOLD_TICKS,
                DEFAULT_DISPLAY_MASTER_VOLUME,
                DEFAULT_HEARING_RANGE_BLOCKS,
                CategoryMutes.NONE_MUTED);
    }

    /**
     * {@code categories.<trade|combat|social|raid>.muted} (docs/spec/domains/display.md §3), the
     * four independent per-category mute toggles docs/spec/domains/reaction.md §3 names.
     */
    public record CategoryMutes(boolean trade, boolean combat, boolean social, boolean raid) {
        public static final CategoryMutes NONE_MUTED = new CategoryMutes(false, false, false, false);
    }
}
