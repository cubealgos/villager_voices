#!/usr/bin/env python3
"""The voice pipeline (`docs/spec/domains/audio.md` §3, `AUDIO-DEC-004`/`AUDIO-DEC-005`/
`AUDIO-DEC-006`), two engines: `piper` (the original, rounds 1-3) and `chatterbox` (round four's
voice-cloning engine, `AUDIO-DEC-006`, the current default choice though not the CLI default flag
value — see `--engine` below).

Reads the 64-line reaction catalogue (the 16 JSON files under
`fabric/src/main/resources/data/villager_voices/reaction/`), feeds each line's own subtitle to the
chosen engine verbatim (round 1's nonsense/CV-syllable transform was rejected outright —
unintelligible; `AUDIO-DEC-004`), runs the output through a fixed sox chain, and writes mono OGG
Vorbis output.

Two modes, either engine:

* `--sample`: a fixed 3-line slice rendered once per candidate voice model **and** once per
  `--chain` (`piper`: `SOX_CHAINS`; `chatterbox`: `CLONE_POST_CHAINS`), for Kevin's timbre approval
  (`AUDIO-FAIL-003`) — never touches the shipped assets.
* `--batch`: all 64 lines against one approved engine/model-or-reference/`--chain` combination,
  written to their shipped paths
  (`fabric/src/main/resources/assets/villager_voices/sounds/reaction/<event>_<n>.ogg`, audio.md §3
  "File naming"), then rewrites `sounds.json` so each entry points at its own file — the only
  change `AUDIO-REQ-003` allows. Do not run `--batch` before Kevin has approved a sample.

Build-time only (`AUDIO-REQ-005`): nothing here is imported by `common` or `fabric`'s Java sources,
and no Gradle task invokes it. `--engine piper` (the default flag value) requires the `piper` binary
and `sox` on PATH or under `tools/voices/.cache/` (`tools/voices/setup.py`); `--engine chatterbox`
requires `sox` plus the `chatterbox-tts` package (run this file with `tools/voices/.venv-clone/`'s
own `python`, `tools/voices/setup.py --clone`) — see `tools/voices/README.md` for both.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

TOOLS_VOICES = Path(__file__).resolve().parent
REPO_ROOT = TOOLS_VOICES.parent.parent
CATALOGUE_DIR = REPO_ROOT / "fabric" / "src" / "main" / "resources" / "data" / "villager_voices" / "reaction"
SOUNDS_JSON = REPO_ROOT / "fabric" / "src" / "main" / "resources" / "assets" / "villager_voices" / "sounds.json"
SHIPPED_SOUNDS_DIR = REPO_ROOT / "fabric" / "src" / "main" / "resources" / "assets" / "villager_voices" / "sounds" / "reaction"

CACHE_DIR = TOOLS_VOICES / ".cache"
PIPER_DIR = CACHE_DIR / "piper"
PIPER_BIN = PIPER_DIR / "piper"
ESPEAK_DATA = PIPER_DIR / "espeak-ng-data"
MODELS_DIR = CACHE_DIR / "models"

# The fixed sox chains (audio.md §3 "Pitch/tempo", `AUDIO-DEC-004`/`AUDIO-DEC-005`): a nasal EQ
# boost, dulled highs, pulled-back lows so "dull" doesn't read as "boomy", and a final loudness
# normalize, on top of a per-chain pitch shift — applied identically in --sample and --batch so the
# approved sample predicts the batch. Round 1's chain (`pitch 500 tempo 0.92`, pitched *up*) was
# rejected outright as unintelligible; round 2 sampled "deep"/"deeper" (round 1's holdovers, kept
# here for history and regenerability, `AUDIO-REQ-006`); Kevin's round-2 verdict on `en_US-norman-
# medium`, the one model that survived: "a little less deep... try the pitch of the normal villager
# sound." Round 3 measures vanilla's own median f0 (`tools/voices/VOICES.md`, 118.5 Hz) and finds it
# *above* Norman's natural (unshifted) pitch, not below it — so landing on it means pitching **up**,
# reversing `AUDIO-DEC-004`'s "never up" — a deliberate, named exception decided here specifically
# so Kevin can hear the vanilla-matched pitch rather than read about it, not a silent reversal of
# the default: `villager_below`/`villager_match`/`villager_above` (`pitch -100`/`0`/`100`) explore a
# small step either side of Norman's own pitch, and `villager_up_250`/`villager_up_400`/
# `villager_vanilla` (`pitch 250`/`400`/`490`, the last being the measured match) push further, all
# the way to vanilla itself, to see where intelligibility holds up.
SOX_FORMAT_ARGS = ["-r", "44100", "-c", "1", "-C", "5"]  # 44.1kHz mono, ~Vorbis quality 5
_NASAL_DULL_TAIL = [
    "equalizer", "1600", "1.2q", "+9",  # nasal band boost
    "treble", "-10", "4000",  # dulled highs
    "lowpass", "5000",
    "bass", "-4",  # lows pulled back so "dull" doesn't read as "boomy"
    "tempo", "0.95",
    "norm", "-3",  # consistent loudness
]
# Same tail, minus the tempo step — for `villager_vanilla_no_tempo`, to isolate whether `tempo
# 0.95` compounds any chirp/artifact from the extreme +490-cent shift (pitch is time-preserving and
# formant-preserving on its own; stacking a second time-domain effect on top of a large shift is
# where that risk lives).
_NASAL_DULL_TAIL_NO_TEMPO = [
    "equalizer", "1600", "1.2q", "+9",
    "treble", "-10", "4000",
    "lowpass", "5000",
    "bass", "-4",
    "norm", "-3",
]
SOX_CHAINS = {
    "deep": ["pitch", "-300", *_NASAL_DULL_TAIL],
    "deeper": ["pitch", "-500", *_NASAL_DULL_TAIL],
    "villager_below": ["pitch", "-100", *_NASAL_DULL_TAIL],
    "villager_match": ["pitch", "0", *_NASAL_DULL_TAIL],
    "villager_above": ["pitch", "100", *_NASAL_DULL_TAIL],
    "villager_up_250": ["pitch", "250", *_NASAL_DULL_TAIL],
    "villager_up_400": ["pitch", "400", *_NASAL_DULL_TAIL],
    "villager_vanilla": ["pitch", "490", *_NASAL_DULL_TAIL],
    "villager_vanilla_no_tempo": ["pitch", "490", *_NASAL_DULL_TAIL_NO_TEMPO],
}

# Piper synthesis parameters, fixed uniformly across every line (see "Determinism" in
# tools/voices/README.md for why noise_scale/noise_w are 0, not a --seed flag).
PIPER_NOISE_SCALE = "0"
PIPER_NOISE_W = "0"
PIPER_LENGTH_SCALE = "1.0"

# --- Clone engine (Chatterbox, `AUDIO-DEC-006`) -----------------------------------------------------
#
# Round four's engine, chosen after three Piper rounds were rejected ("still not good, can't we
# actually use the villager's voice?", Kevin, 2026-09-20): a permissively licensed zero-shot
# voice-cloning TTS (Resemble AI's Chatterbox, MIT code + MIT weights) conditioned on a reference WAV
# built from vanilla's own villager clips (`tools/voices/reference.py`, resolved from the client's
# asset cache at generation time, never bundled — `COMP-REQ-002`). Installed in an isolated venv,
# `tools/voices/.venv-clone/` (`tools/voices/README.md`), not a dependency of this file's own import
# — `_load_chatterbox` imports it lazily so `test_render.py` still runs with no Piper/Chatterbox/sox/
# network dependency on a machine that has set up neither engine.
DEFAULT_EXAGGERATION = 0.5
DEFAULT_CFG_WEIGHT = 0.5
DEFAULT_TEMPERATURE = 0.8  # Chatterbox's own default; round five (AUDIO-DEC-006 amendment) raises it

# A light tail only (unlike Piper's heavier nasal/dull/pitch chain, `SOX_CHAINS` above) — the whole
# point of cloning is that the reference *is* the villager timbre, so round four's post-processing
# only takes the edge off the raw clone output rather than reshaping it: a small nasal-band lift,
# a gentle top-end rolloff, loudness normalized last. "none" ships the raw clone output through
# unchanged (format conversion only) as the control variant.
CLONE_POST_CHAINS = {
    "tail": ["equalizer", "1600", "1.2q", "+4", "lowpass", "6000", "norm", "-3"],
    "none": [],
    # Round five (`AUDIO-DEC-006` amendment): round four's `all` reference read best but "too harsh
    # and robotic and metallic... I rather want them to sound warm and soft" (Kevin). All three aim
    # at less energy in 2.5-5kHz relative to 150-600Hz (the "metallic" band) without starving
    # 1-2kHz (intelligibility): a low-end body lift (`equalizer 250 1q +3`), a cut right where
    # "metallic" lives (`equalizer 3200 1.5q -4`), a top-end rolloff, and a gentle compressor
    # (`compand`) so transients don't reintroduce harshness after the EQ. `highpass 80` clears
    # sub-bass rumble the compander could otherwise pump on.
    "warm": [
        "highpass", "80", "lowpass", "5500",
        "equalizer", "250", "1q", "+3", "equalizer", "3200", "1.5q", "-4",
        "treble", "-6", "8000",
        "compand", "0.02,0.2", "-60,-60,-30,-20,0,-8", "0", "-90", "0.1",
        "norm", "-3",
    ],
    # Same as "warm" but a lower lowpass (darker) and a small amount of room reverb for softness.
    "soft": [
        "highpass", "80", "lowpass", "4500",
        "equalizer", "250", "1q", "+3", "equalizer", "3200", "1.5q", "-4",
        "treble", "-6", "8000",
        "compand", "0.02,0.2", "-60,-60,-30,-20,0,-8", "0", "-90", "0.1",
        "reverb", "8", "30", "20",
        "norm", "-3",
    ],
    # The minimal move: only the low-shelf-ish body lift and the metallic-band cut, nothing else --
    # isolates how much of "warm" the EQ alone buys before the rolloff/compand/reverb are added.
    "plain-warm": [
        "equalizer", "250", "1q", "+3", "equalizer", "3200", "1.5q", "-4",
        "norm", "-3",
    ],
    # Round six (`AUDIO-DEC-006` amendment): Kevin on round five's clone-of-a-clone reference,
    # "better, but still robotic" -- the reference is now a public-domain *human* voice instead
    # (a public-domain human clip, still built by `reference.build_reference_wav`'s `lowpass_hz`/
    # `norm -3`, the vanilla grunt reference is dropped), so
    # post-processing's job changes from "de-metal a clone of a clone" to "add back a mild villager
    # character on top of a clean human voice". `villager_mild`: a nasal lift centred lower than
    # round four/five's 1600/3200Hz bands (this is a *human* voice now, not a clone already carrying
    # some of that colour), a much smaller cut, a gentle lowpass, and a touch more low end -- nasal
    # without metallic.
    "villager_mild": [
        "equalizer", "1200", "1q", "+3", "equalizer", "2600", "1.5q", "-3",
        "lowpass", "6000", "bass", "+2",
        "norm", "-3",
    ],
    # `villager_mild` plus a small pitch shift down -- the only round-six chain that reaches for
    # `pitch` at all, kept separate so `villager_mild`'s own effect is measurable on its own.
    "villager_pitch": [
        "pitch", "-150",
        "equalizer", "1200", "1q", "+3", "equalizer", "2600", "1.5q", "-3",
        "lowpass", "6000", "bass", "+2",
        "norm", "-3",
    ],
    # Round seven (`AUDIO-DEC-006` final amendment): Kevin on giordano/villager_mild, "sometimes
    # he is still hard to understand... it still sounds like someone is speaking into a tin can" --
    # every chain through round six had a lowpass well under 10kHz (villager_mild's own 6000Hz, on
    # top of a reference already lowpassed at 7000Hz in round five; round six's giordano reference
    # itself was never lowpassed, so the "tin can" was coming from the chain, not the source).
    # Round seven's chains explicitly avoid any lowpass under 10kHz and drop the 1200Hz nasal boost
    # entirely -- the opposite move from every prior round's chain design. `rate -v` is an explicit
    # high-quality upsample from Chatterbox's native 24kHz to the shipped 44.1kHz (matches vanilla
    # asset convention, audio.md §3 "Output format"), rather than relying on the output format
    # flag's own (lower-quality-by-default) resample.
    "open": [
        "rate", "-v", "44100",
        "highpass", "70", "equalizer", "3200", "1.5q", "-2", "treble", "-1.5",
        "norm", "-3",
    ],
    # `open` plus a little low-end body back -- the counterweight to the top-end/nasal moves.
    "open_warm": [
        "rate", "-v", "44100",
        "highpass", "70", "equalizer", "3200", "1.5q", "-2", "treble", "-1.5",
        "bass", "+2", "equalizer", "400", "1q", "+1.5",
        "norm", "-3",
    ],
    # The true control: a high-quality resample and a loudness normalize, nothing else -- not even
    # round four/five/six's mild EQ.
    "dry": [
        "rate", "-v", "44100",
        "norm", "-3",
    ],
    # `open` plus a post `tempo 0.92` stretch -- round seven's other finding, "the delivery could
    # be longer": alongside lower `cfg_weight` at generation time (`DEFAULT_CFG_WEIGHT` stays the
    # CLI default; round seven's own render calls pass 0.2 explicitly), a light tempo pull after
    # the fact makes the delivery read as less rushed without another generation pass.
    "open_tempo": [
        "rate", "-v", "44100",
        "highpass", "70", "equalizer", "3200", "1.5q", "-2", "treble", "-1.5",
        "tempo", "0.92",
        "norm", "-3",
    ],
}

# Candidates for the sample round (tools/voices/VOICES.md has the full licence record).
CANDIDATE_MODELS = (
    "en_US-joe-medium",
    "en_US-kristin-medium",
    "en_US-norman-medium",
    "en_GB-northern_english_male-medium",
)

# A fixed 3-line slice spanning the catalogue's tonal range, for the sample round.
SAMPLE_LINE_IDS = ("trade_completed.1", "hurt.2", "panic.2")

# Round 3 (`AUDIO-DEC-005`) introduced a manual override of these 3 sample lines' TTS input, the
# written grunt removed ("Mrrgh — traded! Nice." -> "Traded! Nice.") since the grunt is now played
# by the game itself (`AUDIO-REQ-007`). VV-18 (round four's merge base) then changed the catalogue
# itself to words-only subtitles plus a separate `grunt` field, making that override a no-op for
# every one of these 3 lines (the catalogue's own subtitle already equals what the override used to
# supply) — so it's removed rather than kept as dead weight; `derive_input_text` (still a plain
# no-op) is once again the only transform `run_sample`/`run_batch` apply.


# --- Catalogue -----------------------------------------------------------------------------------

def load_catalogue_entries() -> dict[str, dict]:
    """line_id (e.g. "trade_completed.1") -> the full catalogue entry dict (at least "subtitle";
    "grunt" and "spoken" only when present), from the 16 catalogue JSON files. `load_catalogue` is
    the subtitle-only view most callers want; `derive_input_text`'s callers use this one for the
    optional "spoken" override (VV-11 round six, `AUDIO-DEC-006` amendment)."""
    entries: dict[str, dict] = {}
    for path in sorted(CATALOGUE_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for entry in data["lines"]:
            sound = entry["sound"]
            prefix = "villager_voices:reaction."
            if not sound.startswith(prefix):
                raise ValueError(f"{path}: unexpected sound id {sound!r}")
            line_id = sound[len(prefix):]
            entries[line_id] = entry
    return entries


def load_catalogue() -> dict[str, str]:
    """line_id (e.g. "trade_completed.1") -> subtitle, from the 16 catalogue JSON files."""
    return {line_id: entry["subtitle"] for line_id, entry in load_catalogue_entries().items()}


# --- Input text -------------------------------------------------------------------------------------
#
# Round six (`AUDIO-DEC-006` amendment): Kevin on round five's clone-of-a-clone timbre: "better, but
# still robotic; also they can't pronounce stuff like 'ouuchh' properly, sounds like letter salad."
# `AUDIO-DEC-004`'s "the subtitle itself, verbatim" is retired as a blanket rule — most lines are
# still spoken exactly as written (the villager phrasing already reads as plain English), but a line
# whose subtitle spelling is expressive rather than plainly pronounceable now gets help: either an
# explicit catalogue `"spoken"` override (a human already decided the right TTS input) or this
# module's own general fallback normaliser, never the raw spelling unmodified.

VOWELS = frozenset("aeiouAEIOU")

# Canonical spoken forms for a villager's stock interjections and every expressive misspelling of
# them found while reading through the catalogue (or plausible enough to guard against) — case-
# insensitive, punctuation-insensitive whole-word match. Most of the catalogue's actual interjections
# ("Ha!", "Hmm,", "Ah,") are already spelled exactly as their own canonical form, so this table is a
# no-op identity match for them and only actually changes text for a genuine misspelling.
INTERJECTION_TABLE: dict[str, str] = {
    "ow": "Ow",
    "owww": "Ow",
    "ouch": "Ouch",
    "ouuch": "Ouch",
    "ouuchh": "Ouch",
    "ouchh": "Ouch",
    "ah": "Ah",
    "ahh": "Ah",
    "hmm": "Hmm",
    "hm": "Hmm",
    "hmnh": "Hmm",
    "mm-hmm": "Mm-hmm",
    "mmh-hmm": "Mm-hmm",
    "mmhmm": "Mm-hmm",
    "ha": "Ha",
    "haha": "Ha",
    "psh": "Psh",
    # "Grr" -> "Grrr": not a collapse, an *expansion* -- round six found "Grr" alone renders too
    # short/clipped on this engine, "Grrr" reads as an actual growl (`tools/voices/VOICES.md`
    # "Round 6", empirically checked before this table entry was added, per the ticket's own "only
    # if it renders" condition).
    "grr": "Grrr",
}
_INTERJECTION_RE = re.compile(
    r"\b(" + "|".join(sorted((re.escape(k) for k in INTERJECTION_TABLE), key=len, reverse=True)) + r")\b",
    re.IGNORECASE,
)


def _apply_interjection_table(text: str) -> str:
    """Substitutes a matched word for its canonical spoken form, preserving the matched word's own
    capitalization (lowercase mid-sentence stays lowercase) rather than forcing the table's own
    Title-case storage everywhere -- case doesn't affect a TTS engine's pronunciation, so there's no
    reason to touch it beyond what the actual respelling requires."""
    def _sub(match: re.Match) -> str:
        original = match.group(0)
        canonical = INTERJECTION_TABLE[original.lower()]
        return canonical if original[0].isupper() else canonical.lower()
    return _INTERJECTION_RE.sub(_sub, text)


def _collapse_letter_runs(text: str) -> str:
    """A run of 3+ identical letters (only a run that long -- English spells plenty of real words
    and already-working interjections with a legitimate *double*, "good", "off", "Hmm" itself, so
    this never touches those) collapses to 2 if it's a consonant ("Owwwww" -> "Oww"), or straight to
    1 if it's a vowel ("Nooooo" -> "No") -- a single pass, so a pre-existing double is never at risk
    of being re-matched by a second one."""
    def _replace(match: re.Match) -> str:
        letter = match.group(1)
        return letter if letter in VOWELS else letter * 2
    return re.sub(r"(.)\1{2,}", _replace, text)


def _strip_repeated_punctuation(text: str) -> str:
    """A run of 2+ identical punctuation marks collapses to one -- "??" -> "?", "..." -> "." -- so
    the TTS input reads as a single clean mark rather than one it might try to voice literally."""
    return re.sub(r"([!?.,])\1+", r"\1", text)


def _normalize_dashes(text: str) -> str:
    """A written interruption/trail-off (an em dash, "Ah— not now.", "No—!") doesn't phonemize
    reliably as punctuation -- read as a soft comma-pause when more text follows, dropped entirely
    at the end of a clause (where the sentence's own closing punctuation already carries the stop)."""
    text = re.sub(r"\s*—\s*(?=\w)", ", ", text)
    text = re.sub(r"\s*—\s*", "", text)
    return text


def normalize_spoken_text(text: str) -> str:
    """The default TTS-input transform for a catalogue line with no explicit `spoken` override
    (`derive_input_text`): a written interruption dash is turned into a pause or dropped
    (`_normalize_dashes`), overlong letter runs collapse (`_collapse_letter_runs` -- run first so a
    letter-run misspelling of a known interjection, e.g. "Ouuuch", is already in a shape the table
    below recognizes), known interjection spellings map to a canonical form (`INTERJECTION_TABLE`),
    and repeated punctuation collapses to one mark (`_strip_repeated_punctuation`). A line whose
    subtitle needs more than this — a genuinely truncated word fragment ("Wha—"), a non-pronounceable
    spelling ("Zzz.") — gets an explicit catalogue `"spoken"` field instead, which bypasses this
    function entirely (`derive_input_text`)."""
    text = _normalize_dashes(text)
    text = _collapse_letter_runs(text)
    text = _apply_interjection_table(text)
    text = _strip_repeated_punctuation(text)
    return text


def derive_input_text(line_id: str, subtitle: str, spoken: str | None = None) -> str:
    """The per-line TTS input. Round six (`AUDIO-DEC-006` amendment): a catalogue line's explicit
    `spoken` override, if present, is used verbatim (a human already decided the right words); a
    line without one falls back to `normalize_spoken_text(subtitle)` rather than the subtitle
    completely unmodified — round 1's ruling against a *distinct nonsense/CV-syllable* input
    (`AUDIO-DEC-004`) still stands, this is a much smaller, targeted normalization, not that. Most
    of the catalogue's 64 subtitles are already plainly pronounceable, so `normalize_spoken_text` is
    a no-op for them. `line_id` is unused but kept in the signature so call sites read the same
    regardless of which line they're deriving text for.
    """
    del line_id
    if spoken is not None:
        return spoken
    return normalize_spoken_text(subtitle)


# --- Piper + sox -----------------------------------------------------------------------------------

class PipelineError(RuntimeError):
    pass


def _require_tools(model_name: str) -> Path:
    if not PIPER_BIN.exists():
        raise PipelineError(f"piper binary not found at {PIPER_BIN}; run `python3 tools/voices/setup.py` first")
    if not shutil.which("sox"):
        raise PipelineError("sox not found on PATH; `brew install sox` (or your platform's equivalent)")
    model_path = MODELS_DIR / model_name / f"{model_name}.onnx"
    if not model_path.exists():
        raise PipelineError(f"voice model not found at {model_path}; run `python3 tools/voices/setup.py` first")
    return model_path


def _render_line_piper(model_name: str, text: str, out_ogg: Path, chain: str) -> None:
    """Runs Piper on `text` with `model_name`, then the named sox chain (`SOX_CHAINS`), writing
    `out_ogg`."""
    model_path = _require_tools(model_name)
    if chain not in SOX_CHAINS:
        raise PipelineError(f"unknown sox chain {chain!r}; choose one of {sorted(SOX_CHAINS)}")
    out_ogg.parent.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, DYLD_LIBRARY_PATH=str(PIPER_DIR), LD_LIBRARY_PATH=str(PIPER_DIR))
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / "line.wav"
        piper_cmd = [
            str(PIPER_BIN), "-m", str(model_path), "--espeak_data", str(ESPEAK_DATA),
            "--noise_scale", PIPER_NOISE_SCALE, "--noise_w", PIPER_NOISE_W,
            "--length_scale", PIPER_LENGTH_SCALE, "-f", str(wav_path),
        ]
        proc = subprocess.run(piper_cmd, input=text, capture_output=True, text=True, env=env)
        if proc.returncode != 0 or not wav_path.exists():
            raise PipelineError(f"piper failed for {text!r}: {proc.stderr.strip()}")
        sox_cmd = ["sox", str(wav_path), *SOX_FORMAT_ARGS, str(out_ogg), *SOX_CHAINS[chain]]
        proc = subprocess.run(sox_cmd, capture_output=True, text=True)
        if proc.returncode != 0 or not out_ogg.exists():
            raise PipelineError(f"sox failed for {out_ogg}: {proc.stderr.strip()}")


def derive_seed(line_id: str) -> int:
    """A deterministic torch seed from a line id alone (`AUDIO-REQ-006`): every generation call is
    reproducible from its input text and seed, and Chatterbox accepts a torch seed directly (unlike
    the frozen Piper binary, which has no `--seed` flag at all — see "Determinism" in
    tools/voices/README.md for how the Piper backend gets reproducibility instead). Hashing the line
    id means the seed table needs no hand-maintenance and is stable across reruns and rewrites of
    this file."""
    digest = hashlib.sha256(line_id.encode("utf-8")).hexdigest()
    return int(digest[:16], 16) % (2**32)


_CHATTERBOX_MODEL = None  # lazy singleton: ~9s to load, reused across every line in a run


def _load_chatterbox():
    global _CHATTERBOX_MODEL
    if _CHATTERBOX_MODEL is None:
        try:
            from chatterbox.tts import ChatterboxTTS
        except ImportError as exc:
            raise PipelineError(
                "chatterbox-tts not importable; install it in tools/voices/.venv-clone/ and run "
                "render.py with that venv's python (see tools/voices/README.md)"
            ) from exc
        _CHATTERBOX_MODEL = ChatterboxTTS.from_pretrained(device="cpu")
    return _CHATTERBOX_MODEL


def _render_line_clone(
    line_id: str, text: str, out_ogg: Path, reference_wav: Path, chain: str,
    exaggeration: float, cfg_weight: float, temperature: float,
) -> None:
    """Runs Chatterbox on `text`, conditioned on `reference_wav` (the vanilla-villager reference,
    `tools/voices/reference.py`), seeded deterministically from `line_id` (`derive_seed`,
    `AUDIO-REQ-006`), then the named post-processing chain (`CLONE_POST_CHAINS`), writing
    `out_ogg`."""
    if not reference_wav.exists():
        raise PipelineError(f"reference WAV not found: {reference_wav}")
    if chain not in CLONE_POST_CHAINS:
        raise PipelineError(f"unknown post-processing chain {chain!r}; choose one of {sorted(CLONE_POST_CHAINS)}")
    import torch
    import torchaudio

    model = _load_chatterbox()
    torch.manual_seed(derive_seed(line_id))
    wav = model.generate(
        text, audio_prompt_path=str(reference_wav),
        exaggeration=exaggeration, cfg_weight=cfg_weight, temperature=temperature,
    )
    out_ogg.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / "line.wav"
        torchaudio.save(str(wav_path), wav, model.sr)
        sox_cmd = ["sox", str(wav_path), *SOX_FORMAT_ARGS, str(out_ogg), *CLONE_POST_CHAINS[chain]]
        proc = subprocess.run(sox_cmd, capture_output=True, text=True)
        if proc.returncode != 0 or not out_ogg.exists():
            raise PipelineError(f"sox failed for {out_ogg}: {proc.stderr.strip()}")


def render_line(
    engine: str, line_id: str, text: str, out_ogg: Path, chain: str, *,
    model: str | None = None, reference_wav: Path | None = None,
    exaggeration: float = DEFAULT_EXAGGERATION, cfg_weight: float = DEFAULT_CFG_WEIGHT,
    temperature: float = DEFAULT_TEMPERATURE,
) -> None:
    """Engine dispatcher: `"piper"` (the original engine, `_render_line_piper`) or `"chatterbox"`
    (round four's clone engine, `_render_line_clone`, `AUDIO-DEC-006`). `line_id` is required by both
    so error messages and the clone engine's deterministic seed always have it, even though Piper
    itself doesn't use it."""
    if engine == "piper":
        if not model:
            raise PipelineError("engine 'piper' requires --model")
        _render_line_piper(model, text, out_ogg, chain)
    elif engine == "chatterbox":
        if reference_wav is None:
            raise PipelineError("engine 'chatterbox' requires --reference")
        _render_line_clone(line_id, text, out_ogg, reference_wav, chain, exaggeration, cfg_weight, temperature)
    else:
        raise PipelineError(f"unknown engine {engine!r}; choose 'piper' or 'chatterbox'")


# --- Modes -----------------------------------------------------------------------------------------

def chains_for_engine(engine: str) -> dict[str, list[str]]:
    """The valid `--chain` names for `engine` (`SOX_CHAINS` for `"piper"`, `CLONE_POST_CHAINS` for
    `"chatterbox"`) — one `--chain` flag serves both engines since it always means "the named sox
    chain applied to this engine's raw output," just against a different table."""
    if engine == "piper":
        return SOX_CHAINS
    if engine == "chatterbox":
        return CLONE_POST_CHAINS
    raise PipelineError(f"unknown engine {engine!r}; choose 'piper' or 'chatterbox'")


def run_sample(
    out_dir: Path, engine: str, chains: tuple[str, ...], *,
    models: tuple[str, ...] = (), reference_wav: Path | None = None,
    exaggeration: float = DEFAULT_EXAGGERATION, cfg_weight: float = DEFAULT_CFG_WEIGHT,
    temperature: float = DEFAULT_TEMPERATURE,
) -> None:
    entries = load_catalogue_entries()
    catalogue = {line_id: entry["subtitle"] for line_id, entry in entries.items()}
    out_dir.mkdir(parents=True, exist_ok=True)
    if engine == "piper":
        header = [
            "# Piper voice-model sample round (VV-11)",
            "",
            "Three lines x each candidate voice model x each `SOX_CHAINS` entry passed — see",
            "`tools/voices/render.py` for what each named chain does and `tools/voices/README.md`",
            "for why. For Kevin's timbre approval — nothing here ships.",
            "",
            "| Model | Licence | Chain | Line | Subtitle | Spoken text | File |",
            "|---|---|---|---|---|---|---|",
        ]
    else:
        header = [
            "# Chatterbox clone-engine sample round (VV-11 round four, `AUDIO-DEC-006`)",
            "",
            "Each candidate `--chain` (post-processing tail) against one reference WAV — see",
            "`tools/voices/render.py` for what each does and `tools/voices/reference.py` for how the",
            "reference is built from vanilla's own clips. For Kevin's timbre approval — nothing",
            "here ships.",
            "",
            "| Reference | Exaggeration | CFG weight | Temperature | Chain | Line | Subtitle | File |",
            "|---|---|---|---|---|---|---|---|",
        ]
    readme_lines = list(header)
    model_values = models if engine == "piper" else (None,)
    for model in model_values:
        for chain in chains:
            for line_id in SAMPLE_LINE_IDS:
                subtitle = catalogue[line_id]
                text = derive_input_text(line_id, subtitle, entries[line_id].get("spoken"))
                if engine == "piper":
                    out_ogg = out_dir / model / f"{line_id}.{chain}.ogg"
                    print(f"rendering {model}/{line_id}.{chain}.ogg  ({text!r})")
                    render_line("piper", line_id, text, out_ogg, chain, model=model)
                    licence = CANDIDATE_LICENCES.get(model, "see tools/voices/VOICES.md")
                    readme_lines.append(
                        f"| {model} | {licence} | {chain} | {line_id} | {subtitle} | {text} | "
                        f"`{model}/{line_id}.{chain}.ogg` |"
                    )
                else:
                    out_ogg = out_dir / f"{line_id}.{chain}.ogg"
                    print(f"rendering {line_id}.{chain}.ogg  ({text!r})")
                    render_line(
                        "chatterbox", line_id, text, out_ogg, chain,
                        reference_wav=reference_wav, exaggeration=exaggeration, cfg_weight=cfg_weight,
                        temperature=temperature,
                    )
                    readme_lines.append(
                        f"| {reference_wav} | {exaggeration} | {cfg_weight} | {temperature} | {chain} | "
                        f"{line_id} | {subtitle} | `{line_id}.{chain}.ogg` |"
                    )
    (out_dir / "README.md").write_text("\n".join(readme_lines) + "\n", encoding="utf-8")
    print(f"sample round written to {out_dir}")


CANDIDATE_LICENCES = {
    "en_US-joe-medium": "CC0 (OHF-Voice/voice-datasets)",
    "en_US-kristin-medium": "Public domain (LibriVox)",
    "en_US-norman-medium": "Public domain (LibriVox)",
    "en_GB-northern_english_male-medium": "CC-BY-SA 4.0 (OpenSLR 83)",
}


def run_batch(
    engine: str, chain: str, *, model: str | None = None, reference_wav: Path | None = None,
    exaggeration: float = DEFAULT_EXAGGERATION, cfg_weight: float = DEFAULT_CFG_WEIGHT,
    temperature: float = DEFAULT_TEMPERATURE,
) -> None:
    entries = load_catalogue_entries()
    catalogue = {line_id: entry["subtitle"] for line_id, entry in entries.items()}
    for line_id, subtitle in sorted(catalogue.items()):
        event, n = line_id.rsplit(".", 1)
        text = derive_input_text(line_id, subtitle, entries[line_id].get("spoken"))
        out_ogg = SHIPPED_SOUNDS_DIR / f"{event}_{n}.ogg"
        print(f"rendering {out_ogg.relative_to(REPO_ROOT)}  ({text!r})")
        render_line(
            engine, line_id, text, out_ogg, chain,
            model=model, reference_wav=reference_wav, exaggeration=exaggeration, cfg_weight=cfg_weight,
            temperature=temperature,
        )

    sounds = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
    for line_id in catalogue:
        event, n = line_id.rsplit(".", 1)
        key = f"reaction.{line_id}"
        if key not in sounds:
            raise PipelineError(f"sounds.json has no entry for {key!r} — catalogue/registration drift")
        sounds[key]["sounds"] = [f"reaction/{event}_{n}"]
    SOUNDS_JSON.write_text(json.dumps(sounds, indent=2, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
    print(f"rewrote {SOUNDS_JSON.relative_to(REPO_ROOT)}: 64 entries now point at their own files")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--sample", action="store_true", help="render the fixed sample slice for timbre approval")
    mode.add_argument("--batch", action="store_true", help="render all 64 lines and rewrite sounds.json")
    parser.add_argument("--engine", choices=("piper", "chatterbox"), default="piper",
                         help="'piper' (default, original engine) or 'chatterbox' (round four's clone engine, AUDIO-DEC-006)")
    parser.add_argument("--model", help="piper voice model name (required for --batch with --engine piper)")
    parser.add_argument("--reference", type=Path, dest="reference_wav",
                         help="reference WAV for --engine chatterbox (required for --batch with --engine chatterbox); "
                              "tools/voices/reference.py builds one from vanilla's own clips")
    parser.add_argument("--exaggeration", type=float, default=DEFAULT_EXAGGERATION,
                         help="chatterbox exaggeration (default %(default)s)")
    parser.add_argument("--cfg", type=float, default=DEFAULT_CFG_WEIGHT, dest="cfg_weight",
                         help="chatterbox cfg_weight (default %(default)s)")
    parser.add_argument("--temperature", type=float, default=DEFAULT_TEMPERATURE,
                         help="chatterbox sampling temperature (default %(default)s; round five, "
                              "AUDIO-DEC-006 amendment, raises it to soften the delivery)")
    parser.add_argument("--chain", help="sox chain name: a SOX_CHAINS key for --engine piper, "
                                         "a CLONE_POST_CHAINS key for --engine chatterbox "
                                         "(required for --batch; --sample defaults to all of the engine's chains)")
    parser.add_argument("--out", type=Path, default=CACHE_DIR / "samples", help="--sample output directory")
    args = parser.parse_args()

    try:
        valid_chains = chains_for_engine(args.engine)
        if args.chain is not None and args.chain not in valid_chains:
            parser.error(f"--chain {args.chain!r} is not valid for --engine {args.engine}; choose one of {sorted(valid_chains)}")

        if args.sample:
            chains = (args.chain,) if args.chain else tuple(sorted(valid_chains))
            if args.engine == "chatterbox" and args.reference_wav is None:
                parser.error("--sample with --engine chatterbox requires --reference")
            models = (args.model,) if args.model else CANDIDATE_MODELS
            run_sample(
                args.out, args.engine, chains, models=models, reference_wav=args.reference_wav,
                exaggeration=args.exaggeration, cfg_weight=args.cfg_weight, temperature=args.temperature,
            )
        else:
            if not args.chain:
                parser.error("--batch requires --chain")
            if args.engine == "piper" and not args.model:
                parser.error("--batch with --engine piper requires --model")
            if args.engine == "chatterbox" and args.reference_wav is None:
                parser.error("--batch with --engine chatterbox requires --reference")
            run_batch(
                args.engine, args.chain, model=args.model, reference_wav=args.reference_wav,
                exaggeration=args.exaggeration, cfg_weight=args.cfg_weight, temperature=args.temperature,
            )
    except PipelineError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
