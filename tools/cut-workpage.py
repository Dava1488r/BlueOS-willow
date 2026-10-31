#!/usr/bin/env python3
"""BlueOS 0.7.2: Arona at her desk for the home screen widget, cut out of the game's work page scene.

The scene (spine-models/SpineBackground/Arona_workpage_daytime, Spine 4.2) is a whole classroom with Arona in
three poses and Plana — two 2048² atlas pages (~9.5 MB, ~32 MB of bitmaps in memory). The widget needs only
Arona sitting on the desk (Idle_02, A_02_*), asleep at it (Idle_00, A_00_*) and the desk (A_00_Desk*).
This keeps those regions, packs them into one smaller page and writes a new atlas in the new libgdx format;
the skeleton stays as it is (the runtime skips attachments whose region is missing — AronaDesk).

usage: cut-workpage.py <scene dir> <out dir>
"""
import os
import re
import shutil
import sys

from PIL import Image

KEEP = re.compile(r"^(A_00_|A_02_|A_02B_)")
PAD = 2
WIDTH = 2048


def parse(path):
    """Old (xy/size/orig/offset) or new (bounds/offsets) atlas → pages [{file, regions: [...]}]."""
    pages, page, region = [], None, None
    for raw in open(path, encoding="utf-8"):
        line = raw.rstrip("\n")
        if not line.strip():
            page = region = None
            continue
        if ":" not in line:
            name = line.strip()
            if page is None:
                page = {"file": name, "regions": []}
                pages.append(page)
            else:
                region = {"name": name, "degrees": 0}
                page["regions"].append(region)
            continue
        key, value = [s.strip() for s in line.split(":", 1)]
        if region is None:
            continue                                     # page settings: we write our own
        nums = [int(v) for v in value.split(",")] if re.match(r"^-?\d", value) else None
        if key == "xy":
            region["x"], region["y"] = nums
        elif key == "size":
            region["w"], region["h"] = nums
        elif key == "bounds":
            region["x"], region["y"], region["w"], region["h"] = nums
        elif key == "orig":
            region["ow"], region["oh"] = nums
        elif key == "offset":
            region["ox"], region["oy"] = nums
        elif key == "offsets":
            region["ox"], region["oy"], region["ow"], region["oh"] = nums
        elif key == "rotate":
            # "true" = 90; the game's atlases also have 180 and 270 (the texture is stored turned that way)
            region["degrees"] = 90 if value == "true" else 0 if value == "false" else int(value)
    return pages


def main(src, out):
    atlas = next(f for f in os.listdir(src) if f.endswith(".atlas"))
    skel = next(f for f in os.listdir(src) if f.endswith(".skel"))
    pages = parse(os.path.join(src, atlas))
    crops = []
    for p in pages:
        img = Image.open(os.path.join(src, p["file"])).convert("RGBA")
        for r in p["regions"]:
            if not KEEP.match(r["name"]):
                continue
            # as stored in the texture: turned by 90 or 270 → width and height swapped (180 keeps them)
            tw, th = (r["h"], r["w"]) if r["degrees"] in (90, 270) else (r["w"], r["h"])
            crops.append((r, img.crop((r["x"], r["y"], r["x"] + tw, r["y"] + th))))
    # shelf packing, tallest first
    crops.sort(key=lambda c: -c[1].height)
    x = y = shelf = 0
    placed = []
    for r, im in crops:
        if x + im.width + PAD > WIDTH:
            x, y, shelf = 0, y + shelf + PAD, 0
        placed.append((r, im, x, y))
        x += im.width + PAD
        shelf = max(shelf, im.height)
    height = y + shelf
    page = Image.new("RGBA", (WIDTH, height), (0, 0, 0, 0))
    os.makedirs(out, exist_ok=True)
    lines = ["arona_desk.png", f"size:{WIDTH},{height}", "filter:Linear,Linear"]
    for r, im, px, py in placed:
        page.paste(im, (px, py))
        lines.append(r["name"])
        lines.append(f"bounds:{px},{py},{r['w']},{r['h']}")
        ow, oh = r.get("ow", r["w"]), r.get("oh", r["h"])
        ox, oy = r.get("ox", 0), r.get("oy", 0)
        if (ox, oy, ow, oh) != (0, 0, r["w"], r["h"]):
            lines.append(f"offsets:{ox},{oy},{ow},{oh}")
        if r["degrees"]:
            lines.append(f"rotate:{r['degrees']}")
    page.save(os.path.join(out, "arona_desk.png"), optimize=True)
    open(os.path.join(out, "arona_desk.atlas"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    shutil.copyfile(os.path.join(src, skel), os.path.join(out, "arona_desk.skel"))
    print(f"{len(placed)} regions → {WIDTH}×{height}, png {os.path.getsize(os.path.join(out, 'arona_desk.png')) // 1024} KB")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
