---
schema_version: 1
id: 01M2YYSAD3X8JWC6TMDEYRGYFE
key: M3
title: Audio, compat, and tooling fast-follows
status: backlog
created_at: 2026-09-20T08:26:38Z
---

## Goal

Replace the alpha's placeholders with real audio, add animation-pack compatibility, and round out
the development/testing surface — independent tracks that may land in either order relative to
each other, both preceding beta (`docs/spec/04-architecture.md` `ARCH-DEC-002`).

## Scope

The Piper TTS pipeline generating real `.ogg` files for all 64 lines; the EMF/ETF/Fresh Animations
render-state side channel and talking-state variable; the `/villager_voices debug trigger` command;
the in-world speech bubble display channel (explicitly deferred past the alpha,
`docs/spec/domains/display.md` §3).

## Exit criteria

- All 64 placeholder sounds are replaced with Kevin-approved Piper-generated audio, with zero
  registration or catalogue-format change.
- EMF's `villager_voices.is_talking` variable reads correctly with EMF installed, and causes no
  behavioural difference with EMF absent.
- The debug command force-triggers any of the 16 events on a targeted villager.
- The speech bubble displays in-world, independently toggleable from the action bar.

## Tickets

`VV-11`, `VV-12`, `VV-13`, `VV-14`.

## Depends on

`M1` (the alpha surface these fast-follows extend); `VV-11` additionally depends on `VV-8`'s
placeholder registration path, `VV-12` on `VV-7`'s display queue.
