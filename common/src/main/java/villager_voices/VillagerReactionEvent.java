package villager_voices;

/**
 * The 16 events a villager can react to (docs/spec/domains/reaction.md §3). Detection is a
 * per-loader concern; this enum is the shared vocabulary every {@link VillagerEventSource}
 * implementation reports into the {@link VillagerEventBus}.
 */
public enum VillagerReactionEvent {
    TRADE_COMPLETED,
    OFFER_OPENED,
    HURT,
    KILLED,
    ZOMBIFIED,
    CURED,
    LEVEL_UP,
    RESTOCK,
    SLEEP,
    WAKE,
    RAID_BELL,
    GOLEM_SUMMONED,
    PANIC,
    PLAYER_STARING,
    BREEDING,
    BABY_GROWS,
}
