# `tools/voices/` — the Piper pipeline

Build-time-only (`AUDIO-REQ-005`, `docs/spec/domains/audio.md`): generates the real `.ogg` files
that replace VV-8's placeholder sound, at their existing paths, with zero code, registration, or
catalogue-format change (`AUDIO-REQ-003`). Never invoked at runtime, never imported by `common` or
`fabric`'s Java sources, never run by a Gradle task or CI.

## Setup (once per machine)

```
brew install sox                    # if not already present
python3 tools/voices/setup.py       # fetches the frozen Piper binary + the 3 candidate voice models
```

`setup.py` needs `gh` (authenticated) and network access — the only thing in this directory that
ever touches the network. Everything it fetches lands under the gitignored
`tools/voices/.cache/`. See `tools/voices/VOICES.md` for exactly what gets fetched, from where, and
under what licence, including two packaging bugs in the upstream archived releases that `setup.py`
works around.

## Running it

```
just voices-sample                    # 3 lines x each candidate model, for Kevin's timbre approval — never ships
just voices-batch en_US-joe-medium    # all 64 lines, once a model is approved
```

`voices-sample` writes to the scratchpad-style `--out` directory (default
`tools/voices/.cache/samples/`) plus a `README.md` there listing every file with its subtitle,
derived input text, and model licence — nothing here is copied into `fabric/`'s resources.

`voices-batch` (`render.py --batch --model <name>`) is the only mode that writes shipped assets: it
renders all 64 lines to
`fabric/src/main/resources/assets/villager_voices/sounds/reaction/<event>_<n>.ogg` and rewrites
`fabric/src/main/resources/assets/villager_voices/sounds.json` so each of the 64 entries' `"sounds"`
list points at its own file instead of the shared placeholder — no other part of `sounds.json`, no
registration, and no catalogue JSON changes (`AUDIO-REQ-003`). **Do not run this before Kevin has
approved a sample timbre** (`AUDIO-FAIL-003`) — the ticket that ordered this pipeline (`VV-11`)
explicitly gates the batch on that approval.

## The pipeline, in order

1. **Input text** (`derive_input_text` in `render.py`): a short nonsense/CV-syllable string per
   line, deterministic from `(line_id, subtitle)` — never the subtitle's own English words
   (audio.md §3 "Input"; Piper needs phonemes to shape, not a sentence it would pronounce as
   English). Editing one line's subtitle changes only that line's input text
   (`test_other_lines_unaffected_by_one_subtitle_edit` in `test_render.py`).
2. **Piper**: `noise_scale=0`, `noise_w=0`, `length_scale=1.0` — fixed uniformly across the whole
   batch. See "Determinism" below for why.
3. **sox**: `sox in.wav -r 44100 -c 1 -C 5 out.ogg pitch 500 tempo 0.92` — the exact chain
   `AUDIO-DEC-002` pins (`pitch 500` = +5 semitones, `tempo` decoupled from pitch), `-C 5` for
   audio.md §3's "~Vorbis quality 5 (~160kbps)" output-format target.

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

`test_render.py` (`just test-tools`) covers `derive_input_text` and `load_catalogue` only — pure
Python, no Piper/sox/network dependency, so it runs the same on a machine that hasn't run
`setup.py`. Actually rendering audio is exercised manually via `just voices-sample`, not by the
test suite.
