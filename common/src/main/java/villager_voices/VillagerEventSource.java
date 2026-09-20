package villager_voices;

/**
 * A loader-supplied source of {@link VillagerReactionSignal}s: native events, mixins, or a
 * per-tick poll, depending on the event and loader (docs/spec/04-architecture.md ARCH-DEC-003).
 * Each loader module provides its own implementation, wired to {@code common} through
 * {@code META-INF/services/villager_voices.VillagerEventSource} so {@code common} never imports
 * the loader that supplies it.
 */
public interface VillagerEventSource {

    /** Registers this source's hooks against the given bus. Called once, at mod init. */
    void register(VillagerEventBus bus);
}
