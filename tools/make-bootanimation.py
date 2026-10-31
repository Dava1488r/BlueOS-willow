#!/usr/bin/env python3
"""
Generates BlueOS boot animations (new logo) into media/bootanimation/:
  bootanimation.zip       - light (white background)
  bootanimation-dark.zip  - dark (black background)
plus, with --preview, an mp4 next to them showing how they render in the brand color and
in an example pink palette.

The frames use AOSP "dynamic coloring" (frameworks/base/cmds/bootanimation/FORMAT.md):
R, G, B, A of every PNG are masks for 4 colors that the system fills in from the current
Monet palette (persist.bootanim.color1..4, set by SystemUI - BlueOS patch):
  color1 (R) accent for a light background   color2 (G) accent for a dark background
  color3 (B) text on a light background       color4 (A) white
The light animation uses R+B and A as its white background, the dark one G and A as white
text, so a pixel never has all four channels set (that would render gray).

Timeline (approved mockup): part0 = 1 blank frame where the color transition happens,
part1 = intro (halo draws, "Blue" then "OS" fade in and rise), part2 = loop (halo floats),
part3 = exit (fade out with a slight zoom).
"""

import argparse
import io
import math
import os
import subprocess
import zipfile

import cairo
import numpy as np
from PIL import Image

from blueos_logo import ACCENT_ON_DARK, ACCENT_ON_LIGHT, TEXT_ON_LIGHT, Logo

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "media", "bootanimation")
FPS = 60
SCALE = 4.32              # px per logo unit: logo ~51% of a 1080 px wide screen, as in the mockup
WIDTH, HEIGHT = 640, 336  # frame size; the frame is drawn centered on the screen


def bezier(x1, y1, x2, y2):
    """CSS cubic-bezier easing."""
    def curve(t):
        lo, hi = 0.0, 1.0
        for _ in range(40):
            mid = (lo + hi) / 2
            x = 3 * (1 - mid) ** 2 * mid * x1 + 3 * (1 - mid) * mid ** 2 * x2 + mid ** 3
            lo, hi = (mid, hi) if x < t else (lo, mid)
        s = (lo + hi) / 2
        return 3 * (1 - s) ** 2 * s * y1 + 3 * (1 - s) * s ** 2 * y2 + s ** 3
    return lambda t: curve(min(max(t, 0.0), 1.0))


STANDARD = bezier(0.4, 0, 0.2, 1)
EMPHASIZED = bezier(0.2, 0, 0, 1)
EASE_IN = bezier(0.42, 0, 1, 1)


class Renderer:
    def __init__(self):
        self.logo = Logo()
        x0, y0, x1, y1 = self.logo.bounds()
        self.center = ((x0 + x1) / 2, (y0 + y1) / 2)

    def _layer(self, draw, dy=0.0, zoom=1.0):
        surf = cairo.ImageSurface(cairo.FORMAT_A8, WIDTH, HEIGHT)
        ctx = cairo.Context(surf)
        ctx.translate(WIDTH / 2, HEIGHT / 2)
        ctx.scale(SCALE * zoom, SCALE * zoom)
        ctx.translate(-self.center[0], -self.center[1] + dy)
        ctx.set_source_rgba(0, 0, 0, 1)
        draw(ctx)
        surf.flush()
        stride = surf.get_stride()
        a = np.frombuffer(surf.get_data(), np.uint8).reshape(HEIGHT, stride)[:, :WIDTH]
        return a.astype(np.float32) / 255

    def frame(self, halo=1.0, halo_dy=0.0, blue=(1.0, 0.0), os_=(1.0, 0.0), alpha=1.0, zoom=1.0):
        """Coverage of the text ("Blue") and accent ("OS" + halo) layers, 0..1."""
        text = self._layer(lambda c: self.logo.draw_text(c, "blue"), blue[1], zoom) * blue[0]
        accent = self._layer(lambda c: self.logo.draw_text(c, "os"), os_[1], zoom) * os_[0]
        accent = accent + self._layer(lambda c: self.logo.draw_halo(c, halo), halo_dy, zoom)
        return np.clip(text * alpha, 0, 1), np.clip(accent * alpha, 0, 1)


def fade_rise(t, start, duration=0.7, rise=8.0):
    p = EMPHASIZED((t - start) / duration)
    return p, rise * (1 - p)


def timeline(r):
    blank = np.zeros((HEIGHT, WIDTH), np.float32)
    parts = {"part0": [(blank, blank)], "part1": [], "part2": [], "part3": []}
    for i in range(int(1.9 * FPS) + 1):
        t = i / FPS
        parts["part1"].append(r.frame(halo=STANDARD((t - 0.3) / 0.9),
                                      blue=fade_rise(t, 0.9), os_=fade_rise(t, 1.15)))
    for i in range(int(2.4 * FPS)):
        dy = -3.0 * (1 - math.cos(2 * math.pi * i / (2.4 * FPS))) / 2
        parts["part2"].append(r.frame(halo_dy=dy))
    for i in range(1, int(0.5 * FPS) + 1):
        p = EASE_IN(i / (0.5 * FPS))
        parts["part3"].append(r.frame(alpha=1 - p, zoom=1 + 0.04 * p))
    return parts


def masks(text, accent, dark):
    z = np.zeros_like(text)
    if dark:
        chans = (z, accent, z, text)
    else:
        chans = (accent, z, text, np.clip(1 - accent - text, 0, 1))
    return np.dstack([np.round(c * 255).astype(np.uint8) for c in chans])


def png(rgba):
    buf = io.BytesIO()
    Image.fromarray(rgba, "RGBA").save(buf, "PNG", optimize=True)
    return buf.getvalue()


def write_zip(path, parts, dark):
    bg = "#000000" if dark else "#FFFFFF"
    start = "#%02X%02X%02X #%02X%02X%02X #%02X%02X%02X #FFFFFF" % (
        ACCENT_ON_LIGHT + ACCENT_ON_DARK + TEXT_ON_LIGHT)
    desc = (f"{WIDTH} {HEIGHT} {FPS}\n"
            f"dynamic_colors part0 {start} 0 1\n"
            f"p 1 0 part0 {bg}\n"
            f"c 1 0 part1 {bg}\n"
            f"p 0 0 part2 {bg}\n"
            f"c 1 0 part3 {bg}\n")
    gray = 0
    with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as z:
        z.writestr("desc.txt", desc)
        for name, frames in parts.items():
            for i, (text, accent) in enumerate(frames):
                m = masks(text, accent, dark)
                gray += int(np.all(m > 13, axis=2).sum())   # 0.05 threshold of the shader
                z.writestr(f"{name}/{i:05d}.png", png(m))
    return gray


def shade(m, colors):
    """Same math as the bootanimation dynamic-coloring shader."""
    f = m.astype(np.float32) / 255
    out = sum(f[..., i:i + 1] * (np.array(colors[i], np.float32) / 255) for i in range(4))
    return np.clip(out * 255, 0, 255).astype(np.uint8)


def write_preview(path, parts):
    brand = (ACCENT_ON_LIGHT, ACCENT_ON_DARK, TEXT_ON_LIGHT, (255, 255, 255))
    pink = ((0xA0, 0x2E, 0x66), (0xFF, 0xAE, 0xD3), (0x2B, 0x15, 0x20), (255, 255, 255))
    pw, ph = 360, 800   # each panel: a 1080x2400 screen at 1/3
    seq = parts["part0"] + parts["part1"] + parts["part2"] * 2 + parts["part3"] + \
        [parts["part3"][-1]] * 20
    cmd = ["ffmpeg", "-loglevel", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
           "-s", f"{pw * 4}x{ph}", "-r", str(FPS), "-i", "-", "-pix_fmt", "yuv420p",
           "-c:v", "libx264", "-crf", "18", path]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    small = (WIDTH // 3, HEIGHT // 3)
    for text, accent in seq:
        row = []
        for colors in (brand, pink):
            for dark in (False, True):
                img = Image.fromarray(shade(masks(text, accent, dark), colors)).resize(
                    small, Image.LANCZOS)
                panel = Image.new("RGB", (pw, ph), (0, 0, 0) if dark else (255, 255, 255))
                panel.paste(img, ((pw - small[0]) // 2, (ph - small[1]) // 2))
                row.append(np.asarray(panel))
        proc.stdin.write(np.hstack(row).tobytes())
    proc.stdin.close()
    proc.wait()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true")
    args = ap.parse_args()
    os.makedirs(OUT, exist_ok=True)
    parts = timeline(Renderer())
    for dark in (False, True):
        name = "bootanimation-dark.zip" if dark else "bootanimation.zip"
        gray = write_zip(os.path.join(OUT, name), parts, dark)
        size = os.path.getsize(os.path.join(OUT, name)) / 1024
        print(f"{name}: {sum(len(f) for f in parts.values())} frames, {size:.0f} KiB, "
              f"gray pixels: {gray}")
    if args.preview:
        write_preview(os.path.join(OUT, "preview.mp4"), parts)
        print("preview.mp4 written")


if __name__ == "__main__":
    main()
