---
title: "villager_voices DEC-004 — common/fabric/neoforge module split, Stonecutter, the JEI shape not Architectury"
type: "spec"
category: "villager_voices"
---

# `DEC-004` — `common`/`fabric`/`neoforge` module split, Stonecutter, the JEI shape not Architectury

**Status:** decided by Kevin, 2026-09-20.

"A real `common`/`fabric`/`neoforge` module split from day one (the JEI shape, not Architectury),
Stonecutter for the version axis (1.21.1, 26.2)" (`rulings-2026-09-20.md`). Full technical reasoning
in `04-architecture.md` `ARCH-DEC-001`, drawn from
`vault/technical/minecraft/multi-loader-multi-version-mods-2026.md`: Architectury's `@ExpectPlatform`
codegen and ecosystem runtime buy nothing this mod needs; the official Stonecutter multiloader
template flattens `common` into one shared source directory rather than a real module boundary;
JEI's own `Common/`/`Fabric/`/`NeoForge/` split is the confirmed, at-scale precedent this mod copies
instead.

Unlike every Create Fly sibling (one Gradle project, no sim layer to keep pure), this mod's `common`
module carries genuinely substantial pure logic — the 16-event bus, cooldowns, selection, the
display queue (`04-architecture.md` `ARCH-DEC-003`) — large enough that a real module boundary,
enforced by the build rather than a package-purity check alone, is worth its setup cost here in a
way it was not judged worth it for any of the four smaller siblings.

Alternative considered: one Gradle project per loader/version combination with no shared `common`
module, following the siblings' own single-project pattern. Rejected: this mod's reaction-system
logic is identical across all four combinations, and duplicating it four times risks exactly the
drift a shared `common` module exists to prevent. Cost if wrong: if Stonecutter's version-node
wiring does not fit this module shape as cleanly as the research predicts, the fallback is
per-version-per-loader physical subprojects (the EMF/ETF pattern) — more boilerplate, not a rewrite
of the `common` logic itself (`04-architecture.md` `ARCH-FAIL-002`).
