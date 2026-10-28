#!/usr/bin/env python3
"""Generates the 16x16 strictly monochrome (black / white / grayscale) module icons.

Run:  python3 tools/gen_icons.py            -> writes intermediary-src/client/confis/IconData.java
      python3 tools/gen_icons.py preview.png -> also writes a contact sheet (needs Pillow)

Icons are drawn with a tiny pixel toolkit (shapes + bevel shading + automatic 1px outline) so every
icon shares the same light source (top-left), outline color and palette. Edit the draw_* functions and
re-run to tweak an icon. The Java side only reads the resulting character grids (see Icons.java).
"""
import math
import os
import sys

N = 16

# palette: one character -> 0xRRGGBB. STRICT GRAYSCALE ONLY. '.' = transparent. Light comes from the top-left.
PAL = {
    'k': 0x0A0A0A,  # outline / deepest shadow
    'E': 0x2E2E2E,  # very dark
    'D': 0x575757,  # dark
    'G': 0x8A8A8A,  # mid
    'M': 0xB8B8B8,  # light-mid
    'L': 0xE2E2E2,  # light
    'w': 0xFFFFFF,  # highlight
}


class Canvas:
    def __init__(self):
        self.g = [['.'] * N for _ in range(N)]

    def get(self, x, y):
        return self.g[y][x] if 0 <= x < N and 0 <= y < N else '.'

    def set(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.g[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def disc(self, cx, cy, r, c):
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.set(x, y, c)

    def ring(self, cx, cy, r0, r1, c):
        for y in range(N):
            for x in range(N):
                d = math.hypot(x - cx, y - cy)
                if r0 <= d <= r1:
                    self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c, thick=1):
        steps = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(steps + 1):
            x = round(x0 + (x1 - x0) * i / steps)
            y = round(y0 + (y1 - y0) * i / steps)
            for dx in range(thick):
                for dy in range(thick):
                    self.set(x + dx, y + dy, c)

    def poly(self, pts, c):
        for y in range(N):
            xs = []
            for i in range(len(pts)):
                (ax, ay), (bx, by) = pts[i], pts[(i + 1) % len(pts)]
                if (ay <= y + 0.5 < by) or (by <= y + 0.5 < ay):
                    xs.append(ax + (y + 0.5 - ay) * (bx - ax) / (by - ay))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(math.ceil(xs[i] - 0.5), math.floor(xs[i + 1] - 0.5) + 1):
                    self.set(x, y, c)

    def bevel(self, base, light, dark):
        """Pixels of `base` that touch empty/other space on the top-left get `light`, bottom-right get `dark`."""
        out = [row[:] for row in self.g]
        for y in range(N):
            for x in range(N):
                if self.g[y][x] != base:
                    continue
                tl = self.get(x - 1, y) != base or self.get(x, y - 1) != base
                br = self.get(x + 1, y) != base or self.get(x, y + 1) != base
                if tl and not br:
                    out[y][x] = light
                elif br and not tl:
                    out[y][x] = dark
        self.g = out

    def outline(self, c='k'):
        out = [row[:] for row in self.g]
        for y in range(N):
            for x in range(N):
                if self.g[y][x] == '.':
                    if any(self.get(x + dx, y + dy) != '.' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                        out[y][x] = c
        self.g = out

    def rows(self):
        return [''.join(r) for r in self.g]


# --------------------------------------------------------------------------- icons
# All icons: 16x16, grayscale ramp  w > L > M > G > D > E > k, light from the top-left, 1px 'k' outline.
def pts_on_circle(cx, cy, r, degs):
    return [(round(cx + r * math.cos(math.radians(a))), round(cy - r * math.sin(math.radians(a)))) for a in degs]


def fps():
    c = Canvas()
    c.disc(7.5, 7.5, 6.6, 'L')                         # metal bezel
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 7.5, 4.9, 'E')                         # dial
    c.ring(7.5, 7.5, 4.4, 4.9, 'D')                    # inner rim shadow
    for (x, y) in pts_on_circle(7.5, 7.5, 3.9, (225, 180, 135, 90, 45, 0, -45)):
        c.set(x, y, 'M')                               # gauge ticks
    c.line(7, 8, 11, 4, 'w')                           # needle
    c.set(11, 4, 'L')
    c.rect(7, 7, 8, 8, 'L')                            # hub
    c.set(7, 7, 'w')
    c.rect(6, 11, 9, 11, 'G')                          # readout
    c.outline()
    return c


def ping():
    c = Canvas()
    for i, hgt in enumerate((4, 7, 10, 13)):
        x0 = 1 + i * 4
        top = 14 - hgt
        weak = (i == 3)
        body, light, dark = ('D', 'G', 'E') if weak else ('L', 'w', 'G')
        c.rect(x0, top, x0 + 1, 13, body)
        c.rect(x0, top, x0, 13, light)                 # lit left edge
        c.rect(x0 + 1, top + 1, x0 + 1, 13, dark)      # shaded right edge
        c.set(x0 + 1, top, light)
    c.outline()
    return c


def tps():
    c = Canvas()
    c.poly([(10.5, 0.5), (2.5, 9), (7, 9), (4.5, 15.5), (13.5, 6), (9, 6), (12.5, 0.5)], 'L')
    c.bevel('L', 'w', 'G')
    c.line(9, 2, 6, 7, 'w')                            # specular streak
    c.outline()
    return c


def cps():
    c = Canvas()
    c.rect(4, 2, 11, 14, 'L')                          # mouse body
    for (x, y) in ((4, 2), (11, 2), (4, 14), (11, 14)):
        c.set(x, y, '.')
    c.bevel('L', 'w', 'G')
    c.rect(5, 3, 6, 6, 'w')                            # left button
    c.rect(9, 3, 10, 6, 'M')                           # right button
    c.rect(7, 3, 8, 6, 'E')                            # wheel well
    c.rect(7, 4, 8, 5, 'M')                            # scroll wheel
    c.rect(5, 7, 10, 7, 'D')                           # button seam
    c.rect(7, 8, 8, 12, 'M')                           # palm highlight
    c.outline()
    return c


def clock():
    c = Canvas()
    c.disc(7.5, 7.5, 6.6, 'M')                         # brushed bezel
    c.bevel('M', 'w', 'D')
    c.disc(7.5, 7.5, 5.0, 'E')                         # dial
    c.ring(7.5, 7.5, 4.5, 5.0, 'D')
    for (x, y) in ((7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)):
        c.set(x, y, 'G')                               # hour marks 12/6/9/3
    c.rect(7, 4, 7, 7, 'w')                            # hour hand
    c.rect(7, 7, 10, 7, 'L')                           # minute hand
    c.set(8, 8, 'M')
    c.set(7, 7, 'w')
    c.outline()
    return c


def day():
    c = Canvas()
    c.disc(7.5, 7.5, 3.9, 'L')                         # core
    c.bevel('L', 'w', 'G')
    c.set(6, 6, 'w')
    for (x0, y0, x1, y1) in ((7, 0, 8, 1), (7, 14, 8, 15), (0, 7, 1, 8), (14, 7, 15, 8)):
        c.rect(x0, y0, x1, y1, 'L')                    # main rays
    for (x, y) in ((2, 2), (3, 3), (12, 2), (11, 3)):
        c.set(x, y, 'M')                               # upper diagonal rays
    for (x, y) in ((2, 12), (3, 11), (12, 12), (11, 11)):
        c.set(x, y, 'G')                               # lower rays are in shadow
    c.outline()
    return c


def armor():
    c = Canvas()
    c.rect(3, 4, 12, 14, 'L')                          # torso
    c.rect(0, 2, 4, 7, 'L')                            # pauldrons
    c.rect(11, 2, 15, 7, 'L')
    c.rect(5, 2, 10, 3, '.')                           # neck opening
    c.rect(6, 4, 9, 4, '.')
    c.rect(4, 13, 11, 14, 'L')
    c.set(3, 14, '.')
    c.set(12, 14, '.')
    c.bevel('L', 'w', 'G')
    c.rect(7, 5, 8, 13, 'M')                           # center plate
    c.rect(7, 5, 7, 13, 'L')
    c.rect(8, 6, 8, 13, 'G')
    c.rect(4, 8, 6, 8, 'G')                            # chest lines
    c.rect(9, 8, 11, 8, 'G')
    c.set(1, 3, 'w')
    c.set(5, 5, 'w')
    c.set(13, 3, 'M')
    c.outline()
    return c


ARROWS = {
    'U': ((1, 0), (0, 1), (1, 1), (2, 1)),
    'D': ((0, 0), (1, 0), (2, 0), (1, 1)),
    'L': ((1, 0), (0, 1), (1, 1), (1, 2)),
    'R': ((0, 0), (0, 1), (1, 1), (0, 2)),
}


def keystrokes():
    c = Canvas()

    def key(x0, y0, ch, pressed=False):
        c.rect(x0, y0, x0 + 4, y0 + 5, 'k')
        c.rect(x0 + 1, y0 + 1, x0 + 3, y0 + 4, 'D' if pressed else 'L')
        c.rect(x0 + 1, y0 + 1, x0 + 3, y0 + 1, 'G' if pressed else 'w')   # top highlight
        c.rect(x0 + 1, y0 + 4, x0 + 3, y0 + 4, 'E' if pressed else 'G')   # key-cap shadow
        pts = ARROWS[ch]
        w = 3 if ch in 'UD' else 2
        h = 2 if ch in 'UD' else 3
        ox, oy = x0 + 1 + (3 - w) // 2, y0 + 1 + ((3 - h) // 2 if h == 3 else 1)
        for (px, py) in pts:
            c.set(ox + px, oy + py, 'w' if pressed else 'E')

    key(5, 1, 'U')
    key(0, 7, 'L')
    key(5, 7, 'D', True)
    key(10, 7, 'R')
    return c


def waypoints():
    c = Canvas()
    c.poly([(2.5, 6), (7.5, 15.4), (12.5, 6)], 'L')
    c.disc(7.5, 5.5, 5.2, 'L')
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 5.5, 2.5, 'k')                         # socket
    c.disc(7.5, 5.5, 1.5, 'M')
    c.set(7, 5, 'w')                                   # center marker
    c.rect(7, 12, 8, 13, 'G')                          # shaded tip
    c.outline()
    return c


def bottle(kind):
    c = Canvas()
    c.rect(6, 2, 9, 5, 'L')                            # glass neck
    c.disc(7.5, 10, 5.3, 'L')                          # glass body
    c.bevel('L', 'w', 'G')
    c.rect(5, 1, 10, 1, 'M')                           # lip
    c.rect(6, 0, 9, 1, 'D')                            # cork
    c.set(6, 0, 'G')
    for y in range(6, 15):                             # liquid inside the glass
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 10) ** 2 <= 3.9 ** 2:
                c.set(x, y, 'E' if y < 9 else ('M' if y == 9 else 'G'))
    c.rect(6, 9, 9, 9, 'M')                            # liquid surface
    c.set(5, 8, 'w')                                   # glass highlights
    c.set(5, 9, 'w')
    c.set(5, 11, 'L')
    if kind == 'e':
        c.set(9, 11, 'w')                              # sparkle
        c.set(8, 12, 'L')
        c.set(10, 12, 'L')
        c.set(9, 13, 'L')
    else:
        c.set(8, 11, 'L')                              # bubbles
        c.set(9, 13, 'M')
    c.outline()
    return c


def scoreboard():
    c = Canvas()
    c.rect(0, 1, 15, 14, 'M')
    c.bevel('M', 'w', 'D')
    c.rect(2, 3, 13, 12, 'E')
    c.rect(5, 4, 10, 4, 'w')                           # title
    c.rect(2, 5, 13, 5, 'D')                           # header rule
    for y, nw in ((7, 6), (9, 5), (11, 4)):
        c.rect(3, y, 3 + nw - 1, y, 'L')               # names
        c.rect(11, y, 12, y, 'M')                      # scores
    c.outline()
    return c


def reach():
    c = Canvas()
    c.rect(0, 4, 15, 11, 'L')
    c.set(0, 4, '.')
    c.set(15, 11, '.')
    c.bevel('L', 'w', 'G')
    for x in range(2, 15, 2):
        c.rect(x, 5, x, 9 if x % 6 == 2 else (7 if x % 4 == 0 else 6), 'E')   # graduated marks
    c.rect(1, 10, 14, 10, 'M')
    c.set(14, 8, 'k')                                  # hanging hole
    c.outline()
    return c


def rng():
    c = Canvas()
    c.ring(7.5, 7.5, 5.0, 6.5, 'L')
    c.bevel('L', 'w', 'G')
    c.rect(7, 0, 8, 4, 'w')
    c.rect(7, 11, 8, 15, 'M')
    c.rect(0, 7, 4, 8, 'w')
    c.rect(11, 7, 15, 8, 'M')
    c.rect(7, 7, 8, 8, 'w')
    c.rect(7, 5, 8, 5, 'G')                            # inner ticks
    c.rect(7, 10, 8, 10, 'G')
    c.outline()
    return c


def saturation():
    c = Canvas()
    c.line(8, 8, 13, 13, 'w', 2)                       # bone
    c.rect(12, 14, 14, 14, 'L')
    c.rect(14, 12, 14, 14, 'L')
    c.set(11, 14, 'M')
    c.disc(6, 6, 5.3, 'M')                             # meat
    c.bevel('M', 'w', 'D')
    c.disc(5, 5, 2.3, 'L')
    c.set(4, 4, 'w')
    c.set(5, 4, 'w')
    c.rect(8, 8, 9, 9, 'G')
    c.outline()
    return c


def inventory():
    c = Canvas()
    c.rect(0, 0, 15, 15, 'M')
    c.bevel('M', 'w', 'D')
    for j in range(3):
        for i in range(3):
            x0, y0 = 1 + i * 5, 1 + j * 5
            c.rect(x0, y0, x0 + 3, y0 + 3, 'E')
            c.rect(x0, y0, x0 + 3, y0, 'k')            # inset top shadow
            c.rect(x0, y0 + 3, x0 + 3, y0 + 3, 'D')    # bright bottom lip
    c.rect(7, 7, 8, 8, 'w')                            # items
    c.set(7, 7, 'L')
    c.rect(2, 2, 3, 3, 'L')
    c.set(2, 2, 'w')
    c.rect(12, 12, 13, 12, 'M')
    c.rect(12, 7, 13, 8, 'G')
    return c


def totems():
    c = Canvas()
    c.rect(4, 1, 11, 8, 'L')                           # head
    c.rect(2, 6, 3, 9, 'L')                            # arms
    c.rect(12, 6, 13, 9, 'L')
    c.rect(5, 9, 10, 14, 'G')                          # body
    c.bevel('L', 'w', 'M')
    c.bevel('G', 'M', 'D')
    c.rect(5, 4, 6, 4, 'k')                            # eyes
    c.rect(9, 4, 10, 4, 'k')
    c.rect(7, 6, 8, 6, 'D')                            # mouth
    c.rect(4, 2, 11, 2, 'D')                           # crown band
    c.rect(7, 10, 8, 13, 'D')
    c.outline()
    return c


def zoom():
    c = Canvas()
    c.line(9, 9, 14, 14, 'D', 2)                       # handle
    c.line(9, 9, 13, 13, 'G', 1)
    c.disc(6.5, 6.5, 5.6, 'L')                         # lens ring
    c.bevel('L', 'w', 'G')
    c.disc(6.5, 6.5, 3.8, 'E')                         # glass
    c.rect(4, 4, 5, 4, 'M')                            # glare
    c.set(4, 5, 'M')
    c.rect(5, 6, 8, 6, 'w')                            # plus sign
    c.rect(6, 5, 6, 8, 'w')
    c.rect(7, 5, 7, 8, 'w')
    c.outline()
    return c


def freecam():
    c = Canvas()
    c.rect(1, 4, 14, 13, 'L')
    c.rect(5, 2, 9, 3, 'L')                            # viewfinder bump
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 8.5, 3.7, 'k')                         # lens barrel
    c.ring(7.5, 8.5, 2.9, 3.7, 'D')
    c.disc(7.5, 8.5, 2.5, 'G')
    c.disc(7.5, 8.5, 1.3, 'E')
    c.set(6, 7, 'w')                                   # lens glint
    c.rect(11, 5, 12, 5, 'w')                          # flash
    c.rect(2, 5, 3, 5, 'M')
    c.outline()
    return c


def fullbright():
    c = Canvas()
    c.disc(7.5, 5.8, 4.6, 'L')                         # bulb
    c.bevel('L', 'w', 'G')
    c.rect(6, 9, 9, 10, 'L')                           # neck
    c.rect(5, 11, 10, 11, 'M')                         # screw base
    c.rect(5, 12, 10, 12, 'G')
    c.rect(6, 13, 9, 13, 'D')
    c.rect(7, 14, 8, 14, 'E')
    c.rect(6, 4, 6, 6, 'G')                            # filament
    c.rect(9, 4, 9, 6, 'G')
    c.rect(6, 6, 9, 6, 'D')
    c.set(5, 3, 'w')                                   # glass highlight
    c.set(5, 4, 'w')
    for (x, y) in ((7, 0), (8, 0), (1, 5), (14, 5), (2, 1), (13, 1)):
        c.set(x, y, 'M')                               # light rays
    c.outline()
    return c


def noblind():
    c = Canvas()
    c.poly([(0.5, 8), (4, 4), (8, 3), (12, 4), (15.5, 8), (12, 12), (8, 13), (4, 12)], 'L')
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 8, 2.9, 'D')                           # iris
    c.disc(7.5, 8, 1.4, 'k')                           # pupil
    c.set(6, 7, 'w')
    c.outline()
    c.line(2, 14, 14, 2, 'k', 3)                       # slash
    c.line(2, 14, 14, 2, 'w', 2)
    c.line(2, 14, 13, 3, 'L', 1)
    return c


def tiers():
    c = Canvas()
    c.rect(4, 1, 11, 5, 'L')
    c.rect(5, 6, 10, 6, 'L')
    c.rect(6, 7, 9, 7, 'L')
    c.ring(3.5, 3.5, 1.4, 2.4, 'G')                    # handles
    c.ring(11.5, 3.5, 1.4, 2.4, 'G')
    c.rect(7, 8, 8, 11, 'G')                           # stem
    c.rect(4, 12, 11, 14, 'G')                         # base
    c.bevel('L', 'w', 'M')
    c.rect(5, 12, 10, 12, 'M')
    c.rect(4, 14, 11, 14, 'D')
    c.rect(6, 3, 9, 3, 'D')                            # engraved star
    c.rect(7, 2, 8, 4, 'D')
    c.outline()
    return c


def shulker():
    c = Canvas()
    c.rect(0, 5, 11, 15, 'D')                          # box
    c.rect(0, 5, 11, 9, 'M')                           # lid
    c.bevel('M', 'w', 'G')
    c.rect(0, 9, 11, 9, 'D')
    c.rect(0, 10, 11, 10, 'G')
    c.rect(5, 8, 6, 10, 'w')                           # latch
    c.rect(5, 9, 6, 9, 'G')
    c.outline()
    c.rect(8, 0, 15, 5, 'k')                           # tooltip panel
    c.rect(9, 1, 14, 4, 'E')
    c.rect(9, 1, 14, 1, 'M')
    c.rect(10, 3, 12, 3, 'w')
    return c


def crosshair():
    c = Canvas()
    for (x0, y0, dx, dy) in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
        for i in range(4):
            c.set(x0 + dx * i, y0, 'L')
            c.set(x0, y0 + dy * i, 'L')
        c.set(x0 + dx, y0 + dy, 'w')
    c.rect(7, 4, 8, 6, 'M')
    c.rect(7, 9, 8, 11, 'M')
    c.rect(4, 7, 6, 8, 'M')
    c.rect(9, 7, 11, 8, 'M')
    c.rect(7, 7, 8, 8, 'w')
    c.outline()
    return c


def nodarkness():
    c = Canvas()
    c.disc(7, 8, 6.0, 'L')                             # moon
    c.bevel('L', 'w', 'G')
    c.disc(10, 6, 4.6, '.')                            # bite out of the moon -> crescent
    c.set(11, 2, 'M')
    c.set(13, 5, 'M')                                  # stars
    c.set(12, 3, 'w')
    c.outline()
    c.line(1, 14, 14, 1, 'k', 3)                       # slash = darkness removed
    c.line(1, 14, 14, 1, 'w', 2)
    c.line(1, 14, 13, 2, 'L', 1)
    return c


def glint():
    c = Canvas()
    c.poly([(7.5, 0.5), (9, 6), (14.5, 7.5), (9, 9), (7.5, 14.5), (6, 9), (0.5, 7.5), (6, 6)], 'L')   # 4-point sparkle
    c.bevel('L', 'w', 'G')
    c.rect(7, 6, 8, 9, 'w')
    c.rect(6, 7, 9, 8, 'w')
    for (x, y) in ((13, 1), (14, 1), (13, 2), (12, 1), (13, 0)):
        c.set(x, y, 'M')                               # small sparkle
    for (x, y) in ((2, 13), (1, 13), (3, 13), (2, 12), (2, 14)):
        c.set(x, y, 'G')
    c.outline()
    return c


def cps():
    c = Canvas()
    c.rect(7, 0, 8, 3, 'G')                            # cable
    c.rect(3, 3, 12, 15, 'L')                          # mouse body
    for (x, y) in ((3, 3), (12, 3), (3, 15), (12, 15)):
        c.set(x, y, '.')
    c.bevel('L', 'w', 'G')
    c.rect(4, 4, 6, 8, 'w')                            # left button (lit)
    c.rect(9, 4, 11, 8, 'M')                           # right button
    c.rect(7, 4, 8, 8, 'E')                            # wheel well
    c.rect(7, 5, 8, 6, 'L')                            # scroll wheel
    c.rect(4, 9, 11, 9, 'D')                           # button seam
    c.rect(5, 11, 7, 13, 'M')                          # palm sheen
    c.outline()
    return c


KEYS = {
    'W': ("x.x", "x.x", "xxx", ".x."),
    'A': (".x.", "x.x", "xxx", "x.x"),
    'S': (".xx", "xx.", ".xx", "xx."),
    'D': ("xx.", "x.x", "x.x", "xx."),
}


def keystrokes():
    c = Canvas()

    def key(x0, y0, ch, pressed=False):
        c.rect(x0, y0, x0 + 4, y0 + 5, 'k')                               # key outline
        c.rect(x0 + 1, y0 + 1, x0 + 3, y0 + 4, 'D' if pressed else 'L')   # key cap
        for j, row in enumerate(KEYS[ch]):
            for i, v in enumerate(row):
                if v == 'x':
                    c.set(x0 + 1 + i, y0 + 1 + j, 'w' if pressed else 'E')

    key(5, 1, 'W')
    key(0, 7, 'A')
    key(5, 7, 'S', True)
    key(10, 7, 'D')
    return c


def saturation():
    c = Canvas()
    c.line(8, 8, 12, 12, 'w', 2)                       # bone shaft
    c.disc(12.5, 14, 1.3, 'w')                         # bone knobs
    c.disc(14.5, 12, 1.3, 'w')
    c.set(12, 13, 'M')
    c.disc(6, 6, 5.4, 'G')                             # meat
    c.bevel('G', 'M', 'D')
    c.disc(4.5, 4.5, 2.2, 'M')
    c.set(3, 3, 'w')
    c.set(4, 3, 'w')
    c.set(3, 4, 'w')
    c.line(7, 9, 9, 7, 'D')                            # char marks
    c.line(5, 10, 9, 10, 'D')
    c.outline()
    return c


def nodarkness():
    c = Canvas()
    c.disc(7, 8, 6.2, 'L')                             # moon
    c.bevel('L', 'w', 'G')
    c.disc(10.5, 5.5, 5.0, '.')                        # bite -> crescent
    c.set(12, 2, 'w')
    c.set(13, 4, 'M')
    c.set(13, 1, 'M')
    c.outline()
    c.line(1, 14, 14, 1, 'k', 3)                       # slash = darkness removed
    c.line(1, 14, 14, 1, 'w', 2)
    return c



def freelook():
    c = Canvas()
    c.poly([(0.5, 8), (4, 4.2), (8, 3), (12, 4.2), (15.5, 8), (12, 11.8), (8, 13), (4, 11.8)], 'L')   # eye
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 8, 3.3, 'D')                           # iris
    c.disc(7.5, 8, 1.9, 'E')                           # pupil
    c.set(6, 6, 'w')
    c.set(7, 6, 'M')
    c.set(6, 7, 'M')
    c.set(2, 1, 'M')                                   # little arc = looking around
    c.set(3, 0, 'M')
    c.set(4, 0, 'M')
    c.set(11, 0, 'M')
    c.set(12, 0, 'M')
    c.set(13, 1, 'M')
    c.outline()
    return c


def hitcolor():
    c = Canvas()
    c.poly([(7.5, 0.5), (9.5, 4.5), (14.5, 3.5), (11.5, 7.5), (15, 11.5), (10, 11), (8, 15.5), (6, 11), (1, 11.5), (4.5, 7.5), (1.5, 3.5), (5.5, 4.5)], 'L')   # impact burst
    c.bevel('L', 'w', 'G')
    c.disc(7.5, 7.5, 3.1, 'M')                         # core
    c.bevel('M', 'w', 'D')
    c.disc(7.5, 7.5, 1.4, 'E')                         # point of impact
    c.set(6, 5, 'w')
    c.outline()
    return c


ICONS = {
    'fps': fps, 'ping': ping, 'tps': tps, 'cps': cps, 'clock': clock, 'day': day, 'armor': armor,
    'keystrokes': keystrokes, 'waypoints': waypoints, 'effects': lambda: bottle('e'),
    'scoreboard': scoreboard, 'reach': reach, 'range': rng, 'saturation': saturation,
    'inventory': inventory, 'totems': totems, 'potions': lambda: bottle('p'), 'zoom': zoom,
    'freecam': freecam, 'fullbright': fullbright, 'noblind': noblind, 'tiers': tiers,
    'shulker': shulker, 'crosshair': crosshair, 'nodarkness': nodarkness, 'glint': glint,
    'freelook': freelook, 'hitcolor': hitcolor,
}


def write_java(path):
    lines = ["package client.confis;", "",
             "/** GENERATED by tools/gen_icons.py - do not edit by hand. 16x16 shaded pixel icons as character grids. */",
             "final class IconData {", "    private IconData() {}", "",
             "    /** character -> 0xRRGGBB */",
             "    static final String PALETTE = \"" + ''.join(f"{k}{v:06X}" for k, v in PAL.items()) + "\";", "",
             "    static final String[] IDS = {" + ", ".join(f'"{k}"' for k in ICONS) + "};", "",
             "    static final String[][] ROWS = {"]
    for k, fn in ICONS.items():
        rows = fn().rows()
        lines.append("        {" + ", ".join(f'"{r}"' for r in rows) + "},  // " + k)
    lines += ["    };", "}", ""]
    with open(path, 'w') as f:
        f.write("\n".join(lines))


def preview(path, scale=10):
    from PIL import Image
    cols = 6
    names = list(ICONS)
    rows = (len(names) + cols - 1) // cols
    cell = N * scale + 16
    for bg_name, bg in (('dark', (22, 24, 30)), ('light', (225, 232, 240))):
        img = Image.new('RGB', (cols * cell, rows * cell), bg)
        for idx, k in enumerate(names):
            cv = ICONS[k]()
            ox, oy = (idx % cols) * cell + 8, (idx // cols) * cell + 8
            for y in range(N):
                for x in range(N):
                    ch = cv.g[y][x]
                    if ch != '.':
                        rgb = PAL[ch]
                        col = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
                        for yy in range(scale):
                            for xx in range(scale):
                                img.putpixel((ox + x * scale + xx, oy + y * scale + yy), col)
        img.save(path.replace('.png', f'_{bg_name}.png'))


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    write_java(os.path.join(here, '..', 'intermediary-src', 'client', 'confis', 'IconData.java'))
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    print("wrote IconData.java with", len(ICONS), "icons")
