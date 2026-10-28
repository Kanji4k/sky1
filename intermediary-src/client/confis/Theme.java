package client.confis;

/**
 * Central theme for the Edit Hub menu: 12 colors x (Solid | Glass), plus glass opacity / border / highlight.
 *
 * Every menu color comes from here. Nothing in the renderer hard-codes an RGB value any more.
 * The palette is rebuilt only when a setting actually changes (ensure() is a handful of int compares),
 * so there is no per-frame allocation and no stale colors.
 */
public final class Theme {
    private Theme() {}

    public static final int MIN_OPACITY = 15, MAX_OPACITY = 90, DEF_OPACITY = 50;
    public static final int DEF_BORDER = 70, DEF_HIGHLIGHT = 60;

    public enum ThemeColor {
        BLACK("Black", 0x14171C),
        WHITE("White", 0xF0F3F7),
        YELLOW("Yellow", 0xE6BF1A),
        RED("Red", 0xC8323C),
        BLUE("Blue", 0x2680E0),
        PURPLE("Purple", 0x7E3FD0),
        BROWN("Brown", 0x7A5232),
        GREY("Grey", 0x6B7078),
        PINK("Pink", 0xE86FA8),
        GREEN("Green", 0x2FA85A),
        LIGHT_BLUE("Sky Blue", 0x4DA3F5),
        ORANGE("Orange", 0xE8872A);

        public final String label;
        public final int rgb;

        ThemeColor(String label, int rgb) {
            this.label = label;
            this.rgb = rgb;
        }

        public static ThemeColor byName(String s) {
            if (s != null) for (ThemeColor c : values()) if (c.name().equalsIgnoreCase(s)) return c;
            return BLUE;
        }
    }

    // ------------------------------------------------------------ current selection (cached copy)
    public static ThemeColor color = ThemeColor.BLUE;
    public static boolean glass = true;
    public static int opacity = DEF_OPACITY, border = DEF_BORDER, highlight = DEF_HIGHLIGHT;
    public static boolean animations = true;

    // ------------------------------------------------------------ derived palette (rgb = 0xRRGGBB, a = 0..255)
    public static int panelRgb, panelA, panelGrad;
    public static int cardRgb, cardA, cardOnRgb, cardOnA;
    public static int btnRgb, btnA, btnLightRgb, btnLightA;
    public static int fieldRgb, fieldA;
    public static int rim, rimTopA, rimBotA, rimHotA;
    public static int sheenA, sheenBtnA, innerLineA;
    /** Status colors (same in every theme so Enabled/Disabled stay recognisable). */
    public static final int ON_RGB = 0x14803F, ON_RIM = 0xA8FFC8;
    public static int onA;
    public static boolean darkText;
    /** ARGB text colors for text that sits on a themed surface. */
    public static int text, dim, textOnLight, textOnOn;

    private static boolean built;
    private static String keyName = "";
    private static boolean keyGlass;
    private static int keyOp = -1, keyBo = -1, keyHi = -1;

    // ------------------------------------------------------------------ helpers
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static int lum(int rgb) {
        return (((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114) / 1000;
    }

    /** f > 0 lightens toward white, f < 0 darkens toward black. */
    public static int shade(int rgb, float f) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        if (f >= 0) {
            r += Math.round((255 - r) * f);
            g += Math.round((255 - g) * f);
            b += Math.round((255 - b) * f);
        } else {
            r = Math.round(r * (1 + f));
            g = Math.round(g * (1 + f));
            b = Math.round(b * (1 + f));
        }
        return (clamp(r, 0, 255) << 16) | (clamp(g, 0, 255) << 8) | clamp(b, 0, 255);
    }

    /** Opaque black or near-white text, whichever reads better on a surface of this luminance. */
    public static int contrastText(int surfaceLum) { return surfaceLum > 125 ? 0xFF0B1118 : 0xFFFFFFFF; }

    // ------------------------------------------------------------------ config sync
    /** Clamps / repairs the theme fields of a freshly loaded config (missing or invalid values). */
    public static void sanitize(Config c) {
        c.themeColor = ThemeColor.byName(c.themeColor).name();
        c.glassOpacity = c.glassOpacity == 0 ? DEF_OPACITY : clamp(c.glassOpacity, MIN_OPACITY, MAX_OPACITY);
        c.borderIntensity = clamp(c.borderIntensity, 0, 100);
        c.highlightIntensity = clamp(c.highlightIntensity, 0, 100);
    }

    /** Rebuilds the palette if (and only if) a theme setting changed since the last call. */
    public static void ensure() {
        Config c = Config.get();
        String name = c.themeColor == null ? "BLUE" : c.themeColor;
        int op = clamp(c.glassOpacity, MIN_OPACITY, MAX_OPACITY);
        int bo = clamp(c.borderIntensity, 0, 100);
        int hi = clamp(c.highlightIntensity, 0, 100);
        if (built && c.themeGlass == keyGlass && op == keyOp && bo == keyBo && hi == keyHi && name.equals(keyName)) {
            animations = c.menuAnimations;
            return;
        }
        built = true;
        keyName = name;
        keyGlass = c.themeGlass;
        keyOp = op;
        keyBo = bo;
        keyHi = hi;
        color = ThemeColor.byName(name);
        glass = c.themeGlass;
        opacity = op;
        border = bo;
        highlight = hi;
        animations = c.menuAnimations;
        rebuild();
    }

    public static void setColor(ThemeColor tc) { Config.get().themeColor = tc.name(); ensure(); }
    public static void setGlass(boolean g) { Config.get().themeGlass = g; ensure(); }

    public static void resetGlassSettings() {
        Config c = Config.get();
        c.glassOpacity = DEF_OPACITY;
        c.borderIntensity = DEF_BORDER;
        c.highlightIntensity = DEF_HIGHLIGHT;
        ensure();
    }

    // ------------------------------------------------------------------ palette
    private static void rebuild() {
        int base = color.rgb;
        boolean lightBase = lum(base) > 150;
        float a = opacity / 100f;

        rim = lightBase ? shade(base, -0.50f) : shade(base, 0.78f);

        if (glass) {
            float boost = lightBase ? 1.35f : 1f;       // pale colors need more body to read as frosted
            float cf = lightBase ? 0.90f : 0.68f;
            panelA = clamp(Math.round(255 * a * 0.8f * boost), 0x28, 0xE8);
            cardA = clamp(Math.round(panelA * cf), 0x20, 0xE8);
            cardOnA = clamp(Math.round(panelA * (lightBase ? 1.0f : 0.92f)), 0x28, 0xF0);
            btnA = clamp(Math.round(panelA * 1.1f), 0x30, 0xF0);
            btnLightA = clamp(Math.round(panelA * 2.0f), 0x90, 0xE0);
            fieldA = clamp(Math.round(panelA * 1.1f), 0x30, 0xF0);
            onA = 0xC8;
            panelRgb = base;
            cardRgb = shade(base, lightBase ? -0.12f : -0.25f);
            cardOnRgb = shade(base, lightBase ? -0.02f : 0.03f);
            btnRgb = shade(base, lightBase ? -0.18f : -0.20f);
            fieldRgb = shade(base, lightBase ? -0.35f : -0.65f);
            panelGrad = 0x10;
        } else {
            panelA = 0xF2;
            cardA = cardOnA = btnA = btnLightA = fieldA = 0xFF;
            onA = 0xFF;
            panelRgb = shade(base, lightBase ? -0.03f : -0.45f);
            cardRgb = shade(base, lightBase ? -0.10f : -0.28f);
            cardOnRgb = shade(base, lightBase ? -0.02f : -0.08f);
            btnRgb = shade(base, lightBase ? -0.16f : -0.18f);
            fieldRgb = shade(base, lightBase ? -0.30f : -0.62f);
            panelGrad = 0;
        }
        btnLightRgb = shade(base, lightBase ? -0.55f : 0.55f);   // active tab / accent button: always the inverse of normal buttons

        // border / highlight strengths: defaults (70 / 60) reproduce the original look
        float bs = border / (float) DEF_BORDER;
        rimTopA = clamp(Math.round(0xE8 * bs), 0, 0xFF);
        rimBotA = clamp(Math.round(0x68 * bs), 0, 0xFF);
        rimHotA = clamp(Math.round(0xFF * Math.min(1.2f, bs + 0.2f)), 0, 0xFF);
        if (!glass) {                                  // solid: a clean even border
            rimTopA = rimBotA = clamp(Math.round(0xC0 * bs), 0, 0xFF);
            rimHotA = 0xFF;
        }
        float hs = highlight / (float) DEF_HIGHLIGHT;
        sheenA = glass ? clamp(Math.round(0x28 * hs), 0, 0x90) : 0;
        sheenBtnA = glass ? clamp(Math.round(0x34 * hs), 0, 0xA0) : 0;
        innerLineA = glass ? clamp(Math.round(0x3C * hs), 0, 0xB0) : 0;

        // text: judged against what the card actually looks like (glass lets the dark world through)
        float ca = cardA / 255f;
        double eff = ca * lum(cardRgb) + (1 - ca) * (glass ? 70 : 0);
        darkText = eff > 125;
        text = darkText ? 0xFF0B1118 : 0xFFFFFFFF;
        dim = darkText ? 0xFF3A4654 : 0xFFA9B8C8;
        textOnLight = contrastText(lum(btnLightRgb));
        textOnOn = 0xFFFFFFFF;

        // Glass lets the world through, so make sure text stays readable over dark, mid AND bright backgrounds
        // by nudging the tint (never the opacity you chose) until the contrast ratio is good enough.
        if (glass) {
            int fg = text & 0xFFFFFF;
            cardRgb = fixTint(cardRgb, cardA, fg, 3.0);
            cardOnRgb = fixTint(cardOnRgb, cardOnA, fg, 3.0);
            btnRgb = fixTint(btnRgb, btnA, fg, 3.0);
            fieldRgb = fixTint(fieldRgb, fieldA, fg, 3.0);
            panelRgb = fixTint(panelRgb, panelA, fg, 1.8);
            btnLightRgb = fixTint(btnLightRgb, btnLightA, textOnLight & 0xFFFFFF, 4.5);
            while (btnLightA < 0xF0 && worstContrast(btnLightRgb, btnLightA, textOnLight & 0xFFFFFF) < 4.5) btnLightA += 8;
            while (onA < 0xF8 && worstContrast(ON_RGB, onA, 0xFFFFFF) < 4.5) onA += 8;
        }
    }

    // ------------------------------------------------------------------ contrast helpers
    private static final int[] WORLDS = {0x101010, 0x3F7A3C, 0x8FB8E8, 0xC8D8F0};

    private static double relLum(int rgb) {
        double r = lin((rgb >> 16) & 255), g = lin((rgb >> 8) & 255), b = lin(rgb & 255);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double lin(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double ratio(int a, int b) {
        double x = relLum(a), y = relLum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static int over(int rgb, int a, int bg) {
        int r = (((rgb >> 16) & 255) * a + ((bg >> 16) & 255) * (255 - a)) / 255;
        int g = (((rgb >> 8) & 255) * a + ((bg >> 8) & 255) * (255 - a)) / 255;
        int b = ((rgb & 255) * a + (bg & 255) * (255 - a)) / 255;
        return (r << 16) | (g << 8) | b;
    }

    /** Lowest contrast ratio of text color fg on this translucent surface over the sample backgrounds. */
    private static double worstContrast(int rgb, int a, int fg) {
        double w = 99;
        for (int bg : WORLDS) w = Math.min(w, ratio(fg, over(rgb, a, bg)));
        return w;
    }

    /** Pushes the tint away from the text color (dark under light text, light under dark text) until readable. */
    private static int fixTint(int rgb, int a, int fg, double target) {
        boolean lightText = lum(fg) > 128;
        for (int i = 0; i < 14 && worstContrast(rgb, a, fg) < target; i++) rgb = shade(rgb, lightText ? -0.08f : 0.08f);
        return rgb;
    }
}
