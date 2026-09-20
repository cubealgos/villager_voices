---
title: "villager_voices DEC-005 — Alpha ships the reaction system on Fabric 26.2 only, before any recorded audio"
type: "spec"
category: "villager_voices"
---

# `DEC-005` — Alpha ships the reaction system on Fabric 26.2 only, before any recorded audio

**Status:** decided by Kevin, 2026-09-20.

Kevin's own words: "For implementation I would want to focus on the reaction system first, then the
voice lines and audio; this makes it already usable as an alpha version; I would want to release
that already so I am the first Java port of this idea; we need to be fast"
(`rulings-2026-09-20.md`). The alpha ships:

- All 16 events wired to the `common` bus (`domains/reaction.md`).
- The full 64-line text catalogue, written now, not deferred (`domains/reaction-lines.md`).
- Cooldowns, rate limiting, hearing range, silence rules (`domains/reaction.md` §3).
- The action bar as the primary display channel, with a per-player queue (`domains/display.md`).
- Near-silent placeholder sound events carrying real subtitle text, so the entire
  registration/subtitle/playback code path is exercised and correct from the first release
  (`domains/audio.md` `AUDIO-DEC-001`).
- A config surface (`contracts/data-contract.md`).
- **Fabric 26.2 only** — zero new toolchain downloads on this machine, one loader/version to build
  and test, same-day first buildable jar
  (`vault/technical/minecraft/multi-loader-multi-version-mods-2026.md` "Fastest path for the
  alpha"), on a `common` module kept clean of Minecraft imports from day one
  (`04-architecture.md` `ARCH-DEC-001`) so NeoForge and 1.21.1 are additive fast-follows, not a
  rewrite (`04-architecture.md` `ARCH-DEC-002`, `UC-009`).

**Deferred, not cut**: real recorded/generated audio (`domains/audio.md` `UC-010`); the in-world
speech bubble (`domains/display.md` "Speech bubble"); NeoForge 26.2, then 1.21.1 for both loaders
(`contracts/platform-matrix.md`); the EMF talking-state variable ships once the render-state work
lands, not required for the alpha's own "visibly working" bar, which the action bar alone satisfies.

Alternative considered: a smaller alpha covering only a handful of "hero" events (trade, hurt,
killed) rather than the full 16. Rejected: the per-event hook work (mixin vs. native event vs. poll)
is the actual engineering cost here, not the line-writing, and the research's own hook table already
covers all 16 at equal confidence — cutting the event list would not meaningfully speed the alpha.
Cost if wrong: if a specific event's hook (several are inferred, not confirmed, `domains/reaction.md`
§7) proves broken at the first ticket, that one event ships later without blocking the other 15.
