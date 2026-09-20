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
