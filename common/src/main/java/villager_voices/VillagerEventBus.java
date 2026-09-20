package villager_voices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;

/**
 * Dispatches {@link VillagerReactionSignal}s from every discovered {@link VillagerEventSource} to
 * its subscribers (VV-1's original behaviour, unchanged and always run first), and — once
 * configured with a {@link LineCatalogue}, a {@link LineSink}, a clock, and a random source — also
 * runs each published signal through {@link ReactionRules} and {@link LineSelector} and hands the
 * selected line to the sink (docs/spec/domains/reaction.md §3, REACTION-REQ-005–010, VV-2). The
 * reaction pipeline is purely additive: the no-argument constructor leaves it unconfigured, so
 * {@link #publish} behaves exactly as it did for VV-1's subscriber-only callers.
 *
 * <p>{@link LineCatalogue} and {@link LineSink} are defined in this package as the small
 * interfaces this ticket needs and nothing more — see their own Javadoc. VV-3 (the line catalogue
 * codec) implements {@code LineCatalogue} and VV-7 (the {@code DisplayQueue} and its fabric shim)
 * implements {@code LineSink}, both after this ticket merges.
 *
 * <p>VV-8 adds the five-argument constructor below so a config-sourced {@link ReactionRules} (its
 * own already-public three-argument constructor, docs/spec/contracts/data-contract.md
 * {@code DATA-REQ-002}) can replace the four-argument constructor's hardcoded
 * {@code new ReactionRules()} — plumbing only, {@link ReactionRules}'s own permit/record/cooldown
 * logic is untouched.
 */
public final class VillagerEventBus {

    private final List<Consumer<VillagerReactionSignal>> subscribers = new ArrayList<>();
    private final ReactionRules rules;
    private final LineCatalogue catalogue;
    private final LineSink sink;
    private final LongSupplier clock;
    private final IntUnaryOperator roll;

    /** No reaction pipeline configured: {@link #publish} only fans signals out to subscribers. */
    public VillagerEventBus() {
        this(null, null, null, null);
    }

    /**
     * @param catalogue supplies each event's eligible lines (VV-3)
     * @param sink      receives the selected line for display (VV-7)
     * @param clock     the current tick count; sampled once per {@link #publish}, never cached
     * @param roll      a uniform index source in {@code [0, bound)} given {@code bound}, e.g.
     *                  {@code new Random()::nextInt}
     */
    public VillagerEventBus(LineCatalogue catalogue, LineSink sink, LongSupplier clock, IntUnaryOperator roll) {
        this(catalogue, sink, clock, roll, new ReactionRules());
    }

    /**
     * As the four-argument constructor, but with {@code rules} supplied directly instead of the
     * spec's proposed defaults — VV-8's config-file-overridable cooldowns
     * (docs/spec/contracts/data-contract.md {@code DATA-REQ-002}) construct a {@link ReactionRules}
     * from the loaded config and pass it here.
     *
     * @param rules the cooldown/rate-limit/silence rules this bus's reaction pipeline runs every
     *     published signal through
     */
    public VillagerEventBus(LineCatalogue catalogue, LineSink sink, LongSupplier clock, IntUnaryOperator roll,
            ReactionRules rules) {
        this.catalogue = catalogue;
        this.sink = sink;
        this.clock = clock;
        this.roll = roll;
        this.rules = rules;
    }

    /** Discovers every {@link VillagerEventSource} on the classpath via {@link ServiceLoader}. */
    public static List<VillagerEventSource> discoverSources() {
        List<VillagerEventSource> sources = new ArrayList<>();
        for (VillagerEventSource source : ServiceLoader.load(VillagerEventSource.class)) {
            sources.add(source);
        }
        return sources;
    }

    public void subscribe(Consumer<VillagerReactionSignal> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(VillagerReactionSignal signal) {
        for (Consumer<VillagerReactionSignal> subscriber : subscribers) {
            subscriber.accept(signal);
        }
        if (catalogue != null && sink != null && clock != null && roll != null) {
            react(signal);
        }
    }

    private void react(VillagerReactionSignal signal) {
        UUID villagerId = signal.villagerId();
        VillagerReactionEvent event = signal.event();
        long now = clock.getAsLong();

        if (!rules.permits(villagerId, event, signal.villagerAsleep(), signal.villagerBaby(), now)) {
            return;
        }
        List<LineRef> eligible = catalogue.linesFor(event);
        Optional<LineRef> selected = LineSelector.select(eligible, rules.lastPlayed(villagerId, event), roll);
        if (selected.isEmpty()) {
            return;
        }
        rules.record(villagerId, event, selected.get(), now);

        Set<UUID> recipients = new HashSet<>();
        for (UUID playerId : signal.nearbyPlayerIds()) {
            if (rules.tryConsumePlayerRate(playerId, now)) {
                recipients.add(playerId);
            }
        }
        sink.show(villagerId, recipients, selected.get());
    }
}
