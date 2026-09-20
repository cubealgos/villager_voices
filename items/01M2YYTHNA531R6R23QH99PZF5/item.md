---
schema_version: 1
id: 01M2YYTHNA531R6R23QH99PZF5
key: VV-7
type: feat
title: "Action bar display: per-player DisplayQueue"
created_by: kevin
created_at: 2026-09-20T08:27:19Z
---

## Scope

`docs/spec/domains/display.md`: the primary display channel for a selected line. A per-player
`DisplayQueue` in `common` (pure Java, `ARCH-DEC-001`) holding a line for at least a configured
minimum display time (`DISPLAY-REQ-003`) before advancing; a thin Fabric-side push calling
`ServerPlayer#sendOverlayMessage(Component)` (`DISPLAY-REQ-001`, confirmed method for 26.2,
`04-architecture.md`'s toolchain table). Display format `"<profession>: <line>"`, falling back to
`"Villager: <line>"` for no profession (`DISPLAY-REQ-004`, proposed, confirm here). Hearing range
equals the line's sound event's effective broadcast radius (`DISPLAY-REQ-002`) — real broadcast
radius depends on `VV-8`'s registered `SoundEvent`s, so this ticket may need a placeholder radius
until `VV-8` lands; note the actual dependency in Findings once implemented.

## Approach

`DisplayQueue` holds, per online player, a FIFO of pending lines; a tick-driven advance checks
whether the current line's minimum hold time has elapsed and, if so, pops the next line and calls
the Fabric push. The push itself is a one-line per-loader shim (`fabric.VillagerVoicesFabric` or a
new small class), calling the version-appropriate overlay method — `04-architecture.md`'s loader
adapter table names `ServerPlayer#sendOverlayMessage` for 26.2 specifically so this shim is already
known, not exploratory.

## Acceptance criteria

- [ ] A single line reaches the correct player's action bar via `sendOverlayMessage`, formatted
      `"<profession>: <line>"` (`DISPLAY-REQ-001`, `DISPLAY-REQ-004`).
- [ ] A burst of multiple lines for one player (e.g. a raid-bell scenario) displays each in order,
      held for at least the minimum display time, never overlapping or dropped (`DISPLAY-REQ-003`,
      `DISPLAY-FAIL-002`).
- [ ] A player outside hearing range never receives the line (`DISPLAY-REQ-002`).
- [ ] `display.actionBar` and vanilla subtitles are independently toggleable in principle — the
      action bar push and any subtitle path do not depend on each other (`DISPLAY-REQ-006`); full
      config wiring is `VV-8`'s ticket, this one only needs to not couple the two channels.

## Constraints and prior findings

Blocked by `VV-3` (the catalogue a displayed line comes from). `DISPLAY-DEC-001`/`DISPLAY-DEC-002`:
action bar is primary, not a subtitle supplement, via a queue not an unconditional per-event call —
already decided, implement as specified, not redesigned. The exact minimum display time default
(1.5–2s proposed) is an open question to confirm at this ticket (`display.md` §7), config-overridable
later regardless.
