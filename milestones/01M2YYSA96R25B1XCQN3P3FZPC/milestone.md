---
schema_version: 1
id: 01M2YYSA96R25B1XCQN3P3FZPC
key: M1
title: "Alpha: reaction system on Fabric 26.2"
status: todo
created_at: 2026-09-20T08:26:38Z
---

## Goal

Ship the reaction system as a playable alpha on Fabric 26.2, before any recorded audio exists
(`docs/spec/decisions/DEC-005-alpha-scope.md`).

## Scope

All 16 events wired to the `common` bus with cooldowns, rate limiting, hearing range, and silence
rules; the full 64-line text catalogue; the action bar as the primary display channel with a
per-player queue; near-silent placeholder sound events carrying real subtitle text; the config
surface. Fabric 26.2 only — NeoForge and 1.21.1 are `M2`'s territory, real audio is `M3`'s.

## Exit criteria

- `just check` is green: lint, map-check, unit tests, and a game test per event proving each of the
  16 hooks fires and reaches the bus.
- The action bar displays a selected line, correctly formatted, held for the configured minimum
  display time, for every player within hearing range.
- All 64 sound events are registered with real subtitle text; `just doctor` is fully clean.
- The config file round-trips cooldowns, mutes, display toggles, and queue timing.

## Tickets

`VV-2`, `VV-3`, `VV-4`, `VV-5`, `VV-6`, `VV-7`, `VV-8`.

## Depends on

`M0` (the bootstrap this milestone's tickets build on).
