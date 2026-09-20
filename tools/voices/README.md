# `tools/voices/` — the voice pipeline

Build-time-only (`AUDIO-REQ-005`, `docs/spec/domains/audio.md`): generates the real `.ogg` files
that replace VV-8's placeholder sound, at their existing paths, with zero code, registration, or
catalogue-format change (`AUDIO-REQ-003`). Never invoked at runtime, never imported by `common` or
`fabric`'s Java sources, never run by a Gradle task or CI. Two engines (`--engine`): `piper`
(default, rounds 1–3, kept as the fallback) and `chatterbox` (round four, `AUDIO-DEC-006` — clones
the vanilla villager's own timbre from its grunt clips; see "The clone engine" below).

## Setup (once per machine)

```
brew install sox                    # if not already present
python3 tools/voices/setup.py       # fetches the frozen Piper binary + the 4 candidate voice models
python3 tools/voices/setup.py --clone   # also sets up the Chatterbox clone-engine venv (needs `uv`)
```

`setup.py` needs `gh` (authenticated) and network access — the only thing in this directory that
ever touches the network. Everything it fetches lands under the gitignored
`tools/voices/.cache/`. See `tools/voices/VOICES.md` for exactly what gets fetched, from where, and
under what licence, including two packaging bugs in the upstream archived releases that `setup.py`
works around.

## Running it

```
just voices-sample                             # 3 lines x each candidate model x each chain, for Kevin's timbre approval — never ships
just voices-batch en_US-joe-medium deep         # all 64 lines, once a model AND a chain are approved
```

`voices-sample` writes to the scratchpad-style `--out` directory (default
`tools/voices/.cache/samples/`) plus a `README.md` there listing every file with its subtitle,
chain, and model licence — nothing here is copied into `fabric/`'s resources. Pass `--model` and/or
`--chain` to narrow it to one model or one chain instead of the full cross product.

`voices-batch` (`render.py --batch --model <name> --chain <deep|deeper>`) is the only mode that
writes shipped assets: it renders all 64 lines to
`fabric/src/main/resources/assets/villager_voices/sounds/reaction/<event>_<n>.ogg` and rewrites
`fabric/src/main/resources/assets/villager_voices/sounds.json` so each of the 64 entries' `"sounds"`
list points at its own file instead of the shared placeholder — no other part of `sounds.json`, no
registration, and no catalogue JSON changes (`AUDIO-REQ-003`). **Do not run this before Kevin has
approved a sample timbre** (`AUDIO-FAIL-003`) — the ticket that ordered this pipeline (`VV-11`)
explicitly gates the batch on that approval.

## The clone engine (`--engine chatterbox`, `AUDIO-DEC-006`)

```
tools/voices/.venv-clone/bin/python tools/voices/render.py \
    --sample --engine chatterbox --reference <path-to-reference.wav> \
    [--exaggeration 0.7] [--cfg 0.3] [--chain tail|none]

tools/voices/.venv-clone/bin/python tools/voices/render.py \
    --batch --engine chatterbox --reference <path-to-reference.wav> --chain tail
```

Run with the clone venv's own `python` (`tools/voices/.venv-clone/`, `setup.py --clone`) — this file
still imports cleanly without Chatterbox/torch installed (`test_render.py` covers argument
validation only, never a real render), but actually generating audio needs the venv. `--reference`
is a reference WAV built from vanilla's own villager clips, read from the client's own asset cache
at generation time and never committed (`tools/voices/reference.py`, `COMP-REQ-002`):

```
cd tools/voices && .venv-clone/bin/python -c "
from pathlib import Path
import reference
reference.build_named_reference('talking', Path('/somewhere/outside/the/repo/reference.wav'), Path('/tmp/reference-scratch'))
"
```

`REFERENCE_SETS` in `reference.py` names three: `all` (every villager clip vanilla ships), `talking`
(idle+haggle+yes only — the ones that read as speech), `idle` (idle only, the smallest). `--chain`
selects a `CLONE_POST_CHAINS` entry (`tail`: a light nasal lift + top-end rolloff + normalize;
`none`: raw clone output, format conversion only) — a much lighter touch than Piper's `SOX_CHAINS`,
since the reference itself supplies the villager timbre this time. `--exaggeration`/`--cfg` map
directly to Chatterbox's own `exaggeration`/`cfg_weight` generation parameters (defaults 0.5/0.5).
Every line's torch seed is derived deterministically from its own line id (`render.derive_seed`,
`AUDIO-REQ-006`) so a rerun reproduces byte-identical output (verified: identical MD5 across two
runs of the same seed/text/reference on CPU).

## The pipeline, in order

1. **Input text** (`derive_input_text` in `render.py`): the line's own subtitle, plain English,
   verbatim (audio.md §3 "Input", `AUDIO-DEC-004`). Round 1 fed Piper a scrambled nonsense/CV-
   syllable string instead; Kevin's ruling on hearing it: "they're all shit, I can't understand a
   single thing." `derive_input_text` is now a deliberate no-op, kept as a named function (not
   inlined) so a future line-specific adjustment has one place to land.
2. **Piper**: `noise_scale=0`, `noise_w=0`, `length_scale=1.0` — fixed uniformly across the whole
   batch. See "Determinism" below for why.
3. **sox**, one of two chains (`SOX_CHAINS` in `render.py`), retuned after round 1's sample failed
   intelligibility (`AUDIO-DEC-004` — "the villagers in Villager News speak normal English with a
   nasal tone, deep dull voice"):
   ```
   sox in.wav -r 44100 -c 1 -C 5 out.ogg pitch {-300|-500} equalizer 1600 1.2q +9 treble -10 4000 lowpass 5000 bass -4 tempo 0.95 norm -3
   ```
   `pitch -300`/`-500` — down, never up (round 1's `pitch 500` was +5 semitones **up**, part of why
   it was unintelligible) — `"deep"` and `"deeper"` respectively; `equalizer 1600 1.2q +9` — a nasal
   band boost; `treble -10 4000` + `lowpass 5000` — dulled highs; `bass -4` — lows pulled back so
   "dull" doesn't read as "boomy"; `tempo 0.95` — near round 1's `0.92`; `norm -3` — consistent
   loudness across lines/models. `-C 5` (unchanged) is audio.md §3's "~Vorbis quality 5
   (~160kbps)" output-format target.

## Determinism (`AUDIO-REQ-006`)

audio.md §3 says "every generation call is seeded". The frozen `piper` CLI binary has no `--seed`
flag (checked its `--help` output and its C++ source at the pinned tag — no seed handling anywhere
in `main.cpp` or `piper.cpp`); the VITS model's prosody noise is instead sampled inside the ONNX
graph itself, and two runs of the identical command produced **different** output (`md5sum`
mismatch, different durations) when `noise_scale`/`noise_w` were left at Piper's defaults
(0.667/0.8).

Setting `--noise_scale 0 --noise_w 0` removes that sampling entirely — verified empirically, two
runs of the same input against the same model then produce byte-identical `.wav` output
(`md5sum` match). That's what render.py fixes for every line, so a line's output depends only on
its derived input text and the chosen voice model, satisfying `AUDIO-REQ-006` without a `--seed`
flag this binary doesn't have. The cost is a flatter, less breathy delivery than Piper's default
sampling — an acceptable trade for a pipeline whose whole point is a uniform, regenerable register
(the sox chain already does the actual "villager" shaping).

## AI-generated audio disclosure (`REL-REQ-004`, `operations/compliance.md`)

The shipped `.ogg` files are Piper TTS output, run through a fixed pitch/tempo shift — this is
AI-generated audio and both `NOTICE` and the Modrinth listing disclose it as such. `NOTICE`'s Piper
and voice-model credit is added by this ticket; the Modrinth listing text is VV-15's file to write
(`docs/modrinth/`), not this one's — flag it there rather than duplicating the disclosure text here.

## Tests

`test_render.py` (`just test-tools`) covers `derive_input_text`, `load_catalogue`, `derive_seed`,
`chains_for_engine`, `render_line`'s own argument-validation branches, and (via `reference.py`)
asset-index resolution against a fake fabric-loom asset cache — all pure Python, no Piper/
Chatterbox/sox/network dependency, so it runs the same on a machine that has set up neither engine.
Actually rendering audio (either engine) is exercised manually via `just voices-sample` or the
clone-engine commands above, not by the test suite.
