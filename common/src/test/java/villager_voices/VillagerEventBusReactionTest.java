package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link VillagerEventBus}'s reaction pipeline (VV-2) end to end with a fake
 * {@link LineCatalogue}, a fake {@link LineSink}, a fake clock, and a fixed roll -- the shape
 * docs/spec/operations/testing.md calls for, without any Minecraft dependency.
 */
class VillagerEventBusReactionTest {

    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID PLAYER_A = UUID.randomUUID();
    private static final UUID PLAYER_B = UUID.randomUUID();

    private static final LineRef LINE_1 = new LineRef(VillagerReactionEvent.TRADE_COMPLETED, "villager_voices:reaction.trade_completed.1");
    private static final LineRef LINE_2 = new LineRef(VillagerReactionEvent.TRADE_COMPLETED, "villager_voices:reaction.trade_completed.2");

    private record Shown(UUID villagerId, Set<UUID> playerIds, LineRef line) {
    }

    private static final class FakeCatalogue implements LineCatalogue {
        private final Map<VillagerReactionEvent, List<LineRef>> lines;

        FakeCatalogue(Map<VillagerReactionEvent, List<LineRef>> lines) {
            this.lines = lines;
        }

        @Override
        public List<LineRef> linesFor(VillagerReactionEvent event) {
            return lines.getOrDefault(event, List.of());
        }
    }

    private static final class RecordingSink implements LineSink {
        final List<Shown> shown = new ArrayList<>();

        @Override
        public void show(UUID villagerId, Set<UUID> playerIds, LineRef line) {
            shown.add(new Shown(villagerId, Set.copyOf(playerIds), line));
        }
    }

    private static VillagerEventBus bus(LineCatalogue catalogue, LineSink sink, AtomicLong clock) {
        return new VillagerEventBus(catalogue, sink, clock::get, bound -> 0);
    }

    @Test
    void publishesSelectedLineToSink() {
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1, LINE_2)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        VillagerEventBus bus = bus(catalogue, sink, clock);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(PLAYER_A));
        bus.publish(signal);

        assertEquals(1, sink.shown.size());
        assertEquals(new Shown(VILLAGER, Set.of(PLAYER_A), LINE_1), sink.shown.get(0));
    }

    @Test
    void secondPublishWithinCooldownProducesNoLine() {
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1, LINE_2)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        VillagerEventBus bus = bus(catalogue, sink, clock);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(PLAYER_A));
        bus.publish(signal);
        clock.set(1); // well within the default 60s/1200-tick per-event cooldown
        bus.publish(signal);

        assertEquals(1, sink.shown.size());
    }

    @Test
    void sleepingVillagerProducesNoLineForANonSleepEvent() {
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        VillagerEventBus bus = bus(catalogue, sink, clock);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, true, false, Set.of(PLAYER_A));
        bus.publish(signal);

        assertTrue(sink.shown.isEmpty());
    }

    @Test
    void rateLimitedPlayerIsExcludedFromRecipientsButOthersStillReceiveTheLine() {
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        VillagerEventBus bus = bus(catalogue, sink, clock);

        // Exhaust player A's rate-limit slot on an unrelated villager first (same event, since the
        // fake catalogue only has lines for trade_completed -- the rate limit doesn't care which
        // villager or event actually produced the line, only that one reached this player).
        VillagerReactionSignal warmup = new VillagerReactionSignal(
                UUID.randomUUID(), VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(PLAYER_A));
        bus.publish(warmup);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(PLAYER_A, PLAYER_B));
        bus.publish(signal);

        assertEquals(2, sink.shown.size());
        assertEquals(Set.of(PLAYER_B), sink.shown.get(1).playerIds());
    }

    @Test
    void noRepeatAcrossTwoWellSeparatedPublishes() {
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1, LINE_2)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        // roll always returns index 0: after excluding LINE_1 the sole remaining candidate is LINE_2.
        VillagerEventBus bus = new VillagerEventBus(catalogue, sink, clock::get, bound -> 0);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of());
        bus.publish(signal);
        clock.set(ReactionRules.DEFAULT_PER_EVENT_COOLDOWN_TICKS);
        bus.publish(signal);

        assertEquals(2, sink.shown.size());
        assertEquals(LINE_1, sink.shown.get(0).line());
        assertEquals(LINE_2, sink.shown.get(1).line());
    }

    @Test
    void theFiveArgumentConstructorUsesTheInjectedReactionRulesInsteadOfTheSpecDefaults() {
        // A custom per-event cooldown far shorter than the spec's proposed 1200-tick default: if
        // this constructor actually wires the injected ReactionRules through (VV-8), a second
        // publish just one tick later still produces a line, instead of being suppressed.
        FakeCatalogue catalogue = new FakeCatalogue(Map.of(
                VillagerReactionEvent.TRADE_COMPLETED, List.of(LINE_1, LINE_2)));
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong(0);
        ReactionRules briefCooldown = new ReactionRules(1, 0, 0);
        VillagerEventBus bus = new VillagerEventBus(catalogue, sink, clock::get, bound -> 0, briefCooldown);

        VillagerReactionSignal signal = new VillagerReactionSignal(
                VILLAGER, VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of());
        bus.publish(signal);
        clock.set(1);
        bus.publish(signal);

        assertEquals(2, sink.shown.size());
    }

    @Test
    void unconfiguredBusStillFansOutToSubscribersOnly() {
        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> received = new ArrayList<>();
        bus.subscribe(received::add);

        VillagerReactionSignal signal = new VillagerReactionSignal(VILLAGER, VillagerReactionEvent.TRADE_COMPLETED);
        bus.publish(signal);

        assertEquals(List.of(signal), received);
    }
}
