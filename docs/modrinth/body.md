# Wait, they talk now?

Villagers react to what happens to them, with voices and text.

## Project settings

| Field | Value |
|---|---|
| Name | Wait, they talk now? |
| Slug | `villager-voices-mod` |
| Summary | Villagers react to what happens to them: a short line above your hotbar and a subtitle, for trades, danger, breeding, sleep, raids and more. |
| Categories | Mobs, Game-Mechanics |
| Licence | MIT |
| Client side | Required |
| Server side | Required |
| Loaders | Fabric |
| Game versions | 26.2 |
| Dependencies | Fabric API (required) |
| Icon | `icon.png` in this folder: this mod's own parchment-and-speech-bubble badge, not the cubealgos Create-family navy one (`just icon` regenerates it, `tools/icon.py`) |
| Links | Source `https://github.com/cubealgos/villager_voices` · Issues `https://github.com/cubealgos/villager_voices/issues` · Origin `https://git.cubealgos.de/cubealgos/villager_voices` |

## Version settings

| Field | Value |
|---|---|
| Version number | `0.1.0-alpha.1+26.2-fabric` |
| Version title | Wait, they talk now? 0.1.0-alpha.1 for Fabric 26.2 |
| Channel | Alpha |
| File | `dist/villager_voices-0.1.0-alpha.1+26.2-fabric.jar` |
| Changelog | `CHANGELOG.md` |

## Body

Villagers have always just stood there. This mod gives them a voice: they now react, in text and
sound, to the things that actually happen to them.

### What it does

Sixteen kinds of moment trigger a reaction — trading, being hurt, being killed, turning into a
zombie villager and being cured, leveling up a trade, restocking, sleeping and waking, a raid
bell ringing nearby, a golem being summoned to defend them, panicking, being stared at, breeding,
and a baby growing up. Each has its own small catalogue of lines, four to a catalogue at launch —
64 lines in total, all written and voiced for this project, never copied from anywhere else.

When a reaction fires, its line appears briefly above your hotbar (`"Farmer: Nice trade!"`-style,
tagged with the villager's profession) and plays as a real registered sound with a vanilla
subtitle behind it — so it shows up for players who rely on subtitles even before real voice
audio exists (see the alpha notice below). Reactions are throttled on three independent
cooldowns — per villager per event, per villager overall, and per player across every villager
they can hear — so a busy trading hall never turns into a wall of noise, and only players close
enough to actually hear a line ever see or hear it.

### The alpha

This is the first release, and it ships deliberately incomplete in two ways:

- **The audio is a placeholder.** Every one of the 64 lines is a real, valid, near-silent sound
  file — the full registration, subtitle and playback path already works exactly as it will once
  real audio exists — but nothing audible plays yet. Recorded voice lines, generated from an
  approved voice model, are a fast-follow, not a cut feature; this listing and this mod's
  `NOTICE` file will disclose that the audio is AI-generated the moment it ships.
- **It's Fabric 26.2 only.** NeoForge 26.2 and then both loaders on 1.21.1 are the next releases,
  additive to this one — nothing here changes shape when they land.

### How to configure

The mod writes `config/villager_voices.json` on first launch with these settings:

| Setting | Default | What it does |
|---|---|---|
| `cooldowns.perVillagerPerEvent` | 60s | How long one villager waits before the same kind of event can react again |
| `cooldowns.perVillagerGlobal` | 5s | How long one villager waits before *any* event can react again |
| `cooldowns.serverRatePerPlayer` | 2s | How often a single player can be shown a line at all, however many villagers are nearby |
| `display.actionBar` | on | The line-above-your-hotbar channel |
| `display.masterVolume` | 1.0 | A multiplier on every line's played volume |
| `display.queueMinHoldTicks` | 30 ticks (1.5s) | How long a line stays on screen before the next queued one replaces it |
| `display.hearingRangeBlocks` | 16 | How close you need to be to see or hear a reaction |

Two more fields round-trip in the file already — `display.subtitleHint` and the per-category
`categories.<trade\|combat\|social\|raid>.muted` toggles — reserved for a near-term release; in
this alpha they're saved and read back correctly but don't change anything yet.

### Compatibility

Nothing here ever touches the villager's model or renderer. If you have Entity Model Features,
Entity Texture Features, or a Fresh Animations-style resource pack installed, your villagers look
exactly as those packs already render them — this mod adds a feature layer alongside, never a
replacement. It also exposes a `villager_voices.is_talking` flag through EMF's own animation-
variable API, so a compatible pack can animate a mouth or head bob while a line plays; no pack
does yet, but the hook is there today for one that adds it.

### Privacy

No telemetry, no update checks, no network calls of any kind — nothing this mod does ever leaves
your machine.

### Requirements

Minecraft 26.2, Fabric Loader, and Fabric API.

### Support

Issues and questions go through the tracker linked above. There's no SLA — this is a small,
actively developed project, and reports are read and answered as time allows.
