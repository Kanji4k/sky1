package client.confis.mixin;

import client.confis.HitColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hit Color: swaps the tint color of a recently hit player's model. See HitColor for the full description. */
@Mixin(targets = "net.minecraft.client.render.command.BatchingRenderCommandQueue")
public abstract class HitColorBatchMixin {
    @Shadow
    public abstract <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer renderLayer,
                                         int light, int overlay, int tintedColor, Sprite sprite, int outlineColor,
                                         ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay);

    @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true, require = 0)
    private <S> void confis$hitColor(Model<? super S> model, S state, MatrixStack matrices, RenderLayer renderLayer,
                                     int light, int overlay, int tintedColor, Sprite sprite, int outlineColor,
                                     ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, CallbackInfo ci) {
        if (HitColor.busy) return;
        int tint = HitColor.tint(model, state, tintedColor);
        if (tint == tintedColor) return;
        HitColor.busy = true;
        try {
            this.submitModel(model, state, matrices, renderLayer, light, overlay, tint, sprite, outlineColor, crumblingOverlay);
        } finally {
            HitColor.busy = false;
        }
        ci.cancel();
    }
}
