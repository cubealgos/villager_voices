---
title: "villager_voices spec — glossary"
type: "spec"
category: "villager_voices"
---

# 03 — Glossary

| Term | Means |
|---|---|
| **reaction** | This mod's own concept: a short voice line plus subtitle a villager "speaks" when a trigger event fires on it — trade completed, hurt, zombified, and the rest of `domains/reaction.md`'s 16-event catalogue. Not a vanilla or Create Fly concept. |
| **trigger event** | One of the 16 things a villager can react to (`domains/reaction.md` §3), each with its own per-loader/per-version hook (`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §A). |
| **line** | One specific piece of text plus its own registered `SoundEvent`, `sounds.json` entry, lang subtitle string, and `.ogg` file (or, at alpha, a near-silent placeholder file) — never shared between lines, since a `SoundEvent`'s subtitle is bound at registration, not chosen at play time (research §B1). |
| **line catalogue** | The full set of lines for an event, 3–5 per event, shipped as a datapack JSON file under `data/villager_voices/reaction/<event>.json` (`domains/reaction.md` `REACTION-DEC-001`); a datapack may retune or add to it. |
| **selection rule** | Random among an event's eligible lines, excluding the line that played last for that villager on that event — no immediate repeat (`domains/reaction.md` `REACTION-REQ-005`). |
| **cooldown** | Three independent throttles: per-villager-per-event, per-villager-global, and a server-wide per-player rate (`domains/reaction.md` §3, `REACTION-REQ-006`–`008`); all config defaults, not hard invariants. |
| **hearing range** | The set of players who receive a line's action-bar text and (once real audio exists) hear its sound — the sound's own effective broadcast radius, so the two channels never disagree about who is "close enough" (`domains/display.md` `DISPLAY-REQ-002`). |
| **near-silent placeholder** | The alpha's stand-in audio: a genuine, registered `SoundEvent` with the line's real subtitle text, whose `.ogg` file is a fraction of a second of near-zero-volume audio rather than a recorded line. Subtitle display does not depend on audible volume (research §B4), so the whole accessibility/display pipeline works identically before and after real audio lands (`domains/audio.md` `AUDIO-DEC-001`). |
| **the action bar** | The HUD overlay channel `/title … actionbar` also uses: `Player#displayClientMessage(Component, true)` on 1.21.1, `ServerPlayer#sendOverlayMessage(Component)` on 26.2 (a 26.1-era rename, same wire behaviour) — this mod's primary display channel (`domains/display.md` §3, research §B6). |
| **talking state** | A server-authoritative per-villager boolean, true for the duration a line's sound plays, copied into the render-state snapshot during `extractRenderState` on both loaders and exposed to EMF as `villager_voices.is_talking` via `EMFAnimationApi.registerUniqueAnimationVariableFactory` (`domains/compat.md` §3, research §D1, §D4). |
| **render-state side channel** | The mechanism both loaders already ship for attaching keyed data to an immutable 26.2 render-state snapshot without a custom `EntityRenderState` subclass: Fabric's `FabricRenderState`/`RenderStateDataKey`, NeoForge's `RegisterRenderStateModifiersEvent`/`ContextKey` (research §D4). |
| **`common`/`fabric`/`neoforge` split** | This mod's Gradle module shape: a `common` module with zero Minecraft/loom/moddev imports (the JEI shape, `vault/technical/minecraft/multi-loader-multi-version-mods-2026.md` §A2), plus per-loader modules, with Stonecutter layered on top for the `1.21.1`/`26.2` version axis (§A3, §C). Not Architectury (§A1) and not the official Stonecutter multiloader template's flattened `common` (§A3's caveat). |
| **Piper pipeline** | The build-time-only audio generation tool (`tools/voices/`): short nonsense/CV-syllable text in, one `.ogg` file out, seeded for reproducibility, piped through a fixed sox pitch/tempo chain for the villager timbre (`domains/audio.md` §3, research §C). Never shipped as a runtime dependency — only its output files are. |
