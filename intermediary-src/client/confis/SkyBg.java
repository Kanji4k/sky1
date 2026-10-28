package client.confis;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Identifier;

/** Draws the sky photo as a full-screen background (title screen, Multiplayer, Mod Menu). */
public final class SkyBg {
    private SkyBg() {}

    private static final Identifier TEX = Identifier.of("confis0oooooo", "textures/gui/sky_bg.png");
    private static final int TW = 960, TH = 540;

    public static void draw(DrawContext g, int w, int h) {
        g.drawTexture(RenderPipelines.GUI_TEXTURED, TEX, 0, 0, 0f, 0f, w, h, TW, TH, TW, TH);
        g.fill(0, 0, w, h, 0x18FFFFFF);   // light wash so text stays crisp
    }

    /** Every screen gets the sky background except inventories, chat and the Confis screens (they need the world). */
    public static boolean wanted(Screen s) {
        if (!Config.get().skyScreens) return false;
        if (s instanceof HandledScreen || s instanceof ChatScreen) return false;
        return !s.getClass().getName().startsWith("client.confis");
    }
}
