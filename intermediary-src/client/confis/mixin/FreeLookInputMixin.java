package client.confis.mixin;

import client.confis.FreeLook;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Free Look: mouse movement aimed at the local player is redirected to the camera angles while Free Look is on. */
@Mixin(Entity.class)
public abstract class FreeLookInputMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void confis$freeLookInput(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (FreeLook.onLook((Entity) (Object) this, cursorDeltaX, cursorDeltaY)) ci.cancel();
    }
}
