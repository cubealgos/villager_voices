package villager_voices.fabric.sound;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny tick-delay scheduler (VV-18, docs/spec/domains/audio.md {@code AUDIO-REQ-007}): queues a
 * {@link Runnable} to run once the server tick clock reaches a given tick, e.g. delaying a line's
 * own sound until its grunt has finished. Owned by
 * {@link villager_voices.fabric.VillagerVoicesFabric} as one long-lived instance, drained every
 * tick from {@code ServerTickEvents.END_SERVER_TICK} and cleared on {@code SERVER_STOPPING} so a
 * task referencing a now-dead level or entity never fires after the world it belonged to is gone.
 *
 * <p>Not thread-safe by design: every call is expected from the single server tick thread, the same
 * thread {@code ServerTickEvents.END_SERVER_TICK} itself fires on.
 */
public final class TickScheduler {

    private record ScheduledTask(long dueTick, Runnable task) {
    }

    private final List<ScheduledTask> pending = new ArrayList<>();

    /** Queues {@code task} to run the next time {@link #drain} sees {@code currentTick >= dueTick}. */
    public void schedule(long dueTick, Runnable task) {
        pending.add(new ScheduledTask(dueTick, task));
    }

    /**
     * Runs and removes every task due by {@code currentTick} (i.e. {@code dueTick <= currentTick}),
     * in the order they were scheduled. A task scheduled for a tick already past when {@link
     * #schedule} was called runs on the very next {@link #drain} -- there is no minimum delay.
     */
    public void drain(long currentTick) {
        if (pending.isEmpty()) {
            return;
        }
        List<ScheduledTask> due = new ArrayList<>();
        pending.removeIf(scheduled -> {
            if (scheduled.dueTick() <= currentTick) {
                due.add(scheduled);
                return true;
            }
            return false;
        });
        for (ScheduledTask scheduled : due) {
            scheduled.task().run();
        }
    }

    /** Discards every still-pending task without running it (server stop). */
    public void clear() {
        pending.clear();
    }

    /** How many tasks are currently pending -- a test/inspection seam. */
    public int pendingCount() {
        return pending.size();
    }
}
