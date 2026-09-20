---
schema_version: 1
id: 01M2YYTQQ87XJJ9PQB07MD0VV3
key: VV-11
type: feat
title: "Real audio: Piper pipeline replacing placeholder sounds"
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

`docs/spec/domains/audio.md`'s deferred half of `AUDIO-DEC-001`: a `tools/voices/` Piper TTS
pipeline (the frozen `rhasspy/piper` MIT snapshot, not the GPL-3.0 `OHF-Voice/piper1-gpl` fork)
generating real `.ogg` files that replace `VV-8`'s 64 placeholders file-for-file, "no code,
registration, or catalogue-format change" (`AUDIO-REQ-003`). Resolves two items the research
explicitly left open: the specific Piper voice model and its own licence (`audio.md` §7, "to
verify"), and the per-line nonsense/CV-syllable input text distinct from each line's subtitle
(`audio.md` §3 "Input"). A fixed sox chain (`pitch 500 tempo 0.92`, +5 semitones) applied uniformly
across the batch; Kevin approves the timbre from a small sample before the full 64-line run
(`AUDIO-FAIL-003`).

## Approach

Author one nonsense/CV-syllable input string per line (64 total), seeded generation so every line
is reproducible byte-for-byte from its input text and seed (`AUDIO-REQ-006`). Run a small sample
batch first for timbre approval; on approval, run the full batch, replacing `VV-8`'s placeholder
`.ogg` files at their existing paths (`assets/villager_voices/sounds/reaction/<event>_<n>.ogg`) with
no other change. Update `NOTICE` and the Modrinth listing to disclose the audio as AI-generated
(`REL-REQ-004`), and credit Piper/the chosen voice model by name and licence.

## Acceptance criteria

- [x] The chosen Piper voice model and its licence are confirmed compatible with a public MIT
      release before the full batch runs (`AUDIO-REQ-004`); if not, `AUDIO-FAIL-002`'s response
      applies (regenerate against a different model — the seed/sox chain makes this a rerun).
- [x] Kevin has approved the sample timbre before the full 64-line batch (`AUDIO-FAIL-003`) — this
      gate is manual, not automatable, and blocks the full run.
- [x] All 64 placeholder `.ogg` files are replaced at their existing paths with zero registration,
      code, or catalogue-format change (`AUDIO-REQ-003`).
- [x] The pipeline itself never runs at build-server or runtime CI — build-time-only, on a
      contributor's own machine (`AUDIO-REQ-005`).
- [x] `NOTICE` and the Modrinth listing disclose Piper-generated audio as AI-generated
      (`REL-REQ-004`).

## Constraints and prior findings

Blocked by `VV-8` (the placeholder registration path this replaces bytes into, unchanged). Hard
excludes, non-negotiable: macOS System Voices (Apple SLA bars public sharing at any tier), any
CPML/XTTS-non-commercial model output, Freesound CC-BY-NC (`AUDIO-REQ-004`). Whether
`SoundSource.NEUTRAL` or `VOICE` is the right playback category is still open per `audio.md` §7 —
resolve it here if not already settled by `VV-8`.
