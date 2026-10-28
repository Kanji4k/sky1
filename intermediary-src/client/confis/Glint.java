package client.confis;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Custom Item Glint.
 *
 * Color / opacity / intensity: the two vanilla glint textures (items = enchanted_glint_item, worn armor and entities =
 * enchanted_glint_entity) are re-created as small dynamic textures whose pixels are the vanilla glint pattern tinted
 * with the chosen color. The tint is rebuilt only when a setting changes (64x64 pixels, rate limited while a slider is
 * dragged), so rendering itself costs nothing extra. Switching back to "Vanilla" re-registers the untouched pixels.
 * A resource reload (F3+T) re-registers the vanilla textures; poll() notices and re-applies the tint.
 *
 * Per-kind switches (weapons / armor / tools / other) are done by ItemGlintMixin through hidden(stack).
 */
public final class Glint {
    private static final class Slot {
        final Identifier id;
        int[] pix;                 // vanilla ARGB pixels
        int w, h;
        float maxLum = 1f;
        AbstractTexture tex;       // our texture currently registered under id (null = never overridden)
        int sig = Integer.MIN_VALUE;
        long lastBuild;
        boolean failed;
        Slot(String path) { id = Identifier.of("minecraft", path); }
    }

    private static final Slot ITEM = new Slot("textures/misc/enchanted_glint_item.png");
    private static final Slot ENTITY = new Slot("textures/misc/enchanted_glint_entity.png");
    private static Module module;
    private static int frame;

    private Glint() {}

    // ------------------------------------------------------------------ module
    public static class GlintModule extends Module {
        public GlintModule() {
            super("glint", "Item Glint", "HUD", 0, 0);
            defEnabled = false;
            defBg = 0xFFB464FF;       // items: vanilla-like purple, fully opaque
            defText = 0xFFB464FF;     // armor
            bgLabel = "Items glint color";
            textLabel = "Armor glint color";
            bgToggle = false;
            hint = "Items = weapons, tools & other items. Opacity = how strongly the color shows.";
        }
        @Override public boolean movable() { return false; }
        @Override public int width(MinecraftClient mc) { return 0; }
        @Override public int height(MinecraftClient mc) { return 0; }
        @Override public void draw(net.minecraft.client.gui.DrawContext g, MinecraftClient mc, Config.ModuleCfg c, int x, int y, boolean preview) {}
    }

    private static Module mod() {
        if (module == null) module = Modules.byId("glint");
        return module;
    }

    /** True while the custom glint is in effect (module on and "Custom" selected). */
    public static boolean active() {
        Module m = mod();
        return m != null && m.cfg().enabled && Config.get().glintCustom;
    }

    // ------------------------------------------------------------------ per-kind switches
    private static final IdentityHashMap<Item, Integer> KIND = new IdentityHashMap<>();
    private static final int WEAPON = 0, ARMOR = 1, TOOL = 2, OTHER = 3;

    private static int kind(Item item) {
        Integer k = KIND.get(item);
        if (k != null) return k;
        String p = Registries.ITEM.getId(item).getPath();
        int v = OTHER;
        if (p.endsWith("_helmet") || p.endsWith("_chestplate") || p.endsWith("_leggings") || p.endsWith("_boots")
                || p.equals("elytra")) v = ARMOR;
        else if (p.endsWith("_pickaxe") || p.endsWith("_axe") || p.endsWith("_shovel") || p.endsWith("_hoe")
                || p.equals("shears") || p.equals("fishing_rod") || p.equals("flint_and_steel") || p.equals("brush")
                || p.endsWith("_on_a_stick")) v = TOOL;
        else if (p.endsWith("_sword") || p.endsWith("_spear") || p.equals("trident") || p.equals("bow")
                || p.equals("crossbow") || p.equals("mace")) v = WEAPON;
        KIND.put(item, v);
        return v;
    }

    /** True if the custom glint is active and this kind of item is switched off, so its glint must not show. */
    public static boolean hidden(ItemStack stack) {
        if (!active()) return false;
        Config c = Config.get();
        return switch (kind(stack.getItem())) {
            case WEAPON -> !c.glintWeapons;
            case ARMOR -> !c.glintArmor;
            case TOOL -> !c.glintTools;
            default -> !c.glintOther;
        };
    }

    // ------------------------------------------------------------------ texture tint
    /** Called every frame from Hud.render. Cheap: compares one int per texture unless something changed. */
    public static void poll(MinecraftClient mc) {
        if (mc.getTextureManager() == null || mc.getResourceManager() == null) return;
        frame++;
        Module m = mod();
        if (m == null) return;
        boolean custom = active();
        Config.ModuleCfg mc0 = m.cfg();
        int inten = Config.get().glintIntensity;
        update(mc, ITEM, custom, mc0.bg, inten);
        update(mc, ENTITY, custom, mc0.text, inten);
    }

    private static void update(MinecraftClient mc, Slot s, boolean custom, int argb, int intensity) {
        if (s.failed) return;
        if (!custom && s.tex == null) return;                 // never touched: vanilla texture is still in place
        int sig = custom ? (argb * 31 + intensity) | 1 : 0;   // 0 = vanilla pixels
        boolean changed = sig != s.sig;
        if (!changed && (frame & 63) == 0 && s.tex != null && mc.getTextureManager().getTexture(s.id) != s.tex) {
            changed = true;                                    // a resource reload replaced our texture
        }
        if (!changed) return;
        long now = System.currentTimeMillis();
        if (changed && s.tex != null && sig != s.sig && now - s.lastBuild < 60) return;   // rate limit while dragging
        try {
            if (s.pix == null && !load(mc, s)) { s.failed = true; return; }
            build(mc, s, custom, argb, intensity);
            s.sig = sig;
            s.lastBuild = now;
        } catch (Throwable t) {
            s.failed = true;                                   // never break rendering; the vanilla glint stays
            System.err.println("[Confis] custom glint disabled: " + t);
        }
    }

    private static boolean load(MinecraftClient mc, Slot s) throws Exception {
        Optional<Resource> r = mc.getResourceManager().getResource(s.id);
        if (r.isEmpty()) return false;
        try (InputStream in = r.get().getInputStream(); NativeImage img = NativeImage.read(in)) {
            s.w = img.getWidth();
            s.h = img.getHeight();
            s.pix = new int[s.w * s.h];
            float max = 0.001f;
            for (int y = 0; y < s.h; y++) {
                for (int x = 0; x < s.w; x++) {
                    int p = img.getColorArgb(x, y);
                    s.pix[y * s.w + x] = p;
                    max = Math.max(max, lum(p));
                }
            }
            s.maxLum = max;
        }
        return true;
    }

    private static float lum(int p) {
        return (((p >> 16) & 255) * 0.299f + ((p >> 8) & 255) * 0.587f + (p & 255) * 0.114f) / 255f;
    }

    private static void build(MinecraftClient mc, Slot s, boolean custom, int argb, int intensity) throws Exception {
        NativeImage out = new NativeImage(s.w, s.h, false);
        float op = custom ? ((argb >>> 24) / 255f) : 1f;
        float gain = custom ? intensity / 50f : 1f;
        int cr = (argb >> 16) & 255, cg = (argb >> 8) & 255, cb = argb & 255;
        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int p = s.pix[y * s.w + x];
                if (!custom) { out.setColorArgb(x, y, p); continue; }
                float k = Math.min(1f, lum(p) / s.maxLum) * gain * op;   // pattern strength 0..(2)
                int a = p >>> 24;
                out.setColorArgb(x, y, (a << 24) | (clamp(cr * k) << 16) | (clamp(cg * k) << 8) | clamp(cb * k));
            }
        }
        AbstractTexture tex = make(out);
        mc.getTextureManager().registerTexture(s.id, tex);     // replaces (and frees) the previous texture
        s.tex = tex;
    }

    private static int clamp(float v) { return v <= 0f ? 0 : v >= 255f ? 255 : (int) (v + 0.5f); }

    /** NativeImageBackedTexture's constructor differs between snapshots: pick whichever (NativeImage) / (name, NativeImage) exists. */
    private static AbstractTexture make(NativeImage img) throws Exception {
        for (Constructor<?> c : NativeImageBackedTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0] == NativeImage.class) return (AbstractTexture) c.newInstance(img);
            if (p.length == 2 && p[1] == NativeImage.class) {
                if (p[0] == String.class) return (AbstractTexture) c.newInstance("confis_glint", img);
                if (p[0] == Supplier.class) {
                    Supplier<String> name = () -> "confis_glint";
                    return (AbstractTexture) c.newInstance(name, img);
                }
            }
        }
        throw new IllegalStateException("no usable NativeImageBackedTexture constructor");
    }
}
