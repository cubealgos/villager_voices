---
title: "villager_voices spec — platform matrix: loaders, versions, and the CI grid"
type: "spec"
category: "villager_voices"
---

# Platform matrix (`PLATFORM`)

## The four target combinations

| Combination | Ships in | How it is checked |
|---|---|---|
| Fabric, 26.2 | **Alpha** (`decisions/DEC-005-alpha-scope.md`) | `just doctor`, game tests on Fabric 26.2, `just client` |
| NeoForge, 26.2 | Fast-follow 1 (`UC-009`) | New CI matrix row once its module lands |
| Fabric, 1.21.1 | Fast-follow 2 | New CI matrix row |
| NeoForge, 1.21.1 | Fast-follow 2 | New CI matrix row |

All four before beta (`rulings-2026-09-20.md`: "NeoForge 26.2, then 1.21.1 for both, follow before
beta as additive work").

## Per-row toolchain (source: `04-architecture.md`, `vault/technical/minecraft/multi-loader-multi-version-mods-2026.md` §B, jar- and repo-verified)

| Row | 1.21.1 | 26.2 |
|---|---|---|
| Java | 21 | 25 |
| Fabric Loom | `1.17-SNAPSHOT` | same |
| Fabric Loader | `≥0.19.5` | same |
| Fabric API | `0.116.17+1.21.1` | `0.161.0+26.2` |
| NeoForge MDG | `2.0.147` | same |
| NeoForge version | `21.1.251` | `26.2.0.88` |
| Root Gradle | `9.5.1` | same |

`PLATFORM-REQ-001`: **if** any row moves, **then** `just doctor` fails naming the row.

## Mixin targets, per loader (`domains/reaction.md` §3, `domains/compat.md` §3)

| Loader | Reaction-system mixins | Render-state mixins |
|---|---|---|
| Fabric | `trade_completed`, `offer_opened`(only if GUI-vs-click distinction is needed), `level_up`, `restock`, `raid_bell`, `baby_grows`, `breeding` | `extractRenderState` on 26.2 only |
| NeoForge | `trade_completed`, `level_up`, `restock`, `sleep`, `wake`, `raid_bell`, `baby_grows` | none — `RegisterRenderStateModifiersEvent` is a native event |

`PLATFORM-REQ-002`: **if** a mixin target method is renamed or removed by a Minecraft update,
**then** the build fails at compile time for a direct reference, or mod load fails with a named
error for a looser target — never a silent no-op (`domains/reaction.md` `REACTION-FAIL-001`).

## Client and server

Event detection, cooldowns, selection, and the action-bar push are all server-side; catalogue JSON
is also loaded client-side for the same data-driven bookkeeping vanilla recipes always get. The
render-state side channel and EMF variable are client-only (`domains/compat.md`).

## Soft dependencies

| Dependency | Required? | Behaviour if absent |
|---|---|---|
| EMF | No | `villager_voices.is_talking` is set and copied into the render state but read by nothing (`domains/compat.md` `COMPAT-FAIL-001`) |
| ETF | No | No interaction exists either way (research §D3) |
| Fresh Animations (or any pack reading the variable) | No | Same as EMF absent — no mouth movement, everything else unchanged |

## CI matrix (Woodpecker), one job per shipped combination

At the alpha: one job (Fabric 26.2). Each fast-follow adds exactly one job for its own
loader/version pair, never a combinatorial rebuild of the others — the module boundary
(`04-architecture.md` `ARCH-DEC-001`) means a NeoForge job failing does not block a Fabric release
and vice versa.

`PLATFORM-REQ-003`: **if** a shipped combination's CI job is red, **then** that combination's
Modrinth version is not published; the other combinations are unaffected.
