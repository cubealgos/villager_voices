---
schema_version: 1
id: 01M2YYTQN89TRWY91AMX9HQE63
key: VV-10
type: chore
title: Stonecutter and Minecraft 1.21.1 for both loaders
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

`ARCH-DEC-002`'s second fast-follow: the version axis, deferred at bootstrap alongside NeoForge for
the same reason (`DEC-005-alpha-scope.md`: "NeoForge MDG, Stonecutter, and `loom-back-compat` are
all uncached"). Introduces Stonecutter `0.9.8` over the existing `common`/`fabric`/`neoforge`
module split (`ARCH-DEC-001`: "Stonecutter for the version axis (1.21.1, 26.2)"), with version
nodes for both loaders on Minecraft 1.21.1 (Java 21, Fabric Loom `1.17-SNAPSHOT`, Fabric API
`0.116.17+1.21.1`, NeoForge `21.1.251` — `contracts/platform-matrix.md`). Requires
`dev.kikugie.loom-back-compat` `0.4.2` on the Fabric side, since "MC 26.1+ changed Loom variant
selection enough that 1.21.1-era and 26.x-era Loom configs aren't drop-in interchangeable"
(`04-architecture.md` `ARCH-DEC-001`). Also covers `platform-matrix.md`'s per-row hook differences
for 1.21.1 that `reaction.md` §3 flags as unverified (no 1.21.1 jar was available to the original
research) — this ticket is where those get `javap`-confirmed for real.

## Approach

Layer Stonecutter's version nodes onto the existing module boundary rather than restructuring it —
per `ARCH-DEC-001`'s own risk framing, if this proves awkward the fallback is per-version-per-loader
physical subprojects (`ARCH-FAIL-002`), not a `common` rewrite. Port `VV-4`–`VV-8`'s Fabric hooks and
`VV-9`'s NeoForge hooks to their 1.21.1-era method names/events, confirming each against real
1.21.1-mapped source rather than the original research's docs-only inference where flagged.

## Acceptance criteria

- [ ] Stonecutter builds both `1.21.1-fabric` and `1.21.1-neoforge` version nodes green,
      independently of the `26.2` nodes (`platform-matrix.md` "CI matrix": one new job per
      combination, no rebuild of the others).
- [ ] Every 1.21.1 hook previously marked "unverified" in `reaction.md` §3 is confirmed against real
      mapped source or a real game test, and the table updated to reflect it.
- [ ] `common` is unchanged except for genuinely shared additions.
- [ ] `PLATFORM-REQ-001`: if any toolchain row moves from what `platform-matrix.md` names, `just
      doctor` fails naming the row — confirm this still holds once Stonecutter multiplies the
      version matrix.

## Constraints and prior findings

Blocked by `VV-9` (following `ARCH-DEC-002`'s stated order: NeoForge 26.2, then 1.21.1 for both).
`ARCH-FAIL-002`'s cost-if-wrong is explicitly accepted here as a real possibility, not a hypothetical
— "more subprojects, more boilerplate, but a known-working shape at a much larger scale (EMF: 29,
ETF: 40) than this mod will ever need," so falling back is not a project risk if Stonecutter's node
wiring doesn't fit cleanly.
