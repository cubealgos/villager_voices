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
| Input | The line's own subtitle text, exactly as written — plain English, no scrambling or nonsense/CV-syllable transform. Round 1's nonsense-syllable input (this row's original text, kept below in history) produced an unintelligible sample; Kevin's own villager phrasing already carries the character ("Mrrgh", "Hmnh", "Ah", ...), so nothing is added on top (`AUDIO-DEC-004`). VV-18 later split the written grunt out of the subtitle into its own `grunt` catalogue field (`AUDIO-REQ-007`), so from that point on this row's "as written" is already grunt-free for every line, not just the sample slice round 3 hand-overrode. |
| Generator | **`chatterbox`, a zero-shot voice-cloning TTS (Resemble AI, MIT code + MIT weights), conditioned on a reference WAV built from vanilla's own villager grunt clips** — the primary engine as of round four (`AUDIO-DEC-006`). `piper` (the frozen `rhasspy/piper` MIT snapshot, archived, not the actively-maintained `OHF-Voice/piper1-gpl` GPL-3.0 fork) is kept as the fallback engine, rounds 1–3's work untouched (`tools/voices/render.py --engine piper`, still the default flag value for backward compatibility). Both are build-time tools only, never shipped; only their output `.ogg` files ship (research §C, `COMP-REQ-001`). |
| Reference (clone engine only) | A single WAV concatenating a chosen set of vanilla villager clips (`idle`/`haggle`/`yes`/`no`/`hit`, no separate trade/work clips exist) with 150ms silence between each, normalized, resolved from the client's own asset cache at generation time (`~/.gradle/caches/fabric-loom/assets/`) and never bundled, committed, or copied into the repo or a deliverable (`COMP-REQ-002`, `tools/voices/reference.py`). Three named sets tried in round four's samples: `all` (every clip), `talking` (idle+haggle+yes only), `idle` (idle only) — `tools/voices/VOICES.md` "Round 4" has the sample table and f0 comparison. |
| Seed | Every generation call is seeded, so a given line's output is reproducible byte-for-byte from the same input text and seed. Piper has no native `--seed` flag (`noise_scale`/`noise_w` pinned to 0 instead, see `tools/voices/README.md` "Determinism"); Chatterbox accepts a torch seed directly, set deterministically per line from a hash of the line id (`render.derive_seed`) so no seed table needs hand-maintaining. |
| Pitch/tempo | A fixed sox chain applied uniformly to the whole batch, retuned after round 1's sample failed intelligibility (`AUDIO-DEC-004`): pitch shifted **down**, never up, for a deep register; a nasal band boost; dulled highs; a touch less low end so "dull" doesn't read as "boomy"; loudness normalized last — `sox in.wav -r 44100 -c 1 out.ogg pitch {-300|-500} equalizer 1600 1.2q +9 treble -10 4000 lowpass 5000 bass -4 tempo 0.95 norm -3`. Two depths are in round 2's sample (`-300` "deep", `-500` "deeper"); the batch runs at whichever Kevin approves. Round 1's chain was `pitch 500 tempo 0.92` (+5 semitones **up**) — the opposite direction, and part of why the sample was unintelligible. |
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
| `AUDIO-REQ-007` | Where a catalogue line names a `grunt` sound event, the system shall play that vanilla event first at the line's position, source and volume, and the line's own sound after the grunt's duration, so the two read as one utterance; a line without a `grunt` plays as before. The grunt is referenced by id only; no vanilla audio is bundled. | Must | `AUDIO-DEC-005` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `AUDIO-FAIL-001` | A catalogue line has no matching `.ogg` file at build time | Build fails, naming the missing line id — never ships a silently-missing sound. |
| `AUDIO-FAIL-002` | The chosen Piper voice model's licence, once verified, turns out incompatible with a public MIT release | Regenerate the batch against a different voice model; the pipeline's seed/sox chain makes this a rerun, not a redesign (`AUDIO-REQ-006`). |
| `AUDIO-FAIL-003` | Kevin does not approve the sample timbre | Sox parameters (`pitch`, `tempo`) are retuned and a new sample batch generated before the full run — the approval gate exists specifically to catch this before 64 lines are committed to a register that needs redoing. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| Which reference set (`all`/`talking`/`idle`), settings (exaggeration/cfg_weight), and post-processing chain (`tail`/`none`) for the clone engine | the batch | round four's samples (`tools/voices/VOICES.md` "Round 4"), Kevin's listen — the engine itself is decided: Chatterbox (`AUDIO-DEC-006`) |

Resolved, kept for history: the input-text question (`AUDIO-REQ-006`) — it is the subtitle verbatim,
not a distinct nonsense string (`AUDIO-DEC-004`, reversing §3 "Input"'s original proposal).
`SoundSource.NEUTRAL` vs. `VOICE` — settled as `NEUTRAL` in `fabric`'s
`ReactionSoundPlayer.java` (`VV-8`); this sheet's §3 "Playing server-side" already recorded the
reasoning, this row only confirms code matches it. The exact Piper pitch depth for Norman
(round 3, `AUDIO-DEC-005`) — superseded by round four's engine change (`AUDIO-DEC-006`); Piper stays
wired up as the fallback engine at round 3's own settings if the clone engine is ever abandoned, but
nothing further tunes its pitch depth while it isn't the active engine.

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
- `AUDIO-DEC-004` — **Round 1's sample rejected outright: plain English lines, a deep/nasal/dull but
  intelligible timbre, never a pitch-up.** Decided by Kevin, 2026-09-20, on hearing the nine round-1
  samples (three voice models × three lines, nonsense-syllable input, `pitch 500 tempo 0.92`):
  "they're all shit, I can't understand a single thing; the villagers in Villager News speak normal
  English with a nasal tone, deep dull voice." Two changes follow directly: (1) Piper's input text
  is the line's own subtitle, plain English, not a nonsense/CV-syllable transform — §3 "Input"
  above; (2) the sox chain pitches **down** for a deep register (`pitch -300` or `pitch -500`, round
  2 samples both), adds a nasal band boost (`equalizer 1600 1.2q +9`), dulls the highs
  (`treble -10 4000`, `lowpass 5000`), pulls the lows back slightly so "dull" doesn't read as
  "boomy" (`bass -4`), keeps `tempo` near round 1's (`0.95` vs. `0.92`), and normalizes loudness
  last (`norm -3`) — §3 "Pitch/tempo" above. **Cost if wrong**: another sample round, same as
  `AUDIO-FAIL-003` already anticipates — the sox chain and input-text rule are both still a
  regenerable, uniform pass over the batch, not a redesign.
- `AUDIO-DEC-005` — **Norman, less deep than -300, pitched to the vanilla villager; the game's own
  grunt replaces the written "Hngh"/"Mrrgh".** Decided by Kevin, 2026-09-20, on round 2's samples:
  "I like norman but would want him a little less deep; try the pitch of the normal villager sound;
  also stuff like 'hrngg' should be replaced with the actual fitting villager sound from the game."
  Three consequences. (1) The voice model is `en_US-norman-medium` (public domain, LibriVox); the
  depth is set by measuring the median fundamental of vanilla's `entity.villager.ambient` clips
  (read from the client's asset index at generation time, never copied) and shifting Norman's
  output to land on it, expected between 0 and -300 cents; round 3 offers three depths around that
  measurement. (2) A line's subtitle text loses its written grunt: "Mrrgh — traded! Nice." becomes
  "Traded! Nice." and the TTS input is those words only. (3) The grunt itself is the game's: each
  catalogue line may name a vanilla `SoundEvent` (`entity.villager.trade`, `.hurt`, `.yes`, `.no`,
  `.ambient`, `.celebrate`, `.death`, `.work_*`) as its `grunt`; the mod plays the grunt first and
  the line after the grunt's length, on the same position and source, as one utterance. Vanilla
  audio is never bundled (`COMP-REQ-002`): the reference is by sound event id, resolved on the
  client from its own assets. **Cost if wrong:** one optional catalogue field, one delayed play in
  the sound player, and a regenerated batch; the placeholder path (`AUDIO-DEC-001`) is untouched.
- `AUDIO-DEC-006` — **Clone the villager's own voice: a permissively licensed zero-shot cloning TTS,
  conditioned on the vanilla villager's grunt clips as the cloning reference; Piper kept as the
  fallback engine.** Decided by Kevin, 2026-09-20, on hearing round 3's pitch-matched Norman: "still
  not good, can't we actually use the villager's voice?" — three Piper rounds (nonsense syllables;
  plain English on four voices pitched down; Norman pitched all the way up to vanilla's own measured
  118.5Hz) were each rejected in turn, and pitch-shifting a human-recorded voice dataset was never
  going to land on the villager's actual (synthetic, sample-based) timbre no matter how precisely
  the fundamental was matched — a different kind of engine was the only way to actually answer the
  request. Chatterbox (Resemble AI) is the pick: MIT-licensed code
  (<https://github.com/resemble-ai/chatterbox>) and MIT-licensed weights
  (<https://huggingface.co/ResembleAI/chatterbox>), both verified directly (`gh api`/model-card
  metadata, not taken on trust) before installing anything, satisfying `AUDIO-REQ-004`'s hard
  exclusion of non-commercial-licensed model output (XTTS/CPML, Fish Speech, F5-TTS weights were
  never candidates for exactly this reason). The cloning reference is built from vanilla's own
  `idle`/`haggle`/`yes`/`no`/`hit` clips, concatenated with silence between and normalized, read from
  the client's own asset cache at generation time — never bundled, committed, or copied anywhere
  persistent (`COMP-REQ-002`, `tools/voices/reference.py`). Piper (rounds 1–3, `AUDIO-DEC-002`–
  `005`) is not removed: it stays the pipeline's fallback engine, `--engine piper` still the default
  flag value, in case the clone engine's output doesn't hold up at the full 64-line batch scale or a
  future Minecraft version's villager audio changes enough to need re-cloning against a different
  reference. **Legal note, recorded as a known grey zone rather than resolved outright**: the
  rendered output is a new synthesis conditioned on Mojang's own audio, not a copy of it — comparable
  to a human voice actor doing an impression of a character after listening to it, which is
  generally understood not to infringe the original recording's copyright on its own. `AUDIO-REQ-004`
  and `COMP-REQ-002`'s existing mitigation already covers the sharper edge of this: no Mojang sample,
  reference WAV, or preview file is ever distributed — only the mod's own rendered `.ogg` output,
  built from a reference that is itself never shipped, committed, or reachable outside the machine
  that generated it. This is not a substitute for actual legal review if the mod's distribution scale
  or visibility changes meaningfully; it is the considered call for an alpha/1.0 hobby release under
  the mod's existing compliance posture. **Cost if wrong:** regenerate the batch against Piper
  instead (a rerun of already-decided, already-tuned settings, `AUDIO-DEC-002`–`005`), or drop the
  cloning reference to a synthetic/non-Mojang source and re-render — neither touches the placeholder
  path (`AUDIO-DEC-001`) or the registration/catalogue format (`AUDIO-REQ-003`).
