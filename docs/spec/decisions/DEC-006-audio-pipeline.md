---
title: "villager_voices DEC-006 — Piper TTS plus sox pitch/tempo shift, macOS voices excluded"
type: "spec"
category: "villager_voices"
---

# `DEC-006` — Piper TTS plus sox pitch/tempo shift, macOS voices excluded

**Status:** decided by Kevin, 2026-09-20.

"Piper TTS with a sox pitch and tempo shift for the villager timbre; Kevin approves the timbre from
samples before the batch; macOS voices excluded (licence)" (`rulings-2026-09-20.md`). Full pipeline
detail in `domains/audio.md` §3: the frozen MIT `rhasspy/piper` snapshot (not the GPL-3.0
`OHF-Voice/piper1-gpl` fork), a fixed `sox ... pitch 500 tempo 0.92`-shaped chain applied uniformly
across the batch, seeded generation for reproducibility, and a manual timbre-approval gate on a
sample batch before the full 64-line run.

This confirms and slightly narrows the research's own recommended pipeline
(`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §C), which weighed six
methods (macOS `say`, Piper, eSpeak-NG, Coqui TTS, Kevin recording lines directly, Freesound) and
already named Piper as the primary candidate on licensing and effort grounds alone. Kevin's ruling
adds the frozen-vs-GPL-fork specificity and the approval-gate process, and settles macOS `say`
outright — it was already the research's own hard exclude (Apple's SLA bars public sharing of
System Voice output, at any tier), not a live option Kevin overrode.

Alternative considered: Kevin recording nonsense syllables directly, as the research's own
zero-legal-risk baseline. Not rejected outright — the research names it as a good supplement "for a
handful of hero lines even if TTS supplies the bulk" — but not the primary method, since 64 lines of
studio-quality hand recording is a much larger time cost than a seeded, batchable TTS pipeline for a
project whose whole premise is shipping fast. Cost if wrong: the pipeline's seed/sox-chain
reproducibility means a full regeneration against a different voice model or parameter set is a
rerun, not a rewrite, if the approved timbre or the chosen voice model's licence proves unworkable
at the first ticket (`domains/audio.md` `AUDIO-FAIL-002`).
