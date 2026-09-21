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

Round 1's three single-speaker `en_US` "medium" (22,050Hz) models, chosen for a spread of timbre
and for licences with no non-commercial or attribution-ambiguous clause (`AUDIO-REQ-004`
hard-excludes; ruled out along the way: `ryan` — CC BY-NC-SA 4.0; `l2arctic` — CC BY-NC 4.0;
`hfc_male`/`hfc_female` — CC BY-NC-SA 4.0; `amy`/`danny` — dataset licence only "see URL", not a
clean citation), plus round 2's addition: a naturally lower-register male voice, per Kevin's ruling
that round 1's samples were all unintelligible and none read as "deep dull nasal"
(`AUDIO-DEC-004`).

| Model | Dataset | Licence | Source |
|---|---|---|---|
| `en_US-joe-medium` | `OHF-Voice/voice-datasets` | **CC0** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/joe/medium/MODEL_CARD) |
| `en_US-kristin-medium` | LibriVox (via brycebeattie.com) | **Public domain** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/kristin/medium/MODEL_CARD) |
| `en_US-norman-medium` | LibriVox | **Public domain** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/norman/medium/MODEL_CARD) |
| `en_GB-northern_english_male-medium` | OpenSLR 83 (Northern English speech corpus) | **CC BY-SA 4.0** | [MODEL_CARD](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_GB/northern_english_male/medium/MODEL_CARD) |

Each model file itself (the `.onnx` weights Piper loads) is published by the `rhasspy/piper-voices`
project; the dataset licence above is what the model card cites as the recording's own source
licence, which is the layer `AUDIO-REQ-004` cares about (no CPML/XTTS-style non-commercial output,
no Freesound CC-BY-NC — none of these four are either).

**`en_GB-northern_english_male-medium` carries a ShareAlike clause**, unlike the other three — worth
a deliberate call, not a silent pick, if this is the one Kevin approves: CC BY-SA 4.0 requires
attribution (fits the existing `NOTICE` discipline) and, arguably, that any adaptation (the Piper
model's own training, arguably this pipeline's rendered `.ogg` output) stay under a compatible
licence. `AUDIO-REQ-004`'s hard-excludes name only non-commercial licences, System Voices, and
Freesound CC-BY-NC — ShareAlike isn't on that list, so this isn't a hard block, but it is a real
obligation an MIT-mod release doesn't otherwise carry, and unlike the other three candidates it
would need `NOTICE` to say more than a name-and-licence line. Flagging here rather than deciding it
here — ruled in the vault spec if Kevin picks this one over the three round-1 holdovers.

Other `en_GB`/`en_US` male candidates checked for round 2 and rejected: `en_GB-alan-medium`
(dataset licence only "see URL", same ambiguity as `amy`/`danny`); `en_GB-aru-medium` (CC BY 4.0,
clean, but 12 speakers — a specific `--speaker` id would need picking, deferred rather than adding
another axis to an already-large sample round).

Models are cached at `tools/voices/.cache/models/<name>/<name>.onnx(.json)`, gitignored; fetch with
`python3 tools/voices/setup.py`.

## Round 3: matching vanilla's own pitch (`AUDIO-DEC-005`)

Kevin, on round 2: "I like norman but would want him a little less deep; can you try the pitch of
the normal villager sound." Measured, rather than guessed — the client's own vanilla assets from
`~/.gradle/caches/fabric-loom/assets/` (an ad-hoc measurement script, not part of this repo or the
pipeline: `AUDIO-REQ-005` bars the pipeline itself from ever touching Mojang assets, and no vanilla
audio byte is copied into the repo or the scratchpad deliverable — measurement only, then
discarded).

**Method**: decode each clip to mono 22050Hz PCM (`sox`), split into 40ms frames (50% overlap),
keep frames whose RMS clears 1% of the clip's own peak RMS ("voiced"), estimate each voiced frame's
f0 by normalized autocorrelation restricted to 70–400Hz, accept a frame only if its autocorrelation
peak clears 0.3, and take the median across every accepted frame pooled from all clips in a group.

**Vanilla reference** — `minecraft/sounds/mob/villager/{idle,haggle}{1,2,3}.ogg` (`haggle` is
vanilla's own trade-interaction sound; there is no separate "trade" file), resolved from
`indexes/26.2-32.json`, 6 clips, 99 accepted voiced frames pooled:

| Clip | Median f0 |
|---|---|
| idle1 | 117.6 Hz |
| idle2 | 101.1 Hz |
| idle3 | 137.2 Hz |
| haggle1 | 174.8 Hz |
| haggle2 | 133.3 Hz |
| haggle3 | 94.4 Hz |
| **pooled (idle+haggle)** | **118.5 Hz** — the vanilla reference used below |

**`en_US-norman-medium`'s own natural pitch** (Piper output, `noise_scale=0 noise_w=0`, *no* sox
chain at all) on the three round-3 lines, grunt removed ("Traded! Nice.", "That hurt!", "Danger!"):
86.1 / 95.0 / 86.5 Hz — already **below** vanilla's 118.5 Hz median. Sweeping `sox pitch {0, -100,
-200, -300}` on the same dry output only moves it further away (86–95 Hz at `0`, down to 77–82 Hz
at `-300`) — pitching *down* from here, which is what round 2's `-300`/`-500` did, moves away from
vanilla, not toward it. **Landing exactly on vanilla's median means pitching *up* roughly +490
cents from Norman's natural voice** — a reversal of `AUDIO-DEC-004`'s "never up." Kevin's response
to that finding: "render it for real" — so round 3 goes all the way there, not just partway.

Round 3's seven named `SOX_CHAINS` (Norman only, full nasal/dull/loudness tail except
`villager_vanilla_no_tempo`, measured on the actual rendered output, 3-line average):

| Chain | `pitch` (cents) | Measured median f0 (3-line avg) | Vanilla reference | Gap |
|---|---|---|---|---|
| `villager_below` | -100 | 84.7 Hz | 118.5 Hz | -33.8 Hz |
| `villager_match` | 0 (Norman's natural pitch) | 90.0 Hz | 118.5 Hz | -28.5 Hz |
| `villager_above` | +100 | 92.0 Hz | 118.5 Hz | -26.5 Hz |
| `villager_up_250` | +250 | 99.3 Hz | 118.5 Hz | -19.2 Hz |
| `villager_up_400` | +400 | 108.8 Hz | 118.5 Hz | -9.7 Hz |
| `villager_vanilla` | +490 (the measured match) | 112.1 Hz | 118.5 Hz | -6.4 Hz |
| `villager_vanilla_no_tempo` | +490, tempo step dropped | 113.8 Hz | 118.5 Hz | -4.7 Hz |

None lands exactly on 118.5 Hz — autocorrelation on short, noisy TTS/vanilla-clip audio isn't
lab-grade precision, and `villager_vanilla`/`villager_vanilla_no_tempo` are within a semitone-ish of
it, the closest of the seven, as expected.

**Intelligibility/chirp check at the extreme end** (objective proxy only, not a substitute for a
listen — Kevin's call, not this tool's): frame-to-frame pitch-lock rate does not degrade going up —
it *improves* (80% of voiced frames pitch-locked at `villager_below`/`match`/`above`, 90.6% at
`villager_vanilla`) and estimates get less erratic (coefficient of variation 67%→45%); no clipping
(`sox stat` max amplitude 0.72 of full scale, both with and without the tempo step). This doesn't
prove `villager_vanilla` doesn't chirp — periodicity-lock can't hear a metallic/robotic timbre — it
only means the checks available here found no red flag. `villager_vanilla_no_tempo` (same +490
cents, `tempo 0.95` dropped) measures almost identically (114.2 Hz vs. 114.2 Hz on
`trade_completed.1`; 118.0 Hz vs. 114.2 Hz on `hurt.2`) and runs ~5% shorter, as expected without
the tempo stretch — if `villager_vanilla` does chirp on a listen, this isolates whether `tempo
0.95` is the cause.

Per-line detail, the preview files (vanilla grunt spliced in for listening context, never
committed), and per-chain durations/RMS are in `voices-samples-3/README.md` (scratchpad, not
committed).

Round 3 also drops the written grunt from the three sample lines' TTS input ("Mrrgh — traded!
Nice." → "Traded! Nice.") since the game now plays the vanilla grunt itself
(`AUDIO-REQ-007`, VV-18) — `SAMPLE_TEXT_OVERRIDES` in `render.py`, sample-round-only for the time
being. VV-18 later changed the catalogue itself to words-only subtitles plus a separate `grunt`
field, making that override equal to the catalogue's own subtitle for all three sample lines; round
four (`AUDIO-DEC-006`) removes it as dead weight rather than keep a no-op override around.

## Round 4: cloning the villager's own voice (`AUDIO-DEC-006`)

See `docs/spec/domains/audio.md` `AUDIO-DEC-006` for the full decision. Engine and weights licence
findings:

* **Chatterbox** (Resemble AI) — code: `resemble-ai/chatterbox` on GitHub, **MIT**
  (`LICENSE` at <https://github.com/resemble-ai/chatterbox/blob/master/LICENSE>, confirmed via
  `gh api repos/resemble-ai/chatterbox` — `license.spdx_id: MIT`, not archived). Weights: the
  `ResembleAI/chatterbox` Hugging Face repository, model card declares **MIT**
  (<https://huggingface.co/ResembleAI/chatterbox>, "License: mit"). Both layers permissive —
  `AUDIO-REQ-004`'s hard-excludes (non-commercial models, System Voices, Freesound CC-BY-NC) do not
  apply.
* PyPI package `chatterbox-tts==0.1.7`, `requires_python >=3.10`; pins `torch==2.6.0`/
  `torchaudio==2.6.0` for `python_version < "3.14"` (and `>=2.9.0` for 3.14+, untested here). Runs
  CPU-only on Apple Silicon (arm64) fine — no CUDA/MPS requirement, verified deterministic
  (identical MD5 output across two runs of the same seed/text/reference on CPU).
* Installed in an isolated venv, `tools/voices/.venv-clone/`, Python 3.11.15 (via `uv python`,
  gitignored, reproducible via `tools/voices/setup.py --clone`/README). One install wrinkle:
  `resemble-perth` (Chatterbox's audio watermarker dependency) imports `pkg_resources`, which
  newer `setuptools` (81+) no longer ships — pin `setuptools<81` in the venv or the watermarker
  silently degrades to `None` (`perth`'s own `except ImportError: PerthImplicitWatermarker = None`,
  no error surfaced until `ChatterboxTTS.from_pretrained` crashes constructing it).
* Piper (round 1–3, `AUDIO-DEC-002`–`005`) stays wired up as the fallback engine — `--engine piper`
  is still `render.py`'s default, unchanged.

Reference-set builder: `tools/voices/reference.py`, the first committed version of the asset-index
resolution logic round 3's f0-measurement script used ad-hoc (never itself committed). Three named
sets tried for round four (`REFERENCE_SETS`): `all` (idle1-3, haggle1-3, yes1-3, no1-3, hit1-4 — no
separate trade/work clips exist; `haggle` already established as vanilla's own trade sound, "Round
3" above), `talking` (idle+haggle+yes only), `idle` (idle1-3 only). Built with 150ms silence between
clips, `norm -3`.

Sample round four's files, f0 table, and Kevin's own ranking-from-spectra are in
`voices-samples-4/README.md` (scratchpad, not committed) and the VV-11 round-four report.

## Round 5: warm and soft, not metallic (`AUDIO-DEC-006` amendment)

Kevin on round four: "the `all` samples sound the best, but I find them a bit too harsh and robotic
and metallic; I rather want them to sound warm and soft, but this is the best round so far." Two
levers, both described in `docs/spec/domains/audio.md` §3's "Reference" and "Timbre target" rows:

* **Reference**: `all_warm` — round four's `all` minus the four `hit*` clips (measured, not
  guessed: `sox ... stat` "Maximum amplitude" 0.89-1.00 for `hit1`-`hit4`, vs. 0.20-0.85 for every
  `idle`/`haggle`/`yes`/`no` clip — the loudest, most clipped source material, and the most likely
  to teach the clone a percussive/metallic edge), with a `lowpass 7000` baked into the finished
  reference *before* the cloning engine ever sees it (`reference.build_reference_wav`'s new
  `lowpass_hz` parameter) — so the model conditions on the villager's formants, not the ogg
  encoder's high-frequency crunch. A `tempo 0.9`-slowed reference variant (`tempo` parameter, same
  function) was also tried: measured clearly **worse** in exploratory trials (2.5-5kHz/150-600kHz
  ratio 0.58-0.59 vs. 0.03-0.06 for the lowpass-only reference, same generation settings, same seed
  formula) — sox's `tempo` (WSOLA time-stretch) apparently reintroduces artifacts rather than
  softening the reference, so it's dropped; only the lowpass-only `all_warm` reference is used.
* **Generation settings**: lower `exaggeration` (round four's 0.5 down to 0.3-0.4, per Kevin's
  ask), `cfg_weight` held at 0.5 (also per Kevin's ask), and a raised `temperature` (Chatterbox's
  own default 0.8; round five tries 1.0 and 1.1) — `render.py`'s new `--temperature` flag
  (`DEFAULT_TEMPERATURE`). Exploratory single-seed trials on 1-2 lines found the 2.5-5kHz/
  150-600kHz ratio **noisy seed-to-seed at this scale** (the same nominal settings produced ratios
  differing by an order of magnitude across different seeds/lines) — not clean enough to pick one
  "best" setting from a handful of samples, so both settings within Kevin's requested range are
  carried through to the full 6-line round-five render rather than narrowed to one.
* **Post-processing** (`CLONE_POST_CHAINS`, `tools/voices/render.py`): three new candidates, all
  aimed at less energy in the 2.5-5kHz "metallic" band relative to 150-600kHz body without starving
  1-2kHz (intelligibility) — `warm` (highpass 80, lowpass 5500, `equalizer 250 1q +3` body lift,
  `equalizer 3200 1.5q -4` metallic-band cut, `treble -6 8000` rolloff, a gentle `compand`,
  normalize), `soft` (`warm` but a darker `lowpass 4500` and `reverb 8 30 20`), `plain-warm` (only
  the two EQ moves — isolates how much of "warm" the EQ alone buys).

18 files (a reduced cross-product, same pattern as round four): groups A/B compare the two
generation settings across all 6 sample lines at chain `warm`; group C compares chains `soft`/
`plain-warm` against `warm` (already in group A) on the 3 short lines only. Files, the ratio table
(each file's 2.5-5kHz/150-600kHz ratio next to round four's `all` ratio for the same line), and
Kevin's pick are in `voices-samples-5/README.md` (scratchpad, not committed) and the VV-11
round-five report.

## Round 6: a human voice, not a vanilla clip (`AUDIO-DEC-006` amendment)

Kevin on round five: "better, but still robotic; also they can't pronounce stuff like 'ouuchh'
properly, sounds like letter salad." Two separate fixes: the reference and the input text
(`docs/spec/domains/audio.md` §3 "Reference"/"Input", `AUDIO-DEC-006`'s round-six amendment).

### References — source, licence, speaker id

Three public-domain/CC0 clips, each normalized (`norm -3`) and lightly lowpassed (`lowpass 7000`,
`reference.build_reference_wav`'s existing `lowpass_hz` parameter — no new reference.py code was
needed, it already builds a reference from an arbitrary list of source clips). None of these files
is committed or redistributed; built in the scratchpad, gitignored.

| Name | Source | Licence | Speaker | Detail |
|---|---|---|---|---|
| `joe_chat` | [OHF-Voice/voice-datasets](https://github.com/OHF-Voice/voice-datasets) (Nabu Casa, Home Assistant "Year of Voice"), `en_US-joe` release zip, `3000000001_3000000300_Chat` category (conversational utterances, not read-aloud) | **CC0** (public domain; repo's own `README.md`: "These datasets are licensed under CC0") | Anonymous crowdsourced volunteer, dataset name "joe" (`en_US`, the same dataset `en_US-joe-medium`'s Piper voice was trained from — round 1-3's `VOICES.md` entry) | 6 concatenated utterance clips (`3000000001`/`002`/`003`/`006`/`007`/`015.webm`, decoded via `ffmpeg`), 150ms silence between, ~26s total |
| `giordano` | [LibriVox](https://librivox.org/) recording of Dostoyevsky's *Short Stories*, ch. 1 "An Honest Thief" (`https://www.archive.org/download/dostoyevskyshortstories_1310_librivox/shortstories_01_dostoyevsky_64kb.mp3`), found via LibriVox's own JSON API (`/api/feed/audiobooks/?id=7724&extended=1`, which names the reader per section) | **Public domain** (LibriVox recordings are dedicated to the public domain; source text itself is 19th-century, public domain) | Greg Giordano (LibriVox reader id 8011) | One continuous 25s clip, 30s-55s into the chapter (skips any intro) |
| `pirie` | LibriVox, same book, ch. 3 "An Unpleasant Predicament" pt. 1 (`.../shortstories_03_dostoyevsky_64kb.mp3`) | **Public domain** (LibriVox) | Bruce Pirie (LibriVox reader id 3699) | One continuous 25s clip, 30s-55s into the chapter |

Mozilla Common Voice was considered and rejected per the ticket's own steer (CC0 but noisy —
crowdsourced single-sentence clips recorded on arbitrary hardware, no consistent quality bar).
`kristin`/`norman` (Piper's other public-domain LibriVox-sourced voices, `VOICES.md`'s original
table) were not reused directly here since their own underlying LibriVox source recordings needed
independent lookup either way — `giordano`/`pirie` serve the same "warm, mid-to-low-register male,
public domain" brief from readable, directly-fetchable LibriVox chapter files.

### Villager EQ chains (post-generation only, `render.py`'s `CLONE_POST_CHAINS`)

`villager_mild`: `equalizer 1200 1q +3 equalizer 2600 1.5q -3 lowpass 6000 bass +2 norm -3` — a
nasal lift and a small cut, centred lower and much gentler than round four/five's chains (1600/
3200Hz, +9/-4 swings), since the reference is a clean human voice now, not something that needs
de-metalling. `villager_pitch`: the same tail with `pitch -150` added. `soft` (round five's chain,
unchanged) stays in as the control.

### The `Grr` → `Grrr` interjection-table entry — "only if it renders"

Checked empirically before adding the entry (`render.py`'s `INTERJECTION_TABLE`): both `"Grr!"` and
`"Grrr!"` render as a bounded, non-trivial 1.12s clip on this engine/reference/settings combination
(CPU, `joe_chat` reference, exag 0.3/cfg 0.5/temp 1.1) — neither silence nor a runaway length, so
the round-six ruling that "Grr" should expand to "Grrr" (a more sustained growl) is kept. No
catalogue line currently uses "Grr" — the entry is precautionary, for a future line.

### Samples

24 files (group A: 3 references × the 6 round-four/five sample lines × `villager_mild`; group B:
the best-measured reference × `villager_mild` × `hurt.1` plus the two lines with an explicit
`spoken` override this round added, `sleep.3`/`killed.3`; group C: the best reference × `soft`/
`villager_pitch` on `hurt.1` and `killed.3`) plus 3 grunt-spliced previews. Ratio table, duration
sanity check for the interjection-heavy lines, and Kevin's pick are in `voices-samples-6/README.md`
(scratchpad, not committed) and the VV-11 round-six report.

## Round 7: giordano confirmed, fixing the tin can and the delivery (`AUDIO-DEC-006` final amendment)

Kevin, choosing between round six's three references: "giordano is the best sample." Then, on
giordano + `villager_mild`: "sometimes he is still hard to understand; also the delivery could be
longer; and it still sounds like someone is speaking into a tin can." **Reference: giordano is
final** — LibriVox, public domain, *Short Stories* by Dostoyevsky, reader **Greg Giordano**, to be
credited by name in `NOTICE` once the batch ships.

### The "tin can" finding

Traced to the chain, not the recording. Every chain through round six had a lowpass well under
10kHz: round five's reference-preparation step applied `lowpass 7000` to *every* candidate
reference regardless of voice (carried over unexamined into round six, including giordano); on top
of that, `villager_mild` added its own `lowpass 6000`. Round seven's reference prep drops the
lowpass entirely (`norm -3` only) — `reference.build_reference_wav` with no `lowpass_hz` argument,
already-existing code, no new parameter needed.

**Also re-sourced at a higher bitrate.** Round six's giordano clip came from
`shortstories_01_dostoyevsky_64kb.mp3` (64kbps, the URL LibriVox's own `listen_url` API field
names). LibriVox/archive.org host multiple encodes per chapter (confirmed via
`https://archive.org/metadata/dostoyevskyshortstories_1310_librivox`): a 64Kbps MP3, a 128Kbps MP3,
and the original VBR MP3 (~128kbps average, `ffprobe`-confirmed `bit_rate=128075`, `sample_rate=
44100`) — round seven uses the VBR original,
`https://archive.org/download/dostoyevskyshortstories_1310_librivox/shortstories_01_dostoyevsky.mp3`.
**Cleanest stretch**: RMS measured across the chapter's opening 90s in 10s windows (all
0.027-0.033, no silence gaps or clipping) — settled on 45s-73s (28s), in the more consistent-RMS
region of that scan, clear articulation, no long pause or obvious room tone.

### New chains (`tools/voices/render.py` `CLONE_POST_CHAINS`)

No lowpass under 10kHz anywhere, no 1200Hz boost — the opposite move from every prior round:

* `open` — `rate -v 44100 highpass 70 equalizer 3200 1.5q -2 treble -1.5 norm -3`
* `open_warm` — `open` plus `bass +2 equalizer 400 1q +1.5` (a little low-end body back)
* `dry` — `rate -v 44100 norm -3` only, the control (not even round four-six's mild EQ)
* `open_tempo` — `open` plus `tempo 0.92` (the delivery-length variant, below)

`rate -v` is an explicit, high-quality resample from Chatterbox's native 24kHz to the shipped
44.1kHz (matches vanilla asset convention, `docs/spec/domains/audio.md` §3 "Output format") —
upsampling only, per the ticket's own constraint, never downsampling below the model's own rate.

### Delivery length

A 4-way trial (`exaggeration` 0.4/0.5 × `cfg_weight` 0.2/0.3, `temperature` 0.8 fixed) on
`baby_grows.3`/`level_up.4` (average duration across both lines, seconds):

| Setting | Avg. duration |
|---|---|
| exag 0.4 / cfg 0.2 | 2.28s |
| exag 0.4 / cfg 0.3 | 2.36s |
| **exag 0.5 / cfg 0.2** | **2.54s (longest)** |
| exag 0.5 / cfg 0.3 | 2.12s |

`exaggeration 0.5` / `cfg_weight 0.2` / `temperature 0.8` wins and is round seven's setting for
every sample below — both round six's durations (`baby_grows.3` 1.36s, `level_up.4` 2.04s on
`pirie`/`villager_mild`) are shorter than every one of these four trial settings on giordano.
`open_tempo` (a further `tempo 0.92` post-stretch) tests going longer still.

### Intelligibility: two seeds per line

Each of the 6 sample lines rendered at two candidate seeds (`render.derive_seed(line_id)`, the real
batch's own seed, and an exploration-only alternate); kept whichever has the higher fraction of
pitch-locked voiced frames (round three's own autocorrelation method), ties broken by the lower
"metallic" ratio. Exploration only, not wired into the pipeline — `render.py`'s committed
`derive_seed` is unchanged (`AUDIO-REQ-006`); which lines actually needed the second seed is
recorded in `voices-samples-7/README.md`.

### Samples

24 files (group A: 6 core lines × 2 seeds × chain `open`, seed selection; group B: each line's
winning seed × chain `open_warm`; group C: 3 short lines' winning seed × chain `dry`; group D: the
2 delivery-length test lines plus `trade_completed.1`, winning seed × chain `open_tempo`) plus 3
grunt-spliced previews. Ratio/pitch-lock/duration table and Kevin's pick are in
`voices-samples-7/README.md` (scratchpad, not committed) and the VV-11 round-seven report.

## Round 8: noise and hollowness, `open_warm` decided (`AUDIO-DEC-006` final amendment)

Kevin on round seven: "open_warm is good, but the audio still sounds a bit noisy/hollow." Chain
**family** decided: `open_warm`. Delivery setting unchanged: exaggeration 0.5 / cfg_weight 0.2 /
temperature 0.8.

### Noise floor, measured first

Quietest-100ms-window RMS across round seven's `open_warm` samples (mono, 44.1kHz):

| File | Noise floor |
|---|---|
| `baby_grows.3.seed2.open_warm.ogg` | -90.4 dBFS |
| `hurt.2.seed1.open_warm.ogg` | **-54.9 dBFS** |
| `level_up.3.seed2.open_warm.ogg` | -89.0 dBFS |
| `level_up.4.seed2.open_warm.ogg` | -91.2 dBFS |
| `panic.2.seed2.open_warm.ogg` | -90.7 dBFS |
| `trade_completed.1.seed2.open_warm.ogg` | -92.8 dBFS |

Five of six lines are already near-silent in their quietest window; `hurt.2` stands out with a real,
audible floor. Its spectrum in that 100ms window: 70.8% of energy under 200Hz, peak at **60Hz** (the
mains-hum frequency), only 14%/14% in the 4-8kHz/8-16kHz bands and negligible above — a tonal peak,
not broadband hiss, consistent with a room/electrical tone the model picked up from the reference
rather than a generic noise floor across every render.

### Hollow: checked for reverb, found none at this resolution

Scanned the current giordano reference excerpt (28s, no lowpass, round seven) for a decaying tail
after each of its brief internal pauses (100ms-window RMS, fine-grained 0.1s scan): every pause
found (at 3.3s, 7.8s, 9.4s, 14.2s, 15.7s, 19.7s) drops from normal speech level to near-zero RMS
within a single 100ms window, with no intermediate/decaying values visible — no evidence of a room
reverb tail at this resolution. **The excerpt is kept unchanged** (not re-picked to a "drier"
stretch) — "hollow" is treated as more likely a chain/upsample artifact than a room-acoustics one in
this specific recording, worth confirming or refuting on an actual listen.

### Reference denoising and noise profile

No genuine silence exists *within* the 28s cloning excerpt (it was deliberately picked to have
none, round seven). Built a `noiseprof` instead from a clean, genuinely silent stretch elsewhere in
the *same* source recording: 0.1s-0.8s of the same mp3 file, before the reader's first word (RMS
0.00003-0.00005, ~-88 to -90dBFS) — same reader, same room, same session as the cloning excerpt.
Two reference variants built from it: `ref_giordano_original.wav` (unchanged) and
`ref_giordano_denoised.wav` (`noisered <profile> 0.1` applied to the reference itself, "so the
clone does not learn the room") — both used across round eight's samples, not just one.

### New chain: `open_warm_body`

`open_warm` plus low-mid body (`equalizer 300 1q +2`) and top-end presence
(`equalizer 5000 1q +1.5`, `treble +2 10000` restoring a little air above `open_warm`'s own
`treble -1.5` dulling), and a steep-filter upsample (`rate -v -s` instead of `rate -v`) so nothing
aliases on the 24kHz-to-44.1kHz jump. **Chatterbox's native output rate is 24kHz** (confirmed,
`model.sr`), so this is purely an upsample, never downsampling below the model's own rate.
`open_warm_body_gate` adds a soft downward-expander gate
(`compand 0.005,0.1 -55,-70,-40,-40,0,0 -3 -60 0.02`) on top. `noisered` (0.15) is applied to the
generated *output* by the round-eight sample driver directly (not a fixed `CLONE_POST_CHAINS`
entry, since it needs the external, scratchpad-only profile file path) — see
`tools/voices/render.py`'s `CLONE_POST_CHAINS` for the two new committed chains and
`round8_driver.py`'s own comments (scratchpad) for the noisered insertion.

### Samples

18 files: group A is the control (`open_warm`, original reference, 6 core lines); group B is
`open_warm_body` + output noisered + the denoised reference (6 core lines); group C is
`open_warm_body_gate` on the original reference (3 short lines); group D combines gate + noisered +
the denoised reference (3 short lines) — the original, non-denoised reference is kept in two of the
four groups (A, C), not dropped outright. Noise floor (dBFS), ratio, and duration table, plus
Kevin's pick, are in `voices-samples-8/README.md` (scratchpad, not committed) and the VV-11
round-eight report.

## Round 9: `open_warm_mix`, the shipped chain (`AUDIO-DEC-006` final amendment)

Kevin, choosing between round eight's four groups: "a mixture between the control set and the
denoisedref would be good." Round eight's own measurements pointed the way rather than being
overridden: the gate (group C) matched or beat the control on every line's "metallic" ratio and
`noisered` (groups B/D) carried the only measurable ratio cost — so round nine keeps the body EQ
and the gate, and dials `noisered` back to a lighter strength rather than dropping it.

### The chain

`open_warm_mix` (`tools/voices/render.py` `CLONE_POST_CHAINS`): `open_warm_body`'s low-mid body
(`equalizer 300 1q +2`) and top-end presence (`equalizer 5000 1q +1.5`, `treble +2 10000`) stay
byte-for-byte unchanged, followed by a soft, shallow gate only —

```
compand 0.03,0.2 -70,-74,-45,-47,-25,-25,0,0 0 -90 0.1
```

— a much gentler transfer than `open_warm_body_gate`'s `-55,-70,-40,-40,0,0` (a hard -15dB
expansion at -55dB in): the quietest region (-70dB in) is trimmed to -74dB (-4dB), -45dB in to -47dB
(-2dB), and everything from -25dB up (normal speech level) is passed through unchanged. "Trims the
floor by a few dB, never a hard cut" (Kevin's own framing for the ruling) is a literal description
of this transfer function, not just a summary of it.

### The reference and the output-side denoising

The conditioning reference is round eight's **denoised** giordano variant
(`ref_giordano_denoised.wav`: `noisered` applied to the plain, no-lowpass giordano reference itself,
using a profile built from a genuinely silent stretch of the *same* source recording — round eight's
own method, 0.1s-0.8s of the source mp3, before the reader's first word) — not the original,
matching "the denoisedref" half of Kevin's own framing. A second, separate `noisered` pass runs on
each line's *generated output*, at a lighter 0.08-0.10 (down from round eight's exploratory 0.15,
`DEFAULT_NOISERED_AMOUNT = 0.09` at the middle of that range) — the "mixture ... would be good" half:
some of round eight's noise/hollow fix, not all of it at its strongest setting. Both denoising steps
reuse the same noise-profile file; `tools/voices/README.md` "The clone engine" has the exact commands
to build it and run the shipped batch.

### Implementation, committed rather than scratchpad-only

Round eight's `noisered` insertion lived in a one-off `round8_driver.py` (scratchpad, never
committed) because it needed an external profile-file path that doesn't fit `CLONE_POST_CHAINS`'
fixed effects-list shape. Round nine commits the mechanism instead of repeating the scratchpad
pattern: `render.py` gained `--noisered-profile`/`--noisered-amount` CLI flags (both optional, both
`chatterbox`-only) and a small `_apply_noisered` step in `_render_line_clone` that runs *before*
the named `CLONE_POST_CHAINS` command, on the raw Chatterbox output — so `open_warm_mix` works
with or without a profile (without one, it ships only the chain's own gate, not the output-side
`noisered` half of round nine).

### No sample round this time

Round nine is a direct ruling on round eight's already-rendered samples (`voices-samples-8/`), not a
new sample batch — Kevin picked a specific combination of round eight's own measured groups rather
than asking for new exploratory renders. The 64-line batch (`VV-11`'s own scope from here) renders
directly against `open_warm_mix`.
