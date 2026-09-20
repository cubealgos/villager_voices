---
title: "villager_voices DEC-010 — A data-driven line catalogue for the alpha, not hardcoded"
type: "spec"
category: "villager_voices"
---

# `DEC-010` — A data-driven line catalogue for the alpha, not hardcoded

**Status:** confirmed at the first ticket (`VV-3`). Not a Kevin ruling — the 16-event scope and the
line text are Kevin's own brief and this sheet's own writing respectively
(`domains/reaction-lines.md`); this decision is about how the catalogue is stored and loaded. The
codec's exact field names, left open by this sheet, are confirmed in `domains/reaction-lines.md`
§2.

## The choice

`data/villager_voices/reaction/<event>.json` — 16 ordinary datapack files, one per event, each a
list of `{subtitle, sound}` entries (`domains/reaction.md` §3, `domains/reaction-lines.md` §2) —
over a hardcoded Java `Map<Event, List<Line>>` compiled directly into the jar.

## Why data-driven

- **Matches the research's own lean**: "the research's proposal leans data-driven" (originating
  brief); `vault/technical/minecraft/villager-events-sounds-and-emf-compat.md`'s "Proposal section"
  already frames the alpha's catalogue as a JSON-shaped concept, not Java constants.
- **Matches this family's own established pattern**: `create_synthetic_diamonds`'
  `RECIPE-DEC-001`/`RECIPE-REQ-005` chose datapack-overridable recipe JSON over hardcoded Java for
  the identical reason — a modpack or server operator retuning behaviour without a rebuild, at
  effectively zero extra engineering cost for this mod's own shape.
- **The generator still guarantees consistency**: because `SoundEvent` registration is a genuine
  Minecraft registry requirement that cannot be pure data (research §B1), the catalogue JSON and the
  Java registration list are both generated from one authored source (`domains/reaction-lines.md`),
  so "data-driven" here does not mean "hand-maintained JSON that can drift from the code" — it means
  the *distribution* format is data, not that authorship skips Java entirely.

## The honest limit, named rather than hidden

A datapack cannot introduce a wholly new `SoundEvent` through this catalogue JSON alone — Minecraft's
registry system requires that step in code from some loaded mod (`ACTORS-005`,
`contracts/public-surface.md` `SURFACE-REQ-003`). A datapack **can** reorder, remove, retune, or
extend a catalogue using this mod's own 64 already-registered sound events, or another mod's
already-registered ones; it cannot add a genuinely new voice line pointing at brand-new audio without
that audio's `SoundEvent` existing in code somewhere first. This is stated plainly rather than
implied away by the word "data-driven."

## Alternative considered: hardcoded Java catalogue

Simpler to implement (no JSON loader, no codec, no load-time validation) and would still satisfy the
alpha's own "ship fast" pressure. Rejected because the cost difference is genuinely small — the
`SoundEvent` registration generator has to exist either way (research §B1's per-line boilerplate),
so emitting a JSON catalogue alongside it is not much further work — while the benefit (a server
operator retuning which lines appear, or muting specific lines, without a mod update) is real and
consistent with how this org's other specs treat exactly this tradeoff.

## Cost if wrong

If the JSON loader/codec proves more first-ticket friction than expected, a hardcoded catalogue is a
strict functional subset — delete the loader, inline its generated output as Java constants, keep
the same generator source (`domains/reaction-lines.md`) feeding both registration and text. Not a
redesign of the event/selection/cooldown logic in `domains/reaction.md`, which is unaffected either
way.
