"""Unit tests for the input-text transform and catalogue loading (`tools/voices/render.py`).

No test here touches Piper, sox, or the network — those live behind `render_line`, exercised
manually via `just voices-sample` (`tools/voices/README.md`). This file is what `just test-tools`
runs (`AUDIO-REQ-006`'s reproducibility claim is what `test_derive_input_text_is_deterministic`
checks)."""
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import render  # noqa: E402


class DeriveInputTextTest(unittest.TestCase):
    def test_deterministic_for_same_inputs(self):
        text_a = render.derive_input_text("trade_completed.1", "Mrrgh — traded! Nice.")
        text_b = render.derive_input_text("trade_completed.1", "Mrrgh — traded! Nice.")
        self.assertEqual(text_a, text_b)

    def test_differs_by_line_id(self):
        subtitle = "Mrrgh — traded! Nice."
        text_a = render.derive_input_text("trade_completed.1", subtitle)
        text_b = render.derive_input_text("trade_completed.2", subtitle)
        self.assertNotEqual(text_a, text_b)

    def test_differs_when_subtitle_edited(self):
        text_a = render.derive_input_text("trade_completed.1", "Mrrgh — traded! Nice.")
        text_b = render.derive_input_text("trade_completed.1", "Mrrgh — traded! Great.")
        self.assertNotEqual(text_a, text_b)

    def test_other_lines_unaffected_by_one_subtitle_edit(self):
        before = render.derive_input_text("trade_completed.2", "Hmnh, good trade, that.")
        # Editing a different line's subtitle must not perturb this line's derived text — each
        # line's RNG stream is seeded from its own (line_id, subtitle) pair only.
        _ = render.derive_input_text("trade_completed.1", "a completely different subtitle now")
        after = render.derive_input_text("trade_completed.2", "Hmnh, good trade, that.")
        self.assertEqual(before, after)

    def test_never_reuses_subtitle_words(self):
        subtitle = "Mrrgh — traded! Nice."
        text = render.derive_input_text("trade_completed.1", subtitle)
        subtitle_words = {w.lower() for w in re.findall(r"[A-Za-z']+", subtitle)}
        text_words = {w.lower() for w in re.findall(r"[A-Za-z']+", text)}
        self.assertFalse(subtitle_words & text_words, f"{text!r} reuses a subtitle word from {subtitle!r}")

    def test_punctuation_follows_subtitle_terminal_mark(self):
        self.assertTrue(render.derive_input_text("offer_opened.1", "Lookin' to trade?").endswith("?"))
        self.assertTrue(render.derive_input_text("hurt.1", "Ow! Ow ow ow!").endswith("!"))
        self.assertTrue(render.derive_input_text("zombified.4", "Hrrgh...").endswith("..."))

    def test_nonempty_for_every_catalogue_line(self):
        for line_id, subtitle in render.load_catalogue().items():
            text = render.derive_input_text(line_id, subtitle)
            self.assertTrue(text.strip(), f"{line_id} produced empty input text")


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


if __name__ == "__main__":
    unittest.main()
