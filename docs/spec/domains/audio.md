---
title: "villager_voices spec — AUDIO: sound events, the placeholder, and the Piper pipeline"
type: "spec"
category: "villager_voices"
---

# `AUDIO` — sound events, the near-silent placeholder, and the Piper pipeline

## 1. Purpose

How each line becomes a registered `SoundEvent` with a subtitle, what the alpha ships instead of
real audio, and the build-time-only pipeline that eventually produces real `.ogg` files in Kevin's
approved villager timbre. Not which events exist (`domains/reaction.md`) or how a line's text is
displayed (`domains/display.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The server (`ACTORS-002`) plays each line's sound; a contributor (`ACTORS-009`) runs the Piper pipeline and generates registration boilerplate; Kevin approves the timbre from samples before any batch run (`rulings-2026-09-20.md`). |
| **Over time** | Alpha: every line registers a real `SoundEvent` with real subtitle text, backed by a near-silent placeholder `.ogg`. 1.0 follow-up: placeholders are replaced file-for-file by Piper output, no registration change (`UC-010`). |
| **Multiplicity** | 64 lines at 1.0 (`domains/reaction-lines.md`), each its own `SoundEvent`, `sounds.json` entry, lang string, and `.ogg` file — never shared, since a subtitle is bound to the sound event at registration, not chosen at play time (research §B1). |
| **Unwanted** | A line whose `.ogg` is missing at build time (fails asset validation, `AUDIO-FAIL-001`); a shipped voice sample using a barred source (macOS System Voices, any CPML/XTTS-non-commercial model output, Freesound CC-BY-NC — all hard-excluded, research §C). |
| **Not-you** | A server operator who never touches config hears the shipped catalogue at its shipped volume; nothing here reaches out to any network service — Piper runs fully offline at build time only, never bundled or invoked at runtime (`operations/compliance.md`). |

## 3. Enumerations

### Registration per loader (confirmed, research §B2)

```java
// Fabric — docs.fabricmc.net/develop/sounds/custom
Identifier id = Identifier.of(MOD_ID, "reaction.trade_completed.1");
Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));

// NeoForge — docs.neoforged.net/docs/resources/client/sounds
DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MOD_ID);
Holder<SoundEvent> LINE = SOUNDS.register("reaction.trade_completed.1", SoundEvent::createVariableRangeEvent);
```

Both loaders register into the same vanilla `BuiltInRegistries.SOUND_EVENT` and the same
`sounds.json`/lang-file assets — the cross-loader delta is registration boilerplate only, not asset
format (research §B2). All 64 registrations are generated from `domains/reaction-lines.md`'s single
source list, never hand-typed per line, to keep registration, `sounds.json`, and lang in lockstep.

### Playing server-side (confirmed, research §B3)

`ServerLevel.playSeededSound(entity = null, x, y, z, Holder<SoundEvent>, SoundSource.NEUTRAL,
volume, pitch, seed)` — a pure position-based broadcast, not tied to one player's client-side
prediction. `SoundSource.NEUTRAL` ("Friendly Creatures") matches existing villager ambience;
`SoundSource.VOICE`'s vanilla purpose could not be confirmed (research §B3) and is not used without
further verification. Volume tuned to 0.3–0.6 for a close, conversational feel rather than
village-wide audibility (research §B3's recommendation).

### The near-silent placeholder (alpha)

Every line's `.ogg` at alpha is a genuine, valid mono OGG Vorbis file, roughly 0.1–0.3s of
near-zero-amplitude audio (a very soft click, not true digital silence, so the file is
unambiguously present and playable) — never an empty or missing file. Subtitle display does not
depend on audible volume (research §B4), so the placeholder exercises the exact same registration,
`sounds.json`, lang, and playback code path real audio will use later (`AUDIO-DEC-001`).

### The Piper pipeline (`tools/voices/`, proposal — research §C)

| Step | Detail |
|---|---|
| Input | A short nonsense/CV-syllable string per line, authored alongside the line's subtitle text (not the subtitle text itself — Piper needs phonemes to shape, not a coherent sentence it would pronounce as English). |
| Generator | Piper TTS, the **frozen `rhasspy/piper` MIT snapshot** (archived, not the actively-maintained `OHF-Voice/piper1-gpl` GPL-3.0 fork) — build-time tool only, never shipped; ships only its output `.ogg` files (research §C). Which specific voice model, and that voice model's own licence, is **to verify at the first ticket** (research notes voice licences vary per-voice, "many MIT/CC0-ish," not a blanket claim). |
| Seed | Every generation call is seeded, so a given line's output is reproducible byte-for-byte from the same input text and seed. |
| Pitch/tempo | A fixed sox chain applied uniformly to the whole batch: `sox in.wav -r 44100 -c 1 out.ogg pitch 500 tempo 0.92` (+5 semitones, tempo decoupled from pitch) — one register for the whole catalogue, regenerable in one pass if the register needs tuning (research §C). |
| Timbre approval | Kevin approves the timbre from a small sample batch **before** the full 64-line batch runs (`rulings-2026-09-20.md`) — a manual gate, not automated. |
| Output format | Mono OGG Vorbis, 44.1kHz, 16-bit, ~Vorbis quality 5 (~160kbps) — matches vanilla asset convention; a 1–2s line runs roughly 15–40KB (research §B5). |
| Hard excludes | macOS `say`/System Voices (Apple SLA bars public sharing, at any tier); Freesound CC-BY-NC; any CPML/XTTS-style non-commercial model output (research §C, `AUDIO-REQ-004`). |
| File naming | `assets/villager_voices/sounds/reaction/<event>_<n>.ogg`, matching the sound id `villager_voices:reaction.<event>.<n>` 1:1. |

## 4. Use cases

`UC-001`, `UC-010` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `AUDIO-REQ-001` | The system shall register one `SoundEvent`, one `sounds.json` entry, and one lang subtitle key per catalogue line — 64 at 1.0 — never sharing a sound event between lines. | Must | Research §B1 |
| `AUDIO-REQ-002` | The alpha shall ship a valid, playable, near-silent placeholder `.ogg` for every line, carrying the line's real subtitle text from day one. | Must | Kevin, 2026-09-20 (`rulings-2026-09-20.md`) |
| `AUDIO-REQ-003` | Replacing a placeholder with real audio shall require no code, registration, or catalogue-format change — only the `.ogg` file's bytes at the same path. | Must | `UC-010`; `AUDIO-DEC-001` |
| `AUDIO-REQ-004` | The system shall never ship audio generated by a macOS System Voice, a non-commercial-licensed model's output, or Freesound CC-BY-NC content, at any release tier. | Must | Research §C |
| `AUDIO-REQ-005` | The Piper pipeline shall run entirely at build/generation time, never invoked, bundled, or reachable at runtime. | Must | `operations/compliance.md` `COMP-REQ-001` |
| `AUDIO-REQ-006` | Every generated line shall be reproducible from its input text and seed, so a regenerated batch is diffable against the shipped one. | Should | §3 "Seed" |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `AUDIO-FAIL-001` | A catalogue line has no matching `.ogg` file at build time | Build fails, naming the missing line id — never ships a silently-missing sound. |
| `AUDIO-FAIL-002` | The chosen Piper voice model's licence, once verified, turns out incompatible with a public MIT release | Regenerate the batch against a different voice model; the pipeline's seed/sox chain makes this a rerun, not a redesign (`AUDIO-REQ-006`). |
| `AUDIO-FAIL-003` | Kevin does not approve the sample timbre | Sox parameters (`pitch`, `tempo`) are retuned and a new sample batch generated before the full run — the approval gate exists specifically to catch this before 64 lines are committed to a register that needs redoing. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| The specific Piper voice model and its own licence | `AUDIO-REQ-004` | first ticket (explicitly marked "to verify" per research §C) |
| The exact nonsense/CV-syllable input text per line (distinct from its subtitle) | `AUDIO-REQ-006` | first ticket, alongside the pipeline's first real run |
| Whether `SoundSource.NEUTRAL` or `VOICE` is the right category | §3 "Playing server-side" | first ticket — research could not confirm `VOICE`'s vanilla purpose from mapped source |

## 8. Decisions

- `AUDIO-DEC-001` — **Near-silent placeholder sound events from the alpha, real audio dropped in
  later with zero code change.** Decided by Kevin, 2026-09-20: "the reaction system... ships as an
  alpha before any recorded audio exists" (`rulings-2026-09-20.md`). This exercises the full
  registration/subtitle/playback path from day one, so the only 1.0 follow-up work is asset
  generation, not engineering (`UC-010`).
- `AUDIO-DEC-002` — **Piper TTS plus a fixed sox pitch/tempo chain, the frozen MIT snapshot, not the
  GPL fork.** Decided by Kevin, 2026-09-20: "Piper TTS with a sox pitch and tempo shift for the
  villager timbre" (`rulings-2026-09-20.md`); this sheet adds the frozen-vs-fork licence call,
  following `standards/legal/dependency-license-policy.md`'s GPL-avoidance default even though a
  build-time-only tool sits outside that policy's stated scope (bundled dependencies) — the frozen
  MIT snapshot costs nothing to prefer and removes the question entirely. **Cost if wrong**: the
  active GPL fork is a drop-in CLI replacement for the generation step alone; nothing it produces is
  itself GPL-encumbered once rendered to a plain `.ogg` file, but the frozen snapshot avoids ever
  needing that argument.
- `AUDIO-DEC-003` — **macOS System Voices excluded outright.** Decided by Kevin, 2026-09-20 (§ table
  entry "macOS voices excluded (licence)"), confirming the research's own finding that Apple's SLA
  bars public sharing of System Voice output regardless of profit (research §C). No alternative
  considered — this is a hard licence bar, not a quality tradeoff.
