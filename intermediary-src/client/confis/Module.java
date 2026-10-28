package client.confis;

import net.minecraft.class_310;
import net.minecraft.class_332;

/** One HUD mod (a card in the menu, a draggable box on screen). */
public abstract class Module {
    public final String id, name, category;
    public final double defX, defY;
    public int defBg = 0x80000000, defText = 0xFFFFFFFF, defAccent = 0xFFFFFFFF, defBorder = 0x73FFFFFF;
    /** If non-null, the options screen shows a third color editor with this label. */
    public String accentLabel = null;
    /** Whether a brand-new config entry starts switched on. */
    public boolean defEnabled = true;
    /** Labels of the first two color editors in the options screen. */
    public String bgLabel = "Background color", textLabel = "Text color";
    /** Show the "Background: ON/OFF" switch in the options screen. */
    public boolean bgToggle = true;
    /** Show the color editors at all (switch-only modules such as No Darkness hide them). */
    public boolean colorOptions = true;
    /** Optional line of help text shown in the options screen of modules that have no position. */
    public String hint = null;

    protected Module(String id, String name, String category, double defX, double defY) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defX = defX;
        this.defY = defY;
    }

    public boolean movable() { return true; }

    /** Whether this module draws a glass panel with a border (the options screen then shows a border color + opacity). */
    public boolean hasBorder() { return movable(); }

    /** For modules that are not a movable box (crosshair indicator): draw anywhere on screen. */
    public void drawFixed(class_332 g, class_310 mc, Config.ModuleCfg c, int screenW, int screenH) {}

    public abstract int width(class_310 mc);
    public abstract int height(class_310 mc);
    public abstract void draw(class_332 g, class_310 mc, Config.ModuleCfg c, int x, int y, boolean preview);

    public Config.ModuleCfg cfg() {
        return Config.get().modules.computeIfAbsent(id, k -> {
            Config.ModuleCfg c = new Config.ModuleCfg();
            c.enabled = defEnabled;
            c.fx = defX;
            c.fy = defY;
            c.bg = defBg;
            c.text = defText;
            c.accent = defAccent;
            c.border = defBorder;
            return c;
        });
    }

    public void resetColors() {
        Config.ModuleCfg c = cfg();
        c.bg = defBg;
        c.text = defText;
        c.accent = defAccent;
        c.border = defBorder;
        c.showBg = true;
    }

    public void resetPosition() {
        Config.ModuleCfg c = cfg();
        c.fx = defX;
        c.fy = defY;
    }

    public int sw(class_310 mc) { return Math.round(width(mc) * cfg().scale); }
    public int sh(class_310 mc) { return Math.round(height(mc) * cfg().scale); }

    public int posX(class_310 mc, int screenW) {
        int x = (int) Math.round(cfg().fx * screenW);
        return Math.max(0, Math.min(x, Math.max(0, screenW - sw(mc))));
    }

    public int posY(class_310 mc, int screenH) {
        int y = (int) Math.round(cfg().fy * screenH);
        return Math.max(0, Math.min(y, Math.max(0, screenH - sh(mc))));
    }
}
