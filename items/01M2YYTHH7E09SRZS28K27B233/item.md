---
schema_version: 1
id: 01M2YYTHH7E09SRZS28K27B233
key: VV-5
type: feat
title: "Fabric event wiring: combat and state-change events"
created_by: kevin
created_at: 2026-09-20T08:27:18Z
---

## Scope

The remaining six non-poll events from `docs/spec/domains/reaction.md` §3, on Fabric 26.2: `hurt`
(native, `ServerLivingEntityEvents.AFTER_DAMAGE`), `killed` (native,
`ServerLivingEntityEvents.AFTER_DEATH`), `zombified` (native,
`ServerLivingEntityEvents.MOB_CONVERSION`, inferred not `javap`-confirmed — smoke-test this ticket),
`cured` (same event, same caveat), `sleep` (native, `EntitySleepEvents.START_SLEEPING`), `wake`
(native, `EntitySleepEvents.STOP_SLEEPING`). All six are native-event, no mixin, on Fabric
(`contracts/platform-matrix.md`'s mixin table lists none of these for Fabric).

## Approach

Register all six as `FabricVillagerEventSource` native-event listeners, filtering to `Villager`
entities where the event isn't already villager-specific, and publishing a
`VillagerReactionSignal` to the bus for each. Since `zombified`/`cured` share one Fabric API event
(`MOB_CONVERSION`), distinguish direction (villager→zombie vs. zombie-villager→villager) from the
event's own before/after entity types, not a second hook.

## Acceptance criteria

- [ ] All six events fire and reach the bus in a real Fabric 26.2 world game test
      (`docs/spec/operations/testing.md`).
- [ ] The `zombified`→`cured` direction split is proven by two distinct game tests, not assumed from
      the single `MOB_CONVERSION` hook (closes `reaction.md` §7's "inferred, not confirmed" flag for
      both).
- [ ] `sleep`/`wake` correctly suppress every other event for that villager while asleep
      (`REACTION-REQ-009`) — a game test triggering a second event mid-sleep and confirming it does
      not reach the bus.

## Constraints and prior findings

Blocked by `VV-2` (cooldown/silence logic) and `VV-3` (catalogue). `docs/spec/domains/reaction.md`
§7 flags `zombified`/`cured` as needing a first-ticket smoke test since the research could only
infer, not `javap`-confirm, their hook — this ticket is that first ticket. `REACTION-FAIL-001`:
if a hook target is renamed by a future update, the build must fail at compile time for a direct
reference or mod load with a named error, never a silent no-op.

## Findings

- **`MOB_CONVERSION` verdict: confirmed, not merely inferred — jar-verified end to end.**
  `javap -p -c` on the shipped `fabric-entity-events-v1-5.0.5+06488ac19e.jar`
  (`~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/`) shows exactly one call site for
  `ServerLivingEntityEvents.MOB_CONVERSION`'s invoker: `MobMixin.afterEntityConverted(Entity,
  ConversionParams)`, which wraps an expression inside `Mob.convertTo(EntityType, ConversionParams,
  ConversionParams.AfterConversion)` and fires `onConversion(original, converted, params)` — before
  `ConversionType.convert()` runs, before the `AfterConversion` callback, before the new entity is
  added to the world, and before the old entity is discarded. Separately, `javap -p -c` on the
  shipped 26.2 `minecraft-merged-deobf-26.2.jar`
  (`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/`) shows
  both `Zombie.convertVillagerToZombieVillager` (via `convertToZombieType`) and
  `ZombieVillager.finishConversion` call the *same* public
  `Mob.convertTo(EntityType, ConversionParams, ConversionParams.AfterConversion)` overload — so both
  the zombify and the cure-finish path are jar-proven to route through the one hook Fabric API's
  mixin targets, not just plausible by structural resemblance. Both directions are also proven live
  by `CombatAndStateGameTest.zombifiedNamesTheOriginalVillager` and `.curedNamesTheResultingVillager`
  (fabric/src/gametest/java/villager_voices/fabric/gametest/CombatAndStateGameTest.java).
- **26.2 moved `EntityType` constants out of `EntityType` itself.** `net.minecraft.world.entity.
  EntityType` no longer carries `public static final EntityType<X> Y` fields (confirmed via
  `javap` — it has none beyond `CODEC`/`STREAM_CODEC`); they moved to a new
  `net.minecraft.world.entity.EntityTypes` (plural) class, e.g. `EntityTypes.VILLAGER`,
  `EntityTypes.ZOMBIE_VILLAGER`. Same pattern for beds: `Blocks.RED_BED` etc. no longer exist —
  `net.minecraft.world.level.block.Blocks.BED` is now a `ColorCollection<Block>`, and
  `Blocks.BED.red()` gives the red bed block. Neither move is documented in the read specs; worth
  folding into the vault's 26.2 package-move notes if another ticket hits the same wall.
  `LivingEntity.hurt(DamageSource, float)` is also gone on 26.2 — replaced by
  `hurtServer(ServerLevel, DamageSource, float)` (`LivingEntity.hurtClient` exists too, client-side
  only) — the ticket's own suggested `hurt(damageSource, amount)` call reads as the pre-26.2 name.
- **`hurtServer` wakes a sleeping entity before `AFTER_DAMAGE` fires.** `javap -c` on
  `LivingEntity.hurtServer` shows an `isSleeping()`/`stopSleeping()` call pair near the top of the
  method, before the damage-application logic `AFTER_DAMAGE` is injected at the tail of. So a
  `hurt` fired on a sleeping villager already reports `villagerAsleep=false` on its signal — taking
  damage always wakes first. This made `hurt` unusable as the "second event mid-sleep" case for the
  `REACTION-REQ-009` acceptance criterion; `CombatAndStateGameTest` uses `zombified` instead (proven
  by disassembly not to touch sleeping state).
- **Reading of "confirming it does not reach the bus" (3rd acceptance criterion).**
  `VillagerReactionSignal`'s own Javadoc (VV-2, merged) states a loader adapter always reports an
  accurate signal — asleep suppression is `ReactionRules`'s job inside `VillagerEventBus.react()`,
  downstream of `publish`. `VillagerVoicesFabric.BUS` (the bus this ticket's adapter registers
  against in production) is built with `VillagerEventBus`'s no-argument constructor — no reaction
  pipeline configured yet, since VV-3/VV-7/VV-8 supply `LineCatalogue`/`LineSink`/the config-driven
  cooldowns — so nothing can currently observe suppression "at the bus" in production. Built the
  criterion's game test (`sleepingVillagerSuppressesZombifiedFromTheSink`) against a dedicated,
  fully-configured local `VillagerEventBus` (fake catalogue/sink/clock/roll) instead, and proved
  both halves explicitly: the raw signal *does* reach a plain subscriber (detection is not
  suppressed), but never reaches the sink (selection is) — REACTION-REQ-009's actual, specified
  behaviour. Recording this interpretation explicitly per the standing "bound review loops, record
  an explicit ruling" principle rather than silently picking a reading.
- **Added `VillagerVoicesFabric.BUS` as a public static field** (was a local variable in
  `onInitialize`). No accessor to the mod's bus existed, and a game test cannot observe events
  dispatched through the sources wired at mod init without one (a fresh `VillagerEventBus` created
  inside a test never receives them). VV-4 and VV-6 likely hit the identical need in parallel on
  their own branches — the coordinator should de-duplicate this specific edit at merge time if more
  than one branch touches it the same way.
- **Fabric API's game-test summary counts one more test than are registered.** `runGameTest`'s log
  shows "9 tests are now running" / "All 9 required tests passed" for this module's 8 actually
  registered `@GameTest` methods (1 `SmokeGameTest` + 7 in `CombatAndStateGameTest`, confirmed by
  grepping `debug.log` for "Registering test method"). Reproduced across two separate `runGameTest`
  runs, zero failures either way — looks like a Fabric API gametest-runner counting quirk (possibly
  an internal self-check test), not a missing or duplicated test of ours; not investigated further
  since it doesn't affect pass/fail.
