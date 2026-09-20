---
title: "villager_voices spec — data contract: the config file, and the one thing persisted"
type: "spec"
category: "villager_voices"
---

# Data contract (`DATA`)

## What this mod persists

**One config file, no world-save state.** Unlike `create_synthetic_diamonds` (nothing persisted at
all), this mod needs server-operator-facing settings — cooldowns, category mutes, display toggles,
queue timing (`domains/display.md` §3's config table) — that must survive a restart and are not
sensibly expressed as datapack content, since they tune behaviour rather than define recipes/lines.
Nothing is written to the world save itself: no memory module, no data component, no NBT tag on any
entity or item, no per-player persisted state beyond ordinary Minecraft player-option handling for
the client-side subtitle/action-bar toggles.

## The talking-state flag is transient, not persisted

The per-villager talking-state boolean (`domains/compat.md` `COMPAT-REQ-002`) lives only in memory
for the duration of a playing sound; it is never written to NBT, never survives a server restart or
chunk unload, and is recomputed fresh (false) whenever a villager entity is (re)loaded. The cooldown
timers (`domains/reaction.md` §3) are the same: in-memory, per-server-session, reset on restart —
a restart producing a brief burst of "fresh" reactions is an accepted, harmless edge case, not a bug
to design against.

## Rules

| ID | Rule |
|---|---|
| `DATA-REQ-001` | The system shall write no memory module, data component, capability, or NBT tag of its own to any entity, block entity, or item stack. |
| `DATA-REQ-002` | The system shall write exactly one config file to the server's config folder (format TBD at the first ticket — JSON5/TOML, matching whatever the loader's own config convention is), plus the 16 catalogue JSON files, which are ordinary datapack content, not runtime state. |
| `DATA-REQ-003` | Cooldown timers and the talking-state flag shall be held in memory only, reset on server restart, never persisted. |
| `DATA-REQ-004` | A malformed config file or catalogue JSON on load shall degrade to "that setting/line uses its shipped default," never a crash or a corrupted save. |

## Versioning

The config file gets an internal schema version field from 1.0 (unlike `create_synthetic_diamonds`,
which had no schema to version at all): a future config key addition is forward-compatible (missing
keys default), and a removed or renamed key is a documented migration note in release notes, not a
silent behaviour change. The 16 catalogue JSON files are versioned the same way any vanilla or
Create Fly recipe-shaped datapack content is — old JSON either still decodes against the current
codec or it does not, with no save-file migration involved, since nothing here is ever written to a
save (mirroring `create_synthetic_diamonds`'s `DATA` contract reasoning for its own recipe files).

## Out of scope (sheet §8)

No record of past reactions anywhere — no "last 10 things this villager said" log, no analytics. No
import of another mod's dialogue or event data; no export beyond what a datapack author can already
read directly from the shipped catalogue JSON files.
