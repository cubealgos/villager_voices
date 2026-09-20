---
title: "villager_voices spec — journeys: the use cases end to end"
type: "spec"
category: "villager_voices"
---

# 02 — Journeys

Every step names who acts. `UC` ids are flat across the project; domain files reference them. Every
journey below runs on the alpha's single build (Fabric 26.2) unless stated otherwise.

### `UC-001` — Trading triggers a reaction (the alpha's core loop)

Actor: player (`ACTORS-001`) · Goal: see the mod visibly working

| Step | Actor | Action |
|---|---|---|
| 1 | player | Completes a trade with a villager. |
| 2 | server | Detects `trade_completed` via `AbstractVillager.notifyTrade` (Fabric mixin TAIL inject, `domains/reaction.md` `REACTION-REQ-001`). |
| 3 | server | Checks the villager's per-event and global cooldowns and the player's server-wide rate limit; all clear. |
| 4 | server | Picks one of the event's lines at random, excluding the line that played last on this villager for this event (`domains/reaction.md` `REACTION-REQ-005`). |
| 5 | server | Plays the line's registered `SoundEvent` (near-silent placeholder at alpha) at the villager's position, and pushes the line's text to every player within hearing range via the action bar (`domains/display.md` `DISPLAY-REQ-001`). |
| 6 | player | Sees `"Farmer: Mrrgh — traded! Nice."` (or the villager's profession/fallback label) appear on the action bar, and — once "Show Subtitles" is on — the vanilla subtitle HUD shows the same text. |

### `UC-002` — A villager is hurt, then killed

Actor: player · Goal: see combat-category reactions

| Step | Actor | Action |
|---|---|---|
| 1 | player | Attacks a villager. |
| 2 | server | Detects `hurt` via `ServerLivingEntityEvents.AFTER_DAMAGE` (no mixin, `domains/reaction.md` `REACTION-REQ-002`), plays a combat-category line. |
| 3 | player | Kills the villager on a second hit. |
| 4 | server | Detects `killed` via `ServerLivingEntityEvents.AFTER_DEATH`, plays a final combat-category line at the villager's last position before it despawns. |
| 5 | player | Sees two distinct action-bar lines in quick succession, held by the per-player queue's minimum display time rather than overwriting each other instantly (`domains/display.md` `DISPLAY-REQ-003`). |

### `UC-003` — A raid bell rings and several villagers react at once

Actor: player · Goal: see the per-player queue under load

| Step | Actor | Action |
|---|---|---|
| 1 | player | Rings a bell as a raid starts. |
| 2 | server | Detects `raid_bell` via a mixin on `BellBlockEntity.onHit` (`domains/reaction.md` §3), and every nearby non-sleeping, non-muted villager independently rolls a line. |
| 3 | server | Each villager's line enters the same player's per-player queue; the queue holds each line for its minimum display time (proposed default 1.5–2s, research §B6) before advancing, rather than the last write instantly clobbering the one before it (`domains/display.md` `DISPLAY-REQ-003`). |
| 4 | player | Sees a short, readable sequence of alarm lines instead of a single flickered mess — the reason the queue exists at all. |

### `UC-004` — A sleeping villager stays silent

Actor: player · Goal: confirm the silence rule

| Step | Actor | Action |
|---|---|---|
| 1 | player | Approaches a village at night; a hostile mob wanders near a sleeping villager. |
| 2 | server | `panic` would normally trigger, but the villager is asleep; the silence rule suppresses the reaction entirely, regardless of trigger (`domains/reaction.md` `REACTION-REQ-009`). |
| 3 | server | The same villager wakes at dawn; `wake` fires normally, since the silence rule only applies while `LivingEntity.isSleeping()` is true. |
| 4 | player | Never sees a line from a villager that is currently asleep, for any of the 16 events. |

### `UC-005` — A player mutes a category

Actor: player (`ACTORS-001`) · Goal: quiet the trade chatter without losing raid alerts

| Step | Actor | Action |
|---|---|---|
| 1 | player | Edits the mod's config file (or a config-screen mod, if one is installed) to mute the `trade` category. |
| 2 | server | On the next trade, `trade_completed`/`offer_opened` are detected exactly as before, but the muted category is filtered before selection — no line is picked, no sound plays, no action-bar text appears. |
| 3 | player | Still sees `combat`/`social`/`raid` category lines normally, since categories are independent toggles (`domains/reaction.md` §3, `domains/display.md` `DISPLAY-REQ-005`). |

### `UC-006` — Per-villager cooldown prevents spam

Actor: player · Goal: confirm a chatty situation doesn't flood the action bar

| Step | Actor | Action |
|---|---|---|
| 1 | player | Trades with the same villager three times in ten seconds. |
| 2 | server | The first trade produces a line; the second and third are detected but suppressed by the per-villager-per-event cooldown (proposed default, config-overridable, `domains/reaction.md` `REACTION-REQ-006`). |
| 3 | player | Sees exactly one reaction line for the burst of trades, not three. |

### `UC-007` — A datapack author retunes the line catalogue

Actor: datapack or resource pack author (`ACTORS-005`) · Goal: change what villagers say without touching Java

| Step | Actor | Action |
|---|---|---|
| 1 | author | Writes a datapack overriding `data/villager_voices/reaction/trade_completed.json` with a different subset or order of lines drawn from this mod's own already-registered sound events. |
| 2 | server | Loads the override through the same `SimpleJsonResourceReloadListener`-style loader as the shipped defaults; no Java is touched (`domains/reaction.md` `REACTION-REQ-011`). |
| 3 | server | Selection now draws only from the overridden catalogue for that event. |
| 4 | author | Cannot introduce a brand-new `SoundEvent` of their own through this JSON alone — that requires a companion mod that registers it (`domains/reaction.md` §7, `ACTORS-005`). |

### `UC-008` — A player with EMF and a Fresh-Animations-style pack sees a moving mouth

Actor: player · Goal: see visual feedback beyond the action bar, once a compatible pack exists

| Step | Actor | Action |
|---|---|---|
| 1 | player | Has EMF and a resource pack that reads `villager_voices.is_talking` installed (Fresh Animations itself has no confirmed mouth bone today, research §D2 — this journey needs such a pack to exist). |
| 2 | server | A reaction fires; the server sets the villager's talking-state flag true for the sound's duration. |
| 3 | client | The render-state side channel (`FabricRenderState`/`RenderStateDataKey` on Fabric, `RegisterRenderStateModifiersEvent`/`ContextKey` on NeoForge) copies the flag into the render state during `extractRenderState`; EMF's registered `UniqueVariableFactory` reads it back each frame. |
| 4 | player | Sees the villager's mouth animate for the reaction's duration, driven entirely by the pack's own animation JSON reading this mod's published variable — no code in this mod ever touches the model or renderer (`domains/compat.md`). |

### `UC-009` — NeoForge 26.2 ships as a fast-follow, no rewrite

Actor: contributor (`ACTORS-009`) · Goal: add a second loader on top of the alpha's `common` module

| Step | Actor | Action |
|---|---|---|
| 1 | contributor | Adds the `neoforge/` module beside the existing `fabric/` one, wiring the same `common` reaction-system code to NeoForge's native events (`LivingHurtEvent`, `LivingDeathEvent`, `BabyEntitySpawnEvent`) and mixins for the rest (`domains/reaction.md` §3). |
| 2 | contributor | Wires a `RegisterRenderStateModifiersEvent` listener for the talking-state flag (`domains/compat.md`), mirroring the Fabric side. |
| 3 | CI | Builds and game-tests the NeoForge 26.2 combination as a new matrix row (`contracts/platform-matrix.md`). |
| 4 | contributor | Publishes a second Modrinth version for the same mod version, `Loaders: neoforge` (`operations/release.md`). |

No rewrite happens because `common` was kept free of Minecraft imports from day one — the fast-follow
is additive module work, exactly the pattern
`vault/technical/minecraft/multi-loader-multi-version-mods-2026.md`'s "Fastest path for the alpha"
section describes.

### `UC-010` — Real audio replaces the placeholder, no code change

Actor: contributor · Goal: land recorded/generated voice lines after the alpha ships

| Step | Actor | Action |
|---|---|---|
| 1 | contributor | Runs the Piper pipeline (`domains/audio.md` §3) over the full line catalogue, gets Kevin's timbre approval on a sample batch first (`rulings-2026-09-20.md`). |
| 2 | contributor | Replaces each line's near-silent placeholder `.ogg` file with the generated one, at the identical asset path and identical `sounds.json`/lang entries. |
| 3 | server | No registration, no recipe/data change, no version bump to the catalogue format — only the asset bytes changed (`domains/audio.md` `AUDIO-DEC-001`). |
| 4 | player | Hears the real voice line the next time that reaction fires; the subtitle, action-bar text, cooldowns, and selection rule are all unchanged from the alpha. |
