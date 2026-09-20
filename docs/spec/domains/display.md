---
title: "villager_voices spec — DISPLAY: action bar, queue, subtitles, config"
type: "spec"
category: "villager_voices"
---

# `DISPLAY` — the action bar channel, the queue, subtitles, config

## 1. Purpose

What a player actually sees once `domains/reaction.md` selects a line: the action-bar channel and
its per-player queue, the vanilla subtitle layer, the display format, and the config surface that
controls all of it. Not which event fired or which line was picked (`domains/reaction.md`), and not
the sound asset behind a line (`domains/audio.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The server (`ACTORS-002`) pushes action-bar packets and plays sounds; the player (`ACTORS-001`) reads the action bar and, once real audio exists, hears the subtitle; the loaders (`ACTORS-004`) supply the version-specific method name. |
| **Over time** | A selected line → the server calls the version-specific overlay method for every player within hearing range → each recipient's own queue either shows it immediately or holds it behind whatever is already displaying, for at least the minimum hold time, before advancing. |
| **Multiplicity** | One action-bar slot per player, client-side, single-occupancy, last-write-wins at the engine level (research §B6) — the reason a per-player queue exists at all: without it, several villagers reacting within the same few seconds would flicker unreadably. |
| **Unwanted** | Two lines for the same player in the same tick (queued, not dropped); a line for a player who is not within hearing range (never sent to begin with — no client-side filtering needed); a muted category's line reaching a player who muted it (filtered before selection, `domains/reaction.md` §3). |
| **Not-you** | A player standing outside every villager's hearing range sees no action-bar text ever, regardless of how active the village around them is. A player with `subtitles` config off still sees the action bar; a player with the action bar off still gets vanilla subtitles once real audio exists. |

## 3. Enumerations

### The action-bar channel, per version (confirmed, research §B6)

| Version | Method | Notes |
|---|---|---|
| 1.21.1 | `Player#displayClientMessage(Component, boolean actionBar)`, call with `actionBar = true` | Confirmed against Mojang-mapped 1.21.1 source. |
| 26.2 | `ServerPlayer#sendOverlayMessage(Component)` | A 26.1-era rename of the 1.21.1 method (confirmed independently by GeyserMC PR #6271); converges on the identical client-side `Hud.setOverlayMessage` call and the same 60-tick/3.0s timer. |

Both are vanilla `Player`/`ServerPlayer` methods, no Fabric API or NeoForge wrapper needed — the
version split is a naming difference this mod's platform layer (`04-architecture.md`) absorbs, not
a behavioural one.

### The per-player queue

- Every `sendOverlayMessage`/`displayClientMessage(..., true)` call unconditionally resets the
  client's 60-tick (3.0s) timer and replaces the shown text — no accumulation, no interruption
  protection (confirmed by bytecode read of `Hud.setOverlayMessage`, research §B6).
- This mod's server-side queue, one per online player, holds a line on screen for at least a
  **minimum display time** (proposed default 1.5–2s, research's own recommendation) before advancing
  to the next queued line, rather than firing the overlay call unconditionally on every event
  (`DISPLAY-REQ-003`).
- A queue overflow (more lines arrive than can display before the server-wide rate limit,
  `domains/reaction.md` §3, would suppress new detections anyway) is bounded by that same rate
  limit — the queue itself does not need its own separate cap.

### Display format (proposed, confirm at the first ticket)

`"<profession>: <line>"` — e.g. `"Farmer: Mrrgh — traded! Nice."` — using the villager's current
profession display name; a villager with no profession (nitwit, unemployed) falls back to
`"Villager: <line>"`. No player name, no coordinate, no icon: the action bar's width is the limiting
factor, and the profession label is the one piece of context worth the characters it costs.

### Subtitle layer

Fully automatic once a line's `SoundEvent` plays, driven purely by its registered subtitle key,
gated only by the player's own "Show Subtitles" toggle (research §B4) — no custom HUD, no mixin.
Because the alpha's near-silent placeholder sound events already carry real subtitle text
(`domains/audio.md` `AUDIO-DEC-001`), **the subtitle layer is functionally live from the alpha**,
even though nothing audible plays behind it yet — only the action bar is new UI; subtitles are the
engine's own accessibility feature, exercised unchanged.

### Config surface (`contracts/data-contract.md` names the file)

| Key | Controls |
|---|---|
| `categories.<trade\|combat\|social\|raid>.muted` | Per-category mute (`domains/reaction.md` §3) |
| `display.actionBar` | Action-bar channel on/off, independent of the vanilla subtitle toggle |
| `display.subtitleHint` | This mod's own subtitle on/off, in addition to the vanilla accessibility toggle, for players who want the action bar only |
| `display.queueMinHoldTicks` | The per-player queue's minimum display time |
| `display.masterVolume` | A multiplier on every line's played volume, independent of the player's own sound sliders |
| `cooldowns.perVillagerPerEvent` / `.perVillagerGlobal` / `.serverRatePerPlayer` | The three throttles `domains/reaction.md` §3 names |

### Speech bubble (deferred, not built)

An in-world text bubble over the villager's head, via a client render hook per loader, is a real,
scoped follow-up — not alpha or 1.0 scope. The action bar covers "visibly working" for the first
releases (`rulings-2026-09-20.md`, research "Follow-up releases").

## 4. Use cases

`UC-001`, `UC-002`, `UC-003`, `UC-005` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `DISPLAY-REQ-001` | The system shall push a selected line's text to the version-appropriate overlay method for every player within the line's hearing range, server-side, with no client-side mod code required. | Must | Kevin, 2026-09-20 (`rulings-2026-09-20.md`); §3 |
| `DISPLAY-REQ-002` | Hearing range shall be the line's own sound event's effective broadcast radius, so the action-bar and audio channels agree on recipients. | Must | `domains/reaction.md` "Hearing range" |
| `DISPLAY-REQ-003` | The system shall hold each line on a given player's action bar for at least the configured minimum display time before advancing to the next queued line for that player. | Must | Kevin, 2026-09-20; research §B6 |
| `DISPLAY-REQ-004` | The system shall format displayed text as `"<profession>: <line>"`, falling back to `"Villager: <line>"` for a villager with no profession. Proposed, confirm at the first ticket. | Should | §3 |
| `DISPLAY-REQ-005` | The system shall respect independent per-category mute toggles, an action-bar on/off toggle, and a subtitle-hint on/off toggle, all config, all client- or server-scoped as named in §3's table. | Must | Config surface above |
| `DISPLAY-REQ-006` | The system shall never depend on the action bar for subtitle correctness or vice versa: each channel functions if the other is disabled. | Must | §2 "Not-you" |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `DISPLAY-FAIL-001` | A player's client is on a Minecraft version whose overlay method name this mod did not anticipate | Build fails at compile time for that version's module (`contracts/platform-matrix.md` `PLATFORM-REQ-002`-style discipline), not a silent no-display. |
| `DISPLAY-FAIL-002` | Several villagers react on the same tick for the same player, exceeding a short burst | Queue holds each in order for the minimum display time (`DISPLAY-REQ-003`); the server-wide rate limit (`domains/reaction.md` §3) is the upstream backstop that keeps the queue itself from growing unbounded. |
| `DISPLAY-FAIL-003` | A player disables both the action bar and vanilla subtitles | No display of any kind for that player; sound still plays if not separately muted — an accepted, intentional combination, not an error state. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| The exact display format string (`DISPLAY-REQ-004`) | `DISPLAY-REQ-004` | first ticket |
| The minimum display time's exact default (1.5–2s proposed) | `DISPLAY-REQ-003` | first ticket, config-overridable regardless |

## 8. Decisions

- `DISPLAY-DEC-001` — **The action bar is the primary display channel at the alpha, not a
  supplement to subtitles.** Decided by Kevin, 2026-09-20: "the subtitle system for Minecraft sucks
  as display; ... can't we use that additionally?" — resolved as action bar primary, subtitles as
  the accessibility layer once audio exists (`rulings-2026-09-20.md`). **Cost if wrong**: none — both
  channels are independently toggleable (`DISPLAY-REQ-005`), so neither can regress the other.
- `DISPLAY-DEC-002` — **A per-player queue, not an unconditional overlay call per event**
  (this sheet's implementation of Kevin's ruling, following the confirmed engine behaviour in
  research §B6, and the same mitigation pattern the Spigot/Paper `ActionBarMessager` plugin uses).
  **Cost if wrong**: a simpler last-write-wins call is one branch removed, not a redesign, but would
  visibly flicker under `UC-003`'s raid-bell load.
