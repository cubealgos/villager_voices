---
schema_version: 1
id: 01M2YYTHKBAF4ENHWCKVQXS5A1
key: VV-6
type: feat
title: "Fabric poll-based events: panic and player_staring"
created_by: kevin
created_at: 2026-09-20T08:27:19Z
---

## Scope

The two events with no native or mixin hook at all (`docs/spec/domains/reaction.md` §3): `panic`
(poll `Brain<Villager>.isActive(Activity.PANIC)`) and `player_staring` (no vanilla hook — a
raycast/dot-product check against public API). Both detected via a `common`-module tick poll over
loaded villagers only (`ARCH-DEC-003`), registered as `ServerTickEvents.END_SERVER_TICK` on the
Fabric side.

## Approach

A per-server-tick poll iterating loaded `Villager` entities only (never all entities), edge-detected
per UUID so a steady "still panicking"/"still being stared at" state does not re-fire and does not
allocate on the steady-state case (`REACTION-FAIL-004`). `player_staring`'s exact detection
threshold (distance, angle, duration) is genuinely new design with no research precedent
(`reaction.md` §7) — pick a reasonable default here and record it in this ticket's own findings
once implemented, config-overridable later via `VV-8`/`contracts/data-contract.md`.

## Acceptance criteria

- [x] `panic` fires once per panic episode (edge-detected, not once per tick while panicking),
      proven by a game test.
- [x] `player_staring` fires under a defined, documented threshold, proven by a game test placing a
      test player at a known distance/angle.
- [x] A large-village stress check (many loaded villagers, none panicking or stared at) shows no
      measurable steady-state allocation from the poll (`REACTION-FAIL-004`).

## Constraints and prior findings

Blocked by `VV-2` (cooldown/silence logic). `docs/spec/domains/reaction.md` §7: "`player_staring`'s
exact detection thresholds — genuinely new design, no research precedent, first ticket." This is
that first ticket; the threshold decided here is a design choice to record, not one to defer again.
A config toggle to disable poll-based events entirely is a candidate if the stress check proves
insufficient, but is explicitly "not built at 1.0" (`ARCH-FAIL-004`) — do not build it here.

## Findings

`player_staring`'s three thresholds, decided here per §7's "first ticket" instruction (proposed
defaults, not yet confirmed by Kevin — same status as `reaction.md` §3's cooldown numbers):

- **Dot-product threshold**: `cos(15°) ≈ 0.9659258263` — the player's look vector must point
  within ~15° of the vector to the villager's eyes. Narrow enough to mean "looking at this
  villager specifically", not merely "this villager is somewhere in the player's general view".
- **Range**: 8 blocks — deliberately tighter than `reaction.md` §3's 16-block sound-hearing
  default; hearing a villager and having it notice a stare are different distances, and this event
  is about the latter (close enough to plausibly be noticed).
- **Duration**: 40 ticks (2s at 20 ticks/s) of unbroken qualification — long enough that a passing
  camera swing across the villager does not qualify, short enough to feel responsive.

All three live as named constants on `common`'s `StareDetector`
(`StareDetector.DOT_THRESHOLD`/`RANGE_BLOCKS`/`REQUIRED_TICKS`), ready for `VV-8` to make
config-overridable without touching the detection logic itself.

**Poll cost, measured**: `PolledEventsStressGameTest` spawns 40 idle villagers (none panicking or
stared at) and compares 100 ticks of wall-clock server time before vs. after they're loaded, both
phases with the poll already registered. Three sample runs on this dev machine (JIT-cold, a single
short-lived game-test JVM, so treat as an order-of-magnitude signal, not a tight benchmark):
delta ≈ 696µs, 1.39ms, and 2.41ms per tick for 40 villagers (≈ 17–60µs/villager/tick) — comfortably
under a 20-tick/s budget (50ms/tick) with orders of magnitude of headroom, and the test's own
assertion bound (5ms/tick delta) is set generously above all three to catch a gross regression
(e.g. an accidental full-entity-list scan) rather than to pin an exact number. `REACTION-FAIL-004`'s
"no allocation on the steady-state case" holds by construction: `PanicDetector.sample` and
`StareDetector.sample` mutate an existing `HashSet`/`HashMap` entry in the steady "still
panicking"/"still not staring" case rather than allocating a new one (the one per-call allocation
that remains, `StareDetector`'s small `PlayerVillagerKey` record built for each map lookup, is a
JIT escape-analysis candidate, not a `List`/`Set` copy).

**Known limitation, not addressed here**: neither detector's per-key memory (`PanicDetector`'s
panicking-villager set, `StareDetector`'s consecutive-tick map) is pruned when a villager unloads
or a player disconnects mid-episode — a villager that despawns while mid-panic, or a player who
disconnects while mid-stare, leaves one stale entry behind. Bounded by how often that specific
sequence happens (rare), not by server size, and self-corrects the next time that same UUID pair
is next observed in the opposite state. Flagged for the coordinator's unification pass rather than
fixed here, matching this ticket's own scope discipline (no config toggle, no gold-plating beyond
the acceptance criteria).

**Fabric API surface used**: `EntityType.VILLAGER` no longer exists as a static constant on
`net.minecraft.world.entity.EntityType` in 26.2 — entity-type constants moved to
`net.minecraft.world.entity.EntityTypes` (plural). `ServerLevel.getEntities(EntityTypeTest<Entity,
T>, Predicate<? super T>)` returns `List<? extends T>`, not `List<T>`. Both confirmed by `javap`
against the cached `minecraft-merged-deobf-26.2.jar`, not inferred.

**Game-test isolation gotcha, worth flagging for VV-4/VV-5's own game tests**: Minecraft's own
`@GameTest` framework runs multiple test methods concurrently in the same world at different
coordinate offsets (`ServerLevel.getEntities`/`.players()` aren't scoped to one test's structure).
Since `PolledEvents.register` is a poll over the *whole* server (by design), two `@GameTest`
methods that each register their own `VillagerEventBus` and capture signals both observe every
villager/player in the world, not just their own — a naive per-test assertion on raw captured-list
size is flaky. Fixed by filtering captured signals to the test's own villager UUID
(`PanicGameTest`/`StareGameTest`'s `panicCount`/`stareCount` helpers). Separately, `StareGameTest`
calls `villager.setNoAi(true)` on its spawned villager: the mock player's aim is set once via
`lookAt`, not continuously re-tracked, so a villager left free to wander (vanilla `random_stroll`)
drifts out of the ~15° cone well before the 40-tick requirement elapses — the detector was correct,
the first test draft's villager was simply walking out of frame.

**Deprecation note**: `GameTestHelper.makeMockServerPlayerInLevel()` (used by `StareGameTest` — the
only helper that both creates a mock player and actually adds it to `ServerLevel.players()`) is
marked `@Deprecated(forRemoval = true)` in this 26.2 jar, with no replacement identified in this
ticket. Not a blocker for the alpha (Fabric 26.2 only), but worth a heads-up for whoever ports to a
later Minecraft version.
