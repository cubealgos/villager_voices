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

- [ ] All eight events fire their hook and reach the bus in a real Fabric 26.2 world, proven by a
      game test per event (`docs/spec/operations/testing.md` "Game tests" row, `TEST-REQ-003`).
- [ ] The `trade_completed` mixin observes `notifyTrade` without altering its return value or the
      trade's own outcome (`TEST-REQ-003`).
- [ ] A raid-bell burst is observable as a queued sequence once `VV-7` lands (not required to
      display correctly in this ticket, only to fire the event).
- [ ] `mixins/villager_voices.mixins.json`'s `mixins` array is non-empty for the first time since
      `VV-1`; `client` stays empty (all eight are server-side).

## Constraints and prior findings

`docs/spec/contracts/platform-matrix.md` "Mixin targets, per loader" table confirms the same seven
mixin targets for Fabric (minus `offer_opened`, which platform-matrix.md marks conditional "only if
GUI-vs-click distinction is needed" — reaction.md's own table marks it `N`, native-event only;
follow reaction.md, the more specific source). Blocked by `VV-2` (cooldown/selection logic the
published signal is checked against) and `VV-3` (the catalogue a selected line is drawn from).
`zombified`/`cured` inferred-not-`javap`-confirmed hooks are `VV-5`'s concern, not this ticket's.
