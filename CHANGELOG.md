# Changelog

## Unreleased

- Emotion classes: the 64 voice lines are no longer all cloned from one reference/setting pair.
  Six emotion classes (`calm`, `pleased`, `annoyed`, `hurt`, `alarmed`, `gentle`) are mapped one per
  event (`LINES-DEC-002`), each conditioned on its own matched 15-25s reference segment (scored by
  pitch/energy variance and speaking rate, not picked by ear) and its own Chatterbox
  `exaggeration`/`cfg_weight`, still all from the same public-domain giordano recording and the same
  `open_warm_mix` chain. Every catalogue line carries an optional `mood` field (`common`'s
  `CatalogueCodec`, loader-free, read only by the voice pipeline) so `render.py --batch
  --reference-dir` resolves each line's reference/settings automatically. Fixes lines that
  previously "always sound surprised" regardless of what they're reacting to (`VV-11`).
- Real voice lines: the 64 near-silent placeholder sound events are replaced, file-for-file, by
  AI-generated speech — Chatterbox (Resemble AI, MIT), conditioned on a public-domain human-voice
  reference (Greg Giordano's LibriVox reading of Dostoyevsky's *Short Stories*), the `open_warm_mix`
  post-processing chain (`AUDIO-DEC-006` final amendment). No registration, catalogue, or
  `sounds.json`-shape change — only the `.ogg` bytes at their existing paths (`AUDIO-REQ-003`).
  Disclosed as AI-generated audio in `NOTICE` (`REL-REQ-004`) (`VV-11`).
- Plain spoken lines: all 64 catalogue lines rewritten as natural, under-twelve-word spoken
  sentences — no written grunt, stammer, or interjection string standing in for a sound ("Ow! Ow ow
  ow!", "Zzz.", "Wha—", the "..." trail-offs). Events, per-event counts, and every line's `grunt`
  field are unchanged; the `spoken` field is no longer used by any 1.0 line (`LINES-DEC-001`)
  (`VV-20`).

## 0.1.0-alpha.1+26.2-fabric

The first alpha: villagers react to what happens to them, with a line above your hotbar and a
vanilla subtitle behind it. Fabric, Minecraft 26.2, Fabric Loader ≥0.19.5, Fabric API
0.161.0+26.2.

- All 16 trigger events: `trade_completed`, `offer_opened`, `hurt`, `killed`, `zombified`,
  `cured`, `level_up`, `restock`, `sleep`, `wake`, `raid_bell`, `golem_summoned`, `panic`,
  `player_staring`, `breeding`, `baby_grows` (VV-2, VV-4, VV-5, VV-6).
- The 64-line data-driven catalogue, four lines per event, each with its own registered sound
  event and subtitle, overridable per event by a datapack (VV-3).
- The per-player action-bar queue and display format (VV-7), and the near-silent placeholder
  sound events every line plays through today, backing a real vanilla subtitle from day one
  (VV-8).
- `config/villager_voices.json`, written with shipped defaults on first launch: cooldowns of 60s
  per villager per event, 5s per villager overall, and 2s per player server-wide; the action bar
  on by default; a 1.5s (30-tick) minimum display hold; a 16-block hearing range; and a
  1.0 master volume multiplier. The per-category mute toggles and the subtitle-hint toggle
  round-trip in the file but are not yet wired to any behaviour.
- No recorded audio yet: every line's sound is a genuine, valid, near-silent placeholder — the
  registration/subtitle/playback path is fully live, real voice lines are a later drop-in with no
  code change. Not AI-generated content; no disclosure is due yet (`REL-REQ-004`).
- Bootstrap: `common`/`fabric` module split, CI, docs (VV-1).
