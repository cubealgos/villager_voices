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
| Input | The line's own subtitle text, plain English, no scrambling or nonsense/CV-syllable transform (`AUDIO-DEC-004`, still standing) — but no longer strictly *verbatim* as of round six (`AUDIO-DEC-006` amendment): Kevin on round five's output, "they can't pronounce stuff like 'ouuchh' properly, sounds like letter salad." A catalogue line's explicit `spoken` field, when present, is used as-is (`domains/reaction-lines.md` §2); a line without one falls back to `tools/voices/render.py`'s own `normalize_spoken_text` — a known-interjection lookup table, a written-interruption em dash turned into a pause or dropped, a 3+-letter-run collapse, and repeated-punctuation stripping — which is a no-op for the large majority of the catalogue's already-plainly-written subtitles. Only two of the 64 lines needed an explicit `spoken` override (`sleep.3`, `killed.3`; `domains/reaction-lines.md` §3's "Spoken" column has both). VV-18's grunt split (`AUDIO-REQ-007`) still applies underneath this: every subtitle is already grunt-free before any of the above runs. |
| Generator | **`chatterbox`, a zero-shot voice-cloning TTS (Resemble AI, MIT code + MIT weights), conditioned on a reference WAV built from vanilla's own villager grunt clips** — the primary engine as of round four (`AUDIO-DEC-006`). `piper` (the frozen `rhasspy/piper` MIT snapshot, archived, not the actively-maintained `OHF-Voice/piper1-gpl` GPL-3.0 fork) is kept as the fallback engine, rounds 1–3's work untouched (`tools/voices/render.py --engine piper`, still the default flag value for backward compatibility). Both are build-time tools only, never shipped; only their output `.ogg` files ship (research §C, `COMP-REQ-001`). |
| Reference (clone engine only) | **Round six (`AUDIO-DEC-006` amendment): a public-domain human voice, not a vanilla clip.** Kevin, round five: "better, but still robotic; also they can't pronounce stuff like 'ouuchh' properly, sounds like letter salad." Rounds four/five's vanilla-clip reference (`all`/`all_warm`) is retired — a clone of the game's own short, non-speech grunt clips was never going to stop sounding like a clone of short, non-speech grunt clips no matter how it was built or filtered. Round six's reference is instead a single clean 15-30s clip of a public-domain or CC0 human voice (warm, male, mid-to-low register), normalized (`norm -3`) and lightly lowpassed (`lowpass 7000`) the same way the vanilla-clip references were (`tools/voices/reference.py`'s `build_reference_wav`, unchanged function, different source audio) — never bundled, committed, or redistributed; source URL, licence, and speaker id recorded per clip in `tools/voices/VOICES.md` "Round 6". The vanilla grunt reference and its asset-cache resolution machinery (`tools/voices/reference.py`'s `REFERENCE_SETS`) stay in the codebase (harmless, and the `grunt` catalogue field / `AUDIO-REQ-007` splice-before-the-line mechanism is completely unrelated and unaffected) but are no longer used to build the *cloning* reference. |
| Timbre target (round five/six, `AUDIO-DEC-006` amendment) | **Warm and soft, not harsh/robotic/metallic; villager character added back mildly, after generation.** Kevin, round four: "I find them a bit too harsh and robotic and metallic; I rather want them to sound warm and soft, but this is the best round so far" — round five addressed this with generation settings (lower `exaggeration` 0.3-0.4, `cfg_weight` 0.5, raised `temperature` 1.0-1.1 — `render.py`'s `--temperature` flag, `DEFAULT_TEMPERATURE`) and heavier post-processing (`warm`/`soft`/`plain-warm`, `tools/voices/VOICES.md` "Round 5"). Round six keeps the generation settings ("Setting 2": exag 0.3/cfg 0.5/temp 1.1) but, with a human voice as the reference instead of a clone-of-a-clone, redesigns post-processing as a *much milder* pass — adding back just enough nasal villager character on top of an already-clean human voice, not fighting a metallic edge the reference itself no longer has: `villager_mild` (a small nasal-band lift, a small cut, a gentle lowpass, a touch more low end) and `villager_pitch` (`villager_mild` plus a small pitch shift down), both far gentler than round four/five's chains. Measured ratios and Kevin's pick are in `tools/voices/VOICES.md` "Round 6" / `voices-samples-6/README.md` (scratchpad). |
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
| Which specific human-voice reference clip and which post-processing chain (`villager_mild`/`villager_pitch`/`soft`) for the clone engine | the batch | round six's samples (`tools/voices/VOICES.md` "Round 6"), Kevin's listen — the reference *kind* (a public-domain human voice, not a vanilla clip) and the generation settings (exag 0.3/cfg 0.5/temp 1.1, "Setting 2") are decided (`AUDIO-DEC-006` amendment) |

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

  **Round five amendment** (Kevin, 2026-09-20, on round four's samples): "the `all` samples sound
  the best, but I find them a bit too harsh and robotic and metallic; I rather want them to sound
  warm and soft, but this is the best round so far." Two changes, both recorded in §3 above: the
  reference set is decided (`all`, refined to `all_warm` — round four's `all` minus the four clipped
  `hit*` clips, plus a `lowpass 7000` baked into the reference before conditioning; a `tempo 0.9`
  variant was tried and measured worse, dropped); and the timbre target is decided (warm and soft),
  pursued both at generation time (lower `exaggeration`, higher `temperature`, `render.py`'s new
  `--temperature` flag) and in post-processing (`CLONE_POST_CHAINS`' new `warm`/`soft`/`plain-warm`
  candidates).

  **Round six amendment** (Kevin, 2026-09-20, on round five's samples): "better, but still robotic;
  also they can't pronounce stuff like 'ouuchh' properly, sounds like letter salad." Two separate
  problems, two separate fixes, both recorded in §3 above. The lingering "robotic" quality wasn't a
  settings or post-processing problem — round four/five's reference was itself a clone of vanilla's
  own short, non-speech grunt clips, and no amount of pitch-matching, EQ, or generation-parameter
  tuning was ever going to make a clone of that sound like a person talking. Round six drops the
  vanilla-clip reference entirely in favour of a single clean clip of a public-domain human voice,
  and redesigns post-processing as a *much* milder pass on top of it (`villager_mild`/
  `villager_pitch`) rather than the heavier de-metalling round four/five needed. This also
  simplifies the legal note above: round six's cloning reference is no longer conditioned on Mojang
  audio at all, so the "new synthesis conditioned on Mojang's own audio" question doesn't apply to
  it (the unrelated vanilla-grunt-splice mechanism, `AUDIO-REQ-007`, is untouched and was never part
  of that question in the first place). The "letter salad" pronunciation problem is a text problem,
  not a voice problem: `tools/voices/render.py` gains an optional per-line `spoken` catalogue
  override (`domains/reaction-lines.md` §2) plus a fallback `normalize_spoken_text` — a known-
  interjection lookup table, letter-run collapsing, em-dash handling, and repeated-punctuation
  stripping — so a line no longer has to be fed its exact, sometimes expressively-misspelled,
  written subtitle. **Cost if wrong:** the vanilla-clip reference and round four/five's chains are
  still in git history and `tools/voices/reference.py`'s asset-index machinery is untouched, so
  reverting the reference choice alone is a rerun, not a redesign; the `spoken` field is additive
  and optional, so removing it changes nothing else.
