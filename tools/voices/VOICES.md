# Piper: tool licence and candidate voice models (VV-11)

Resolves `docs/spec/domains/audio.md` §7's "to verify" item: the specific Piper voice model and its
own licence (`AUDIO-REQ-004`).

## The tool itself

`AUDIO-DEC-002` already ruled out `OHF-Voice/piper1-gpl` (GPL-3.0) in favour of the frozen,
archived, MIT `rhasspy/piper`. One finding from this ticket that sheet didn't anticipate: **the
PyPI package name `piper-tts` now belongs to the GPL fork**, not the frozen snapshot —
`pip install piper-tts` installs version 1.8.0, whose own `pip show` reports
`Home-page: http://github.com/OHF-voice/piper1-gpl` and `License: GPL-3.0-or-later`. Pinning
`piper-tts==1.2.0` (the last release published before the handoff, MIT) does not resolve on this
machine either: it depends on `piper-phonemize~=1.1.0`, which PyPI carries only as `cp39`–`cp312`
wheels for Linux and macOS **x86_64** — no `cp313`/`cp314` build and no macOS **arm64** (Apple
Silicon) build at all (checked against PyPI's file listing for `piper-phonemize` directly).

So this pipeline uses **the pinned MIT git tag** instead, per the ticket's own fallback wording —
the archived `rhasspy/piper` GitHub release `2023.11.14-2` (MIT; `rhasspy/piper` repo confirmed
`archived: true`, `license.spdx_id: MIT` via `gh api repos/rhasspy/piper`), which ships the `piper`
CLI binary directly, no `pip` involved. Two wrinkles found while wiring this up, both recorded here
so nobody re-discovers them:

* That release's own `piper_macos_aarch64.tar.gz` asset is missing its shared libraries
  (`libespeak-ng*.dylib`, `libpiper_phonemize*.dylib`, `libonnxruntime*.dylib` are simply absent
  from the archive — a packaging bug in that specific asset, not documented upstream). The sibling,
  also-archived, also-MIT `rhasspy/piper-phonemize` repo's later release `2023.11.14-4` ships a
  standalone `piper-phonemize_macos_aarch64.tar.gz` that *does* contain those libraries (same
  project family, same licence, a bugfix build after `piper`'s own last release) — `setup.py`
  fetches both and merges the second's `lib/*.dylib` into the first's directory.
* Despite the `aarch64` name, every binary in both assets is x86_64 Mach-O (`file`/`otool -hv`
  confirm `X86_64`, not `arm64`) — another mislabelling in the archived project. It runs fine under
  Rosetta 2 on Apple Silicon; no code change needed, just don't expect a native arm64 binary.

Exact provenance: `rhasspy/piper@2023.11.14-2` (binary + `espeak-ng-data`) +
`rhasspy/piper-phonemize@2023.11.14-4` (shared libraries) — both MIT, both archived. See
`tools/voices/setup.py` for the fetch.

## Candidate voice models

Three single-speaker `en_US` "medium" (22,050Hz) models from the `rhasspy/piper-voices` Hugging
Face repository, chosen for a spread of timbre and for licences with no non-commercial or
attribution-ambiguous clause (`AUDIO-REQ-004` hard-excludes; ruled out along the way: `ryan` —
CC BY-NC-SA 4.0; `l2arctic` — CC BY-NC 4.0; `hfc_male`/`hfc_female` — CC BY-NC-SA 4.0; `amy`/`danny`
— dataset licence only "see URL", not a clean citation).

| Model | Dataset | Licence | Source |
|---|---|---|---|
| `en_US-joe-medium` | `OHF-Voice/voice-datasets` | **CC0** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/joe/medium/MODEL_CARD) |
| `en_US-kristin-medium` | LibriVox (via brycebeattie.com) | **Public domain** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/kristin/medium/MODEL_CARD) |
| `en_US-norman-medium` | LibriVox | **Public domain** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/norman/medium/MODEL_CARD) |

Each model file itself (the `.onnx` weights Piper loads) is published by the `rhasspy/piper-voices`
project; the dataset licence above is what the model card cites as the recording's own source
licence, which is the layer `AUDIO-REQ-004` cares about (no CPML/XTTS-style non-commercial output,
no Freesound CC-BY-NC — none of these three are either).

Models are cached at `tools/voices/.cache/models/<name>/<name>.onnx(.json)`, gitignored; fetch with
`python3 tools/voices/setup.py`.
