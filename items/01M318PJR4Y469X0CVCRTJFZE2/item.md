---
schema_version: 1
id: 01M318PJR4Y469X0CVCRTJFZE2
key: VV-20
type: feat
title: "Plain spoken lines: the 64 reactions rewritten without written grunts"
created_by: kevin
created_at: 2026-09-21T05:58:23Z
---

## Scope

Rewrite all 64 lines in the 1.0 catalogue (`docs/spec/domains/reaction-lines.md` §3, the 16
`data/villager_voices/reaction/*.json` files, and the matching `subtitles.villager_voices.reaction.*`
lang keys) as plain spoken sentences a villager would actually say, dropping every written
grunt/stammer/interjection string ("Ow! Ow ow ow!", "Hngh", "Mrgh", "Zzz.", "Wha—", "No—!",
"Ah— not now.", the "..." trail-offs). In scope only: subtitle/lang text and the now-dead `spoken`
overrides on `sleep.3`/`killed.3`. Not in scope: the `grunt` field (unchanged per event, VV-18's own
choice stands), the event list or per-event line count (16 x 4 = 64, unchanged), any Java
registration/codec code, and the render pipeline itself (VV-11's own ticket covers the
`open_warm_mix` chain and the batch render).

## Approach

1. Vault first: rewrite the line tables in
   `heimathafen/vault/projects/villager_voices/spec/domains/reaction-lines.md` §3, record the
   ruling as `LINES-DEC-001`, then `just spec-sync` to refresh `docs/spec/`.
2. Update the 16 catalogue JSON files' `subtitle` fields to the new text; drop the `spoken`
   override on `sleep.3` and `killed.3` (both new subtitles are already plainly pronounceable, so
   the override is dead weight — `domains/reaction-lines.md` §2's own rule: drop `spoken` once the
   subtitle it existed to fix is gone).
3. Update `fabric/src/main/resources/assets/villager_voices/lang/en_us.json`'s 64
   `subtitles.villager_voices.reaction.*` keys to match the catalogue 1:1
   (`DefaultCatalogueResourcesTest.everyLineSubtitleMatchesItsLangKeyTextOneToOne`).
4. No Java test changes needed: `CatalogueCodecTest`/`CatalogueTest`/`MiniJsonTest` exercise the
   codec's parsing mechanics against their own fixture strings, not the real catalogue's text;
   `DefaultCatalogueResourcesTest`/`SoundsJsonResourcesTest`/`ReactionSoundGameTest` check
   structural consistency (counts, uniqueness, subtitle/lang/sound-id agreement, non-blank text),
   never pin specific wording — verified by reading all of them before writing this ticket.

## Acceptance criteria

- [x] All 64 catalogue lines are original, natural, under-twelve-word spoken sentences with no
      written grunt, stammer, or interjection string, and no trailing "..." used as a stand-in for
      a sound.
- [x] Every line's `grunt` field and its event/count are unchanged from the current 1.0 catalogue.
- [x] The `spoken` field is gone from every line (both prior uses were made redundant by the
      rewrite itself).
- [x] `fabric/.../lang/en_us.json`'s 64 subtitle keys match the catalogue text 1:1.
- [x] `docs/spec/domains/reaction-lines.md` reflects the new line tables and cites `LINES-DEC-001`,
      synced from the vault, never hand-edited directly.
- [x] `just check` is green (`common`/`fabric` tests, including
      `DefaultCatalogueResourcesTest`/`SoundsJsonResourcesTest`, and `just doctor`/`just map` stay
      clean — no public-API shape changed, so no map regen is expected).

## Constraints and prior findings

Kevin, 2026-09-21, on round eight's samples/lines: "the lines aren't very good themselves; they do
too much 'um, ahh, ouhdfubv' for my liking; they don't need to in their lines, they can talk now."
Examples he gave directly: "That hurt.", "Careful, that is my last one.", "Good trade. Come back
tomorrow.", "Danger. Get inside." The game's own vanilla grunt still plays first on every line that
names one (`AUDIO-REQ-007`, VV-18) — this ticket only changes what the villager *says* after that,
never whether a grunt plays. This ticket does not touch the voice pipeline, the `open_warm_mix`
chain, or the 64-line audio batch (VV-11) — line text lands first so the batch renders the final
wording, not a placeholder that needs a second pass. Not claimed and no branch created by this
change — the coordinator (VV-11's own work) books it; do not merge or finish it from here.
