"""
BlueOS logo geometry: "Blue" + "OS" set in Outfit SemiBold (600) with a small tilted halo
over the "O". Coordinates are "logo units" (the approved mockup's 200x80 box, y down).

Used by make-logo-assets.py (SVG, VectorDrawable, recovery PNG) and make-bootanimation.py.
"""

import math
import os

from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FONT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts", "Outfit[wght].ttf")
WEIGHT = 600
FONT_SIZE = 40.0      # logo units
SPLIT_X = 118.0       # "Blue" ends / "OS" starts here
BASELINE = 64.0
HALO_RX, HALO_RY, HALO_ANGLE, HALO_STROKE = 17.0, 5.0, -10.0, 3.5
HALO_GAP = 17.0       # halo center above the top of the "O"

# Brand color "Голубой BA" (seed #128AFA), tone-corrected for light and dark backgrounds
ACCENT_ON_LIGHT = (0x14, 0x78, 0xE0)
ACCENT_ON_DARK = (0x5F, 0xB2, 0xFF)
TEXT_ON_LIGHT = (0x16, 0x23, 0x3A)
TEXT_ON_DARK = (0xFF, 0xFF, 0xFF)


class Logo:
    def __init__(self):
        font = instantiateVariableFont(TTFont(FONT), {"wght": WEIGHT})
        self._glyphs = font.getGlyphSet()
        self._cmap = font.getBestCmap()
        self._scale = FONT_SIZE / font["head"].unitsPerEm
        self._kern = self._read_kerning(font)

        blue_width = self._text_width("Blue")
        self.blue = self._outline("Blue", SPLIT_X - blue_width)
        self.os = self._outline("OS", SPLIT_X)

        # Halo sits over the "O" (first glyph of "OS")
        bounds = self._glyph_bounds("O")
        self.halo_cx = SPLIT_X + (bounds[0] + bounds[2]) / 2 * self._scale
        self.halo_cy = BASELINE - bounds[3] * self._scale - HALO_GAP

    # --- font helpers ---------------------------------------------------------------------

    @staticmethod
    def _read_kerning(font):
        """Pair kerning from GPOS PairPos format 1 (enough for "Blue"/"OS")."""
        pairs = {}
        if "GPOS" not in font:
            return pairs
        for lookup in font["GPOS"].table.LookupList.Lookup:
            for sub in lookup.SubTable:
                if lookup.LookupType == 9:
                    sub = sub.ExtSubTable
                if getattr(sub, "LookupType", lookup.LookupType) != 2 or sub.Format != 1:
                    continue
                for first, pairset in zip(sub.Coverage.glyphs, sub.PairSet):
                    for rec in pairset.PairValueRecord:
                        if rec.Value1 is not None and getattr(rec.Value1, "XAdvance", 0):
                            pairs.setdefault((first, rec.SecondGlyph), rec.Value1.XAdvance)
        return pairs

    def _names(self, text):
        return [self._cmap[ord(c)] for c in text]

    def _advances(self, text):
        names = self._names(text)
        out = []
        for i, name in enumerate(names):
            adv = self._glyphs[name].width
            if i + 1 < len(names):
                adv += self._kern.get((name, names[i + 1]), 0)
            out.append(adv)
        return names, out

    def _text_width(self, text):
        return sum(self._advances(text)[1]) * self._scale

    def _glyph_bounds(self, char):
        from fontTools.pens.boundsPen import BoundsPen
        pen = BoundsPen(self._glyphs)
        self._glyphs[self._cmap[ord(char)]].draw(pen)
        return pen.bounds

    def _outline(self, text, x0):
        """Recorded outline of text in logo units (y down)."""
        return self.outline(text, x0, BASELINE, FONT_SIZE)

    def outline(self, text, x0, baseline, size, tracking=0.0):
        """Outline of any text: size in output units, tracking in em."""
        rec = RecordingPen()
        names, advances = self._advances(text)
        s = self._scale * size / FONT_SIZE
        upem = FONT_SIZE / self._scale
        x = x0
        for name, adv in zip(names, advances):
            self._glyphs[name].draw(TransformPen(rec, (s, 0, 0, -s, x, baseline)))
            x += (adv + tracking * upem) * s
        return rec

    def text_width(self, text, size, tracking=0.0):
        upem = FONT_SIZE / self._scale
        names, advances = self._advances(text)
        return (sum(advances) + tracking * upem * (len(names) - 1)) * self._scale * size / FONT_SIZE

    def fill(self, ctx, rec):
        import cairo
        ctx.set_fill_rule(cairo.FILL_RULE_WINDING)
        self._replay_cairo(ctx, rec)
        ctx.fill()

    # --- exports --------------------------------------------------------------------------

    @staticmethod
    def svg_path(rec, dx=0.0, dy=0.0):
        pen = SVGPathPen(None, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
        rec.replay(TransformPen(pen, (1, 0, 0, 1, dx, dy)))
        return pen.getCommands()

    def bounds(self):
        """Bounding box of the whole logo (x0, y0, x1, y1), halo stroke included."""
        from fontTools.pens.boundsPen import BoundsPen
        pen = BoundsPen(None)
        self.blue.replay(pen)
        self.os.replay(pen)
        x0, y0, x1, y1 = pen.bounds
        a = math.radians(HALO_ANGLE)
        hx = math.hypot(HALO_RX * math.cos(a), HALO_RY * math.sin(a)) + HALO_STROKE / 2
        hy = math.hypot(HALO_RX * math.sin(a), HALO_RY * math.cos(a)) + HALO_STROKE / 2
        return (min(x0, self.halo_cx - hx), min(y0, self.halo_cy - hy),
                max(x1, self.halo_cx + hx), max(y1, self.halo_cy + hy))

    @staticmethod
    def halo_length():
        a, b = HALO_RX, HALO_RY
        h = ((a - b) / (a + b)) ** 2
        return math.pi * (a + b) * (1 + 3 * h / (10 + math.sqrt(4 - 3 * h)))

    # --- cairo drawing --------------------------------------------------------------------

    @staticmethod
    def _replay_cairo(ctx, rec):
        cur = (0.0, 0.0)
        for op, args in rec.value:
            if op == "moveTo":
                ctx.move_to(*args[0])
                cur = args[0]
            elif op == "lineTo":
                ctx.line_to(*args[0])
                cur = args[0]
            elif op == "curveTo":
                (x1, y1), (x2, y2), (x3, y3) = args
                ctx.curve_to(x1, y1, x2, y2, x3, y3)
                cur = args[-1]
            elif op == "qCurveTo":
                # TrueType quadratic spline: implied on-curve points between off-curve ones
                pts = list(args)
                if pts[-1] is None:
                    raise ValueError("closed off-curve-only contours are not expected")
                offs, end = pts[:-1], pts[-1]
                for i, off in enumerate(offs):
                    nxt = end if i == len(offs) - 1 else (
                        (off[0] + offs[i + 1][0]) / 2, (off[1] + offs[i + 1][1]) / 2)
                    c1 = (cur[0] + 2 / 3 * (off[0] - cur[0]), cur[1] + 2 / 3 * (off[1] - cur[1]))
                    c2 = (nxt[0] + 2 / 3 * (off[0] - nxt[0]), nxt[1] + 2 / 3 * (off[1] - nxt[1]))
                    ctx.curve_to(c1[0], c1[1], c2[0], c2[1], nxt[0], nxt[1])
                    cur = nxt
            elif op in ("closePath", "endPath"):
                ctx.close_path()

    def draw_text(self, ctx, which):
        import cairo
        ctx.set_fill_rule(cairo.FILL_RULE_WINDING)
        self._replay_cairo(ctx, self.blue if which == "blue" else self.os)
        ctx.fill()

    def draw_halo(self, ctx, progress=1.0):
        """Stroke the halo; progress < 1 draws only part of it (clockwise from the right)."""
        if progress <= 0:
            return
        ctx.save()
        ctx.translate(self.halo_cx, self.halo_cy)
        ctx.rotate(math.radians(HALO_ANGLE))
        ctx.scale(HALO_RX, HALO_RY)
        ctx.new_path()
        ctx.arc(0, 0, 1, 0, 2 * math.pi)
        ctx.restore()
        ctx.set_line_width(HALO_STROKE)
        if progress < 1:
            length = self.halo_length()
            ctx.set_dash([length * progress, length * 2])
        ctx.stroke()
        ctx.set_dash([])
