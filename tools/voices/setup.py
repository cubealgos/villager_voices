#!/usr/bin/env python3
"""Fetches the frozen MIT Piper snapshot and the candidate voice models into the gitignored
`tools/voices/.cache/` (see `tools/voices/VOICES.md` for why this combination, not a single
release asset, and its exact provenance), and (`--clone`) sets up the Chatterbox clone engine's
isolated venv (`AUDIO-DEC-006`). Run once per machine; re-run is a no-op for anything already
present. Requires `gh` (authenticated) and network access — this script itself is the only thing
here that ever touches the network; `render.py` never does (`AUDIO-REQ-005`).

Piper setup downloads, in order:
1. `rhasspy/piper` release `2023.11.14-2` (archived, MIT) — the `piper` CLI binary and its
   `espeak-ng-data`. That release's own macOS aarch64 asset ships without its shared libraries (a
   packaging bug in that specific asset — confirmed by inspection, not documented upstream).
2. `rhasspy/piper-phonemize` release `2023.11.14-4` (archived, MIT, same project family, a later
   bugfix build) — supplies the missing `libespeak-ng*.dylib`, `libpiper_phonemize*.dylib`, and
   `libonnxruntime*.dylib` that (1)'s binary links against.
3. The candidate voice models (`tools/voices/VOICES.md`), from the `rhasspy/piper-voices` Hugging
   Face repository.

Both macOS "aarch64" release assets are in fact x86_64 Mach-O binaries (a labelling bug in the
archived project, not something this script can fix) and run under Rosetta 2 on Apple Silicon.

`--clone` setup (`tools/voices/VOICES.md` "Round 4"): creates `tools/voices/.venv-clone/` (Python
3.11 via `uv python`, gitignored) and installs `chatterbox-tts` (MIT code, MIT weights) plus a
pinned `setuptools<81` — `resemble-perth`, one of Chatterbox's own dependencies, still imports the
deprecated `pkg_resources`, which newer `setuptools` no longer ships; without the pin its watermarker
silently degrades to `None` and `ChatterboxTTS.from_pretrained` crashes constructing it. CPU-only
(no CUDA/MPS requirement) — runs on Apple Silicon and x86-under-Rosetta alike. Requires `uv`
(https://docs.astral.sh/uv/) on PATH.
"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import tarfile
import urllib.request
from pathlib import Path

CACHE_DIR = Path(__file__).resolve().parent / ".cache"
PIPER_DIR = CACHE_DIR / "piper"
MODELS_DIR = CACHE_DIR / "models"

CLONE_VENV = Path(__file__).resolve().parent / ".venv-clone"
CLONE_PYTHON_VERSION = "3.11"
CLONE_PACKAGES = ("chatterbox-tts==0.1.7", "setuptools<81", "attrs")
# `attrs` (VV-11 round ten): chatterbox-tts's own dependency chain pulls in omegaconf 2.3.1, whose
# `_utils.py` only *tries* `import attr` and silently sets `attr = None` on failure -- but a later
# `is_attr_class` call reaches `attr.has(obj)` unconditionally regardless, an AttributeError on
# `None` rather than the ImportError omegaconf's own try/except was meant to guard against. `uv`'s
# resolver doesn't always pull `attrs` in as a transitive dependency of chatterbox-tts/omegaconf, so
# it's pinned here explicitly rather than left to chance -- found the hard way when round ten's
# actual renders failed on a clean `--clone` venv.

PIPER_RELEASE = ("rhasspy/piper", "2023.11.14-2", "piper_macos_aarch64.tar.gz")
PHONEMIZE_RELEASE = ("rhasspy/piper-phonemize", "2023.11.14-4", "piper-phonemize_macos_aarch64.tar.gz")

VOICES = (
    "en_US-joe-medium",
    "en_US-kristin-medium",
    "en_US-norman-medium",
    "en_GB-northern_english_male-medium",  # round 2: a naturally lower-register male voice
)
VOICE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/{lang}/{locale}/{voice}/{quality}/{name}"


def run(*cmd: str) -> None:
    subprocess.run(cmd, check=True)


def fetch_release_asset(repo: str, tag: str, asset: str, dest: Path) -> Path:
    dest.mkdir(parents=True, exist_ok=True)
    archive = dest / asset
    if not archive.exists():
        print(f"fetching {repo}@{tag}/{asset}")
        run("gh", "release", "download", tag, "-R", repo, "-p", asset, "-D", str(dest), "--clobber")
    return archive


def setup_piper() -> None:
    if (PIPER_DIR / "piper").exists() and list(PIPER_DIR.glob("libespeak-ng*.dylib")):
        print("piper: already present")
        return
    piper_archive = fetch_release_asset(*PIPER_RELEASE, CACHE_DIR)
    phonemize_archive = fetch_release_asset(*PHONEMIZE_RELEASE, CACHE_DIR)
    with tarfile.open(piper_archive) as tar:
        tar.extractall(CACHE_DIR)  # -> .cache/piper/
    extracted_phonemize = CACHE_DIR / "piper-phonemize"
    with tarfile.open(phonemize_archive) as tar:
        tar.extractall(CACHE_DIR)  # -> .cache/piper-phonemize/
    for dylib in (extracted_phonemize / "lib").glob("*.dylib"):
        shutil.copy2(dylib, PIPER_DIR / dylib.name)
    shutil.rmtree(extracted_phonemize)
    (PIPER_DIR / "piper").chmod(0o755)
    print(f"piper: installed at {PIPER_DIR}")


def setup_models() -> None:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    for voice_name in VOICES:
        # voice_name looks like "en_US-joe-medium" or "en_GB-northern_english_male-medium";
        # split into locale ("en_US"), voice ("joe"), quality ("medium").
        locale, rest = voice_name.split("-", 1)
        voice, quality = rest.rsplit("-", 1)
        lang = locale.split("_")[0]
        target = MODELS_DIR / voice_name
        target.mkdir(exist_ok=True)
        for suffix in (".onnx", ".onnx.json"):
            file_name = f"{voice_name}{suffix}"
            dest = target / file_name
            if dest.exists():
                continue
            url = VOICE_URL.format(lang=lang, locale=locale, voice=voice, quality=quality, name=file_name)
            print(f"fetching {url}")
            urllib.request.urlretrieve(url, dest)
    print(f"models: installed at {MODELS_DIR}")


def setup_clone_venv() -> None:
    if shutil.which("uv") is None:
        print("error: uv not found on PATH; required to fetch a pinned Python for the clone venv "
              "(https://docs.astral.sh/uv/)", file=sys.stderr)
        raise SystemExit(1)
    python_bin = CLONE_VENV / "bin" / "python"
    if python_bin.exists():
        print(f"clone venv: already present at {CLONE_VENV}")
    else:
        print(f"creating clone venv (Python {CLONE_PYTHON_VERSION}) at {CLONE_VENV}")
        subprocess.run(["uv", "venv", "--python", CLONE_PYTHON_VERSION, str(CLONE_VENV)], check=True)
    print(f"installing {', '.join(CLONE_PACKAGES)}")
    subprocess.run(["uv", "pip", "install", "--python", str(python_bin), *CLONE_PACKAGES], check=True)
    print(f"clone venv ready: {python_bin}")
    print("run render.py with it, e.g.:")
    print(f"  {python_bin} render.py --sample --engine chatterbox --reference <wav>")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clone", action="store_true", help="also (or only, with --clone-only) set up the Chatterbox clone-engine venv")
    parser.add_argument("--clone-only", action="store_true", help="set up only the clone-engine venv, skip Piper")
    args = parser.parse_args()

    if not args.clone_only:
        if shutil.which("gh") is None:
            print("error: gh (GitHub CLI) not found on PATH; required to fetch the frozen piper release", file=sys.stderr)
            return 1
        if shutil.which("sox") is None:
            print("warning: sox not found on PATH; `brew install sox` before running render.py", file=sys.stderr)
        setup_piper()
        setup_models()
    if args.clone or args.clone_only:
        setup_clone_venv()
    return 0


if __name__ == "__main__":
    sys.exit(main())
