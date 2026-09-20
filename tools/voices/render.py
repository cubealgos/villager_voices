#!/usr/bin/env python3
"""The Piper voice pipeline (`docs/spec/domains/audio.md` §3, `AUDIO-DEC-004`/`AUDIO-DEC-005`).

Reads the 64-line reaction catalogue (the 16 JSON files under
`fabric/src/main/resources/data/villager_voices/reaction/`), feeds each line's own subtitle to the
frozen MIT `rhasspy/piper` snapshot verbatim (round 1's nonsense/CV-syllable transform was rejected
outright — unintelligible; `AUDIO-DEC-004`), runs the output through a fixed sox chain, and writes
mono OGG Vorbis output.

Two modes:

* `--sample`: a fixed 3-line slice rendered once per candidate voice model **and** once per pitch
  `--chain` (`SOX_CHAINS`), for Kevin's timbre approval (`AUDIO-FAIL-003`) — never touches the
  shipped assets.
* `--batch`: all 64 lines against one approved `--model` and `--chain`, written to their shipped
  paths (`fabric/src/main/resources/assets/villager_voices/sounds/reaction/<event>_<n>.ogg`,
  audio.md §3 "File naming"), then rewrites `sounds.json` so each entry points at its own file —
  the only change `AUDIO-REQ-003` allows. Do not run `--batch` before Kevin has approved a sample.

Build-time only (`AUDIO-REQ-005`): nothing here is imported by `common` or `fabric`'s Java sources,
and no Gradle task invokes it. Requires the `piper` binary and `sox` on PATH or under
`tools/voices/.cache/` — see `tools/voices/setup.py` and `tools/voices/README.md`.
"""
from __future__ import annotations

import argparse
import json
import os
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

# Candidates for the sample round (tools/voices/VOICES.md has the full licence record).
CANDIDATE_MODELS = (
    "en_US-joe-medium",
    "en_US-kristin-medium",
    "en_US-norman-medium",
    "en_GB-northern_english_male-medium",
)

# A fixed 3-line slice spanning the catalogue's tonal range, for the sample round.
SAMPLE_LINE_IDS = ("trade_completed.1", "hurt.2", "panic.2")

# Round 3 only (`AUDIO-DEC-005`): a manual override of these 3 sample lines' TTS input, the written
# grunt removed ("Mrrgh — traded! Nice." -> "Traded! Nice.") since the grunt is now played by the
# game itself (`AUDIO-REQ-007`, VV-18). This is *not* yet a general rule for all 64 lines — how to
# mechanically strip a grunt from an arbitrary subtitle is undecided — so `derive_input_text` stays
# a plain no-op and this override applies only to `run_sample`'s fixed slice.
SAMPLE_TEXT_OVERRIDES = {
    "trade_completed.1": "Traded! Nice.",
    "hurt.2": "That hurt!",
    "panic.2": "Danger!",
}


# --- Catalogue -----------------------------------------------------------------------------------

def load_catalogue() -> dict[str, str]:
    """line_id (e.g. "trade_completed.1") -> subtitle, from the 16 catalogue JSON files."""
    lines: dict[str, str] = {}
    for path in sorted(CATALOGUE_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for entry in data["lines"]:
            sound = entry["sound"]
            prefix = "villager_voices:reaction."
            if not sound.startswith(prefix):
                raise ValueError(f"{path}: unexpected sound id {sound!r}")
            line_id = sound[len(prefix):]
            lines[line_id] = entry["subtitle"]
    return lines


# --- Input text -------------------------------------------------------------------------------------

def derive_input_text(line_id: str, subtitle: str) -> str:
    """The per-line Piper input: the subtitle itself, plain English, verbatim (audio.md §3 "Input",
    `AUDIO-DEC-004`). Round 1 fed Piper a scrambled nonsense/CV-syllable string instead — Kevin's
    ruling on hearing it: "they're all shit, I can't understand a single thing." The subtitle's own
    villager interjections ("Mrrgh", "Hmnh", "Ah", ...) already carry the character; this function
    is deliberately a no-op (kept, not inlined, so call sites read the same as round 1's and so a
    future line-specific adjustment has one place to land) — `line_id` is unused but kept in the
    signature for that reason.
    """
    del line_id  # unused: the transform no longer varies by line identity, only by subtitle.
    return subtitle


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


def render_line(model_name: str, text: str, out_ogg: Path, chain: str) -> None:
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


# --- Modes -----------------------------------------------------------------------------------------

def run_sample(out_dir: Path, models: tuple[str, ...], chains: tuple[str, ...]) -> None:
    catalogue = load_catalogue()
    out_dir.mkdir(parents=True, exist_ok=True)
    readme_lines = [
        "# Piper voice-model sample round (VV-11)",
        "",
        "Three lines x each candidate voice model x each `SOX_CHAINS` entry passed — see",
        "`tools/voices/render.py` for what each named chain does and `tools/voices/README.md` for",
        "why. \"Spoken text\" is what Piper actually receives; it differs from \"Subtitle\" only where",
        "`SAMPLE_TEXT_OVERRIDES` applies (round 3, `AUDIO-DEC-005`: the written grunt removed, since",
        "the game plays the vanilla grunt itself). For Kevin's timbre approval — nothing here ships.",
        "",
        "| Model | Licence | Chain | Line | Subtitle | Spoken text | File |",
        "|---|---|---|---|---|---|---|",
    ]
    for model in models:
        for chain in chains:
            for line_id in SAMPLE_LINE_IDS:
                subtitle = catalogue[line_id]
                text = SAMPLE_TEXT_OVERRIDES.get(line_id) or derive_input_text(line_id, subtitle)
                out_ogg = out_dir / model / f"{line_id}.{chain}.ogg"
                print(f"rendering {model}/{line_id}.{chain}.ogg  ({text!r})")
                render_line(model, text, out_ogg, chain)
                licence = CANDIDATE_LICENCES.get(model, "see tools/voices/VOICES.md")
                readme_lines.append(
                    f"| {model} | {licence} | {chain} | {line_id} | {subtitle} | {text} | `{model}/{line_id}.{chain}.ogg` |"
                )
    (out_dir / "README.md").write_text("\n".join(readme_lines) + "\n", encoding="utf-8")
    print(f"sample round written to {out_dir}")


CANDIDATE_LICENCES = {
    "en_US-joe-medium": "CC0 (OHF-Voice/voice-datasets)",
    "en_US-kristin-medium": "Public domain (LibriVox)",
    "en_US-norman-medium": "Public domain (LibriVox)",
    "en_GB-northern_english_male-medium": "CC-BY-SA 4.0 (OpenSLR 83)",
}


def run_batch(model: str, chain: str) -> None:
    catalogue = load_catalogue()
    for line_id, subtitle in sorted(catalogue.items()):
        event, n = line_id.rsplit(".", 1)
        text = derive_input_text(line_id, subtitle)
        out_ogg = SHIPPED_SOUNDS_DIR / f"{event}_{n}.ogg"
        print(f"rendering {out_ogg.relative_to(REPO_ROOT)}  ({text!r})")
        render_line(model, text, out_ogg, chain)

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
    parser.add_argument("--model", help="voice model name (required for --batch; --sample defaults to all candidates)")
    parser.add_argument("--chain", choices=sorted(SOX_CHAINS),
                         help="sox pitch chain (required for --batch; --sample defaults to all chains)")
    parser.add_argument("--out", type=Path, default=CACHE_DIR / "samples", help="--sample output directory")
    args = parser.parse_args()

    try:
        if args.sample:
            models = (args.model,) if args.model else CANDIDATE_MODELS
            chains = (args.chain,) if args.chain else tuple(sorted(SOX_CHAINS))
            run_sample(args.out, models, chains)
        else:
            if not args.model:
                parser.error("--batch requires --model")
            if not args.chain:
                parser.error("--batch requires --chain")
            run_batch(args.model, args.chain)
    except PipelineError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
