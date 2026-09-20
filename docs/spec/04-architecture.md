---
title: "villager_voices spec — architecture: common/fabric/neoforge, Stonecutter, the alpha's one build"
type: "spec"
category: "villager_voices"
---

# 04 — Architecture

Sheet §3. Everything here follows
`vault/technical/minecraft/multi-loader-multi-version-mods-2026.md`; where the research left a gap
or a proposal rather than a settled fact, the gap is named and marked accordingly.

Unlike the four Create Fly add-ons, this mod has no single-fork pin: it targets two loaders and two
Minecraft versions from its own architecture, not from a later port.

## Shape

```
 datapack / JSON              common (pure Java, zero MC imports)         fabric / neoforge modules
 ┌───────────────────┐       ┌──────────────────────────────────┐       ┌───────────────────────┐
 │ data/villager_     │ load  │ VillagerEventBus                  │  wire  │ per-loader adapters:   │
 │  voices/reaction/   │──────►│  (16-event dispatch, cooldowns,   │◄───────│  native events, mixins, │
 │  <event>.json       │       │   selection, silence rules)       │        │   tick polls            │
 └───────────────────┘       │ DisplayQueue (per-player queue)    │       │ sound/lang registration │
                              │ SoundCatalogue (line → SoundEvent  │       │ render-state side       │
                              │  id mapping, generator-fed)        │       │  channel (COMPAT)       │
                              └──────────────────────────────────┘       └───────────────────────┘
                              versions/ (Stonecutter nodes: 1.21.1-fabric, 1.21.1-neoforge,
                                         26.2-fabric, 26.2-neoforge) — alpha ships 26.2-fabric only
```

**`common` never imports Minecraft, Fabric, or NeoForge classes** — the same hard boundary
`docs/spec/04-architecture.md` `ARCH-DEC-001` states for `create_civilization`'s own `core`/`sim`
projects, applied here to a much smaller mod. Everything platform-specific (event hooks, mixins,
sound registration, render-state side channels) lives in `fabric/`/`neoforge/` and is discovered by
`common` through a `ServiceLoader`-style platform interface, mirroring the layout
`multi-loader-multi-version-mods-2026.md` §C lays out for JEI's own shape.

## `ARCH-DEC-001` — a real `common`/`fabric`/`neoforge` module split from day one, Stonecutter for the version axis

**Decided by Kevin, 2026-09-20**: "a real `common`/`fabric`/`neoforge` module split from day one
(the JEI shape, not Architectury), Stonecutter for the version axis (1.21.1, 26.2)"
(`rulings-2026-09-20.md`). Following `multi-loader-multi-version-mods-2026.md`'s own headline
recommendation:

- **Not Architectury** (§A1): its `@ExpectPlatform` codegen and `architectury-api` runtime buy
  interoperability with the wider Architectury ecosystem (REI etc.) this mod has no need of.
- **Not the official Stonecutter multiloader template as-is** (§A3's caveat): that template
  flattens `common`/`fabric`/`neoforge` into one shared `src/main/java`, differentiated only by
  `//? if` preprocessing — nothing stops a "common" file from importing a Fabric class by accident,
  only a *different loader's build* failing to compile catches it. This mod restores the real module
  boundary the template omits, exactly as `multi-loader-multi-version-mods-2026.md` §C recommends.
- **Confirmed real-world precedent**: JEI (mezz/JustEnoughItems) runs exactly this shape —
  `Common/`, `Fabric/`, `NeoForge/` root subprojects, no Architectury, Forge dropped from its current
  branch — proof this scales to a mod with heavy platform-specific code (§A2).
- Stonecutter (`0.9.8`, current) layers the version axis on top, needing `dev.kikugie.loom-back-compat`
  `0.4.2` for the Fabric side, since MC 26.1+ changed Loom variant selection enough that 1.21.1-era
  and 26.x-era Loom configs are not drop-in interchangeable within Fabric alone (§A3).

**Cost if wrong**: if Stonecutter's version-node wiring proves awkward against this module shape at
the first ticket, the fallback is per-version-per-loader physical Gradle subprojects (the
EMF/ETF pattern, §A4) — more subprojects, more boilerplate, but a known-working shape at a much
larger scale (EMF: 29, ETF: 40) than this mod will ever need.

## `ARCH-DEC-002` — the alpha ships Fabric 26.2 only; NeoForge, then 1.21.1, are additive fast-follows

**Decided by Kevin, 2026-09-20**: "the alpha ships as Fabric 26.2 only on the existing toolchain;
NeoForge 26.2, then 1.21.1 for both, follow before beta as additive work" (`rulings-2026-09-20.md`).
Reasoning from `multi-loader-multi-version-mods-2026.md`'s own "Fastest path for the alpha" section:

- Loom `1.17.21` and a built `minecraft-merged.jar` for MC 26.2 already sit in
  `~/.gradle/caches/fabric-loom/` on this machine — zero new toolchain downloads for the alpha.
- NeoForge MDG (`2.0.147`), Stonecutter (`0.9.8`), and `loom-back-compat` (`0.4.2`) are all uncached
  — several hundred MB of first-run downloads, plus a 4-way build matrix to get green before any
  release, if attempted all at once.
- Because `common` is kept clean of Minecraft imports from day one (`ARCH-DEC-001`), the fast-follow
  order is "add `fabric/`'s NeoForge sibling and Stonecutter nodes around the existing `common/`
  code" (`UC-009`), not a rewrite.

**Order after the alpha**: (1) NeoForge 26.2, (2) 1.21.1 for both loaders, (3) real audio replacing
placeholders (`domains/audio.md` `UC-010`) — audio and the platform expansion are independent
tracks and may land in either order relative to each other, but both precede beta.

## `ARCH-DEC-003` — a `VillagerEventBus` in `common`, fed by three source kinds

Following `vault/technical/minecraft/villager-events-sounds-and-emf-compat.md`'s own recommended
pattern (`domains/reaction.md` §3): loader-native events wired through a thin per-loader adapter,
a `common`-module tick poll for the two events with no hook at all (`panic`, `player_staring`), and
mixins for the remaining events behind a shared `VillagerEventSource` interface — one mixin config
per loader, same target methods. The bus itself, the cooldown/selection/silence logic, and the
`DisplayQueue` are all pure Java, package-purity-checked the same way
`create_synthetic_diamonds`' weighted-pick algorithm is (`operations/testing.md`).

## `ARCH-DEC-004` — no renderer or model replacement, ever

Restated from `domains/compat.md` `COMPAT-REQ-001`: this mod's only render-facing code is an added
feature renderer (1.21.1) or a mixin into `extractRenderState` plus a render-state side channel
(26.2), on both loaders — never a subclassed or replaced villager renderer, model, or
`EntityRenderState`.

## Loader adapters, condensed

| Loader | Event wiring | Sound registration | Render-state side channel |
|---|---|---|---|
| Fabric | `fabric-events-interaction-v0`/`fabric-entity-events-v1` natives where they exist; mixins for the rest, one config | `Registry.register(BuiltInRegistries.SOUND_EVENT, ...)` | 1.21.1: `LivingEntityFeatureRendererRegistrationCallback`. 26.2: `FabricRenderState`/`RenderStateDataKey`, mixin at `extractRenderState` TAIL |
| NeoForge | NeoForge event bus natives where they exist; mixins for the rest, a separate config | `DeferredRegister<SoundEvent>` | 1.21.1: `EntityRenderersEvent.AddLayers`. 26.2: `RegisterRenderStateModifiersEvent`/`ContextKey` |

Full per-event hook detail is `domains/reaction.md` §3's table, not repeated here.

## Java, Gradle, and loader/version toolchain (source: research §B, jar- and repo-verified, not inferred)

| | MC 1.21.1 (Java 21) | MC 26.2 (Java 25) |
|---|---|---|
| Fabric Loom | `1.17-SNAPSHOT` (cached: `1.17.21`) | same |
| Fabric Loader | `≥0.19.5` | same |
| Fabric API | `0.116.17+1.21.1` | `0.161.0+26.2` |
| NeoForge MDG | `2.0.147` | same |
| NeoForge version | `21.1.251` | `26.2.0.88` |
| Root Gradle | `9.5.1` (the higher pin, covers both) | same |

## Runtime topology (sheet §3.1)

Client and server processes only, no daemon or file of its own beyond the recipe-equivalent
catalogue JSON and the config file (`contracts/data-contract.md`). Event detection, cooldowns,
selection, and the action-bar push are all server-side. The `common` module's pure logic (selection,
cooldown math, weight/queue arithmetic) has no client/server distinction of its own — it runs
wherever it's called, but this mod calls it only from server-side code paths, consistent with
`ACTORS-002`'s "trust no client input" rule.

## Failure modes with no single owner (sheet §3.6)

| ID | Failure | Response |
|---|---|---|
| `ARCH-FAIL-001` | EMF, ETF, or Fresh Animations missing or an incompatible version | Nothing: all three are soft dependencies with no required-version check at all (`domains/compat.md`). |
| `ARCH-FAIL-002` | Stonecutter's version-node wiring does not fit the restored `common` module boundary as cleanly as `multi-loader-multi-version-mods-2026.md` §C predicts | Falls back to per-version-per-loader physical subprojects (`ARCH-DEC-001`'s cost-if-wrong). |
| `ARCH-FAIL-003` | A mixin target (one of the several `domains/reaction.md` §3 marks `Y`) is renamed by a future Minecraft or Create-unrelated game update | Build fails at compile time for a direct target reference, or mod load fails with a named error for a looser one — never a silent no-op (`domains/reaction.md` `REACTION-FAIL-001`). |
| `ARCH-FAIL-004` | The `common`-module tick poll (`panic`, `player_staring`) costs more per-tick time than expected on a large village | Iterates loaded villagers only, edge-detects per UUID, allocates nothing on the steady-state case (`domains/reaction.md` `REACTION-FAIL-004`) — a config toggle to disable poll-based events entirely is a candidate if this proves insufficient, not built at 1.0. |
