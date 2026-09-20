---
title: "villager_voices spec — actors: who triggers, hears, and configures a reaction"
type: "spec"
category: "villager_voices"
---

# 01 — Actors

| ID | Actor | May | May not |
|---|---|---|---|
| `ACTORS-001` | **Player** | Trigger a reaction by trading with, hurting, curing, breeding, or simply standing near a villager; see the resulting line on the action bar and, once real audio exists, hear it and read its vanilla subtitle; mute categories, adjust the queue hold time, and toggle the action bar or subtitles off in config (`domains/display.md`) | Force a specific line to play, or change another player's config from their own client |
| `ACTORS-002` | **Server** | Detect every trigger event server-side, run the cooldown/rate checks, pick a line, play its sound, and push the action-bar text to every player within hearing range, once per event occurrence (`domains/reaction.md`) | Trust any client input for triggering, selecting, or displaying a reaction: exactly like `create_synthetic_diamonds`'s press roll, this is entirely server-authoritative |
| `ACTORS-003` | **Villager** (the entity) | Carry the server-authoritative talking-state flag during the line's sound duration, exposed to EMF for mouth animation (`domains/compat.md`) | Be rendered differently by this mod in any other respect: no renderer replacement, no model change, no new geometry (`04-architecture.md` `ARCH-DEC-002`) |
| `ACTORS-004` | **Fabric / NeoForge loaders** (dependency) | Supply the event hooks, mixin targets, render-state side channels, and sound/subtitle registries this mod builds on, one thin adapter per loader (`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §A, §D4) | Be modified: this mod adds mixins/events/registrations of its own and touches no loader or vanilla source |
| `ACTORS-005` | **Datapack or resource pack author** | Retune or extend an event's line catalogue via `data/villager_voices/reaction/<event>.json`, or override the near-silent placeholder/real `.ogg` files via a resource pack, without touching Java (`domains/reaction.md` `REACTION-DEC-001`) | Register a wholly new `SoundEvent` through data alone — Minecraft's registry system requires that step in code, a hard limit this mod does not work around (`domains/reaction.md` §7) |
| `ACTORS-006` | **Server operator** | Install the mod, edit its config file for cooldowns/categories/display (`contracts/data-contract.md`), remove it without a save-format consequence (`contracts/data-contract.md`) | Configure per-player settings from the server side: category mutes and queue timing are server config, but subtitle/action-bar on-off is a client toggle in `domains/display.md` |
| `ACTORS-007` | **EMF / ETF / Fresh Animations** (soft dependencies) | Read the `villager_voices.is_talking` per-entity animation variable via EMF's public API to animate a mouth, once a pack defines one; render villager textures (ETF) with zero interaction from this mod at all (`domains/compat.md`) | Be required to run this mod at all — every EMF/ETF/Fresh-Animations interaction is soft-dependency, additive, and silently absent when they are not installed (research §D1, §D3) |
| `ACTORS-008` | **Modrinth visitor / potential server operator** | Read the listing, described entirely on this mod's own terms — no Villager News, Element Animation, or Bedrock mentioned anywhere (`decisions/DEC-009-positioning.md`) — and decide whether to install based on that description and the alpha/beta scope stated in it | Expect NeoForge or 1.21.1 builds, or recorded voice audio, at the alpha (`contracts/platform-matrix.md`, `operations/release.md`) |
| `ACTORS-009` | **Contributor** | Build, test, and change the mod under MIT; add a new trigger event or line to the catalogue following `domains/reaction.md`'s data format | Add telemetry, an outbound network call, or non-original audio/text of any kind (`operations/compliance.md`) |

## Findings from writing this

- **`FINDING-1`** Unlike `create_synthetic_diamonds` (server rolls, nothing to see) or the earlier
  Create Fly siblings, this mod's whole point is a player-visible, player-audible surface — the
  action bar and, later, real audio — so `ACTORS-001`'s "may" column is the longest of any sibling
  spec so far, even though the mod adds no screen at all (`domains/display.md`).
- **`FINDING-2`** Only the server ever triggers, selects, or applies a reaction (`ACTORS-002`); the
  client's only role is receiving the action-bar packet and (once EMF is installed) reading a
  render-state variable — the identical "server decides, client only displays" shape
  `create_synthetic_diamonds`' press roll uses, just with a visible/audible result instead of an
  inventory change.
- **`FINDING-3`** `ACTORS-005`'s ceiling — a datapack cannot register a wholly new `SoundEvent` — is
  a real, load-bearing constraint on how far "data-driven" goes here, and is named plainly rather
  than glossed over (`domains/reaction.md` `REACTION-DEC-001`, §7).
- **`FINDING-4`** `ACTORS-007` is this mod's only genuinely optional dependency family: EMF, ETF,
  and Fresh Animations are all soft, all additive, and the mod runs identically — reaction lines,
  action bar, sound, subtitles — with none of the three installed (`domains/compat.md`).
