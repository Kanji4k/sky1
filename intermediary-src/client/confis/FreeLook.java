package client.confis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Free Look: the camera turns with the mouse while your real player keeps its own rotation, so W / A / S / D, attacks
 * and the crosshair target stay tied to where the player faces and only the view moves.
 *
 * How it works (nothing of the player is ever modified, so there is nothing to "restore" afterwards):
 *  - FreeLookInputMixin swallows the mouse movement meant for the local player while Free Look is on and adds it to
 *    the camera angles kept here instead.
 *  - FreeLookCameraMixin puts the camera on those angles after the vanilla camera update (first and third person).
 *  - Turning Free Look off simply stops both hooks, the vanilla camera follows the player again.
 *
 * It switches itself off when: the module is switched off, a menu (other than chat) opens, the game window loses
 * focus, the player or world goes away (death, disconnect, dimension change) or Freecam takes over the camera.
 * Both mixins live in their own mixin config (confis0oooooo.freelook.mixins.json) so a failed hook can never take the
 * rest of the mod down with it; if the camera hook never runs the module turns itself off after 1.5 seconds.
 */
public final class FreeLook {
    private static boolean on, keyWas, cameraSeen;
    private static float yaw, pitch;
    private static long startedNs;

    private FreeLook() {}

    public static boolean active() { return on; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    /** Called by the camera hook every frame it runs. */
    public static void markSeen() { cameraSeen = true; }

    /** Forget everything (disconnect / world change). The player's own rotation was never touched. */
    public static void reset() {
        on = false;
        keyWas = false;
        cameraSeen = false;
    }

    private static boolean moduleOn() {
        Module m = Modules.byId("freelook");
        return m != null && m.cfg().enabled;
    }

    private static boolean windowFocused(long win) {
        return win != 0L && GLFW.glfwGetWindowAttrib(win, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    /** Called every frame from Hud.render: key handling and all the safety checks. */
    public static void poll(MinecraftClient mc) {
        Config cfg = Config.get();
        long win = GLFW.glfwGetCurrentContext();
        boolean down = windowFocused(win) && GLFW.glfwGetKey(win, cfg.freeLookKey) == GLFW.GLFW_PRESS;
        boolean edge = down && !keyWas;
        keyWas = down;

        boolean enabled = moduleOn();
        boolean world = mc.player != null && mc.world != null && mc.player.isAlive();
        boolean freeToLook = enabled && world && !Freecam.active() && windowFocused(win)
                && (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen);

        if (on) {
            if (!freeToLook) { stop(mc, false); return; }
            if (cfg.freeLookHold ? !down : edge) { stop(mc, true); return; }
            // safety net: the camera hook never ran (mixin did not apply on this version) -> give the camera back
            if (!cameraSeen && System.nanoTime() - startedNs > 1_500_000_000L) {
                stop(mc, false);
                if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal("Free Look is not supported on this version"), false);
            }
            return;
        }
        if (edge && freeToLook && mc.currentScreen == null) start(mc);
    }

    private static void start(MinecraftClient mc) {
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        cameraSeen = false;
        startedNs = System.nanoTime();
        on = true;
        mc.inGameHud.setOverlayMessage(Text.literal("Free Look ON  [" + Zoom.keyName(Config.get().freeLookKey) + "]"), false);
    }

    private static void stop(MinecraftClient mc, boolean message) {
        on = false;
        if (message && mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal("Free Look OFF"), false);
    }

    /**
     * Mouse hook (FreeLookInputMixin). Returns true if the movement was taken for the camera; the vanilla
     * code that would turn the player is then skipped. Same 0.15 degrees per mouse unit as vanilla.
     */
    public static boolean onLook(Entity e, double dx, double dy) {
        if (!on) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e != mc.player) return false;
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
        return true;
    }

    // ------------------------------------------------------------------ module (HUD box)
    public static class FreeLookModule extends Module {
        public FreeLookModule() {
            super("freelook", "Free Look", "HUD", 0.100, 0.520);
            defAccent = 0xFF55FF55;
            accentLabel = "Free Look on color";
        }

        private String text() {
            return "Free Look [" + Zoom.keyName(Config.get().freeLookKey) + "]  " + (FreeLook.active() ? "ON" : "OFF");
        }

        @Override public int width(MinecraftClient mc) { return mc.textRenderer.getWidth(text()) + 8; }
        @Override public int height(MinecraftClient mc) { return 13; }

        @Override
        public void draw(DrawContext g, MinecraftClient mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            if (c.showBg) Glass.draw(g, x, y, width(mc), 13, c.bg, c.border);
            g.drawText(mc.textRenderer, text(), x + 4, y + 3, Glass.tc(FreeLook.active() ? c.accent : c.text), true);
        }
    }
}
