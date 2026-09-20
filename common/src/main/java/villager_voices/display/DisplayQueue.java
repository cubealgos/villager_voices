package villager_voices.display;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player FIFO queue absorbing the action bar's single-occupancy, last-write-wins behaviour
 * (docs/spec/domains/display.md §3 "The per-player queue"; {@code DISPLAY-DEC-002}). Every
 * {@code sendOverlayMessage}/{@code displayClientMessage(..., true)} call unconditionally resets
 * the client's own 60-tick timer and replaces the shown text, so without this queue several
 * villagers reacting within the same few seconds would flicker unreadably (research
 * {@code villager-events-sounds-and-emf-compat.md} §B6). Instead, a line holds on a given player's
 * action bar for at least {@code minHoldTicks} before {@link #tick} advances to the next queued
 * line for that player ({@code DISPLAY-REQ-003}); a burst beyond {@code capPerPlayer} drops the
 * oldest line still waiting in that player's backlog — never the line currently shown
 * ({@code DISPLAY-FAIL-002}).
 *
 * <p>Not thread-safe: intended to be driven from one thread only (the server thread, via the
 * fabric module's {@code ServerTickEvents.END_SERVER_TICK} listener).
 */
public final class DisplayQueue {

    /**
     * Proposed default minimum hold time (docs/spec/domains/display.md §7: "1.5–2s proposed"),
     * confirmed at this ticket as 1.5s: 30 ticks at Minecraft's fixed 20 ticks/second.
     */
    public static final long DEFAULT_MIN_HOLD_TICKS = 30;

    /**
     * Default per-player backlog cap, confirmed at this ticket. Bounds a burst independent of the
     * server-wide rate limit upstream (docs/spec/domains/reaction.md §3), which is expected to
     * keep bursts well under this in practice — this cap is a hard backstop for the queue itself,
     * not the primary throttle.
     */
    public static final int DEFAULT_CAP_PER_PLAYER = 8;

    private final long minHoldTicks;
    private final int capPerPlayer;
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();

    /** A queue using {@link #DEFAULT_MIN_HOLD_TICKS} and {@link #DEFAULT_CAP_PER_PLAYER}. */
    public DisplayQueue() {
        this(DEFAULT_MIN_HOLD_TICKS, DEFAULT_CAP_PER_PLAYER);
    }

    /**
     * @param minHoldTicks minimum ticks a line stays current before {@link #tick} may advance to
     *     the next queued line for that player; {@code display.queueMinHoldTicks}'s eventual
     *     config value (docs/spec/domains/display.md §3 "Config surface") feeds this constructor,
     *     not the reverse
     * @param capPerPlayer maximum backlog size per player, oldest still-pending line dropped first
     *     beyond it ({@code DISPLAY-FAIL-002})
     */
    public DisplayQueue(long minHoldTicks, int capPerPlayer) {
        if (minHoldTicks < 0) {
            throw new IllegalArgumentException("minHoldTicks must be >= 0, was " + minHoldTicks);
        }
        if (capPerPlayer < 1) {
            throw new IllegalArgumentException("capPerPlayer must be >= 1, was " + capPerPlayer);
        }
        this.minHoldTicks = minHoldTicks;
        this.capPerPlayer = capPerPlayer;
    }

    /**
     * Enqueues {@code line} for {@code playerId}. When the resulting backlog (lines not yet
     * shown) exceeds {@code capPerPlayer}, the oldest still-pending line is dropped — the line
     * currently shown, if any, is never affected by this.
     */
    public void enqueue(UUID playerId, DisplayLine line) {
        PlayerState state = players.computeIfAbsent(playerId, id -> new PlayerState());
        state.pending.addLast(line);
        while (state.pending.size() > capPerPlayer) {
            state.pending.removeFirst();
        }
    }

    /**
     * Advances every player with a non-empty backlog whose current line (if any) has held for at
     * least {@code minHoldTicks}, popping their next line and marking it shown as of
     * {@code nowTicks}.
     *
     * @param nowTicks the current tick count, expected to be non-decreasing across calls (the
     *     fabric module passes {@code MinecraftServer#getTickCount()})
     * @return the line newly pushed to each advanced player's action bar this call, keyed by
     *     player id; a player with an empty backlog, or whose current line has not yet held for
     *     {@code minHoldTicks}, is absent
     */
    public Map<UUID, DisplayLine> tick(long nowTicks) {
        Map<UUID, DisplayLine> pushed = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
            PlayerState state = entry.getValue();
            boolean canAdvance = state.current == null || nowTicks - state.shownAtTicks >= minHoldTicks;
            if (canAdvance && !state.pending.isEmpty()) {
                state.current = state.pending.removeFirst();
                state.shownAtTicks = nowTicks;
                pushed.put(entry.getKey(), state.current);
            }
        }
        return pushed;
    }

    /** The number of lines still waiting behind whatever is currently shown for {@code playerId}. */
    public int pendingCount(UUID playerId) {
        PlayerState state = players.get(playerId);
        return state == null ? 0 : state.pending.size();
    }

    /** The line currently shown for {@code playerId}, or {@code null} if none has been pushed yet. */
    public DisplayLine current(UUID playerId) {
        PlayerState state = players.get(playerId);
        return state == null ? null : state.current;
    }

    private static final class PlayerState {
        private final Deque<DisplayLine> pending = new ArrayDeque<>();
        private DisplayLine current;
        private long shownAtTicks;
    }
}
