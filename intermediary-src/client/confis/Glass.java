package client.confis;

import net.minecraft.client.gui.DrawContext;

/**
 * iOS-style "liquid glass" drawing for the HUD modules and the menu.
 * Frosted translucent body, a bright specular sheen on top, a glowing rim that fades towards the
 * bottom and an optional soft shadow. Built only from fill() calls so it works anywhere.
 */
public final class Glass {
    private Glass() {}

    private static int lerp(int a, int b, float t) { return Math.round(a + (b - a) * t); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int white(int a) { return (a & 255) << 24 | 0xFFFFFF; }
    private static int rgba(int a, int rgb) { return (a & 255) << 24 | (rgb & 0xFFFFFF); }
    private static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }
    private static int lum(int c) {
        return (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000;
    }

    /** Horizontal inset of corner row i (0 = outermost row) for corner radius r. */
    private static int inset(int i, int r) {
        if (i >= r) return 0;
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Inset of row i of a shape that is h rows tall (same shape top and bottom). */
    private static int d(int i, int h, int r) {
        i = Math.max(0, Math.min(h - 1, i));
        return inset(Math.min(i, h - 1 - i), r);
    }

    private static void span(DrawContext g, int x0, int x1, int y, int color) {
        if (x1 > x0) g.fill(x0, y, x1, y + 1, color);
    }

    // ---- run merging: consecutive rows with the same x-range and color become ONE fill() call.
    // Every fill() is a separate GUI element in 1.21.x, so this is what keeps the FPS up.
    private static boolean runOpen;
    private static int runX0, runX1, runY0, runY1, runCol;

    private static void run(DrawContext g, int x0, int x1, int y, int color) {
        if (x1 <= x0) return;
        if (runOpen && x0 == runX0 && x1 == runX1 && y == runY1 && color == runCol) {
            runY1 = y + 1;
            return;
        }
        flush(g);
        runOpen = true;
        runX0 = x0; runX1 = x1; runY0 = y; runY1 = y + 1; runCol = color;
    }

    private static void flush(DrawContext g) {
        if (runOpen) g.fill(runX0, runY0, runX1, runY1, runCol);
        runOpen = false;
    }

    /** Rounds an alpha value to a step so neighbouring rows share a color and can be merged. */
    private static int q(int a, int step) { return (a / step) * step; }

    // ------------------------------------------------------------------ core
    private static void render(DrawContext g, int x, int y, int w, int h, int r,
                               int bodyA, int cr, int cg, int cb,
                               boolean shadow, int sheenMax, int rimTop, int rimBottom, int rimRgb) {
        render(g, x, y, w, h, r, bodyA, 0x10, cr, cg, cb, shadow, sheenMax, rimTop, rimBottom, rimRgb, 0x3C);
    }

    private static void render(DrawContext g, int x, int y, int w, int h, int r,
                               int bodyA, int grad, int cr, int cg, int cb,
                               boolean shadow, int sheenMax, int rimTop, int rimBottom, int rimRgb, int innerA) {
        r = Math.max(1, Math.min(r, Math.min(h / 2, (w + 1) / 2)));

        if (shadow) {
            g.fill(x + 3, y + h, x + w - 3, y + h + 1, 0x26000000);
            g.fill(x + 5, y + h + 1, x + w - 5, y + h + 2, 0x12000000);
        }

        int sheenRows = Math.max(2, h * 45 / 100);

        // pass 1: body (a touch more opaque at the top, lighter at the bottom)
        for (int i = 0; i < h; i++) {
            int ins = d(i, h, r);
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            int a = q(clamp(lerp(bodyA + grad, bodyA - grad, t), 0x18, 0xFF), 10);
            run(g, x + ins, x + w - ins, y + i, argb(a, cr, cg, cb));
        }
        flush(g);

        // pass 2: specular sheen across the top half
        if (sheenMax > 0) {
            for (int i = 0; i < Math.min(sheenRows, h); i++) {
                int ins = d(i, h, r);
                int sa = q(Math.round(sheenMax * (1f - (float) i / sheenRows)), 12);
                if (sa > 0) run(g, x + ins, x + w - ins, y + i, white(sa));
            }
            flush(g);
        }

        // pass 3: glowing rim (bright at the top, fading towards the bottom): top/bottom edge rows
        for (int i = 0; i < h; i += Math.max(1, h - 1)) {
            int ins = d(i, h, r);
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            span(g, x + ins, x + w - ins, y + i, rgba(lerp(rimTop, rimBottom, t), rimRgb));
            if (h <= 1) break;
        }
        // left side, then right side
        for (int side = 0; side < 2; side++) {
            for (int i = 1; i < h - 1; i++) {
                int ins = d(i, h, r);
                int x0 = x + ins, x1 = x + w - ins;
                float t = h <= 1 ? 0f : (float) i / (h - 1);
                int rw = i < h / 2
                        ? Math.max(1, d(i - 1, h, r) - ins)
                        : Math.max(1, d(i + 1, h, r) - ins);
                int col = rgba(q(lerp(rimTop, rimBottom, t), 32), rimRgb);
                if (side == 0) run(g, x0, Math.min(x0 + rw, x1), y + i, col);
                else run(g, Math.max(x1 - rw, x0), x1, y + i, col);
            }
            flush(g);
        }

        // thin inner highlight just under the top edge, and a faint glow above the bottom edge
        if (h >= 9 && sheenMax > 0 && innerA > 0) {
            int tx = d(1, h, r) + 3;
            span(g, x + tx, x + w - tx, y + 1, white(innerA));
            int bx = d(h - 2, h, r) + 3;
            span(g, x + bx, x + w - bx, y + h - 2, white(innerA * 4 / 10));
        }
    }

    // ------------------------------------------------------------ HUD modules
    /** Alpha (0-255) of a color, scaled by an opacity fraction. */
    private static int scaleA(int a, int of255) { return (a * of255 + 127) / 255; }

    /**
     * Text colors: Minecraft's text renderer can treat an alpha below 4 as "fully opaque", so a 0% text opacity
     * is mapped to an almost-invisible 4/255 instead. Everything else passes through unchanged.
     */
    public static int tc(int argb) {
        return (argb >>> 24) < 4 ? (argb & 0xFFFFFF) | 0x04000000 : argb;
    }

    /**
     * Liquid-glass panel for a HUD module.
     * bg: body color, its alpha byte is the opacity (0 = nothing drawn, 255 = fully solid color).
     * border: rim color, same rule. Shadow and sheen fade out together with the body, so 0% is truly see-through.
     */
    public static void draw(DrawContext g, int x, int y, int w, int h, int bg, int border) {
        int bgA = bg >>> 24, bdA = border >>> 24;
        if (bgA == 0 && bdA == 0) return;
        boolean sky = Config.get().skyHud;
        if (sky) border = rgba(bdA, 0xFFFFFF);
        if (w < 6 || h < 6) {
            if (bgA > 0) g.fill(x, y, x + w, y + h, bg);
            return;
        }
        int r = Math.max(1, Math.min(7, Math.min(h / 2, (w + 1) / 2)));

        // soft shadow under the panel (scales with the body opacity)
        int sh1 = scaleA(0x26, bgA), sh2 = scaleA(0x12, bgA);
        if (sh1 > 0) g.fill(x + 3, y + h, x + w - 3, y + h + 1, sh1 << 24);
        if (sh2 > 0) g.fill(x + 5, y + h + 1, x + w - 5, y + h + 2, sh2 << 24);

        // body: exactly the chosen color + opacity (no tinting), rows merged into as few fills as possible
        if (bgA > 0) {
            for (int i = 0; i < h; i++) {
                int ins = d(i, h, r);
                int col = bg;
                if (sky) col = rgba(bgA, mix(0x7FC0FF, 0x2F7FE0, Math.min(5, i * 6 / h) / 5f));   // sky gradient, light top to deeper blue
                run(g, x + ins, x + w - ins, y + i, col);
            }
            flush(g);

            // specular sheen over the top half, faded with the body opacity
            int sheenMax = scaleA(0x34, bgA);
            int sheenRows = Math.max(2, h * 45 / 100);
            if (sheenMax >= 12) {
                for (int i = 0; i < Math.min(sheenRows, h); i++) {
                    int ins = d(i, h, r);
                    int sa = q(Math.round(sheenMax * (1f - (float) i / sheenRows)), 12);
                    if (sa > 0) run(g, x + ins, x + w - ins, y + i, white(sa));
                }
                flush(g);
            }
        }

        // border: one uniform 1px rim in the border color at exactly the border opacity
        if (bdA > 0) {
            int col = border;
            span(g, x + d(0, h, r), x + w - d(0, h, r), y, col);
            span(g, x + d(h - 1, h, r), x + w - d(h - 1, h, r), y + h - 1, col);
            for (int side = 0; side < 2; side++) {
                for (int i = 1; i < h - 1; i++) {
                    int ins = d(i, h, r);
                    int x0 = x + ins, x1 = x + w - ins;
                    int rw = i < h / 2 ? Math.max(1, d(i - 1, h, r) - ins) : Math.max(1, d(i + 1, h, r) - ins);
                    if (side == 0) run(g, x0, Math.min(x0 + rw, x1), y + i, col);
                    else run(g, Math.max(x1 - rw, x0), x1, y + i, col);
                }
                flush(g);
            }
        }
    }

    /** Small rounded "well" used for inventory slots. alpha = 0-255 of the white well. */
    public static void slot(DrawContext g, int x, int y, int w, int h, int alpha) {
        if (alpha > 0) fillRound(g, x, y, w, h, 3, white(alpha));
    }

    public static void slot(DrawContext g, int x, int y, int w, int h) {
        slot(g, x, y, w, h, 0x26);
    }

    // -------------------------------------------------------------------- menu
    // Every color below comes from Theme (12 colors x Solid/Glass). Only flat fills + static banded
    // gradients: no blur, no shaders, no refraction, no framebuffer work.

    private static int mix(int rgbA, int rgbB, float t) {
        return (lerp((rgbA >> 16) & 255, (rgbB >> 16) & 255, t) << 16)
                | (lerp((rgbA >> 8) & 255, (rgbB >> 8) & 255, t) << 8)
                | lerp(rgbA & 255, rgbB & 255, t);
    }

    private static int amul(int a, float f) { return clamp(Math.round(a * f), 0, 255); }

    /** ARGB of the theme's rim color at alpha a (scrollbars, dividers, selection outlines). */
    public static int rimArgb(int a) {
        Theme.ensure();
        return rgba(a, Theme.rim);
    }

    /** ARGB of a theme body color (swatch backdrops, sliders). */
    public static int bodyArgb(int a) {
        Theme.ensure();
        return rgba(a, Theme.cardRgb);
    }

    /** Sky photo behind a menu window (clipped to the window). */
    private static void skyPanel(DrawContext g, int x, int y, int w, int h) {
        if (!Config.get().skyScreens) return;
        net.minecraft.client.util.Window win = net.minecraft.client.MinecraftClient.getInstance().getWindow();
        g.enableScissor(x + 2, y + 2, x + w - 2, y + h - 2);
        SkyBg.draw(g, win.getScaledWidth(), win.getScaledHeight());
        g.disableScissor();
    }

    /** The big menu window. */
    public static void panel(DrawContext g, int x, int y, int w, int h) {
        Theme.ensure();
        skyPanel(g, x, y, w, h);
        if (Theme.glass && Theme.border > 0) {              // soft outer glow (4 thin fills)
            int glow = rgba(amul(0x30, Theme.border / (float) Theme.DEF_BORDER), Theme.rim);
            g.fill(x + 10, y - 1, x + w - 10, y, glow);
            g.fill(x + 10, y + h, x + w - 10, y + h + 1, glow);
            g.fill(x - 1, y + 10, x, y + h - 10, glow);
            g.fill(x + w, y + 10, x + w + 1, y + h - 10, glow);
        }
        int c = Theme.panelRgb;
        render(g, x, y, w, h, 12, Theme.panelA, Theme.panelGrad, (c >> 16) & 255, (c >> 8) & 255, c & 255,
                false, Theme.sheenA, Theme.rimTopA, Theme.rimBotA, Theme.rim, Theme.innerLineA);
    }

    /**
     * Panel for the secondary screens (Tools, Friends, Item Guide ...). Same hue/mode as the menu, but always deep
     * enough that those screens' white text stays readable, even in the light themes.
     */
    public static void panelDeep(DrawContext g, int x, int y, int w, int h) {
        Theme.ensure();
        skyPanel(g, x, y, w, h);
        int c = Theme.shade(Theme.color.rgb, Theme.darkText ? -0.62f : -0.40f);
        int a = Math.max(Theme.panelA, Theme.glass ? 0xB0 : 0xF2);
        render(g, x, y, w, h, 12, a, Theme.panelGrad, (c >> 16) & 255, (c >> 8) & 255, c & 255,
                false, Theme.sheenA, Theme.rimTopA, Theme.rimBotA, Theme.rim, Theme.innerLineA);
    }

    /** A module card in the menu grid. */
    public static void card(DrawContext g, int x, int y, int w, int h, boolean hover, boolean on) {
        Theme.ensure();
        int a = on ? Theme.cardOnA : Theme.cardA;
        int c = on ? Theme.cardOnRgb : Theme.cardRgb;
        if (hover) { a = Math.min(0xFF, a + 0x1C); c = Theme.shade(c, 0.08f); }
        int rim = on ? mix(Theme.rim, Theme.ON_RIM, 0.5f) : Theme.rim;
        int top = hover ? Theme.rimHotA : amul(Theme.rimTopA, on ? 1f : 0.92f);
        int bot = hover ? amul(Theme.rimBotA, 1.45f) : amul(Theme.rimBotA, 0.85f);
        render(g, x, y, w, h, 8, a, Theme.panelGrad == 0 ? 0 : 0x10, (c >> 16) & 255, (c >> 8) & 255, c & 255,
                false, Theme.sheenA, top, bot, rim, Theme.innerLineA);
    }

    /**
     * A pill button. Bright fills (active tab, EDIT HUD) use the inverted "light" style, dark ones the normal style.
     * Returns the text color to use on it.
     */
    public static int button(DrawContext g, int x, int y, int w, int h, int fill, boolean hover) {
        Theme.ensure();
        boolean light = lum(fill) > 140;
        int a = light ? Theme.btnLightA : Theme.btnA;
        int c = light ? Theme.btnLightRgb : Theme.btnRgb;
        if (hover) { a = Math.min(0xFF, a + 0x28); c = Theme.shade(c, 0.12f); }
        render(g, x, y, w, h, 7, a, Theme.panelGrad == 0 ? 0 : 0x10, (c >> 16) & 255, (c >> 8) & 255, c & 255,
                false, light ? Theme.sheenBtnA + 0x0C : Theme.sheenBtnA,
                hover ? Theme.rimHotA : amul(Theme.rimTopA, 0.95f), hover ? amul(Theme.rimBotA, 1.5f) : amul(Theme.rimBotA, 1.05f),
                Theme.rim, Theme.innerLineA);
        return light ? Theme.textOnLight : Theme.text;
    }

    /** Enabled / Disabled pill: green when on, neutral surface when off. Returns the text color. */
    public static int toggle(DrawContext g, int x, int y, int w, int h, boolean on, boolean hover) {
        Theme.ensure();
        int a = on ? Theme.onA : Theme.btnA;
        int c = on ? Theme.ON_RGB : Theme.btnRgb;
        if (hover) { a = Math.min(0xFF, a + 0x28); c = Theme.shade(c, 0.12f); }
        render(g, x, y, w, h, 7, a, Theme.panelGrad == 0 ? 0 : 0x10, (c >> 16) & 255, (c >> 8) & 255, c & 255,
                false, Theme.sheenBtnA,
                hover ? 0xFF : 0xE0, hover ? 0xA0 : 0x70, on ? Theme.ON_RIM : Theme.rim, Theme.innerLineA);
        return on ? Theme.textOnOn : Theme.text;
    }

    /** Inset text field (search box). Focused = rim in the accent color. */
    public static void field(DrawContext g, int x, int y, int w, int h, boolean focus, int accent) {
        Theme.ensure();
        int c = Theme.fieldRgb;
        render(g, x, y, w, h, 7, Theme.fieldA, 0, (c >> 16) & 255, (c >> 8) & 255, c & 255, false, 0,
                focus ? 0xFF : amul(Theme.rimTopA, 0.8f), focus ? 0xD0 : amul(Theme.rimBotA, 0.9f),
                focus ? (accent & 0xFFFFFF) : Theme.rim, 0);
    }

    /** Color swatch for the theme picker: rounded chip, bright border + check mark when selected. */
    public static void swatch(DrawContext g, int x, int y, int w, int h, int rgb, boolean selected, boolean hover) {
        Theme.ensure();
        fillRound(g, x - 1, y - 1, w + 2, h + 2, 3, selected ? 0xFFFFFFFF : hover ? 0xB0FFFFFF : 0x80000000);
        fillRound(g, x, y, w, h, 3, 0xFF000000 | rgb);
        if (selected) {                                     // small check mark in a contrasting color
            int ck = lum(rgb) > 140 ? 0xFF101820 : 0xFFFFFFFF;
            int cx = x + w / 2 - 3, cy = y + h / 2 - 2;
            g.fill(cx, cy + 2, cx + 1, cy + 3, ck);
            g.fill(cx + 1, cy + 3, cx + 2, cy + 4, ck);
            g.fill(cx + 2, cy + 2, cx + 3, cy + 3, ck);
            g.fill(cx + 3, cy + 1, cx + 4, cy + 2, ck);
            g.fill(cx + 4, cy, cx + 5, cy + 1, ck);
            g.fill(cx + 5, cy - 1, cx + 6, cy, ck);
        }
    }

    /** Plain rounded fill in one color (sliders, scrollbars, slots). */
    public static void fillRound(DrawContext g, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(1, Math.min(r, Math.min(h / 2, (w + 1) / 2)));
        int i = 0;
        while (i < h) {
            int ins = d(i, h, r);
            int j = i + 1;
            while (j < h && d(j, h, r) == ins) j++;      // rows with the same inset = one fill
            if (w - 2 * ins > 0) g.fill(x + ins, y + i, x + w - ins, y + j, argb);
            i = j;
        }
    }
}
