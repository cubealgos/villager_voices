---
title: "villager_voices spec — public surface: what a datapack, resource pack, or animation pack may rely on"
type: "spec"
category: "villager_voices"
---

# Public surface (`SURFACE`)

| Surface | Stable from | What it is |
|---|---|---|
| Mod id `villager_voices` | 1.0 (alpha carries it already) | Fabric/NeoForge mod id, both loaders (`decisions/DEC-002-name.md`) |
| Reaction catalogue JSON `data/villager_voices/reaction/<event>.json`, 16 files | Alpha | Line list per event: subtitle text, sound event id (`domains/reaction.md` `REACTION-DEC-001`); datapack-overridable |
| Sound event ids `villager_voices:reaction.<event>.<n>` | Alpha | One per catalogue line, 64 at 1.0 (`domains/audio.md` `AUDIO-REQ-001`); a resource pack may replace the `.ogg` behind any id without touching JSON |
| Lang keys `subtitles.villager_voices.reaction.<event>.<n>` | Alpha | Vanilla subtitle text per line, resource-pack-overridable like any translation key |
| EMF animation variable `villager_voices.is_talking` | Fast-follow (ships once EMF integration lands, not the alpha) | Per-entity boolean via `registerUniqueAnimationVariableFactory` (`domains/compat.md` `COMPAT-REQ-004`) |
| Config file (`contracts/data-contract.md`) | Alpha | Cooldowns, category mutes, display toggles, queue timing — all server operator-facing, none of it a save-format concern |
| The action-bar display format `"<profession>: <line>"` | Alpha, proposed | Not itself overridable at 1.0 — a config toggle for format is a candidate later change, not built now (`domains/display.md` `DISPLAY-REQ-004`) |

Not public: the `common`-module `VillagerEventBus`/`DisplayQueue` Java classes' internal shape, the
exact mixin target list (an implementation detail of *how* an event is detected, not a contract any
external pack depends on), the selection algorithm's exact random-draw implementation beyond the
"no immediate repeat" guarantee (`domains/reaction.md` `REACTION-REQ-005`). Versioned by SemVer over
the surface above (`operations/release.md`).

`SURFACE-REQ-001`: a change to a stable surface is a major version.
`SURFACE-REQ-002`: **where** a datapack's catalogue override names a `sound` id that is not a
registered `SoundEvent`, the system shall reject that entry at load with a clear error naming the
missing id (`domains/reaction.md` `REACTION-REQ-012`), never silently drop it or crash the load.
`SURFACE-REQ-003`: a datapack cannot register a wholly new `SoundEvent` through the catalogue JSON
alone — this is a Minecraft registry limit, not a policy choice, and is not a "not yet built"
promise (`domains/reaction.md` §7, `ACTORS-005`).
