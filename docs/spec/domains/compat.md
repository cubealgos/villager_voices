---
title: "villager_voices spec — COMPAT: the EMF talking variable, never replacing the renderer"
type: "spec"
category: "villager_voices"
---

# `COMPAT` — EMF, ETF, Fresh Animations: never replace the renderer

## 1. Purpose

How this mod exposes a per-villager "talking" state for other mods' animation packs to read,
without ever owning or replacing the villager's own renderer or model. Not the event detection that
sets the flag (`domains/reaction.md`) and not the sound that determines its duration
(`domains/audio.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The server (`ACTORS-002`) sets the flag; the villager entity (`ACTORS-003`) carries it for the sound's duration; EMF/ETF/Fresh Animations (`ACTORS-007`) read it, entirely as soft, optional dependencies. |
| **Over time** | A line's sound starts → the flag goes true on the server → each loader's render-state side channel copies it into that frame's immutable render-state snapshot during `extractRenderState` → EMF's registered variable factory reads it back → the sound ends → the flag goes false. |
| **Multiplicity** | One boolean per villager, no per-line variation, no per-animation-pack special-casing — one variable, `villager_voices.is_talking`, for any pack to read however it likes. |
| **Unwanted** | A pack expecting a richer signal (which line, which category) than the single boolean this mod exposes — out of scope, a flat "is talking" flag is judged sufficient for a mouth-movement cue (§8 `COMPAT-DEC-002`). |
| **Not-you** | A player with none of EMF/ETF/Fresh Animations installed sees the exact vanilla villager model and texture, unaffected in any way — the flag exists, is set, and is simply never read by anything (`ACTORS-007`). |

## 3. Enumerations

### `ARCH-DEC-002`-equivalent for this domain: never replace the renderer

Confirmed per loader (research §D4):

| Loader/version | Mechanism | Confirmed how |
|---|---|---|
| Fabric 1.21.1 | `LivingEntityFeatureRendererRegistrationCallback` (`fabric-rendering-v1`) — filter to villager, **add** a `FeatureRenderer`, never replace the base renderer | Confirmed against Fabric API source |
| NeoForge 1.21.1 | `EntityRenderersEvent.AddLayers` (client mod bus) — `addLayer` on the existing renderer | Confirmed against NeoForge docs |
| Fabric 26.2 | `FabricRenderState`/`RenderStateDataKey` — mixin into `VillagerRenderer`/`ZombieVillagerRenderer`'s `extractRenderState`, `state.setData(IS_TALKING_KEY, ...)` at TAIL; a `FeatureRenderer` reads it back in `submit(...)` | Confirmed by direct source read; Fabric API's own test mod ships the identical worked pattern for a different entity (`PigRendererMixin`) |
| NeoForge 26.2 | `RegisterRenderStateModifiersEvent`, a `ContextKey<T>`, `event.registerEntityModifier(...)`, read back via `EntityRenderState#getRenderData...` | Summarized from a docs read (NeoForge issue #1638), **not** raw-source-confirmed like the Fabric side — re-confirm exact 1.21.1-era package names at implementation time |

**Why 26.2 needs this at all**: 26.2 replaced live-entity rendering with an immutable per-frame
render-state snapshot (`VillagerRenderState`/`ZombieVillagerRenderState`, confirmed via `javap` on
the merged jar); a feature renderer cannot read the live entity at render time any more, so the
talking flag must be copied into the snapshot during extraction — exactly the side-channel pattern
both loaders already ship (research §D4).

### The EMF variable

`traben.entity_model_features.EMFAnimationApi.registerUniqueAnimationVariableFactory(sourceModId,
variableName, explanationKeyOrText, UniqueVariableFactory)` — the per-entity-context variant, paired
with `EMFAnimationApi.getCurrentEntity()` inside the factory (research §D1). Registered as
`villager_voices.is_talking`, a boolean, true for the exact duration the villager's current line's
sound plays. **Confirmed**: the mechanism exists and is the intended external-mod API (no evidence
of an NBT-introspection alternative, research §D1). **Not verified**: the exact
`UniqueVariableFactory` wiring for per-entity keying against EMF's actual interface — read it before
implementation (research §D1's own flag).

### ETF and Fresh Animations

- **ETF**: scoped to textures only (random/emissive textures, player skin features); no shared
  renderer hooks or data with this mod at all — genuinely a non-issue, confirmed (research §D3).
  Nothing in this mod touches ETF, and nothing needs to.
- **Fresh Animations**: no confirmed dedicated jaw/mouth bone for the villager model today — the
  only villager-specific changelog items found are cosmetic (hat overlay UV, nose offset), and no
  model file was unpacked to check directly (research §D2, labelled inferred not confirmed). The
  reference animation snippet below exists so a pack author — including a future
  Fresh-Animations-style pack once one adds a mouth bone — has a working example to copy, not
  because Fresh Animations supports it today.

### Reference animation snippet (ship in the repo, not registered as a real asset)

A minimal jaw-bone rotation keyed to the variable, illustrative only — a real EMF/CEM animation
JSON authored against whichever mouth-bone name a compatible model eventually uses:

```json
{
  "description": "villager_voices reference snippet — copy into a compatible model's own animation file",
  "condition": "query.variable('villager_voices.is_talking')",
  "bones": {
    "head_mouth": {
      "rotation": ["math.sin(query.anim_time * 4000) * 6", 0, 0]
    }
  }
}
```

Shipped under `docs/` or `examples/` in the repo, not under `assets/`, since it is not a real,
loaded resource of this mod's own — it registers nothing and affects no in-game model.

## 4. Use cases

`UC-008` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `COMPAT-REQ-001` | The system shall never replace, subclass, or duplicate the vanilla villager renderer or model, on any loader or version. | Must | Kevin, 2026-09-20 (`rulings-2026-09-20.md`) |
| `COMPAT-REQ-002` | The system shall set a server-authoritative per-villager talking-state boolean, true for exactly the duration of the villager's currently-playing reaction sound. | Must | §3 "The EMF variable" |
| `COMPAT-REQ-003` | The system shall copy the talking-state flag into each frame's render-state snapshot via the loader-appropriate side channel (§3's table), never via a custom `EntityRenderState` subclass. | Must | Research §D4 |
| `COMPAT-REQ-004` | The system shall expose the flag to EMF as `villager_voices.is_talking` via `registerUniqueAnimationVariableFactory`, soft-dependency, absent with no error when EMF is not installed. | Must | Research §D1 |
| `COMPAT-REQ-005` | The system shall ship a reference animation snippet in the repository, not as a loaded game asset. | Should | §3 "Reference animation snippet" |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `COMPAT-FAIL-001` | EMF is not installed | The flag is still set and copied into the render state; nothing reads it; zero behavioural difference from EMF's absence in any other respect. |
| `COMPAT-FAIL-002` | NeoForge's `RegisterRenderStateModifiersEvent`/`ContextKey` shape differs from the docs-only summary at implementation time | Re-confirm against raw NeoForge source before wiring (research §D4's own flag); the Fabric side is unaffected either way, since the two loaders' side channels are independent. |
| `COMPAT-FAIL-003` | A future EMF release changes `registerUniqueAnimationVariableFactory`'s signature | Build fails at compile time against EMF's API jar (a `compileOnly`/soft dependency), not a silent runtime no-op. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| The exact `UniqueVariableFactory` wiring against EMF's real interface | `COMPAT-REQ-004` | first ticket (research §D1's own flag) |
| NeoForge 26.2's exact `RegisterRenderStateModifiersEvent` package/class names | `COMPAT-REQ-003` (NeoForge row) | first ticket (research §D4's own flag) |

## 8. Decisions

- `COMPAT-DEC-001` — **Render-state side channel, not a custom `EntityRenderState` subclass, on
  26.2.** Both loaders already ship exactly this mechanism (research §D4); a custom subclass would
  be strictly more invasive for no benefit. **Cost if wrong**: if neither side channel works as
  documented, the fallback is a full custom `EntityRenderState` subclass per villager renderer — a
  larger, loader-specific change, not attempted first.
- `COMPAT-DEC-002` — **One flat boolean, not a richer per-line or per-category signal.** A single
  `is_talking` variable is judged sufficient for a mouth-movement cue, matching what EMF's own API
  shape (a `BooleanSupplier`/per-entity factory) is built for. **Cost if wrong**: a second variable
  (e.g. `is_talking_urgently` for combat/raid lines) is additive, not a breaking change to the first.
