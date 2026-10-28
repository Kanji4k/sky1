package client.confis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

/**
 * No Darkness (a switch-only HUD module, like Fullbright).
 *
 * - "Darkness removed" = 100%: the client-side Darkness status effect is dropped every frame, so the lightmap pulse
 *   and the fog dimming of the Warden / sculk Darkness effect never show (same way No Blindness works).
 * - Below 100%: the effect is kept but the vanilla "Darkness Pulsing" option is lowered to (100 - removed)%, which
 *   scales the pulsing of the effect. The player's own option value is stored and put back when the module is off.
 *
 * Costs one hash lookup per frame while off, nothing else.
 */
public final class NoDarkness {
    private static boolean scaleApplied;
    private static Module module;

    private NoDarkness() {}

    private static boolean on() {
        if (module == null) module = Modules.byId("nodarkness");
        return module != null && module.cfg().enabled;
    }

    /** Called every frame from Hud.render. */
    public static void poll(MinecraftClient mc) {
        if (mc.options == null) return;
        if (!on()) {
            if (scaleApplied) restore(mc);
            return;
        }
        Config cfg = Config.get();
        int removed = cfg.darknessReduction;
        if (removed >= 100) {
            if (scaleApplied) restore(mc);          // the effect itself is removed below, the option is not needed
            if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
                mc.player.removeStatusEffect(StatusEffects.DARKNESS);   // client side only
            }
            return;
        }
        var opt = mc.options.getDarknessEffectScale();
        double target = (100 - removed) / 100.0;
        if (!scaleApplied) {
            // after a crash the lowered value may still be in options.txt: keep the stored original in that case
            if (cfg.savedDarknessScale < 0) {
                cfg.savedDarknessScale = opt.getValue();
                Config.save();
            }
            scaleApplied = true;
        }
        if (Math.abs(opt.getValue() - target) > 1e-4) opt.setValue(target);
    }

    /** Puts the player's own "Darkness Pulsing" value back. Also called when the game closes. */
    public static void restore(MinecraftClient mc) {
        Config cfg = Config.get();
        if (mc.options != null && cfg.savedDarknessScale >= 0) {
            mc.options.getDarknessEffectScale().setValue(Math.max(0.0, Math.min(1.0, cfg.savedDarknessScale)));
            cfg.savedDarknessScale = -1;
            Config.save();
        }
        scaleApplied = false;
    }
}
