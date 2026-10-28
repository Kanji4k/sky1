package client.confis;

import java.util.HashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Hit Color: tints a player's armor and / or body (skin) in your own color for a short time after that player takes
 * damage. It is separate from the vanilla red damage flash (that one is left alone).
 *
 * Detection (client tick): a player's own hurtTime counter jumps up the moment the server reports damage to the
 * client (vanilla uses the same counter for the red flash), so a rise of that counter is "this player was hit".
 * Nothing is guessed from health, so healing, absorption changes or potion ticks never trigger it.
 *
 * Rendering: every entity model is sent through the render command queue with a tint color (the same slot vanilla
 * uses for dyed leather and translucent bodies). HitColorQueueMixin / HitColorBatchMixin ask tint() for a replacement
 * color for the model that belongs to a recently hit player. The tint is MULTIPLIED with the texture, so the skin,
 * armor patterns, trims and dye colors are still visible, only colored; the alpha byte (transparency) is kept. The
 * glint pass has its own layer and stays vanilla. No texture is ever written, no shader, no full-screen effect, and
 * once the duration ends tint() returns the original color so the model is exactly vanilla again.
 *
 * Which model is which is read from the model's parts: the player body model has a "jacket" part, an armor model has
 * head + body but no jacket and no cape. Capes, elytra, parrots, held items etc. are never tinted.
 *
 * Limits: only players are tinted; your own player is tinted in third person (the first-person hand is not).
 */
public final class HitColor {
    public static final int DEF_COLOR = 0xFF3B3B;
    public static final int DEF_DURATION = 500;
    public static final int DEF_INTENSITY = 80;
    public static final String ARMOR = "ARMOR", BODY = "BODY", BOTH = "BOTH";

    /** Re-entrancy guard shared by the two queue mixins (set while a replaced call is being forwarded). */
    public static boolean busy;

    private static final class Hit {
        int prev, seen;
        long startNs;      // 0 = not hit
    }

    private static final HashMap<Integer, Hit> HITS = new HashMap<>();
    private static int ticks;
    private static Module module;

    private HitColor() {}

    // ------------------------------------------------------------------ config helpers
    static void sanitize(Config c) {
        if (!ARMOR.equals(c.hitMode) && !BODY.equals(c.hitMode) && !BOTH.equals(c.hitMode)) c.hitMode = BOTH;
        c.hitDuration = Math.max(100, Math.min(2000, c.hitDuration <= 0 ? DEF_DURATION : c.hitDuration));
        c.hitIntensity = Math.max(0, Math.min(100, c.hitIntensity));
        c.hitColor &= 0xFFFFFF;
    }

    /** Reset to Default: settings only, the module switch stays as it is. */
    public static void resetDefaults() {
        Config c = Config.get();
        c.hitMode = BOTH;
        c.hitArmor = true;
        c.hitColor = DEF_COLOR;
        c.hitDuration = DEF_DURATION;
        c.hitIntensity = DEF_INTENSITY;
        Config.save();
    }

    public static boolean enabled() {
        if (module == null) module = Modules.byId("hitcolor");
        return module != null && module.cfg().enabled;
    }

    public static String modeName(String mode) {
        return ARMOR.equals(mode) ? "Armor Only" : BODY.equals(mode) ? "Body / Skin Only" : "Both";
    }

    /** True if armor is coloured with the current settings. */
    public static boolean armorOn(Config c) { return !BODY.equals(c.hitMode) && c.hitArmor; }

    /** True if the body / skin is coloured with the current settings. */
    public static boolean bodyOn(Config c) { return !ARMOR.equals(c.hitMode); }

    // ------------------------------------------------------------------ hit detection
    /** Client tick: spot players whose hurtTime just went up. */
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || !enabled()) {
            if (!HITS.isEmpty()) HITS.clear();
            return;
        }
        ticks++;
        for (PlayerEntity p : mc.world.getPlayers()) {
            int ht = p.hurtTime;
            Hit h = HITS.get(p.getId());
            if (h == null) {
                h = new Hit();
                HITS.put(p.getId(), h);
            } else if (ht > h.prev) {
                h.startNs = System.nanoTime();
            }
            h.prev = ht;
            h.seen = ticks;
        }
        if ((ticks & 63) == 0) HITS.values().removeIf(h -> ticks - h.seen > 40);
    }

    public static void reset() { HITS.clear(); }

    /** Effect strength 0..1 for this player right now: full for the first quarter of the duration, then fading out. */
    private static float strength(int entityId, Config c) {
        Hit h = HITS.get(entityId);
        if (h == null || h.startNs == 0L) return 0f;
        float t = (System.nanoTime() - h.startNs) / 1_000_000f / c.hitDuration;
        if (t >= 1f) {
            h.startNs = 0L;
            return 0f;
        }
        float fade = t < 0.25f ? 1f : 1f - (t - 0.25f) / 0.75f;
        return fade * c.hitIntensity / 100f;
    }

    // ------------------------------------------------------------------ rendering hook
    /** Tint color to use for this model instead of {@code tinted} (returned unchanged when nothing applies). */
    public static int tint(Object model, Object state, int tinted) {
        if (!(state instanceof PlayerEntityRenderState ps) || !(model instanceof Model<?> m) || HITS.isEmpty()) return tinted;
        Config c = Config.get();
        float s = strength(ps.id, c);
        if (s <= 0f) return tinted;

        ModelPart root = m.getRootPart();
        boolean body = root.hasChild("jacket");
        boolean armor = !body && !root.hasChild("cape") && root.hasChild("head") && root.hasChild("body");
        if (body ? !bodyOn(c) : armor ? !armorOn(c) : true) return tinted;
        return multiply(tinted, c.hitColor, s);
    }

    /** base * mix(white, color, s) per channel; the alpha byte of base is kept. */
    public static int multiply(int base, int rgb, float s) {
        int r = mix((rgb >> 16) & 255, s), g = mix((rgb >> 8) & 255, s), b = mix(rgb & 255, s);
        int br = (base >> 16) & 255, bg = (base >> 8) & 255, bb = base & 255;
        return (base & 0xFF000000) | ((br * r / 255) << 16) | ((bg * g / 255) << 8) | (bb * b / 255);
    }

    private static int mix(int channel, float s) { return Math.round(255 + (channel - 255) * s); }

    // ------------------------------------------------------------------ module (switch only, no HUD box)
    public static class HitColorModule extends Module {
        public HitColorModule() {
            super("hitcolor", "Hit Color", "HUD", 0, 0);
            defEnabled = false;
            colorOptions = false;
        }
        @Override public boolean movable() { return false; }
        @Override public int width(MinecraftClient mc) { return 0; }
        @Override public int height(MinecraftClient mc) { return 0; }
        @Override public void draw(DrawContext g, MinecraftClient mc, Config.ModuleCfg c, int x, int y, boolean preview) {}
    }
}
