#!/usr/bin/env python3
"""Fetches the frozen MIT Piper snapshot and the candidate voice models into the gitignored
`tools/voices/.cache/` (see `tools/voices/VOICES.md` for why this combination, not a single
release asset, and its exact provenance). Run once per machine; re-run is a no-op for anything
already present. Requires `gh` (authenticated) and network access — this script itself is the only
thing here that ever touches the network; `render.py` never does (`AUDIO-REQ-005`).

Downloads, in order:
1. `rhasspy/piper` release `2023.11.14-2` (archived, MIT) — the `piper` CLI binary and its
   `espeak-ng-data`. That release's own macOS aarch64 asset ships without its shared libraries (a
   packaging bug in that specific asset — confirmed by inspection, not documented upstream).
2. `rhasspy/piper-phonemize` release `2023.11.14-4` (archived, MIT, same project family, a later
   bugfix build) — supplies the missing `libespeak-ng*.dylib`, `libpiper_phonemize*.dylib`, and
   `libonnxruntime*.dylib` that (1)'s binary links against.
3. The three candidate voice models (`tools/voices/VOICES.md`), from the `rhasspy/piper-voices`
   Hugging Face repository.

Both macOS "aarch64" release assets are in fact x86_64 Mach-O binaries (a labelling bug in the
archived project, not something this script can fix) and run under Rosetta 2 on Apple Silicon.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tarfile
import urllib.request
from pathlib import Path

CACHE_DIR = Path(__file__).resolve().parent / ".cache"
PIPER_DIR = CACHE_DIR / "piper"
MODELS_DIR = CACHE_DIR / "models"

PIPER_RELEASE = ("rhasspy/piper", "2023.11.14-2", "piper_macos_aarch64.tar.gz")
PHONEMIZE_RELEASE = ("rhasspy/piper-phonemize", "2023.11.14-4", "piper-phonemize_macos_aarch64.tar.gz")

VOICES = ("en_US-joe-medium", "en_US-kristin-medium", "en_US-norman-medium")
VOICE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/{voice}/{quality}/{name}"


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
        # voice_name looks like "en_US-joe-medium"; split into ("joe", "medium").
        _, rest = voice_name.split("-", 1)
        voice, quality = rest.rsplit("-", 1)
        target = MODELS_DIR / voice_name
        target.mkdir(exist_ok=True)
        for suffix in (".onnx", ".onnx.json"):
            file_name = f"{voice_name}{suffix}"
            dest = target / file_name
            if dest.exists():
                continue
            url = VOICE_URL.format(voice=voice, quality=quality, name=file_name)
            print(f"fetching {url}")
            urllib.request.urlretrieve(url, dest)
    print(f"models: installed at {MODELS_DIR}")


def main() -> int:
    if shutil.which("gh") is None:
        print("error: gh (GitHub CLI) not found on PATH; required to fetch the frozen piper release", file=sys.stderr)
        return 1
    if shutil.which("sox") is None:
        print("warning: sox not found on PATH; `brew install sox` before running render.py", file=sys.stderr)
    setup_piper()
    setup_models()
    return 0


if __name__ == "__main__":
    sys.exit(main())
