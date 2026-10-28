package client.confis.mixin;

import client.confis.Freecam;
import client.confis.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Free Look: after the vanilla camera update, re-aim the camera at the Free Look angles. The camera is put back on the
 * player's eye first, so third person (back and front view) orbits the player and is pulled in by walls like vanilla.
 */
@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void moveBy(float surge, float heave, float sway);
    @Shadow protected abstract float clipToSpace(float distance);
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void confis$freeLook(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                 float tickProgress, CallbackInfo ci) {
        if (!FreeLook.active() || Freecam.active()) return;
        if (focusedEntity != MinecraftClient.getInstance().player) return;   // spectating someone else: leave it alone
        FreeLook.markSeen();

        float yaw = FreeLook.yaw(), pitch = FreeLook.pitch();
        if (inverseView) { yaw += 180f; pitch = -pitch; }

        Vec3d lerped = focusedEntity.getLerpedPos(tickProgress);
        double ex = lerped.x, ey = lerped.y + MathHelper.lerp(tickProgress, lastCameraY, cameraY), ez = lerped.z;
        // distance vanilla used for third person (0 in first person); keeps non-default camera distances
        double vanilla = ((Camera) (Object) this).getCameraPos().distanceTo(new Vec3d(ex, ey, ez));

        setRotation(yaw, pitch);
        setPos(ex, ey, ez);
        if (thirdPerson) moveBy(-clipToSpace(Math.max(4.0f, (float) vanilla)), 0f, 0f);
    }
}
