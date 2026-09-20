"""Unit tests for the input-text rule and catalogue loading (`tools/voices/render.py`).

No test here touches Piper, sox, or the network — those live behind `render_line`, exercised
manually via `just voices-sample` (`tools/voices/README.md`). This file is what `just test-tools`
runs."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
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
    (`AUDIO-DEC-004`). Round 3 deliberately tests a small pitch-up again (`villager_above`,
    `AUDIO-DEC-005`) — vanilla's own measured median f0 sits *above* `en_US-norman-medium`'s
    natural pitch, not below it, so landing on it means going up — but only a modest +100 cents,
    only as one of three explicitly-named `villager_*` sample chains for Kevin's listen, never as
    a silent change to the pipeline's default direction."""

    def test_every_chain_starts_with_a_pitch_effect(self):
        for name, args in render.SOX_CHAINS.items():
            self.assertEqual(args[0], "pitch", f"{name}: expected 'pitch' first, got {args!r}")

    def test_pitch_is_within_the_explored_range(self):
        for name, args in render.SOX_CHAINS.items():
            cents = int(args[1])
            self.assertTrue(-500 <= cents <= 100, f"{name}: pitch {cents} outside the explored range")

    def test_only_named_villager_chains_pitch_up_or_flat(self):
        for name, args in render.SOX_CHAINS.items():
            cents = int(args[1])
            if cents >= 0:
                self.assertTrue(name.startswith("villager_"), f"{name}: pitch {cents} is up/flat but not a named villager_* chain")

    def test_every_chain_shares_the_same_tail_after_pitch(self):
        tails = {tuple(args[2:]) for args in render.SOX_CHAINS.values()}
        self.assertEqual(len(tails), 1, "every chain should share the same nasal/dull/loudness tail")

    def test_deep_and_deeper_share_everything_but_the_pitch_depth(self):
        self.assertEqual(render.SOX_CHAINS["deep"][2:], render.SOX_CHAINS["deeper"][2:])
        self.assertNotEqual(render.SOX_CHAINS["deep"][1], render.SOX_CHAINS["deeper"][1])


class SampleTextOverridesTest(unittest.TestCase):
    """Round 3's `AUDIO-DEC-005` overrides — the written grunt removed for the sample slice only,
    since the game now plays the vanilla grunt itself (`AUDIO-REQ-007`)."""

    def test_every_override_key_is_a_sample_line(self):
        self.assertTrue(set(render.SAMPLE_TEXT_OVERRIDES) <= set(render.SAMPLE_LINE_IDS))

    def test_overrides_have_no_leading_villager_grunt(self):
        for line_id, text in render.SAMPLE_TEXT_OVERRIDES.items():
            subtitle = render.load_catalogue()[line_id]
            self.assertNotEqual(text, subtitle, f"{line_id}: override should differ from the subtitle")
            self.assertTrue(text[0].isupper(), f"{line_id}: {text!r} should read as a clean sentence")


class LoadCatalogueTest(unittest.TestCase):
    def test_reads_all_64_lines_from_the_real_catalogue(self):
        catalogue = render.load_catalogue()
        self.assertEqual(len(catalogue), 64)
        self.assertEqual(catalogue["trade_completed.1"], "Mrrgh — traded! Nice.")

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


if __name__ == "__main__":
    unittest.main()
