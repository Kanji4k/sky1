package client.confis;

import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Sky-style main menu (real sky photo background) that replaces the vanilla title screen (switch: Config.skyTitle).
 * Left column of frosted glass buttons, drifting clouds over a sky gradient, quit at the bottom centre.
 * Drawn with fill() only; clicks are polled from GLFW (same trick as KeyPoll), so no input signatures are needed.
 */
public class SkyTitleScreen extends class_437 {
    private static final String[] LABELS = {"single player", "multiplayer", "mod menu", "sky mods", "options"};
    private final class_310 mc = class_310.method_1551();
    private boolean mouseWas = true;   // true at start so the click that opened the screen is ignored

    public SkyTitleScreen() { super(class_2561.method_43470("Sky Client")); }

    @Override public void method_25420(class_332 g, int mx, int my, float d) {}
    @Override public boolean method_25421() { return false; }
    @Override public void method_25419() {}   // Esc does nothing on the main menu

    private static int bw(int w) { return Math.max(130, Math.min(190, w / 4)); }

    @Override
    public void method_25394(class_332 g, int mx, int my, float d) {
        int W = field_22789, H = field_22790;
        SkyBg.draw(g, W, H);

        boolean down = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = down && !mouseWas;
        mouseWas = down;

        int w = bw(W), h = 26, gap = 8, x0 = Math.max(20, W / 16);
        int total = LABELS.length * (h + gap) - gap;
        int y0 = Math.max(36, (H - total) / 2 - 10);
        g.method_51439(field_22793, class_2561.method_43470("SKY CLIENT"), x0, y0 - 18, 0xFFFFFFFF, true);
        for (int i = 0; i < LABELS.length; i++) {
            int y = y0 + i * (h + gap);
            boolean hov = mx >= x0 && mx < x0 + w && my >= y && my < y + h;
            button(g, hov ? x0 + 6 : x0, y, w, h, LABELS[i], hov, i == 0);
            if (hov && click) activate(i);
        }
        int qw = Math.min(150, W / 3), qx = (W - qw) / 2, qy = H - 40;
        boolean qh = mx >= qx && mx < qx + qw && my >= qy && my < qy + h;
        button(g, qx, qy, qw, h, "quit", qh, false);
        if (qh && click) mc.scheduleStop();
    }

    private void button(class_332 g, int x, int y, int w, int h, String label, boolean hover, boolean accent) {
        int body = accent ? 0xCC1083FE : hover ? 0xD9FFFFFF : 0x8CFFFFFF;
        Glass.fillRound(g, x - 1, y - 1, w + 2, h + 2, 7, hover ? 0xFFFFFFFF : 0xB0FFFFFF);
        Glass.fillRound(g, x, y, w, h, 6, body);
        Glass.fillRound(g, x + 2, y + 1, w - 4, h / 2 - 1, 5, 0x30FFFFFF);
        int col = accent ? 0xFFFFFFFF : 0xFF0A2A52;
        g.method_51439(field_22793, class_2561.method_43470(label), x + 12, y + (h - 8) / 2, col, false);
    }

    private void activate(int i) {
        switch (i) {
            case 0 -> mc.method_1507(new SelectWorldScreen(this));
            case 1 -> mc.method_1507(new MultiplayerScreen(this));
            case 2 -> openModMenu();
            case 3 -> mc.method_1507(new ConfigScreen());
            case 4 -> mc.method_1507(new OptionsScreen(this, mc.field_1690));
        }
    }

    /** Opens Mod Menu if it is installed (looked up by name, no dependency), otherwise the Confis menu. */
    private void openModMenu() {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            mc.method_1507((class_437) c.getConstructor(class_437.class).newInstance(this));
        } catch (Throwable t) {
            mc.method_1507(new ConfigScreen());
        }
    }
}
