---
schema_version: 1
id: 01M2YYTHQAY394VFKJ4TK45RC0
key: VV-8
type: feat
title: Sound and subtitle registration, and the config surface
created_by: kevin
created_at: 2026-09-20T08:27:19Z
---

## Scope

Two of `decisions/DEC-005-alpha-scope.md`'s remaining alpha bullets. First,
`docs/spec/domains/audio.md`: register all 64 `SoundEvent`s (one per catalogue line, generated from
`VV-3`'s catalogue, never hand-typed, `AUDIO-REQ-001`), their `sounds.json` entries, and their
`subtitles.villager_voices.reaction.<event>.<n>` lang keys, each backed by a valid near-silent
placeholder `.ogg` carrying the line's real subtitle text (`AUDIO-DEC-001`, `AUDIO-REQ-002`). Play
server-side via `ServerLevel.playSeededSound(..., SoundSource.NEUTRAL, ...)`
(`audio.md` §3 "Playing server-side"). Second, `docs/spec/contracts/data-contract.md`: the one
config file (cooldowns, category mutes, display toggles, queue timing — `DATA-REQ-002`), format
TBD at this ticket, wiring `VV-2`'s hardcoded cooldown defaults and `VV-7`'s hardcoded display
defaults to operator-overridable values.

## Approach

A generator (build-time or a small annotation-free loop over `VV-3`'s catalogue at mod init) that
registers `Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))`
for all 64 ids, writes the matching `sounds.json` entries and lang strings, and resolves each
catalogue line's now-real broadcast radius back into `VV-7`'s hearing-range check
(`DISPLAY-REQ-002`). Generate every line's placeholder `.ogg`: a genuine mono OGG Vorbis file,
~0.1–0.3s near-zero amplitude, never empty or missing (`audio.md` §3 "The near-silent placeholder").
Config: one file in the server's config folder, JSON5/TOML per the loader's own convention (format
choice recorded in Findings), degrading to shipped defaults on a malformed file rather than
crashing or corrupting anything (`DATA-REQ-004`), carrying an internal schema version from 1.0
(`data-contract.md` "Versioning").

## Acceptance criteria

- [ ] All 64 `SoundEvent`s registered, one `sounds.json` entry and one lang key each, generated from
      `VV-3`'s catalogue, never hand-typed (`AUDIO-REQ-001`).
- [ ] Every line's `.ogg` is a valid, playable file that is genuinely present (`AUDIO-REQ-002`); a
      catalogue line with no matching `.ogg` fails the build naming the missing line id
      (`AUDIO-FAIL-001`).
- [ ] Vanilla subtitles appear for a placeholder sound with "Show Subtitles" on, with zero custom
      HUD code (`display.md` §3 "Subtitle layer").
- [ ] The config file's cooldowns/mutes/display toggles/queue timing round-trip correctly and a
      malformed file falls back to defaults without crashing (`DATA-REQ-002`, `DATA-REQ-004`).
- [ ] `AUDIO-REQ-005`: nothing in this ticket makes a network call of any kind — placeholder
      generation is a build-time-only step, never invoked at runtime.

## Constraints and prior findings

Blocked by `VV-3` (the catalogue this generates registrations from). Real Piper-generated audio
(`VV-11`) replaces these `.ogg` files "file-for-file... no registration, code, or catalogue-format
change" (`AUDIO-REQ-003`) — do not design the registration path around eventually swapping it, it
already satisfies that requirement by construction. `AUDIO-REQ-006`'s reproducibility (seeded
generation) applies to `VV-11`'s real Piper batch, not this ticket's placeholder generator, which
has no voice model to seed.
