---
schema_version: 1
id: 01M30STC9MY3ZTD1911D1MS1HW
key: VV-19
type: docs
title: "Modrinth icon: the villager head with a cream speech bubble on plain navy"
created_by: kevin
created_at: 2026-09-21T01:38:19Z
---

## Scope

Regenerate `docs/modrinth/icon.png` per Kevin's 2026-09-21 icon ruling: the vanilla villager's
own head, rendered in real 3D from vanilla's `villager.png` entity texture, composited onto the
plain cubealgos navy badge (not this mod's own parchment badge, and not the Create-grid navy
badge either), with a small cream speech bubble top-right holding three dots. This replaces the
mod's current from-scratch parchment-and-speech-bubble badge entirely.

Out of scope: any other Modrinth listing content (summary, categories, links, changelog); any
change to the mod's actual in-game behaviour, reaction system, or line catalogue.

## Approach

Ports the inline 3D projector and navy-badge pattern the sibling repos' `tools/icon.py` already
use (`create_metered_motor` for the projector: `rot_axis`, `display_transform`,
`affine_from_points`, `invert_affine`, `build_faces`, `fit_scale`, `render_model`, the unchanged
`SHADE` shading table; `grounded_villages` for the box-UV helper and the plain-navy,
never-vendor-the-texture jar-sourcing pattern) directly into this repo's own `tools/icon.py`, with
no import of heimathafen or a sibling repo at build time.

The subject is two cuboid elements against vanilla's own 64x64 `villager.png`: `head`
(4,3,4)-(12,13,12), box-UV texOffs (0,0) size 8x10x8, and `nose` (7,4,2)-(9,8,4), box-UV texOffs
(24,0) size 2x4x2. No hat/hood layer -- the second head-layer UV region at texOffs (32,0) is
fully transparent on the base (unemployed) villager skin. The GUI display tilt is `[25, 200,
-12]` (picked from a rendered candidate sheet; the siblings' fleet tilt `[30, 315, -45]` was
tried and rejected because it hides the nose and one eye), fit box 224px (70% of the fleet's
standard 320px box), LANCZOS "smooth" compose mode since this is an anti-aliased 3D render, not a
pixel-art texture crop.

The badge is the plain cubealgos navy disc (white rim, pale band, dark ring, blueprint-navy
disc; no blueprint grid, no centre glow) -- the same choice `grounded_villages` already made for
its own icon, since villager_voices is likewise not a Create Fly add-on.

The speech bubble reuses this repo's own existing bubble shape code (rounded-rectangle body plus
triangular tail, three subtitle dots), resized and repositioned to sit top-right of the smaller
subject, with its fill recoloured to a cream sampled directly from the icon it replaces, and its
outline and dots recoloured to dark navy pulled from the badge's own palette (see Constraints
below for the exact values and reasoning).

The villager texture is read straight out of a local Minecraft client jar at build time --
globbing
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/*.jar`
first (verified via a zip-entry check), falling back to
`~/.gradle/caches/fabric-loom/26.2/**/*.jar`, with a `--jar PATH` override -- and is never
vendored into this repo.

## Acceptance criteria

- [x] `tools/icon.py` regenerates `docs/modrinth/icon.png` deterministically from the jar, with
      no vendored texture file added to the repo
- [x] `NOTICE` documents the jar-sourced asset (which texture, that it's composed at build time,
      that only the rendered composite is committed)
- [x] `docs/modrinth/body.md`'s icon row and the `justfile`'s `icon:` recipe comment describe the
      new render (villager head, plain navy badge, cream bubble), not the old parchment badge
- [x] `just map` (or `python3 tools/map.py`) and the available `tools/` tests still pass
- [x] `docs/modrinth/icon.png` is a 512x512 PNG, visually verified at both full size and a 64px
      downscale (head readable, both eyes and nose visible, bubble dots legible)

## Constraints and prior findings

- GUI display tilt: `[25, 200, -12]` (not the sibling fleet tilt `[30, 315, -45]`, which hides
  the nose and one eye on this subject).
- Fit box: 224px (`EFFECTIVE_BOX = round(320 * 0.7)`), LANCZOS "smooth" compose mode.
- Villager texture path: `assets/minecraft/textures/entity/villager/villager.png`, read from
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/
  minecraft-merged-deobf-26.2.jar` in this environment (26.2 client jar).
- Head cuboid: `(4,3,4)`-`(12,13,12)`, box-UV texOffs `(0,0)` size `8,10,8`. Nose cuboid:
  `(7,4,2)`-`(9,8,4)`, box-UV texOffs `(24,0)` size `2,4,2`. No hat/hood layer (texOffs `(32,0)`
  is fully transparent on the base skin).
- Bubble fill `(247, 231, 196, 255)`: sampled directly from the shipped icon this replaces via
  `Image.open("docs/modrinth/icon.png").convert("RGBA").getpixel((256, 6))` -- the badge's own
  outer RIM/cream tone, not invented.
- Bubble outline and dots: dark navy, `(9, 12, 27, 255)` (badge `RING`) for the outline and
  `(13, 18, 38, 255)` (badge `BLUEPRINT`) for the dots -- **not** a darker cream. Cream-on-cream
  (dots on the new cream fill) is illegible at 64px, which is exactly why the old
  green-fill/cream-dot pairing can't simply carry over with the fill swapped. Navy dots give the
  strongest contrast against the cream fill at thumbnail size and visually tie the bubble back to
  the badge it sits on; a darker-cream dot was tried by eye and reads as only a faint, muddy
  variation of the fill once shrunk to 64px. Decided by rendering both and comparing at 512px and
  a 64px downscale.
