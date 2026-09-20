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

- [ ] `panic` fires once per panic episode (edge-detected, not once per tick while panicking),
      proven by a game test.
- [ ] `player_staring` fires under a defined, documented threshold, proven by a game test placing a
      test player at a known distance/angle.
- [ ] A large-village stress check (many loaded villagers, none panicking or stared at) shows no
      measurable steady-state allocation from the poll (`REACTION-FAIL-004`).

## Constraints and prior findings

Blocked by `VV-2` (cooldown/silence logic). `docs/spec/domains/reaction.md` §7: "`player_staring`'s
exact detection thresholds — genuinely new design, no research precedent, first ticket." This is
that first ticket; the threshold decided here is a design choice to record, not one to defer again.
A config toggle to disable poll-based events entirely is a candidate if the stress check proves
insufficient, but is explicitly "not built at 1.0" (`ARCH-FAIL-004`) — do not build it here.
