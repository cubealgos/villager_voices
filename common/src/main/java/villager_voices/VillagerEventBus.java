package villager_voices;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Dispatches {@link VillagerReactionSignal}s from every discovered {@link VillagerEventSource} to
 * its subscribers. Cooldowns, selection and silence rules (docs/spec/domains/reaction.md §3) are
 * not implemented here yet — this bootstrap ticket (VV-1) establishes the loader-free wiring the
 * fast-follow tickets fill in.
 */
public final class VillagerEventBus {

    private final List<Consumer<VillagerReactionSignal>> subscribers = new ArrayList<>();

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
    }
}
