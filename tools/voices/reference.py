#!/usr/bin/env python3
"""Builds the villager-voice-cloning reference WAV from vanilla's own grunt clips, read from the
client's asset cache at generation time (`AUDIO-DEC-006`, `docs/spec/domains/audio.md`). Never
bundles or ships a Mojang audio byte (`COMP-REQ-002`): the reference this module writes is a
throwaway file for the cloning engine to consume, built in the scratchpad, never committed
(`.gitignore`'s `tools/voices/.reference-cache/` entry covers a machine-local one too).

This is the first *committed* version of the asset-index resolution logic VV-11 round three used to
measure vanilla's median f0 (`tools/voices/VOICES.md` "Round 3") — that script was ad-hoc and never
landed in the repo; this module reuses its approach (fabric-loom's `indexes/<version>.json` maps a
vanilla asset path to a content hash, the object itself sits at
`objects/<first two hex of the hash>/<hash>`) and is now the pipeline's own reusable copy.
"""
from __future__ import annotations

import json
import subprocess
import wave
from pathlib import Path

DEFAULT_ASSETS_ROOT = Path.home() / ".gradle" / "caches" / "fabric-loom" / "assets"
VANILLA_VILLAGER_PREFIX = "minecraft/sounds/mob/villager/"

# Named reference sets (round four, `AUDIO-DEC-006`): every clip vanilla ships for the villager
# ("all"), only the ones that read as speech rather than a reaction noise ("talking" — round three's
# VOICES.md already established `haggle` *is* vanilla's own trade-interaction sound, so it stands in
# for "trade"; there is no separate `work_*` clip in the villager sound folder to include), and idle
# alone, the smallest and most neutral set.
REFERENCE_SETS: dict[str, tuple[str, ...]] = {
    "all": (
        "idle1", "idle2", "idle3",
        "haggle1", "haggle2", "haggle3",
        "yes1", "yes2", "yes3",
        "no1", "no2", "no3",
        "hit1", "hit2", "hit3", "hit4",
    ),
    "talking": ("idle1", "idle2", "idle3", "haggle1", "haggle2", "haggle3", "yes1", "yes2", "yes3"),
    "idle": ("idle1", "idle2", "idle3"),
    # Round five (`AUDIO-DEC-006` amendment, Kevin: "all sounds the best, but... too harsh and
    # robotic and metallic; I rather want them to sound warm and soft"): the same broad clip
    # coverage as "all" minus the four `hit*` clips, which measured as this villager's loudest and
    # most clipped source material (`sox ... stat` "Maximum amplitude" 0.89-1.00, vs. 0.20-0.85 for
    # every idle/haggle/yes/no clip) — the percussive "hit" transient is exactly the kind of source
    # content likely to teach a cloning model a harsh, metallic edge. No other clip in "all" clips.
    "all_warm": (
        "idle1", "idle2", "idle3",
        "haggle1", "haggle2", "haggle3",
        "yes1", "yes2", "yes3",
        "no1", "no2", "no3",
    ),
}

SILENCE_BETWEEN_CLIPS_S = 0.150
REFERENCE_SAMPLE_RATE = 44100


class ReferenceError(RuntimeError):
    pass


# --- Asset index resolution -----------------------------------------------------------------------

def load_asset_index(index_path: Path) -> dict[str, str]:
    """asset path (e.g. "minecraft/sounds/mob/villager/idle1.ogg") -> content hash, from a
    fabric-loom asset index JSON (`<assets_root>/indexes/<version>.json`)."""
    data = json.loads(index_path.read_text(encoding="utf-8"))
    try:
        objects = data["objects"]
    except KeyError as exc:
        raise ReferenceError(f"{index_path}: not an asset index (no top-level 'objects' key)") from exc
    return {name: entry["hash"] for name, entry in objects.items()}


def find_default_asset_index(assets_root: Path = DEFAULT_ASSETS_ROOT) -> Path:
    """The single asset index under `<assets_root>/indexes/`. Fails loudly rather than silently
    guessing the wrong Minecraft version's index when the client has cached more than one, or none
    at all (run the game client once to populate it)."""
    indexes_dir = assets_root / "indexes"
    candidates = sorted(indexes_dir.glob("*.json"))
    if not candidates:
        raise ReferenceError(f"no asset index found under {indexes_dir}; run the game client once to populate it")
    if len(candidates) > 1:
        names = ", ".join(c.name for c in candidates)
        raise ReferenceError(f"multiple asset indexes under {indexes_dir} ({names}); pass --asset-index explicitly")
    return candidates[0]


def resolve_object(clip_hash: str, assets_root: Path = DEFAULT_ASSETS_ROOT) -> Path:
    """The on-disk object path for a content hash: `objects/<first two hex>/<hash>` (fabric-loom's
    own, Minecraft's own content-addressed asset store layout)."""
    path = assets_root / "objects" / clip_hash[:2] / clip_hash
    if not path.exists():
        raise ReferenceError(f"asset object {clip_hash} not found at {path}; run the game client once to populate it")
    return path


def resolve_clips(
    clip_names: tuple[str, ...], index: dict[str, str], assets_root: Path = DEFAULT_ASSETS_ROOT
) -> list[Path]:
    """Short clip names (e.g. "idle1") -> resolved on-disk object paths, in the given order."""
    paths = []
    for name in clip_names:
        key = f"{VANILLA_VILLAGER_PREFIX}{name}.ogg"
        clip_hash = index.get(key)
        if clip_hash is None:
            raise ReferenceError(f"{key!r} not present in the asset index")
        paths.append(resolve_object(clip_hash, assets_root))
    return paths


# --- Reference WAV assembly -------------------------------------------------------------------------

def _write_silence_wav(out_path: Path, duration_s: float = SILENCE_BETWEEN_CLIPS_S, rate: int = REFERENCE_SAMPLE_RATE) -> None:
    """A plain digital-silence mono WAV, written directly (no sox dependency for this one step —
    generating silence by shelling out is a well-known sox footgun across versions)."""
    n_frames = int(duration_s * rate)
    with wave.open(str(out_path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(rate)
        wav.writeframes(b"\x00\x00" * n_frames)


def build_reference_wav(
    clip_paths: list[Path], out_wav: Path, tmp_dir: Path, *,
    lowpass_hz: float | None = None, tempo: float | None = None,
) -> None:
    """Concatenates `clip_paths` (any sox-readable format) with 150ms of silence between each,
    normalized to -3dB, mono 44.1kHz, written to `out_wav`. `tmp_dir` is scratch space the caller
    owns the lifetime of — nothing is written outside it and `out_wav`.

    `lowpass_hz` (round five, `AUDIO-DEC-006` amendment) applies a gentle lowpass to the finished
    reference before the cloning engine ever sees it, so the model conditions on the villager's own
    formants rather than the ogg encoder's high-frequency crunch — Kevin: "too harsh and robotic and
    metallic". `tempo` (round five) slows the reference slightly (e.g. `0.9`) on the theory that a
    more unhurried reference delivery biases the clone toward a softer one. Either, both, or neither
    may be given; when given, the reference is re-normalized to -3dB after applying them so the
    final loudness convention stays the same regardless."""
    if not clip_paths:
        raise ReferenceError("no clips to build a reference from")
    tmp_dir.mkdir(parents=True, exist_ok=True)
    silence = tmp_dir / "_silence.wav"
    _write_silence_wav(silence)

    decoded: list[Path] = []
    for i, clip in enumerate(clip_paths):
        wav_path = tmp_dir / f"_clip{i}.wav"
        proc = subprocess.run(
            ["sox", str(clip), "-r", str(REFERENCE_SAMPLE_RATE), "-c", "1", str(wav_path)],
            capture_output=True, text=True,
        )
        if proc.returncode != 0 or not wav_path.exists():
            raise ReferenceError(f"sox failed decoding {clip}: {proc.stderr.strip()}")
        decoded.append(wav_path)

    sequence: list[Path] = []
    for i, wav_path in enumerate(decoded):
        sequence.append(wav_path)
        if i != len(decoded) - 1:
            sequence.append(silence)

    out_wav.parent.mkdir(parents=True, exist_ok=True)
    post_effects: list[str] = []
    if lowpass_hz is not None:
        post_effects += ["lowpass", str(lowpass_hz)]
    if tempo is not None:
        post_effects += ["tempo", str(tempo)]
    cmd = ["sox", *[str(p) for p in sequence], str(out_wav), "norm", "-3", *post_effects]
    if post_effects:
        cmd += ["norm", "-3"]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0 or not out_wav.exists():
        raise ReferenceError(f"sox failed concatenating the reference: {proc.stderr.strip()}")


def build_named_reference(
    reference_set: str,
    out_wav: Path,
    tmp_dir: Path,
    assets_root: Path = DEFAULT_ASSETS_ROOT,
    asset_index_path: Path | None = None,
    *,
    lowpass_hz: float | None = None,
    tempo: float | None = None,
) -> None:
    """End-to-end: resolve `reference_set` (a `REFERENCE_SETS` key) against the client's own asset
    cache and write the concatenated, normalized reference WAV to `out_wav`, with round five's
    optional `lowpass_hz`/`tempo` post-processing (`build_reference_wav`)."""
    if reference_set not in REFERENCE_SETS:
        raise ReferenceError(f"unknown reference set {reference_set!r}; choose one of {sorted(REFERENCE_SETS)}")
    index_path = asset_index_path or find_default_asset_index(assets_root)
    index = load_asset_index(index_path)
    clip_paths = resolve_clips(REFERENCE_SETS[reference_set], index, assets_root)
    build_reference_wav(clip_paths, out_wav, tmp_dir, lowpass_hz=lowpass_hz, tempo=tempo)
