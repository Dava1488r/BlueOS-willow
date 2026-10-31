#!/usr/bin/env python3
"""
BlueOS "Moe" lock screen clocks: digit pictures from Moe Counter themes
(github.com/journey-ad/Moe-Counter, MIT; sources in media/moe/src/<theme>/):

  kivotos      - the moebooru girls repainted as Blue Archive students (fan art, personal
                 use): hair and eye colors changed, a halo added above each head
  moebooru, gelbooru, original-new, asoul, miku, yousa-ling - as they are
  kasuterura-1 - white background removed

Writes media/moe/out/<theme>/<d>.png + media/moe/preview.png, and with --install copies them
into frameworks/base SystemUI customization (res/drawable-nodpi/blueos_moe_<theme>_<d>.png)
together with the generated theme table BlueOSMoeThemes.kt (then commit there).
"""

import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
MOE = os.path.join(HERE, "..", "media", "moe")
SRC = os.path.join(MOE, "src", "moebooru")
OUT = os.path.join(MOE, "out")
TOP = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
SYSUI = os.path.join(TOP, "frameworks/base/packages/SystemUI/customization")
HALO_ROOM = 9  # empty rows added on top for the halo

# id (resources / clock id), source dir, name in the clock picker, pixel art (sharp scaling)
THEMES = [
    ("kivotos", "moebooru", None, True),  # name: string resource (translated)
    ("moebooru", "moebooru", "moebooru", True),
    ("gelbooru", "gelbooru", "gelbooru", True),
    ("original_new", "original-new", "Original", True),
    ("asoul", "asoul", "A-SOUL", True),
    ("miku", "miku", "Miku", False),
    ("kasuterura", "kasuterura-1", "Kasuterura", True),
    ("yousa_ling", "yousa-ling", "Yousa Ling", False),
]


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# Per digit: who she is, which colors are her hair (flood-filled from a seed so the same
# colors in clothes stay), the new hair ramp (light -> dark), eye recolors in a face box,
# halo color.
CAST = {
    "0": dict(name="Plana", halo="#5a5a80",
              eyes=((14, 18, 36, 24), {"#0c4aa5": "#6e6488", "#376fc3": "#8e86a8"})),
    "1": dict(name="Arona", halo="#7cc8ff",
              hair=((20, 6), ["#49aaea", "#376fc3", "#0c4aa5", "#113577"], 30),
              ramp=["#bfe8fc", "#8fd0f4", "#5fb0e4", "#3b80c4"],
              eyes=((15, 13, 35, 21), {"#b41817": "#2f7fe0", "#e18176": "#7ab8f0"})),
    "2": dict(name="Aris", halo="#5ad8ff",
              hair=((20, 9), ["#737c9e", "#5c6288", "#504d60", "#2f323f"], 30),
              ramp=["#55556a", "#40404f", "#2c2c38", "#1a1a22"],
              eyes=((13, 16, 29, 24), {"#504d60": "#3a8fe0", "#737c9e": "#7cc4ff"})),
    "3": dict(name="Hina", halo="#b47cf0",
              eyes=((14, 18, 34, 24), {"#0c4aa5": "#7a3cb8", "#376fc3": "#a46ade", "#113577": "#53287e"})),
    "4": dict(name="Hifumi", halo="#ffe070",
              eyes=((16, 17, 34, 26), {"#467fbe": "#8a9a3c", "#376fc3": "#a4b450", "#113577": "#5a6828"})),
    "5": dict(name="Hoshino", halo="#ff9cc4",
              hair=((15, 8), ["#ffffff", "#d5dae8", "#a1a4ba", "#737c9e"], 50),
              ramp=["#ffd6e6", "#f7aac8", "#e486ac", "#bc628c"],
              eyes=((27, 15, 35, 26), {"#0c4aa5": "#e07a20", "#376fc3": "#f4a040", "#113577": "#a04a10"})),
    "6": dict(name="Mika", halo="#ffd870",
              hair=((14, 12), ["#fef8d4", "#fbf0e4", "#fecfae", "#ebc9b7", "#da7e5d"], 40),
              ramp=["#ffe4ee", "#ffd2e2", "#f8b4cc", "#f0a2be", "#d97a9e"],
              eyes=((16, 17, 33, 24), {"#0c4aa5": "#c8902a", "#49aaea": "#f0c050", "#113577": "#8a5a10"})),
    "7": dict(name="Yuuka", halo="#6fa0ff",
              hair=((20, 16), ["#49aaea", "#376fc3", "#0c4aa5", "#113577", "#11353b"], 75),
              ramp=["#6c7cc8", "#5060ac", "#3a4890", "#28326e", "#1a2048"]),
    "8": dict(name="Shiroko", halo="#5aa8ff"),
    "9": dict(name="Serika", halo="#ff5a72",
              hair=((15, 28), ["#6b87bc", "#47599b", "#27358b", "#282638"], 60),
              ramp=["#4e4858", "#383240", "#28222e", "#18141c"],
              eyes=((13, 41, 30, 48), {"#376fc3": "#d83048", "#0c4aa5": "#a01830",
                                       "#113577": "#701020"})),
}


def flood(px, size, seed, colors, ymax):
    w, h = size
    todo, seen = [seed], set()
    while todo:
        x, y = todo.pop()
        if (x, y) in seen or not (0 <= x < w and 0 <= y < min(h, ymax)):
            continue
        p = px[x, y]
        if p[3] == 0 or p[:3] not in colors:
            continue
        seen.add((x, y))
        todo += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    return seen


def repaint(digit):
    ch = CAST[digit]
    src = Image.open(os.path.join(SRC, digit + ".gif")).convert("RGBA")
    w, h = src.size
    im = Image.new("RGBA", (w, h + HALO_ROOM), (0, 0, 0, 0))
    im.alpha_composite(src, (0, HALO_ROOM))
    px = im.load()

    def at(x, y):  # source coordinates -> canvas
        return x, y + HALO_ROOM

    hair_px = set()
    if "hair" in ch:
        (sx, sy), names, ymax = ch["hair"]
        colors = [hexc(c) for c in names]
        hair_px = flood(px, im.size, at(sx, sy), set(colors), ymax + HALO_ROOM)
        # map by brightness rank: lightest source color -> lightest target
        order = sorted(colors, key=lum, reverse=True)
        ramp = [hexc(c) for c in ch["ramp"]]
        cmap = {c: ramp[min(i, len(ramp) - 1)] for i, c in enumerate(order)}
        for (x, y) in hair_px:
            px[x, y] = cmap[px[x, y][:3]] + (255,)

    if "eyes" in ch:
        (x0, y0, x1, y1), m = ch["eyes"]
        m = {hexc(k): hexc(v) for k, v in m.items()}
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                X, Y = at(x, y)
                p = px[X, Y]
                if p[3] and p[:3] in m and (X, Y) not in hair_px:
                    px[X, Y] = m[p[:3]] + (255,)

    # halo: tilted ring above the top of the head
    opaque = [(x, y) for y in range(im.size[1]) for x in range(w) if px[x, y][3]]
    top = min(y for _, y in opaque)
    row = [x for x, y in opaque if y <= top + 3]
    cx = (min(row) + max(row)) / 2
    cy = top - 3.5
    halo = hexc(ch["halo"])
    light = tuple(min(255, v + 70) for v in halo)
    for y in range(0, top + 2):
        for x in range(w):
            dx, dy = x - cx, y - cy
            u = dx * 0.978 - dy * 0.208
            v = dx * 0.208 + dy * 0.978
            d = (u / 10.5) ** 2 + (v / 3.0) ** 2
            if 0.5 <= d <= 1.3 and (px[x, y][3] == 0 or v < 0):
                px[x, y] = (light if v < 0 else halo) + (255,)
    return im


def load(src_dir, digit):
    for ext in ("gif", "png"):
        path = os.path.join(MOE, "src", src_dir, f"{digit}.{ext}")
        if os.path.exists(path):
            return Image.open(path).convert("RGBA")
    raise FileNotFoundError(f"{src_dir}/{digit}")


def remove_white_background(im, threshold=236):
    """Clear the near-white area connected to the border (kasuterura has an opaque one)."""
    px = im.load()
    w, h = im.size
    todo = [(x, y) for x in range(w) for y in (0, h - 1)] + \
           [(x, y) for y in range(h) for x in (0, w - 1)]
    seen = set()
    while todo:
        x, y = todo.pop()
        if (x, y) in seen or not (0 <= x < w and 0 <= y < h):
            continue
        r, g, b, a = px[x, y]
        if a and min(r, g, b) < threshold:
            continue
        seen.add((x, y))
        px[x, y] = (0, 0, 0, 0)
        todo += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    return im


def make_theme(theme_id, src_dir):
    cards = {}
    for d in "0123456789":
        if theme_id == "kivotos":
            cards[d] = repaint(d)
        else:
            cards[d] = load(src_dir, d)
            if theme_id == "kasuterura":
                cards[d] = remove_white_background(cards[d])
    return cards


def kotlin_table(names):
    rows = []
    for theme_id, _, label, pixel in THEMES:
        w, h = names[theme_id]
        digits = ", ".join(f"R.drawable.blueos_moe_{theme_id}_{d}" for d in range(10))
        rows.append(f'        BlueOSMoeTheme(\n            "{theme_id}",\n'
                    f'            {"null" if label is None else chr(34) + label + chr(34)},\n'
                    f"            listOf({digits}),\n"
                    f"            {w}f / {h}f,\n            {str(pixel).lower()},\n        ),")
    return f"""/*
 * SPDX-FileCopyrightText: BlueOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.shared.clocks

import com.android.systemui.customization.R

// BlueOS "Moe" lock screen clocks: digit pictures from Moe Counter themes
// (github.com/journey-ad/Moe-Counter). Generated by vendor/blueos/tools/make-moe.py - do not edit.

data class BlueOSMoeTheme(
    val id: String,
    /{chr(42)}{chr(42)} Name in the clock picker; null = R.string.clock_blueos_moe_kivotos {chr(42)}/
    val name: String?,
    /{chr(42)}{chr(42)} Pictures of the digits 0..9 {chr(42)}/
    val digits: List<Int>,
    /{chr(42)}{chr(42)} Width / height of the pictures {chr(42)}/
    val aspect: Float,
    /{chr(42)}{chr(42)} Pixel art: scaled up without smoothing {chr(42)}/
    val isPixelArt: Boolean,
) {{
    val clockId: String
        get() = "BLUEOS_MOE_" + id.uppercase()

    companion object {{
        fun forClockId(clockId: String?): BlueOSMoeTheme? = THEMES.firstOrNull {{ it.clockId == clockId }}

        val THEMES =
            listOf(
{chr(10).join(rows)}
            )
    }}
}}
"""


def main():
    install = "--install" in sys.argv
    sizes, all_cards = {}, {}
    for theme_id, src_dir, _, _ in THEMES:
        cards = make_theme(theme_id, src_dir)
        all_cards[theme_id] = cards
        sizes[theme_id] = cards["0"].size
        d = os.path.join(OUT, theme_id)
        os.makedirs(d, exist_ok=True)
        for k, im in cards.items():
            im.save(os.path.join(d, k + ".png"))

    # preview: every theme scaled to the same height, "0123456789" per row
    H, gap = 120, 4
    rows = []
    for theme_id, *_ in THEMES:
        row = [im.resize((round(im.size[0] * H / im.size[1]), H), Image.NEAREST)
               for im in all_cards[theme_id].values()]
        rows.append(row)
    width = max(sum(i.size[0] + gap for i in r) for r in rows)
    prev = Image.new("RGBA", (width, len(rows) * (H + gap)), (122, 163, 194, 255))
    for r, row in enumerate(rows):
        x = 0
        for im in row:
            prev.alpha_composite(im, (x, r * (H + gap)))
            x += im.size[0] + gap
    prev.save(os.path.join(MOE, "preview.png"))

    if install:
        res = os.path.join(SYSUI, "res", "drawable-nodpi")
        os.makedirs(res, exist_ok=True)
        for f in os.listdir(res):
            if f.startswith("blueos_moe_"):
                os.remove(os.path.join(res, f))
        for theme_id, cards in all_cards.items():
            for k, im in cards.items():
                im.save(os.path.join(res, f"blueos_moe_{theme_id}_{k}.png"), optimize=True)
        kt = os.path.join(SYSUI, "src/com/android/systemui/shared/clocks/BlueOSMoeThemes.kt")
        with open(kt, "w") as f:
            f.write(kotlin_table(sizes))
    print("themes:", ", ".join(f"{t}{sizes[t]}" for t in sizes))


if __name__ == "__main__":
    main()
