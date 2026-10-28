package client.confis.mixin;

import client.confis.SkyBg;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenSkyMixin {
    @Inject(method = "renderBackground", at = @At("TAIL"), require = 0)
    private void confis$sky(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (SkyBg.wanted(self)) SkyBg.draw(ctx, self.width, self.height);
    }
}
