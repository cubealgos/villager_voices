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
    """`derive_input_text` never scrambles into nonsense/CV-syllables (`AUDIO-DEC-004` still
    stands) — but since round six (`AUDIO-DEC-006` amendment, Kevin: "they can't pronounce stuff
    like 'ouuchh' properly, sounds like letter salad") it is no longer a strict verbatim no-op: an
    explicit `spoken` override wins outright, and a line without one is run through
    `normalize_spoken_text`, which is itself a no-op for a subtitle that's already plainly
    pronounceable."""

    def test_an_explicit_spoken_override_wins_outright(self):
        # Deliberately a string normalize_spoken_text would itself change, to prove the override
        # bypasses it rather than being run through it too.
        self.assertEqual(render.derive_input_text("sleep.3", "Zzz.", "Shh."), "Shh.")

    def test_no_override_falls_back_to_the_normalizer(self):
        self.assertEqual(
            render.derive_input_text("trade_completed.2", "Hmnh, good trade, that."),
            render.normalize_spoken_text("Hmnh, good trade, that."),
        )

    def test_already_plain_subtitle_is_unchanged_by_the_fallback(self):
        subtitle = "That hurt!"
        self.assertEqual(render.derive_input_text("hurt.2", subtitle), subtitle)

    def test_ignores_line_id(self):
        subtitle = "Hmnh, good trade, that."
        text_a = render.derive_input_text("trade_completed.2", subtitle)
        text_b = render.derive_input_text("some_other.9", subtitle)
        self.assertEqual(text_a, text_b)

    def test_matches_every_catalogue_line_s_own_spoken_form(self):
        # Every line either has an explicit "spoken" (used verbatim) or falls back to the
        # normalizer -- this pins down that derive_input_text never does anything a third thing.
        for line_id, entry in render.load_catalogue_entries().items():
            expected = entry.get("spoken") or render.normalize_spoken_text(entry["subtitle"])
            self.assertEqual(render.derive_input_text(line_id, entry["subtitle"], entry.get("spoken")), expected)


class NormalizeSpokenTextTest(unittest.TestCase):
    """The round-six (`AUDIO-DEC-006` amendment) fallback normalizer: interjection-table lookup,
    letter-run collapse, em-dash handling, repeated-punctuation stripping."""

    def test_plain_text_is_unchanged(self):
        self.assertEqual(render.normalize_spoken_text("That hurt!"), "That hurt!")

    def test_known_interjection_already_correct_is_unchanged(self):
        for word in ("Ha!", "Hmm,", "Ah,", "Ow!"):
            self.assertEqual(render.normalize_spoken_text(word), word)

    def test_misspelled_interjection_maps_to_canonical_form(self):
        self.assertEqual(render.normalize_spoken_text("Hmnh, good trade, that."), "Hmm, good trade, that.")
        self.assertEqual(render.normalize_spoken_text("Mmh-hmm, pleasure doing business."), "Mm-hmm, pleasure doing business.")
        self.assertEqual(render.normalize_spoken_text("Ouuchh!"), "Ouch!")

    def test_interjection_lookup_is_case_insensitive_but_preserves_case(self):
        self.assertEqual(render.normalize_spoken_text("hmnh, quiet now."), "hmm, quiet now.")
        self.assertEqual(render.normalize_spoken_text("Hmnh, quiet now."), "Hmm, quiet now.")

    def test_letter_run_of_three_or_more_consonants_collapses_to_two(self):
        self.assertEqual(render.normalize_spoken_text("Owwwww!"), "Oww!")

    def test_letter_run_of_three_or_more_vowels_collapses_to_one(self):
        self.assertEqual(render.normalize_spoken_text("Nooooo!"), "No!")

    def test_a_pre_existing_double_letter_is_never_touched(self):
        # The bug this test guards against: an earlier version of the collapse rule matched *any*
        # doubled vowel anywhere, not just a 3+ run, and turned "good"/"look" into "god"/"lok".
        for word in ("good", "look", "been", "feet", "off", "all", "less", "Hmm"):
            self.assertEqual(render.normalize_spoken_text(word), word)

    def test_em_dash_mid_line_becomes_a_comma_pause(self):
        self.assertEqual(render.normalize_spoken_text("Ah— not now."), "Ah, not now.")

    def test_em_dash_at_end_of_clause_is_dropped(self):
        self.assertEqual(render.normalize_spoken_text("No—!"), "No!")

    def test_repeated_punctuation_collapses_to_one_mark(self):
        self.assertEqual(render.normalize_spoken_text("Cold..."), "Cold.")
        self.assertEqual(render.normalize_spoken_text("What??"), "What?")

    def test_idempotent(self):
        # Running the normalizer twice should never change its own output further.
        for text in ("Hmnh, good trade, that.", "Ah— not now.", "Owwwww!", "Nooooo!", "Cold..."):
            once = render.normalize_spoken_text(text)
            twice = render.normalize_spoken_text(once)
            self.assertEqual(once, twice)


class InterjectionTableTest(unittest.TestCase):
    def test_every_value_is_one_of_the_seven_canonical_forms(self):
        canonical = {"Ow", "Ouch", "Ah", "Hmm", "Mm-hmm", "Ha", "Psh", "Grrr"}
        self.assertTrue(set(render.INTERJECTION_TABLE.values()) <= canonical)

    def test_every_key_is_lowercase(self):
        for key in render.INTERJECTION_TABLE:
            self.assertEqual(key, key.lower(), f"{key!r} should be stored lowercase")


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


class LoadCatalogueEntriesTest(unittest.TestCase):
    """`load_catalogue_entries` (VV-11 round six) -- the full-entry view `load_catalogue` (subtitle
    only) is now a thin wrapper over."""

    def test_matches_load_catalogue_on_subtitle(self):
        entries = render.load_catalogue_entries()
        catalogue = render.load_catalogue()
        self.assertEqual({lid: e["subtitle"] for lid, e in entries.items()}, catalogue)

    def test_sleep_3_has_the_expected_spoken_override(self):
        entries = render.load_catalogue_entries()
        self.assertEqual(entries["sleep.3"]["subtitle"], "Zzz.")
        self.assertEqual(entries["sleep.3"].get("spoken"), "Shh.")

    def test_killed_3_has_the_expected_spoken_override(self):
        entries = render.load_catalogue_entries()
        self.assertEqual(entries["killed.3"].get("spoken"), "What, no!")

    def test_most_lines_have_no_spoken_override(self):
        entries = render.load_catalogue_entries()
        with_override = [lid for lid, e in entries.items() if e.get("spoken")]
        self.assertEqual(sorted(with_override), ["killed.3", "sleep.3"])


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

    def test_round_five_chains_present(self):
        # AUDIO-DEC-006 amendment: "warm"/"soft"/"plain-warm", none reaching for pitch/tempo either.
        for name in ("warm", "soft", "plain-warm"):
            self.assertIn(name, render.CLONE_POST_CHAINS)
            chain = render.CLONE_POST_CHAINS[name]
            self.assertNotIn("pitch", chain)
            self.assertNotIn("tempo", chain)
            self.assertIn("norm", chain)

    def test_soft_is_darker_than_warm(self):
        # "soft" = "warm" but a lower lowpass and added reverb (tools/voices/render.py comments).
        warm_lowpass = int(render.CLONE_POST_CHAINS["warm"][render.CLONE_POST_CHAINS["warm"].index("lowpass") + 1])
        soft_lowpass = int(render.CLONE_POST_CHAINS["soft"][render.CLONE_POST_CHAINS["soft"].index("lowpass") + 1])
        self.assertLess(soft_lowpass, warm_lowpass)
        self.assertIn("reverb", render.CLONE_POST_CHAINS["soft"])
        self.assertNotIn("reverb", render.CLONE_POST_CHAINS["warm"])

    def test_plain_warm_is_eq_only(self):
        plain = render.CLONE_POST_CHAINS["plain-warm"]
        self.assertNotIn("compand", plain)
        self.assertNotIn("reverb", plain)
        self.assertNotIn("highpass", plain)
        self.assertIn("equalizer", plain)

    def test_round_six_villager_chains_present(self):
        # AUDIO-DEC-006 amendment: a human-voice reference now supplies the base timbre, so
        # "villager_mild"/"villager_pitch" only add back a mild villager character on top.
        for name in ("villager_mild", "villager_pitch"):
            self.assertIn(name, render.CLONE_POST_CHAINS)
            self.assertIn("norm", render.CLONE_POST_CHAINS[name])

    def test_villager_pitch_is_villager_mild_plus_a_pitch_shift(self):
        mild = render.CLONE_POST_CHAINS["villager_mild"]
        pitch = render.CLONE_POST_CHAINS["villager_pitch"]
        self.assertNotIn("pitch", mild)
        self.assertEqual(pitch[:2], ["pitch", "-150"])
        self.assertEqual(pitch[2:], mild)

    def test_villager_mild_bands_are_lower_and_gentler_than_round_four_five(self):
        # Round four/five's clone-of-a-clone chains centred their nasal lift/cut at 1600/3200Hz with
        # a +9/-4 swing; round six's human-voice-based chain is centred lower (1200/2600Hz) and
        # much gentler (+3/-3), since the reference itself is no longer contributing the metallic
        # edge those chains had to fight.
        mild = render.CLONE_POST_CHAINS["villager_mild"]
        self.assertEqual(mild[:4], ["equalizer", "1200", "1q", "+3"])
        self.assertEqual(mild[4:8], ["equalizer", "2600", "1.5q", "-3"])

    def test_round_seven_chains_present(self):
        # AUDIO-DEC-006 final amendment: "open"/"open_warm"/"dry"/"open_tempo" -- Kevin: "it still
        # sounds like someone is speaking into a tin can." No lowpass under 10kHz anywhere, and no
        # 1200Hz boost.
        for name in ("open", "open_warm", "dry", "open_tempo"):
            self.assertIn(name, render.CLONE_POST_CHAINS)
            chain = render.CLONE_POST_CHAINS[name]
            self.assertIn("norm", chain)
            self.assertNotIn("1200", chain)
            if "lowpass" in chain:
                cutoff = int(chain[chain.index("lowpass") + 1])
                self.assertGreaterEqual(cutoff, 10000)

    def test_round_seven_chains_upsample_explicitly(self):
        # Chatterbox's native rate is 24kHz; round seven upsamples explicitly and at high quality
        # (`rate -v`) rather than relying on the output format flag's own resample.
        for name in ("open", "open_warm", "dry", "open_tempo"):
            chain = render.CLONE_POST_CHAINS[name]
            self.assertEqual(chain[:3], ["rate", "-v", "44100"])

    def test_open_tempo_is_open_plus_a_tempo_stretch(self):
        open_chain = render.CLONE_POST_CHAINS["open"]
        open_tempo = render.CLONE_POST_CHAINS["open_tempo"]
        self.assertEqual(open_tempo[:-4], open_chain[:-2])
        self.assertEqual(open_tempo[-4:-2], ["tempo", "0.92"])
        self.assertEqual(open_tempo[-2:], ["norm", "-3"])

    def test_open_warm_body_present_and_steep_upsample(self):
        # Round eight (AUDIO-DEC-006 final amendment, chain family decided: open_warm): Kevin,
        # "open_warm is good, but the audio still sounds a bit noisy/hollow." open_warm_body adds
        # low-mid body and top-end presence and switches to a steep upsample so nothing aliases.
        body = render.CLONE_POST_CHAINS["open_warm_body"]
        self.assertEqual(body[:4], ["rate", "-v", "-s", "44100"])
        self.assertIn("300", body)
        self.assertIn("5000", body)
        self.assertEqual(body[-2:], ["norm", "-3"])

    def test_open_warm_body_extends_open_warm(self):
        # open_warm_body is open_warm's own tail (everything after the steep-upsample swap) plus
        # the extra low-mid/presence EQ.
        warm = render.CLONE_POST_CHAINS["open_warm"]
        body = render.CLONE_POST_CHAINS["open_warm_body"]
        self.assertEqual(warm[3:-2], body[4:len(warm) - 1])

    def test_open_warm_body_gate_adds_a_compand_before_the_final_normalize(self):
        body = render.CLONE_POST_CHAINS["open_warm_body"]
        gate = render.CLONE_POST_CHAINS["open_warm_body_gate"]
        self.assertEqual(gate[:-8], body[:-2])
        self.assertEqual(gate[-8], "compand")
        self.assertEqual(gate[-2:], ["norm", "-3"])

    def test_dry_is_resample_and_normalize_only(self):
        dry = render.CLONE_POST_CHAINS["dry"]
        self.assertEqual(dry, ["rate", "-v", "44100", "norm", "-3"])

    def test_open_warm_is_open_plus_low_end(self):
        open_chain = render.CLONE_POST_CHAINS["open"]
        open_warm = render.CLONE_POST_CHAINS["open_warm"]
        self.assertEqual(open_warm[:len(open_chain) - 2], open_chain[:-2])
        self.assertIn("bass", open_warm)


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
    """`reference.REFERENCE_SETS` — the named reference sets round four/five samples against
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

    def test_all_warm_drops_only_the_hit_clips_from_all(self):
        # Round five (AUDIO-DEC-006 amendment): all_warm = all minus the four hit* clips, which
        # measured as this villager's loudest/most-clipped source material.
        dropped = set(reference.REFERENCE_SETS["all"]) - set(reference.REFERENCE_SETS["all_warm"])
        self.assertEqual(dropped, {"hit1", "hit2", "hit3", "hit4"})
        self.assertTrue(set(reference.REFERENCE_SETS["all_warm"]) <= set(reference.REFERENCE_SETS["all"]))

    def test_all_warm_contains_talking(self):
        self.assertTrue(set(reference.REFERENCE_SETS["talking"]) <= set(reference.REFERENCE_SETS["all_warm"]))


class BuildReferenceWavPostProcessingTest(unittest.TestCase):
    """`build_reference_wav`'s round-five `lowpass_hz`/`tempo` post-processing arguments — checked
    at the sox-command-construction level via a stubbed `subprocess.run`, not a real sox call, so
    this runs without sox installed."""

    def _fake_run_factory(self, calls: list[list[str]]):
        class _Result:
            returncode = 0
            stderr = ""

        def _fake_run(cmd, **kwargs):
            calls.append(cmd)
            # build_reference_wav checks `out_wav.exists()` after the concatenation command; the
            # decode step also checks `wav_path.exists()` per clip — touch every `.wav` path any
            # call was asked to produce so both checks pass without a real sox binary.
            if cmd and cmd[0] == "sox":
                for tok in cmd:
                    if tok.endswith(".wav"):
                        Path(tok).parent.mkdir(parents=True, exist_ok=True)
                        Path(tok).touch()
            return _Result()

        return _fake_run

    def test_no_post_processing_by_default(self):
        calls: list[list[str]] = []
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            clip = tmp_path / "clip.ogg"
            clip.write_bytes(b"fake")
            original_run = reference.subprocess.run
            reference.subprocess.run = self._fake_run_factory(calls)
            try:
                reference.build_reference_wav([clip], tmp_path / "out.wav", tmp_path / "scratch")
            finally:
                reference.subprocess.run = original_run
        concat_cmd = calls[-1]
        self.assertNotIn("lowpass", concat_cmd)
        self.assertNotIn("tempo", concat_cmd)

    def test_lowpass_and_tempo_appended_to_concat_command(self):
        calls: list[list[str]] = []
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            clip = tmp_path / "clip.ogg"
            clip.write_bytes(b"fake")
            original_run = reference.subprocess.run
            reference.subprocess.run = self._fake_run_factory(calls)
            try:
                reference.build_reference_wav(
                    [clip], tmp_path / "out.wav", tmp_path / "scratch", lowpass_hz=7000, tempo=0.9,
                )
            finally:
                reference.subprocess.run = original_run
        concat_cmd = calls[-1]
        self.assertIn("lowpass", concat_cmd)
        self.assertIn("7000", concat_cmd)
        self.assertIn("tempo", concat_cmd)
        self.assertIn("0.9", concat_cmd)
        # re-normalized after the post-processing effects, not only once at the start
        self.assertEqual(concat_cmd.count("norm"), 2)


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
