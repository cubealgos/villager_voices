#!/usr/bin/env python3
"""Render docs/modrinth/icon.png: the vanilla villager's own head, real 3D rendered, on the
cubealgos navy badge (VV-19, Kevin's icon ruling, 2026-09-21).

villager_voices is not a create_civilization/Create Fly add-on
(docs/spec/00-context.md), so -- exactly as `grounded_villages`' `tools/icon.py` already
documents for its own icon -- the badge is the *plain* cubealgos navy disc: no blueprint grid,
no centre glow. That plain-navy choice is `standards/marketing/modrinth/navy-badge.py
--no-grid`'s ruling for mods that are not Create Fly add-ons; the badge disc/rim/ring/outline
constants below are unchanged from the Create-family badge, only the grid and its glow are left
out.

This replaces the mod's previous from-scratch parchment-and-speech-bubble badge, which sampled no
vanilla texture at all. The new subject is the villager's own head: two cuboid elements --
`head` (4,3,4)-(12,13,12), box-UV texOffs (0,0) size 8x10x8, and `nose` (7,4,2)-(9,8,4), box-UV
texOffs (24,0) size 2x4x2 -- textured straight from vanilla's own 64x64
`assets/minecraft/textures/entity/villager/villager.png`, using Minecraft's own box-UV layout
(the same `_box_uv` helper `grounded_villages`' `tools/icon.py` ported from
`ModelPart$Cube`'s constructor). There is deliberately no hat/hood layer: the second head-layer
UV region at texOffs (32,0) is 100% transparent on the base (unemployed) villager skin, so
rendering it would add nothing but a wasted face.

The GUI display tilt is `[25, 200, -12]`, not the sibling repos' fleet tilt of `[30, 315, -45]`
-- that fleet tilt was tried first and rejected because, on a head this shape, it hides the nose
and one eye; `[25, 200, -12]` was picked from a rendered candidate sheet specifically because it
keeps both readable. The head sits at 224px (70% of the fleet's standard 320px fit box), the same
`FIT = 0.7` reduction `grounded_villages` applies to its own pixel-art bell, here applied to an
anti-aliased 3D render instead -- LANCZOS "smooth" compose mode, not a pixel-art zoom.

Speech bubble: same rounded-rectangle-plus-tail silhouette and three-dot layout this mod's own
previous `tools/icon.py` already drew, kept because the shape itself was never the problem -- only
the colours were. The old bubble was villager-robe green with cream dots; Kevin's ruling keeps the
bubble but recolours it cream (`BUBBLE_FILL`, sampled directly from the shipped icon this replaces
-- see the constant's own comment -- so it matches this mod's own parchment tone exactly, not an
invented approximation) to fit the new navy badge, where green would look like a leftover. Cream
dots on a cream fill would be invisible at 64px (exactly why the old green-fill/cream-dot pairing
cannot just be re-used with the fill inverted), so `DOT` and `BUBBLE_OUTLINE` are both a dark navy
pulled from the badge's own palette instead: high contrast against the cream fill, and it visually
ties the bubble back to the badge it sits on.

Texture sourcing: the villager texture is read straight out of a local Minecraft client jar at
build time and is never vendored into this repo (NOTICE documents the read). The jar is found by
globbing
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/*.jar`
first, then falling back to any `~/.gradle/caches/fabric-loom/26.2/**/*.jar` that actually
contains the villager texture entry; pass `--jar PATH` to use a specific jar instead. Ports the
inline 3D projector and navy-badge code from the sibling repos' own `tools/icon.py`
(`create_metered_motor`, `grounded_villages`), including their unchanged `SHADE` table, so this
repo never imports from heimathafen or a sibling repo at build time. Requires Pillow.

Usage: python3 tools/icon.py [--jar PATH_TO_MINECRAFT_CLIENT_JAR]
"""
from __future__ import annotations

import argparse
import math
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "modrinth" / "icon.png"

GRADLE_CACHE = Path.home() / ".gradle" / "caches" / "fabric-loom"
PRIMARY_GLOB = GRADLE_CACHE / "minecraftMaven" / "net" / "minecraft" / "minecraft-merged-deobf" / "26.2"
FALLBACK_ROOT = GRADLE_CACHE / "26.2"
VILLAGER_TEXTURE = "assets/minecraft/textures/entity/villager/villager.png"

GUI_TILT = {"rotation": [25, 200, -12]}  # VV-19: keeps the nose and one eye readable; the
# siblings' fleet tilt [30, 315, -45] was tried first and rejected because it hides both.


# ============================================================== villager head geometry
# Two cuboid elements, box-UV against vanilla's own 64x64 villager.png. No hat/hood layer: the
# second head-layer UV region at texOffs (32,0) is 100% transparent on the base skin.
def _box_uv(tu: float, tv: float, dx: float, dy: float, dz: float) -> dict:
    """Minecraft's own box-UV layout, decompiled from ModelPart$Cube's constructor (ported from
    grounded_villages' tools/icon.py): a blank dz-wide gutter then down/up (each dx x dz) in row
    1, west/north/east/south (each dy tall) in row 2. "up" is vertically flipped relative to
    "down" (v1 > v2), matching the bytecode."""
    return {
        "down": [tu + dz, tv, tu + dz + dx, tv + dz],
        "up": [tu + dz + dx, tv + dz, tu + dz + 2 * dx, tv],
        "west": [tu, tv + dz, tu + dz, tv + dz + dy],
        "north": [tu + dz, tv + dz, tu + dz + dx, tv + dz + dy],
        "east": [tu + dz + dx, tv + dz, tu + 2 * dz + dx, tv + dz + dy],
        "south": [tu + 2 * dz + dx, tv + dz, tu + 2 * dz + 2 * dx, tv + dz + dy],
    }


def _cuboid_element(frm, to, tex_key: str, uv: dict) -> dict:
    return {
        "from": list(frm),
        "to": list(to),
        "faces": {face: {"uv": uv[face], "texture": f"#{tex_key}"} for face in uv},
    }


def villager_head_elements() -> list[dict]:
    head_uv = _box_uv(0, 0, 8, 10, 8)
    nose_uv = _box_uv(24, 0, 2, 4, 2)
    return [
        _cuboid_element((4, 3, 4), (12, 13, 12), "skin", head_uv),
        _cuboid_element((7, 4, 2), (9, 8, 4), "skin", nose_uv),
    ]


# ============================================================== 3D projection
# Ported inline from the sibling repos' tools/icon.py (create_metered_motor, grounded_villages),
# themselves ported from heimathafen's standards/marketing/modrinth/block-model-render.py.

RENDER_SIZE = 512
RENDER_SUPERSAMPLE = 4
RENDER_CANVAS = RENDER_SIZE * RENDER_SUPERSAMPLE

FACE_VERTS = {
    "down":  [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)],
    "up":    [(0, 1, 1), (0, 1, 0), (1, 1, 0), (1, 1, 1)],
    "north": [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    "south": [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    "west":  [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    "east":  [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
FACE_NORMAL = {
    "down": (0, -1, 0), "up": (0, 1, 0),
    "north": (0, 0, -1), "south": (0, 0, 1),
    "west": (-1, 0, 0), "east": (1, 0, 0),
}
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def rot_axis(p, axis, deg):
    ang = math.radians(deg)
    c, s = math.cos(ang), math.sin(ang)
    x, y, z = p
    if axis == "x":
        return (x, y * c - z * s, y * s + z * c)
    if axis == "y":
        return (x * c + z * s, y, -x * s + z * c)
    return (x * c - y * s, x * s + y * c, z)


def rotate_about(p, origin, axis, deg):
    rel = (p[0] - origin[0], p[1] - origin[1], p[2] - origin[2])
    r = rot_axis(rel, axis, deg)
    return (r[0] + origin[0], r[1] + origin[1], r[2] + origin[2])


def display_transform(p, pivot, rx, ry, rz):
    rel = (p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2])
    rel = rot_axis(rel, "x", rx)
    rel = rot_axis(rel, "y", ry)
    rel = rot_axis(rel, "z", rz)
    return rel


def affine_from_points(src, dst):
    """3-point affine solve: dst = A*src + t. Returns forward (a,b,c,d,e,f)."""
    (x0, y0), (x1, y1), (x2, y2) = src
    (u0, v0), (u1, v1), (u2, v2) = dst
    mat = [[x0, y0, 1], [x1, y1, 1], [x2, y2, 1]]
    det = (mat[0][0] * (mat[1][1] * mat[2][2] - mat[1][2] * mat[2][1])
           - mat[0][1] * (mat[1][0] * mat[2][2] - mat[1][2] * mat[2][0])
           + mat[0][2] * (mat[1][0] * mat[2][1] - mat[1][1] * mat[2][0]))
    if abs(det) < 1e-9:
        return None

    def solve(vals):
        res = []
        for col in range(3):
            m2 = [row[:] for row in mat]
            for r in range(3):
                m2[r][col] = vals[r]
            d = (m2[0][0] * (m2[1][1] * m2[2][2] - m2[1][2] * m2[2][1])
                 - m2[0][1] * (m2[1][0] * m2[2][2] - m2[1][2] * m2[2][0])
                 + m2[0][2] * (m2[1][0] * m2[2][1] - m2[1][1] * m2[2][0]))
            res.append(d / det)
        return res

    a, b, c = solve([u0, u1, u2])
    d, e, f = solve([v0, v1, v2])
    return (a, b, c, d, e, f)


def invert_affine(coef):
    a, b, c, d, e, f = coef
    det = a * e - b * d
    if abs(det) < 1e-9:
        return None
    ia = e / det
    ib = -b / det
    ic = -(ia * c + ib * f)
    id_ = -d / det
    ie = a / det
    if_ = -(id_ * c + ie * f)
    return (ia, ib, ic, id_, ie, if_)


def build_faces(model, textures, pivot, gui):
    """Returns list of (depth, canvas_quad[4], texture_img, uv_patch_quad[4], shade)."""
    faces = []
    rx, ry, rz = gui["rotation"]
    for elem in model["elements"]:
        frm, to = elem["from"], elem["to"]
        erot = elem.get("rotation")
        corners = {}
        for bx in (0, 1):
            for by in (0, 1):
                for bz in (0, 1):
                    p = (
                        frm[0] if bx == 0 else to[0],
                        frm[1] if by == 0 else to[1],
                        frm[2] if bz == 0 else to[2],
                    )
                    if erot:
                        p = rotate_about(p, erot["origin"], erot["axis"], erot["angle"])
                    corners[(bx, by, bz)] = p
        for face_name, face in elem.get("faces", {}).items():
            verts_frac = FACE_VERTS[face_name]
            verts3d = [corners[v] for v in verts_frac]

            normal = FACE_NORMAL[face_name]
            if erot:
                normal = rot_axis(normal, erot["axis"], erot["angle"])
            cam_normal = rot_axis(rot_axis(rot_axis(normal, "x", rx), "y", ry), "z", rz)
            if cam_normal[2] <= 1e-4:
                continue  # backface culled

            cam_pts = [display_transform(p, pivot, rx, ry, rz) for p in verts3d]
            depth = sum(p[2] for p in cam_pts) / 4.0
            canvas_quad = [(p[0], -p[1]) for p in cam_pts]

            tex_key = face["texture"].lstrip("#")
            tex_img = textures[tex_key]
            u1, v1, u2, v2 = face["uv"]
            flip_x = u1 > u2
            flip_y = v1 > v2
            lo = (min(u1, u2), min(v1, v2))
            hi = (max(u1, u2), max(v1, v2))
            patch = tex_img.crop((round(lo[0]), round(lo[1]), round(hi[0]), round(hi[1])))
            if patch.width == 0 or patch.height == 0:
                continue
            if flip_x:
                patch = patch.transpose(Image.FLIP_LEFT_RIGHT)
            if flip_y:
                patch = patch.transpose(Image.FLIP_TOP_BOTTOM)
            pw, ph = patch.size
            default_patch_quad = [(0, 0), (0, ph), (pw, ph), (pw, 0)]
            rotation = face.get("rotation", 0)
            shift = (rotation // 90) % 4
            patch_quad = [default_patch_quad[(i + shift) % 4] for i in range(4)]

            faces.append((depth, canvas_quad, patch, patch_quad, SHADE[face_name]))
    faces.sort(key=lambda f: f[0])  # far to near
    return faces


def fit_scale(faces, canvas, margin=0.88):
    xs, ys = [], []
    for _, quad, *_ in faces:
        for x, y in quad:
            xs.append(x)
            ys.append(y)
    w = max(xs) - min(xs)
    h = max(ys) - min(ys)
    cx = (max(xs) + min(xs)) / 2
    cy = (max(ys) + min(ys)) / 2
    scale = (canvas * margin) / max(w, h)
    return scale, cx, cy


def render_model(model, textures, gui):
    pivot = (8.0, 8.0, 8.0)
    faces = build_faces(model, textures, pivot, gui)
    scale, cx, cy = fit_scale(faces, RENDER_CANVAS)

    canvas = Image.new("RGBA", (RENDER_CANVAS, RENDER_CANVAS), (0, 0, 0, 0))
    for depth, quad, patch, patch_quad, shade in faces:
        dst = [((x - cx) * scale + RENDER_CANVAS / 2, (y - cy) * scale + RENDER_CANVAS / 2)
               for x, y in quad]
        patch = patch.convert("RGBA")
        if shade != 1.0:
            r, g, b, a = patch.split()
            r = r.point(lambda v: int(v * shade))
            g = g.point(lambda v: int(v * shade))
            b = b.point(lambda v: int(v * shade))
            patch = Image.merge("RGBA", (r, g, b, a))

        fwd = affine_from_points(patch_quad[:3], dst[:3])
        if fwd is None:
            continue
        inv = invert_affine(fwd)
        if inv is None:
            continue
        layer = patch.transform((RENDER_CANVAS, RENDER_CANVAS), Image.AFFINE, inv,
                                 resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
        canvas.alpha_composite(layer)

    return canvas.resize((RENDER_SIZE, RENDER_SIZE), Image.LANCZOS)


# ============================================================== jar / texture resolution

def jar_has_entry(jar: Path, entry: str) -> bool:
    try:
        with zipfile.ZipFile(jar) as zf:
            return entry in zf.namelist()
    except (OSError, zipfile.BadZipFile):
        return False


def find_jar(explicit: Path | None) -> Path:
    if explicit is not None:
        if not explicit.exists():
            raise SystemExit(f"icon: --jar {explicit} does not exist")
        return explicit
    for candidate in sorted(PRIMARY_GLOB.glob("*.jar")):
        if jar_has_entry(candidate, VILLAGER_TEXTURE):
            return candidate
    for candidate in sorted(FALLBACK_ROOT.glob("**/*.jar")):
        if jar_has_entry(candidate, VILLAGER_TEXTURE):
            return candidate
    raise SystemExit(
        "icon: no Minecraft client jar with "
        f"{VILLAGER_TEXTURE} found under {PRIMARY_GLOB} or {FALLBACK_ROOT}; "
        "run a Gradle build to populate the cache, or pass --jar PATH"
    )


def load_texture(jar: Path, entry: str) -> Image.Image:
    with zipfile.ZipFile(jar) as zf:
        with zf.open(entry) as fh:
            return Image.open(fh).convert("RGBA").copy()


# ============================================================== navy badge
# Ported inline from the sibling repos' tools/icon.py, themselves ported from heimathafen's
# standards/marketing/modrinth/navy-badge.py. Plain navy only (no grid/glow parameter): this mod
# is never a Create Fly add-on, so there is no second variant to switch between.
BADGE_SIZE = 512
BADGE_CENTRE = BADGE_SIZE // 2
BADGE_SUPERSAMPLE = 4
RIM = (255, 255, 255, 255)
BAND = (232, 236, 244, 255)
RING = (9, 12, 27, 255)
BLUEPRINT = (13, 18, 38, 255)
OUTLINE = (255, 255, 255, 235)
SHADOW = (20, 50, 90, 130)
FIT_BOX = 320  # the fleet's standard fit box, smooth mode, already-rendered subject
FIT = 0.7  # VV-19: 70% of the standard box, matching grounded_villages' own reduction
EFFECTIVE_BOX = round(FIT_BOX * FIT)  # 224


def badge() -> Image.Image:
    """The plain navy badge: white rim, pale band, dark ring, blueprint disc -- no blueprint
    grid, no centre glow (`standards/marketing/modrinth/navy-badge.py --no-grid`'s ruling for
    mods that are not Create Fly add-ons, the same choice grounded_villages' own `badge(grid=
    False)` already makes)."""
    big = BADGE_SIZE * BADGE_SUPERSAMPLE
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for radius, colour in ((256, RIM), (250, BAND), (238, RING), (200, BLUEPRINT)):
        r = radius * BADGE_SUPERSAMPLE
        c = BADGE_CENTRE * BADGE_SUPERSAMPLE
        draw.ellipse((c - r, c - r, c + r, c + r), fill=colour)
    return img.resize((BADGE_SIZE, BADGE_SIZE), Image.LANCZOS)


def compose(base: Image.Image, sprite: Image.Image, box: int = EFFECTIVE_BOX) -> Image.Image:
    """"smooth" mode: sprite is already-rendered/anti-aliased art (the head's projection), so it
    is LANCZOS-scaled to fit, not zoomed like a raw pixel-art texture."""
    w, h = sprite.size
    longest = max(w, h)
    factor = box / longest
    size = (round(w * factor), round(h * factor))
    scale = size[0] / w
    sprite = sprite.resize(size, Image.LANCZOS)
    alpha = sprite.getchannel("A")
    x = BADGE_CENTRE - size[0] // 2
    y = BADGE_CENTRE - size[1] // 2
    step = max(1, round(scale))
    grown = Image.new("L", (BADGE_SIZE, BADGE_SIZE), 0)
    for dx in (-step, 0, step):
        for dy in (-step, 0, step):
            grown.paste(alpha, (x + dx, y + dy), alpha)
    shadow = Image.new("RGBA", (BADGE_SIZE, BADGE_SIZE), (0, 0, 0, 0))
    shadow.paste(SHADOW, (0, 0), grown.transform(grown.size, Image.AFFINE, (1, 0, -14, 0, 1, -14)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    outline = Image.new("RGBA", (BADGE_SIZE, BADGE_SIZE), (0, 0, 0, 0))
    outline.paste(OUTLINE, (0, 0), grown)
    img = Image.alpha_composite(base, shadow)
    img = Image.alpha_composite(img, outline)
    img.alpha_composite(sprite, (x, y))
    return img


# ============================================================== speech bubble
# Same silhouette (rounded body + triangular tail) and three-dot layout this mod's previous
# tools/icon.py already drew (_bubble_mask/bubble); only the colours and the size/anchor change
# (VV-19: smaller, top-right of the rendered head, cream instead of villager-robe green).

# Sampled directly from the icon this ticket replaces:
#   Image.open("docs/modrinth/icon.png").convert("RGBA").getpixel((256, 6))
# -- near the outer edge, clear of the old bubble -- landing on that badge's outer RIM tone. This
# is "the fleet's cream" Kevin's ruling refers to for the new bubble fill: this mod's own existing
# parchment tone, not an invented approximation.
BUBBLE_FILL = (247, 231, 196, 255)

# VV-19 dot/outline colour call: cream-on-cream (dots on the new cream fill) is illegible at 64px
# -- the exact reason the old green-fill/cream-dot pairing can't just carry over with the fill
# swapped. Both the outline and the three dots use dark navy pulled from the badge's own palette
# (RING/BLUEPRINT) instead of a darker cream: navy gives the strongest contrast against the cream
# fill at thumbnail size, and it visually ties the bubble back to the badge it sits on, whereas a
# darker-cream dot (this badge's BAND-equivalent tone) reads as only a faint, muddy variation of
# the fill once shrunk. Checked by eye at both 512px and a 64px downscale before deciding.
BUBBLE_OUTLINE = RING
DOT = BLUEPRINT

SIZE = BADGE_SIZE  # bubble() operates on the same 512px canvas as the badge.


def _bubble_mask(size: tuple[int, int]) -> Image.Image:
    """A rounded speech-bubble shape (body + a small tail), supersampled then downscaled."""
    big = (size[0] * BADGE_SUPERSAMPLE, size[1] * BADGE_SUPERSAMPLE)
    mask = Image.new("L", big, 0)
    draw = ImageDraw.Draw(mask)
    s = BADGE_SUPERSAMPLE
    body = (0, 0, size[0] * s - 1, int(size[1] * 0.78) * s)
    draw.rounded_rectangle(body, radius=int(size[1] * 0.32) * s, fill=255)
    tail = [
        (int(size[0] * 0.20) * s, int(size[1] * 0.70) * s),
        (int(size[0] * 0.34) * s, int(size[1] * 0.70) * s),
        (int(size[0] * 0.14) * s, int(size[1] * 0.98) * s),
    ]
    draw.polygon(tail, fill=255)
    return mask.resize(size, Image.LANCZOS)


def bubble(img: Image.Image) -> Image.Image:
    # VV-19: ~104-120 wide, ~68-80 tall, anchored top-right of the rendered head (roughly
    # centred around x=300, y=90 on the 512px canvas), tail pointing down-left toward the head.
    bw, bh = 112, 74
    cx, cy = 300, 90
    x0 = cx - bw // 2
    y0 = cy - int(bh * 0.5)

    shape = _bubble_mask((bw, bh))

    # Drop shadow, offset and blurred.
    shadow_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    shadow_canvas = Image.new("L", (SIZE, SIZE), 0)
    shadow_canvas.paste(shape, (x0 + 6, y0 + 8))
    shadow_solid = Image.new("RGBA", (SIZE, SIZE), SHADOW)
    shadow_layer.paste(shadow_solid, (0, 0), shadow_canvas)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(6))
    img = Image.alpha_composite(img, shadow_layer)

    # Outline, slightly larger than the fill so it reads as a stroke.
    outline_shape = _bubble_mask((bw + 10, bh + 8))
    outline_canvas = Image.new("L", (SIZE, SIZE), 0)
    outline_canvas.paste(outline_shape, (x0 - 5, y0 - 4))
    outline_layer = Image.new("RGBA", (SIZE, SIZE), BUBBLE_OUTLINE)
    outline_rgba = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    outline_rgba.paste(outline_layer, (0, 0), outline_canvas)
    img = Image.alpha_composite(img, outline_rgba)

    # Fill.
    fill_canvas = Image.new("L", (SIZE, SIZE), 0)
    fill_canvas.paste(shape, (x0, y0))
    fill_layer = Image.new("RGBA", (SIZE, SIZE), BUBBLE_FILL)
    fill_rgba = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    fill_rgba.paste(fill_layer, (0, 0), fill_canvas)
    img = Image.alpha_composite(img, fill_rgba)

    # Three subtitle-caret dots, evenly spaced across the bubble's centre.
    dot_r = 6
    dot_cy = y0 + int(bh * 0.36)
    spacing = 26
    draw = ImageDraw.Draw(img)
    for i in (-1, 0, 1):
        dcx = cx + i * spacing
        draw.ellipse((dcx - dot_r, dot_cy - dot_r, dcx + dot_r, dot_cy + dot_r), fill=DOT)

    return img


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jar", type=Path, default=None,
                         help="Minecraft client jar to read the villager texture from")
    args = parser.parse_args()

    jar = find_jar(args.jar)
    skin = load_texture(jar, VILLAGER_TEXTURE)

    model = {"elements": villager_head_elements()}
    sprite = render_model(model, {"skin": skin}, GUI_TILT)
    icon = bubble(compose(badge(), sprite))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes) from {jar}")


if __name__ == "__main__":
    main()
