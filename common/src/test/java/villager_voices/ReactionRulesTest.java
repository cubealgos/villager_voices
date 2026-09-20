package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionRulesTest {

    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID OTHER_VILLAGER = UUID.randomUUID();
    private static final UUID PLAYER = UUID.randomUUID();
    private static final LineRef LINE = new LineRef(VillagerReactionEvent.TRADE_COMPLETED, "villager_voices:reaction.trade_completed.1");

    // --- REACTION-REQ-006: per-villager-per-event cooldown ---

    @Test
    void perEventCooldownSuppressesWithinWindow() {
        ReactionRules rules = new ReactionRules(100, 0, 0);
        assertTrue(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, 0));
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        assertFalse(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, 99));
    }

    @Test
    void perEventCooldownAllowsExactlyAtWindowBoundary() {
        ReactionRules rules = new ReactionRules(100, 0, 0);
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        assertTrue(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, 100));
    }

    @Test
    void perEventCooldownIsIndependentPerEvent() {
        ReactionRules rules = new ReactionRules(100, 0, 0);
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        // A different event on the same villager is not suppressed by trade_completed's own cooldown.
        assertTrue(rules.permits(VILLAGER, VillagerReactionEvent.HURT, false, false, 1));
    }

    // --- REACTION-REQ-007: per-villager-global cooldown ---

    @Test
    void globalCooldownSuppressesAnyEventWithinWindow() {
        ReactionRules rules = new ReactionRules(0, 50, 0);
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        // A different event, same villager: still suppressed by the global cooldown.
        assertFalse(rules.permits(VILLAGER, VillagerReactionEvent.HURT, false, false, 49));
    }

    @Test
    void globalCooldownAllowsExactlyAtWindowBoundary() {
        ReactionRules rules = new ReactionRules(0, 50, 0);
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        assertTrue(rules.permits(VILLAGER, VillagerReactionEvent.HURT, false, false, 50));
    }

    @Test
    void globalCooldownIsIndependentPerVillager() {
        ReactionRules rules = new ReactionRules(0, 50, 0);
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        assertTrue(rules.permits(OTHER_VILLAGER, VillagerReactionEvent.HURT, false, false, 1));
    }

    // --- REACTION-REQ-008: per-player server-wide rate limit ---

    @Test
    void playerRateLimitSuppressesSecondLineWithinWindow() {
        ReactionRules rules = new ReactionRules(0, 0, 40);
        assertTrue(rules.tryConsumePlayerRate(PLAYER, 0));

        assertFalse(rules.tryConsumePlayerRate(PLAYER, 39));
    }

    @Test
    void playerRateLimitAllowsExactlyAtWindowBoundary() {
        ReactionRules rules = new ReactionRules(0, 0, 40);
        rules.tryConsumePlayerRate(PLAYER, 0);

        assertTrue(rules.tryConsumePlayerRate(PLAYER, 40));
    }

    @Test
    void playerRateLimitIsIndependentOfVillagerCount() {
        // REACTION-REQ-008: suppressed independent of how many villagers are nearby — modeled here
        // as two separate reaction events for the same player consuming the same rate-limit slot.
        ReactionRules rules = new ReactionRules(0, 0, 40);
        assertTrue(rules.tryConsumePlayerRate(PLAYER, 0));
        assertFalse(rules.tryConsumePlayerRate(PLAYER, 1));
    }

    @Test
    void playerRateLimitIsIndependentPerPlayer() {
        ReactionRules rules = new ReactionRules(0, 0, 40);
        rules.tryConsumePlayerRate(PLAYER, 0);

        UUID otherPlayer = UUID.randomUUID();
        assertTrue(rules.tryConsumePlayerRate(otherPlayer, 1));
    }

    // --- REACTION-REQ-009 / REACTION-REQ-010: silence rules ---

    @Test
    void asleepSuppressesEveryEventButSleep() {
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.TRADE_COMPLETED, true, false));
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.HURT, true, false));
        assertFalse(ReactionRules.isSilenced(VillagerReactionEvent.SLEEP, true, false));
    }

    @Test
    void babySuppressesEveryEventButBabyGrows() {
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.TRADE_COMPLETED, false, true));
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.SLEEP, false, true));
        assertFalse(ReactionRules.isSilenced(VillagerReactionEvent.BABY_GROWS, false, true));
    }

    @Test
    void awakeAdultIsNeverSilenced() {
        for (VillagerReactionEvent event : VillagerReactionEvent.values()) {
            assertFalse(ReactionRules.isSilenced(event, false, false), event + " should not be silenced");
        }
    }

    @Test
    void sleepingBabyStillSilencesSleepEvent() {
        // The two rules combine and neither is exempt from the other: asleep's own exemption is
        // SLEEP only, so a sleeping baby's BABY_GROWS is still silenced by the asleep rule; baby's
        // own exemption is BABY_GROWS only, so a sleeping baby's SLEEP is still silenced by the
        // baby rule. A sleeping baby villager is silenced for every event, with no exemption at all.
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.SLEEP, true, true));
        assertTrue(ReactionRules.isSilenced(VillagerReactionEvent.BABY_GROWS, true, true));
    }

    @Test
    void permitsReturnsFalseWhenSilencedRegardlessOfCooldownState() {
        ReactionRules rules = new ReactionRules(0, 0, 0);
        assertFalse(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, true, false, 0));
    }

    // --- record()/lastPlayed() bookkeeping ---

    @Test
    void lastPlayedIsEmptyBeforeAnyRecord() {
        ReactionRules rules = new ReactionRules();
        assertEquals(java.util.Optional.empty(), rules.lastPlayed(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED));
    }

    @Test
    void recordUpdatesLastPlayedPerVillagerAndEvent() {
        ReactionRules rules = new ReactionRules();
        rules.record(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, LINE, 0);

        assertEquals(java.util.Optional.of(LINE), rules.lastPlayed(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED));
        assertEquals(java.util.Optional.empty(), rules.lastPlayed(VILLAGER, VillagerReactionEvent.HURT));
        assertEquals(java.util.Optional.empty(), rules.lastPlayed(OTHER_VILLAGER, VillagerReactionEvent.TRADE_COMPLETED));
    }

    @Test
    void suppressedDetectionDoesNotStartACooldownWindow() {
        ReactionRules rules = new ReactionRules(100, 0, 0);
        // Silenced, never recorded -- permits() alone never calls record().
        assertFalse(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, true, false, 0));

        assertTrue(rules.permits(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, 1));
    }

    @Test
    void defaultsMatchTheSpecsProposedTable() {
        assertEquals(1200L, ReactionRules.DEFAULT_PER_EVENT_COOLDOWN_TICKS); // 60s @ 20 ticks/s
        assertEquals(100L, ReactionRules.DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS); // 5s @ 20 ticks/s
        assertEquals(40L, ReactionRules.DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS); // 2s @ 20 ticks/s
    }
}
