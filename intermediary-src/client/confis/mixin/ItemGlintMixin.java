package client.confis.mixin;

import client.confis.Glint;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Custom Item Glint: hides the glint of the kinds of items (weapons / armor / tools / other) that are switched off. */
@Mixin(ItemStack.class)
public class ItemGlintMixin {
    @Inject(method = "hasGlint", at = @At("RETURN"), cancellable = true, require = 0)
    private void confis$glint(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && Glint.hidden((ItemStack) (Object) this)) cir.setReturnValue(false);
    }
}
