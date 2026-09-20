---
schema_version: 1
id: 01M2YYTHF6ZQX6K0TS6ESZ3NRB
key: VV-4
type: feat
title: "Fabric event wiring: trade and social events"
created_by: kevin
created_at: 2026-09-20T08:27:18Z
---

## Scope

Eight of the 16 events, on Fabric 26.2, per `docs/spec/domains/reaction.md` §3's hook table:
`trade_completed` (mixin, `AbstractVillager.notifyTrade` TAIL), `offer_opened` (native,
`UseEntityCallback.EVENT`, filter `instanceof Villager`), `level_up` (mixin, `setVillagerData` HEAD,
diff old vs. new level), `restock` (mixin, `Villager.restock()` TAIL), `raid_bell` (mixin, two
targets: `BellBlockEntity.onHit` and `Raids.createOrExtendRaid`), `breeding` (native,
`BabyEntitySpawnEvent`, filter `Villager`), `baby_grows` (mixin, `Villager.ageBoundaryReached()`),
`golem_summoned` (native, `ServerEntityEvents.ENTITY_LOAD`, filter `IRON_GOLEM` + proximity).
Implements `FabricVillagerEventSource.register` (currently a no-op stub from `VV-1`) for these
eight events only; `VV-5` covers the remaining combat/state events, `VV-6` the two poll-based ones.

## Approach

One `villager_voices.fabric.mixin` config entry per mixin target (`trade_completed`, `level_up`,
`restock`, `raid_bell` ×2, `baby_grows`); native-event listeners for `offer_opened`, `breeding`,
`golem_summoned` registered directly in `FabricVillagerEventSource.register`. Every hook builds a
`VillagerReactionSignal` (`VV-1`) and publishes it to the bus passed into `register`. Per
`PLATFORM-REQ-002`: a mixin target renamed or removed by a future update must fail the build at
compile time for a direct method reference, or mod load with a named error for a looser target —
never a silent no-op.

## Acceptance criteria

- [x] All eight events fire their hook and reach the bus in a real Fabric 26.2 world, proven by a
      game test per event (`docs/spec/operations/testing.md` "Game tests" row, `TEST-REQ-003`).
- [x] The `trade_completed` mixin observes `notifyTrade` without altering its return value or the
      trade's own outcome (`TEST-REQ-003`).
- [x] A raid-bell burst is observable as a queued sequence once `VV-7` lands (not required to
      display correctly in this ticket, only to fire the event).
- [x] `mixins/villager_voices.mixins.json`'s `mixins` array is non-empty for the first time since
      `VV-1`; `client` stays empty (all eight are server-side).

## Constraints and prior findings

`docs/spec/contracts/platform-matrix.md` "Mixin targets, per loader" table confirms the same seven
mixin targets for Fabric (minus `offer_opened`, which platform-matrix.md marks conditional "only if
GUI-vs-click distinction is needed" — reaction.md's own table marks it `N`, native-event only;
follow reaction.md, the more specific source). Blocked by `VV-2` (cooldown/selection logic the
published signal is checked against) and `VV-3` (the catalogue a selected line is drawn from).
`zombified`/`cured` inferred-not-`javap`-confirmed hooks are `VV-5`'s concern, not this ticket's.

## Findings

**`breeding`'s Approach text was wrong on two independent counts, both caught against the jar
rather than assumed — implemented as a mixin on the real hook, not the Approach's named native
event.** (1) `Villager` does not extend `Animal` — `javap` on both classes shows they extend
`AgeableMob` directly, as siblings, so `Animal.spawnChildFromBreeding` is never invoked for a
villager at all, mixin or not (confirmed the hard way: mixing into it and filtering
`self instanceof Villager` fails to *compile* — javac rejects the pattern match as provably
impossible between two unrelated concrete classes). (2) `BabyEntitySpawnEvent` does not exist
anywhere in Fabric API `0.161.0+26.2` — checked by extracting every one of its ~45 bundled module
jars and grepping for the class; it is NeoForge-only, matching the research note's own Fabric-API
column for that row ("none"). Traced the real vanilla call path by grepping the merged jar's
classes for bytecode references to `Villager.getBreedOffspring` (the one villager-specific breeding
method that exists): `net.minecraft.world.entity.ai.behavior.VillagerMakeLove` (a Brain behavior,
not a goal) drives it. `BreedingMixin` now injects `Villager.getBreedOffspring(ServerLevel,
AgeableMob)` at TAIL (full descriptor pinned, since the method has a synthetic covariant-return
bridge sharing its name), filtered to a non-null return, publishing `BREEDING` for both parents
when the other parent is also a `Villager` (the only case vanilla ever calls it for).
docs/spec/domains/reaction.md §3 and platform-matrix.md's own tables already said "mixin" for this
row — only the ticket's Approach paragraph had the wrong target/mechanism named. Recommend
`docs/spec/domains/reaction.md` §3's breeding row gets a footnote pointing at the real method next
time it's touched, so a future reader doesn't re-derive this from bytecode.

**26.2 moved entity-type constants off `EntityType` onto a new `EntityTypes` class** (plural) —
`net.minecraft.world.entity.EntityType` itself now carries no `public static final EntityType<...>`
fields at all; `net.minecraft.world.entity.EntityTypes.VILLAGER`/`.IRON_GOLEM`/etc. hold them.
Not called out in `vault/technical/minecraft/villager-events-sounds-and-emf-compat.md`'s 26.2
package-move note (which only covers `Villager`/`Zombie`-family package moves) — worth adding there
for whichever ticket next needs an entity-type constant on 26.2. Same pattern held for `Blocks`
(unmoved, `Blocks.BELL` confirmed) and `Items` (`Items.EMERALD` confirmed) — only the entity-type
constants moved.

**Mixin targets confirmed by `javap -p`/`-p -c` against `minecraft-merged-deobf-26.2.jar`**, one
per implemented mixin: `AbstractVillager.notifyTrade(MerchantOffer)` (public, TAIL — trade_completed),
`Villager.setVillagerData(VillagerData)` (public, HEAD — level_up), `Villager.restock()` (public,
TAIL — restock), `BellBlockEntity.onHit(Direction)` (public, TAIL — raid_bell ring target),
`Raids.createOrExtendRaid(ServerPlayer, BlockPos)` (public, non-static, TAIL — raid_bell raid-start
target; `Raids` itself holds no `Level` reference, so the mixin uses the `ServerPlayer` argument's
own `.level()`), `Villager.getBreedOffspring(ServerLevel, AgeableMob)` (public, TAIL, full
descriptor pinned against its bridge-method twin — breeding), `Villager.ageBoundaryReached()`
(protected, a real override on `Villager` itself not just inherited from `AgeableMob`, TAIL —
baby_grows; confirmed accessible to a game test only via `setAge` crossing the baby/adult zero
boundary, not by calling the protected method directly).

**The `Signals` helper the ticket brief asked to share with VV-5/VV-6**:
`fabric/src/main/java/villager_voices/fabric/events/Signals.java`, package
`villager_voices.fabric.events`. `Signals.of(LivingEntity entity, VillagerReactionEvent event)` →
`VillagerReactionSignal` (asleep/baby read off `entity`, `nearbyPlayerIds` via
`Signals.nearbyPlayerIds(LivingEntity)`, a 16-block search — docs/spec/domains/reaction.md §3's
engine-default hearing-range cap, `Signals.HEARING_RANGE_BLOCKS`). Deliberately typed to
`LivingEntity`, not `Villager`, so VV-5's `ZombieVillager` (not a `Villager` subtype) and VV-4's own
`AbstractVillager`-typed `notifyTrade` hook both fit without a second helper.
`TradeAndSocialEvents.publishToNearbyVillagers(Level, BlockPos, VillagerReactionEvent)` (in the same
`events` package, not `Signals` itself) is the burst helper behind `raid_bell`/`golem_summoned` —
worth reusing if VV-5/VV-6 ever need a "every nearby villager reacts" pattern too.

**`VillagerVoicesFabric.BUS` is now a public static field**, not just a local variable inside
`onInitialize()` — added because game tests need a reference to the exact bus every mixin and
native listener publishes into (mixins are compiled-in hooks against one instance; a test cannot
substitute its own). VV-5/VV-6's own game tests will want the same field; no need to add another.

**Gametest classes must be listed explicitly** in
`fabric/src/gametest/resources/fabric.mod.json`'s `entrypoints.fabric-gametest` array — a bare
`@GameTest`-annotated class is not auto-discovered by classpath/package scanning. Cost me one
silent-pass debugging round (`runGameTest` reported "2 tests" instead of the expected 11 until the
new class was added to that array). VV-5/VV-6 will hit the same thing for their own gametest
classes — the array is a shared file, so expect a small merge there too, same as
`villager_voices.mixins.json`.

**Check line**: `just check` green, including `./gradlew :fabric:runGameTest` →
`All 11 required tests passed :)` (9 of this ticket's own + `SmokeGameTest`'s `theModLoads` + one
test the `fabric-gametest-api-v1` module itself appears to register — not investigated further,
harmless, passed).
