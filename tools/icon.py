#!/usr/bin/env python3
"""Render docs/modrinth/icon.png: this mod's own badge, not the cubealgos Create-family navy one.

villager_voices is not a create_civilization/Create Fly add-on
(docs/spec/00-context.md), so it does not use the wrench-on-navy badge theme
(`standards/marketing/modrinth-collection-icon.md`, cubealgos heimathafen layer) — that badge is
reserved for actual Create Fly add-ons and would misrepresent this mod's relationship to Create.
This is a from-scratch design instead: a warm parchment round badge (villager trading, not
Create's blueprint-navy) carrying a villager-green speech bubble with three subtitle dots — no
vanilla Minecraft texture is sampled or reused anywhere in it (`docs/spec/00-context.md` "What it
will not do").

Every shape is drawn at SUPERSAMPLE x this size and downscaled for anti-aliased edges, the same
technique the Create-family icon scripts use. Requires Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
CENTRE = SIZE // 2
SUPERSAMPLE = 4
OUT = Path("docs/modrinth/icon.png")

# Warm parchment badge -- villager trading paper, not Create's blueprint navy.
RIM = (247, 231, 196, 255)  # outer cream rim
BAND = (231, 205, 155, 255)  # thin band between rim and disc
DISC_EDGE = (196, 158, 100, 255)  # disc, darkened toward the edge (drawn first, larger)
DISC_FACE = (222, 196, 148, 255)  # disc face (drawn second, smaller, sits inside DISC_EDGE)
VIGNETTE = (120, 90, 45, 70)  # soft warm shadow hugging the inner rim

# Villager-robe green speech bubble.
BUBBLE_OUTLINE = (43, 66, 34, 255)
BUBBLE_FILL = (90, 140, 71, 255)
BUBBLE_HIGHLIGHT = (118, 168, 96, 255)  # soft inner top highlight, gives the bubble some depth
DOT = (247, 236, 209, 255)  # cream, matching the rim -- the three subtitle dots
SHADOW = (60, 45, 20, 90)


def _circle(draw: ImageDraw.ImageDraw, radius: float, colour) -> None:
    r = radius * SUPERSAMPLE
    c = CENTRE * SUPERSAMPLE
    draw.ellipse((c - r, c - r, c + r, c + r), fill=colour)


def badge() -> Image.Image:
    big = SIZE * SUPERSAMPLE
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for radius, colour in ((256, RIM), (246, BAND), (232, DISC_EDGE), (222, DISC_FACE)):
        _circle(draw, radius, colour)
    img = img.resize((SIZE, SIZE), Image.LANCZOS)

    # A soft vignette hugging the inner edge of the disc, for a little warmth/depth.
    vignette = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette)
    vdraw.ellipse((CENTRE - 222, CENTRE - 222, CENTRE + 222, CENTRE + 222), outline=VIGNETTE, width=40)
    vignette = vignette.filter(ImageFilter.GaussianBlur(24))
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).ellipse((CENTRE - 222, CENTRE - 222, CENTRE + 222, CENTRE + 222), fill=255)
    clipped = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    clipped.paste(vignette, (0, 0), mask)
    return Image.alpha_composite(img, clipped)


def _bubble_mask(size: tuple[int, int]) -> Image.Image:
    """A rounded speech-bubble shape (body + a small tail), supersampled then downscaled."""
    big = (size[0] * SUPERSAMPLE, size[1] * SUPERSAMPLE)
    mask = Image.new("L", big, 0)
    draw = ImageDraw.Draw(mask)
    s = SUPERSAMPLE
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
    bw, bh = 300, 220
    x0 = CENTRE - bw // 2
    y0 = CENTRE - int(bh * 0.58)

    shape = _bubble_mask((bw, bh))

    # Drop shadow, offset and blurred.
    shadow_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    shadow_canvas = Image.new("L", (SIZE, SIZE), 0)
    shadow_canvas.paste(shape, (x0 + 10, y0 + 14))
    shadow_solid = Image.new("RGBA", (SIZE, SIZE), SHADOW)
    shadow_layer.paste(shadow_solid, (0, 0), shadow_canvas)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(img, shadow_layer)

    # Outline, slightly larger than the fill so it reads as a stroke.
    outline_shape = _bubble_mask((bw + 14, bh + 12))
    outline_canvas = Image.new("L", (SIZE, SIZE), 0)
    outline_canvas.paste(outline_shape, (x0 - 7, y0 - 6))
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

    # A soft highlight along the upper third, for a little glassy depth.
    highlight_mask = Image.new("L", (SIZE, SIZE), 0)
    hdraw = ImageDraw.Draw(highlight_mask)
    hdraw.ellipse((x0 + 20, y0 + 6, x0 + bw - 20, y0 + int(bh * 0.42)), fill=140)
    highlight_mask = Image.composite(highlight_mask, Image.new("L", (SIZE, SIZE), 0), fill_canvas)
    highlight_layer = Image.new("RGBA", (SIZE, SIZE), BUBBLE_HIGHLIGHT)
    highlight_rgba = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    highlight_rgba.paste(highlight_layer, (0, 0), highlight_mask)
    img = Image.alpha_composite(img, highlight_rgba)

    # Three subtitle-caret dots, evenly spaced across the bubble's centre.
    dot_r = 17
    dot_cy = y0 + int(bh * 0.36)
    spacing = 76
    draw = ImageDraw.Draw(img)
    for i in (-1, 0, 1):
        cx = CENTRE + i * spacing
        draw.ellipse((cx - dot_r, dot_cy - dot_r, cx + dot_r, dot_cy + dot_r), fill=DOT)

    return img


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img = bubble(badge())
    img.save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
