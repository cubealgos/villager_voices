---
title: "villager_voices spec — index"
type: "spec"
category: "villager_voices"
repo: "villager_voices"
---

# villager_voices — "Wait, they talk now?" — specification

Villagers react to what happens to them, with voices and text: a multi-loader, multi-version
Minecraft mod (Fabric and NeoForge, 1.21.1 and 26.2) — not a `create_civilization` building block
and not a Create Fly add-on. Kevin's idea, verbatim: "There is one more mod I'd love to build: for
Bedrock there is a new Villager News add-on; I want to build the same thing as a mod for
Fabric/NeoForge/Forge across as many versions as possible... I would love for it to be compatible
with EMF and ETF and Fresh Animations so they also get animated properly"
(`rulings-2026-09-20.md`). The listing, mod, and repository never name that or any other reference
product (`decisions/DEC-009-positioning.md`); the writing and audio are original throughout.

This spec is the distributed-product spec sheet in the chunked format, following
`vault/projects/create_synthetic_diamonds/spec/`'s own shape, adapted for a non-Create,
multi-loader, multi-version mod with a genuinely larger public surface (two loaders, two versions,
16 trigger events, an audio pipeline, a soft-dependency animation API) than any of the four Create
Fly siblings.

| Sheet section | File |
|---|---|
| §1 Document control | this file: identifiers, state, decisions |
| §2 Executive summary and business context | `00-context.md` |
| §3 Product architecture and runtime topology | `04-architecture.md` |
| §4 Domain-driven functional specifications | `01-actors.md`, `02-journeys.md`, `03-glossary.md`, `domains/reaction.md`, `domains/reaction-lines.md`, `domains/display.md`, `domains/audio.md`, `domains/compat.md` |
| §5 Interface contracts and integration | `contracts/platform-matrix.md`, `contracts/public-surface.md`, `contracts/data-contract.md` |
| §6 Compliance, security and governance | `operations/compliance.md` |
| §7 Release engineering, distribution and support | `operations/release.md`, `operations/testing.md` |
| §8 Migration, compatibility and out of scope | `00-context.md` §What it will not do, `contracts/data-contract.md` |
| Appendix: technical blueprints | `04-architecture.md` §Shape |

## Files and state

| File | Domain prefix | State |
|---|---|---|
| `00-context.md` | — | written |
| `01-actors.md` | `ACTORS` | written |
| `02-journeys.md` | `UC` | written |
| `03-glossary.md` | — | written |
| `04-architecture.md` | `ARCH` | written |
| `domains/reaction.md` | `REACTION` | written |
| `domains/reaction-lines.md` | `LINES` | written |
| `domains/display.md` | `DISPLAY` | written |
| `domains/audio.md` | `AUDIO` | written |
| `domains/compat.md` | `COMPAT` | written |
| `contracts/platform-matrix.md` | `PLATFORM` | written |
| `contracts/public-surface.md` | `SURFACE` | written |
| `contracts/data-contract.md` | `DATA` | written |
| `operations/compliance.md` | `COMP` | written |
| `operations/release.md` | `REL` | written |
| `operations/testing.md` | `TEST` | written |

## Identifiers

`<DOMAIN>-<KIND>-<NNN>`: `REACTION-REQ-006`, `DISPLAY-UC-001` (use cases are flat, see below),
`AUDIO-FAIL-001`, `ARCH-DEC-002`. Use cases themselves are `UC-NNN`, flat across the project.
Permanent; a withdrawn item keeps its number.

## Verifications

Every row is a claim in this sheet traced to
`vault/technical/minecraft/multi-loader-multi-version-mods-2026.md` and
`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` (both 2026-09-20, read via
`javap` against the local merged 26.2 jar and Fabric API module jars where noted, otherwise from
official repo/docs sources) — or to this sheet's own direct Modrinth API check (2026-09-20, item
14). A claim not in this table and not in one of those three sources is marked "to verify at the
first ticket" where it appears.

| # | Claim | Section |
|---|---|---|
| 1 | A real `common`/`fabric`/`neoforge` module split (the JEI shape) beats both Architectury and the official Stonecutter multiloader template's own flattened `common` for this project's "zero Minecraft imports in common" requirement | `04-architecture.md` `ARCH-DEC-001` |
| 2 | Toolchain versions per version/loader (Fabric Loom `1.17-SNAPSHOT`, Fabric API `0.116.17+1.21.1`/`0.161.0+26.2`, NeoForge MDG `2.0.147`, NeoForge `21.1.251`/`26.2.0.88`, Java 21/25, Gradle `9.5.1`) are fetched directly from official MDK/template repos, not inferred | `contracts/platform-matrix.md`, `04-architecture.md` |
| 3 | Loom `1.17.21` and a built MC 26.2 jar already sit in this machine's Gradle cache; NeoForge MDG, Stonecutter, and `loom-back-compat` are all uncached | `decisions/DEC-005-alpha-scope.md` |
| 4 | Modrinth's `modrinth-publish.py` sends one jar per `cmd_version` call; a full four-combination release needs four separate invocations, one per loader/version jar | `operations/release.md` |
| 5 | The 16-event hook table (method per event, Fabric API/NeoForge event class where one exists, mixin-or-not) is jar-verified for 26.2, docs-verified for NeoForge, and unverified for 1.21.1 vanilla names (no 1.21.1 jar was available to the research) | `domains/reaction.md` §3 |
| 6 | A `sounds.json` subtitle is bound to the `SoundEvent` at registration, not chosen at play time — each line needs its own registered sound event, `sounds.json` entry, lang string, and `.ogg` file | `domains/audio.md` §3, `domains/reaction-lines.md` §2 |
| 7 | The action-bar method is `Player#displayClientMessage(Component, boolean)` on 1.21.1 and was renamed to `ServerPlayer#sendOverlayMessage(Component)` on 26.2 (confirmed by `javap` and independently by a GeyserMC PR); every call resets a 60-tick/3.0s client timer with no accumulation (confirmed by bytecode read of `Hud.setOverlayMessage`) | `domains/display.md` §3, `decisions/DEC-007-display-channel.md` |
| 8 | `EMFAnimationApi.registerUniqueAnimationVariableFactory` exists and is EMF's intended per-entity external-mod animation-variable mechanism | `domains/compat.md` §3 |
| 9 | Fresh Animations has no confirmed dedicated villager jaw/mouth bone today — inferred from its changelog's absence of one, not from unpacking its model files | `domains/compat.md` §3 "Fresh Animations" |
| 10 | ETF is scoped to textures only; no shared renderer hooks or data with this mod, confirmed | `domains/compat.md` §3 "ETF" |
| 11 | Both loaders already ship a supported "attach keyed data during render-state extraction, read it back in an added layer" mechanism on 26.2 (`FabricRenderState`/`RenderStateDataKey` confirmed by source; NeoForge's `RegisterRenderStateModifiersEvent`/`ContextKey` confirmed by docs only, not raw source) — neither loader needs a custom `EntityRenderState` subclass | `domains/compat.md` §3, `04-architecture.md` "Loader adapters" |
| 12 | The proposed Modrinth slug `villager-voices` is taken (an unrelated resource pack); `villager-voices-mod` and six other alternatives are free — checked directly against the Modrinth API, 2026-09-20, by this spec pass itself | `decisions/DEC-002-name.md` |
| 13 | macOS `say`/System Voice output is barred from any shipped, publicly-shared asset by Apple's own SLA, regardless of profit | `domains/audio.md` §3, `decisions/DEC-006-audio-pipeline.md` |
| 14 | The original `rhasspy/piper` snapshot is MIT and frozen (archived); the actively-maintained `OHF-Voice/piper1-gpl` fork is GPL-3.0 | `domains/audio.md` `AUDIO-DEC-002` |

## Divergences from heimathafen standards

| Standard | Divergence | Recorded in |
|---|---|---|
| `standards/legal/dependency-license-policy.md` | MIT, no CLA | `decisions/DEC-003-licence.md` |
| "no remote unless justified later" | Public on Forgejo under `cubealgos` from the bootstrap, mirrored to GitHub with the issue tracker there | `decisions/DEC-003-licence.md` |
| Kevin's own proposed Modrinth slug (`villager-voices`) | Taken by an unrelated project; an alternative is proposed, not yet confirmed | `decisions/DEC-002-name.md` |

## Decisions

| ID | Decision | State |
|---|---|---|
| `DEC-001` | Distributed product, full spec sheet | written |
| `DEC-002` | Repo and mod id `villager_voices`, title "Wait, they talk now?"; proposed Modrinth slug taken, alternative flagged | written |
| `DEC-003` | MIT, no CLA; public under the cubealgos organisation from the first commit | written |
| `DEC-004` | `common`/`fabric`/`neoforge` module split, Stonecutter for the version axis, the JEI shape not Architectury | written |
| `DEC-005` | Alpha ships the reaction system on Fabric 26.2 only, before any recorded audio | written |
| `DEC-006` | Piper TTS plus a sox pitch/tempo shift, frozen MIT snapshot, macOS voices excluded | written |
| `DEC-007` | The action bar is the primary display channel, with a per-player queue | written |
| `DEC-008` | Never replace the villager renderer or model; expose talking state via EMF's own API | written |
| `DEC-009` | Positioning: described on its own terms, no reference product named anywhere | written |
| `DEC-010` | A data-driven line catalogue for the alpha, not hardcoded; proposed, to confirm at the first ticket | written |

## Open questions gathered

- **The Modrinth slug** — `villager-voices` is taken; `villager-voices-mod` is proposed but not
  confirmed by Kevin (`decisions/DEC-002-name.md`).
- **The icon** — explicitly a proposal, not decided: this mod's own identity, distinct from the
  Create-family navy badge, a speech-bubble or subtitle-caret motif over a villager silhouette is
  the starting idea, not designed in this pass (`rulings-2026-09-20.md`).
- **Cooldown/rate-limit exact defaults** (60s/5s/1-per-2s proposed) and the action-bar minimum
  display time (1.5–2s proposed) — config-overridable regardless, confirmed at the first ticket
  (`domains/reaction.md` §7, `domains/display.md` §7).
- **`player_staring`'s exact detection thresholds** — genuinely new design, no research precedent,
  first ticket (`domains/reaction.md` §7).
- **The Piper voice model and its own licence**, and the exact nonsense/CV-syllable input text per
  line — first ticket (`domains/audio.md` §7).
- **Whether `SoundSource.NEUTRAL` or `VOICE` is correct** for playing lines — `VOICE`'s vanilla
  purpose could not be confirmed from mapped source (`domains/audio.md` §7).
- **The exact `UniqueVariableFactory` wiring against EMF's real interface**, and NeoForge 26.2's
  exact `RegisterRenderStateModifiersEvent` package/class names — both flagged unverified by the
  research itself (`domains/compat.md` §7).
- **The exact catalogue JSON codec field names**, and whether several inferred-not-confirmed hooks
  (`zombified`, `cured`, NeoForge `sleep`/`wake`) hold against a running server (`domains/reaction.md`
  §7).
- **NeoForge's own licence**, needed before `NOTICE` is complete once the NeoForge module lands
  (`operations/compliance.md`).
