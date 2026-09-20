"""Unit tests for the input-text rule, catalogue loading, and engine dispatch/argument-validation
(`tools/voices/render.py`), plus the reference-set builder (`tools/voices/reference.py`).

No test here touches Piper, sox, Chatterbox, or the network — those live behind `render_line`'s two
backends, exercised manually via `just voices-sample` (`tools/voices/README.md`) or the round-four
scratchpad driver. `render_line`'s own argument-validation branches (missing `--model`/`--reference`,
an unknown engine) are pure Python and short-circuit before either backend's own heavy import, so
they're covered here without either engine installed. This file is what `just test-tools` runs."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import reference  # noqa: E402
import render  # noqa: E402


class DeriveInputTextTest(unittest.TestCase):
    """`derive_input_text` is a no-op since `AUDIO-DEC-004` (round 1's nonsense/CV-syllable
    transform was rejected outright — unintelligible); these tests pin that down so nobody
    reintroduces scrambling by accident."""

    def test_returns_the_subtitle_verbatim(self):
        subtitle = "Mrrgh — traded! Nice."
        self.assertEqual(render.derive_input_text("trade_completed.1", subtitle), subtitle)

    def test_ignores_line_id(self):
        subtitle = "Hmnh, good trade, that."
        text_a = render.derive_input_text("trade_completed.2", subtitle)
        text_b = render.derive_input_text("some_other.9", subtitle)
        self.assertEqual(text_a, text_b)
        self.assertEqual(text_a, subtitle)

    def test_matches_every_catalogue_line_s_own_subtitle(self):
        for line_id, subtitle in render.load_catalogue().items():
            self.assertEqual(render.derive_input_text(line_id, subtitle), subtitle)


class SoxChainsTest(unittest.TestCase):
    """Round 1's `pitch 500` (way up, paired with scrambled input text) was rejected outright
    (`AUDIO-DEC-004`). Round 3 deliberately tests pitching up again — vanilla's own measured median
    f0 sits *above* `en_US-norman-medium`'s natural pitch, not below it, so landing on it (`pitch
    490`, `villager_vanilla`) means going up, all the way to the measured match, plus two waypoints
    on the way there (`villager_up_250`/`villager_up_400`) and a smaller step
    (`villager_below`/`villager_match`/`villager_above`, `-100`/`0`/`100`) — every one of them a
    named, explicit `villager_*` exception decided here specifically so Kevin can hear it, never a
    silent change to the pipeline's default direction."""

    def test_every_chain_starts_with_a_pitch_effect(self):
        for name, args in render.SOX_CHAINS.items():
            self.assertEqual(args[0], "pitch", f"{name}: expected 'pitch' first, got {args!r}")

    def test_pitch_is_within_the_explored_range(self):
        for name, args in render.SOX_CHAINS.items():
            cents = int(args[1])
            self.assertTrue(-500 <= cents <= 490, f"{name}: pitch {cents} outside the explored range")

    def test_only_named_villager_chains_pitch_up_or_flat(self):
        for name, args in render.SOX_CHAINS.items():
            cents = int(args[1])
            if cents >= 0:
                self.assertTrue(name.startswith("villager_"), f"{name}: pitch {cents} is up/flat but not a named villager_* chain")

    def test_every_chain_shares_the_same_tail_after_pitch_except_the_named_no_tempo_variant(self):
        tails = {name: tuple(args[2:]) for name, args in render.SOX_CHAINS.items() if name != "villager_vanilla_no_tempo"}
        self.assertEqual(len(set(tails.values())), 1, "every chain but villager_vanilla_no_tempo should share the same tail")

    def test_villager_vanilla_no_tempo_matches_its_tempo_sibling_minus_tempo(self):
        with_tempo = render.SOX_CHAINS["villager_vanilla"]
        without_tempo = render.SOX_CHAINS["villager_vanilla_no_tempo"]
        self.assertEqual(with_tempo[1], without_tempo[1], "both should shift pitch by the same amount")
        self.assertNotIn("tempo", without_tempo)
        expected = [arg for arg in with_tempo[2:] if arg not in ("tempo", "0.95")]
        self.assertEqual(without_tempo[2:], expected)

    def test_deep_and_deeper_share_everything_but_the_pitch_depth(self):
        self.assertEqual(render.SOX_CHAINS["deep"][2:], render.SOX_CHAINS["deeper"][2:])
        self.assertNotEqual(render.SOX_CHAINS["deep"][1], render.SOX_CHAINS["deeper"][1])


class LoadCatalogueTest(unittest.TestCase):
    def test_reads_all_64_lines_from_the_real_catalogue(self):
        catalogue = render.load_catalogue()
        self.assertEqual(len(catalogue), 64)
        self.assertEqual(catalogue["trade_completed.1"], "Traded! Nice.")

    def test_line_ids_match_sound_naming(self):
        for line_id in render.load_catalogue():
            self.assertRegex(line_id, r"^[a-z_]+\.[1-4]$")

    def test_reads_from_a_fixture_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            fixture = Path(tmp) / "trade_completed.json"
            fixture.write_text(json.dumps({
                "lines": [{"subtitle": "Test line.", "sound": "villager_voices:reaction.trade_completed.1"}]
            }), encoding="utf-8")
            original = render.CATALOGUE_DIR
            render.CATALOGUE_DIR = Path(tmp)
            try:
                catalogue = render.load_catalogue()
            finally:
                render.CATALOGUE_DIR = original
            self.assertEqual(catalogue, {"trade_completed.1": "Test line."})

    def test_rejects_a_sound_id_outside_the_reaction_namespace(self):
        with tempfile.TemporaryDirectory() as tmp:
            fixture = Path(tmp) / "bad.json"
            fixture.write_text(json.dumps({
                "lines": [{"subtitle": "Bad.", "sound": "villager_voices:other.bad.1"}]
            }), encoding="utf-8")
            original = render.CATALOGUE_DIR
            render.CATALOGUE_DIR = Path(tmp)
            try:
                with self.assertRaises(ValueError):
                    render.load_catalogue()
            finally:
                render.CATALOGUE_DIR = original


class DeriveSeedTest(unittest.TestCase):
    """`derive_seed` (`AUDIO-REQ-006`): the clone engine's deterministic torch seed, hashed from the
    line id alone so the batch is reproducible without a hand-maintained seed table."""

    def test_same_line_id_same_seed(self):
        self.assertEqual(render.derive_seed("trade_completed.1"), render.derive_seed("trade_completed.1"))

    def test_different_line_ids_different_seeds(self):
        seeds = {render.derive_seed(line_id) for line_id in render.load_catalogue()}
        self.assertEqual(len(seeds), len(render.load_catalogue()), "expected no seed collisions across the real catalogue")

    def test_seed_is_a_valid_32_bit_unsigned_value(self):
        for line_id in ("trade_completed.1", "hurt.2", "panic.2"):
            seed = render.derive_seed(line_id)
            self.assertIsInstance(seed, int)
            self.assertTrue(0 <= seed < 2**32)


class ClonePostChainsTest(unittest.TestCase):
    """`CLONE_POST_CHAINS` (round four, `AUDIO-DEC-006`): a light tail only, unlike Piper's heavier
    `SOX_CHAINS`, plus a "none" control variant that changes nothing."""

    def test_none_chain_is_empty(self):
        self.assertEqual(render.CLONE_POST_CHAINS["none"], [])

    def test_tail_chain_has_no_pitch_or_tempo_effect(self):
        # Unlike Piper's chains, the clone engine's timbre comes from the reference itself — its
        # post-processing must never reach for `pitch` or `tempo`, only tone-shaping and loudness.
        tail = render.CLONE_POST_CHAINS["tail"]
        self.assertNotIn("pitch", tail)
        self.assertNotIn("tempo", tail)
        self.assertIn("norm", tail)


class ChainsForEngineTest(unittest.TestCase):
    def test_piper_returns_sox_chains(self):
        self.assertIs(render.chains_for_engine("piper"), render.SOX_CHAINS)

    def test_chatterbox_returns_clone_post_chains(self):
        self.assertIs(render.chains_for_engine("chatterbox"), render.CLONE_POST_CHAINS)

    def test_unknown_engine_raises(self):
        with self.assertRaises(render.PipelineError):
            render.chains_for_engine("nonsense")


class RenderLineDispatchTest(unittest.TestCase):
    """`render_line`'s own validation branches, all reachable without either backend's heavy
    dependency (piper binary / chatterbox+torch) installed."""

    def test_unknown_engine_raises(self):
        with self.assertRaises(render.PipelineError):
            render.render_line("nonsense", "trade_completed.1", "Traded! Nice.", Path("/tmp/out.ogg"), "deep")

    def test_piper_without_model_raises(self):
        with self.assertRaises(render.PipelineError):
            render.render_line("piper", "trade_completed.1", "Traded! Nice.", Path("/tmp/out.ogg"), "deep")

    def test_chatterbox_without_reference_raises(self):
        with self.assertRaises(render.PipelineError):
            render.render_line("chatterbox", "trade_completed.1", "Traded! Nice.", Path("/tmp/out.ogg"), "tail")


class ReferenceSetsTest(unittest.TestCase):
    """`reference.REFERENCE_SETS` — the three named reference sets round four samples against
    (`AUDIO-DEC-006`)."""

    def test_all_contains_every_clip_in_talking_and_idle(self):
        self.assertTrue(set(reference.REFERENCE_SETS["talking"]) <= set(reference.REFERENCE_SETS["all"]))
        self.assertTrue(set(reference.REFERENCE_SETS["idle"]) <= set(reference.REFERENCE_SETS["talking"]))

    def test_idle_is_the_smallest_set(self):
        sizes = {name: len(clips) for name, clips in reference.REFERENCE_SETS.items()}
        self.assertEqual(min(sizes, key=sizes.get), "idle")

    def test_no_duplicate_clips_within_a_set(self):
        for name, clips in reference.REFERENCE_SETS.items():
            self.assertEqual(len(clips), len(set(clips)), f"{name}: duplicate clip name")


class AssetIndexResolutionTest(unittest.TestCase):
    """Asset-index resolution against a fake fabric-loom asset cache (index JSON + `objects/`
    layout), so this is testable without a real Minecraft client install."""

    def _fake_assets_root(self, tmp: Path, clip_names=("idle1", "idle2")) -> tuple[Path, dict[str, str]]:
        assets_root = tmp / "assets"
        indexes_dir = assets_root / "indexes"
        objects_dir = assets_root / "objects"
        indexes_dir.mkdir(parents=True)
        objects_dir.mkdir(parents=True)
        objects = {}
        for i, name in enumerate(clip_names):
            clip_hash = f"{'a' * 38}{i:02d}"  # 40 hex-shaped chars, unique per clip
            key = f"{reference.VANILLA_VILLAGER_PREFIX}{name}.ogg"
            objects[key] = {"hash": clip_hash, "size": 1234}
            obj_dir = objects_dir / clip_hash[:2]
            obj_dir.mkdir(exist_ok=True)
            (obj_dir / clip_hash).write_bytes(b"fake-ogg-bytes")
        index_path = indexes_dir / "26.2-32.json"
        index_path.write_text(json.dumps({"objects": objects}), encoding="utf-8")
        return assets_root, {k: v["hash"] for k, v in objects.items()}

    def test_load_asset_index(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, expected = self._fake_assets_root(Path(tmp))
            index = reference.load_asset_index(assets_root / "indexes" / "26.2-32.json")
            self.assertEqual(index, expected)

    def test_find_default_asset_index_single_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, _ = self._fake_assets_root(Path(tmp))
            found = reference.find_default_asset_index(assets_root)
            self.assertEqual(found, assets_root / "indexes" / "26.2-32.json")

    def test_find_default_asset_index_none_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root = Path(tmp) / "assets"
            (assets_root / "indexes").mkdir(parents=True)
            with self.assertRaises(reference.ReferenceError):
                reference.find_default_asset_index(assets_root)

    def test_find_default_asset_index_multiple_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, _ = self._fake_assets_root(Path(tmp))
            (assets_root / "indexes" / "other.json").write_text("{}", encoding="utf-8")
            with self.assertRaises(reference.ReferenceError):
                reference.find_default_asset_index(assets_root)

    def test_resolve_object_found(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, hashes = self._fake_assets_root(Path(tmp))
            clip_hash = next(iter(hashes.values()))
            path = reference.resolve_object(clip_hash, assets_root)
            self.assertTrue(path.exists())
            self.assertEqual(path.read_bytes(), b"fake-ogg-bytes")

    def test_resolve_object_missing_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, _ = self._fake_assets_root(Path(tmp))
            with self.assertRaises(reference.ReferenceError):
                reference.resolve_object("f" * 40, assets_root)

    def test_resolve_clips_in_order(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, hashes = self._fake_assets_root(Path(tmp), clip_names=("idle1", "idle2", "idle3"))
            index = reference.load_asset_index(assets_root / "indexes" / "26.2-32.json")
            paths = reference.resolve_clips(("idle3", "idle1"), index, assets_root)
            self.assertEqual(len(paths), 2)
            self.assertEqual(paths[0].name, hashes[f"{reference.VANILLA_VILLAGER_PREFIX}idle3.ogg"])
            self.assertEqual(paths[1].name, hashes[f"{reference.VANILLA_VILLAGER_PREFIX}idle1.ogg"])

    def test_resolve_clips_missing_clip_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, _ = self._fake_assets_root(Path(tmp), clip_names=("idle1",))
            index = reference.load_asset_index(assets_root / "indexes" / "26.2-32.json")
            with self.assertRaises(reference.ReferenceError):
                reference.resolve_clips(("haggle1",), index, assets_root)

    def test_build_named_reference_unknown_set_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            assets_root, _ = self._fake_assets_root(Path(tmp))
            with self.assertRaises(reference.ReferenceError):
                reference.build_named_reference(
                    "nonsense", Path(tmp) / "out.wav", Path(tmp) / "scratch",
                    assets_root=assets_root, asset_index_path=assets_root / "indexes" / "26.2-32.json",
                )


if __name__ == "__main__":
    unittest.main()
