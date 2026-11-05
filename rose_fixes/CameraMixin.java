package rose.fixes.mixin;

import net.minecraft.class_1297;
import net.minecraft.class_1922;
import net.minecraft.class_243;
import net.minecraft.class_4184;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rose.fixes.RoseClient;

/** Moves the view to the chosen battle camera, and shows the player's own body while it's there. */
@Mixin(value = class_4184.class, remap = false)
public abstract class CameraMixin {
    @Shadow
    private boolean field_18719;

    @Shadow
    protected abstract void method_19325(float yaw, float pitch);

    @Shadow
    protected abstract void method_19322(class_243 position);

    @Inject(method = "method_19321", at = @At("TAIL"))
    private void rose$battleCamera(class_1922 area, class_1297 entity, boolean thirdPerson, boolean inverse, float delta, CallbackInfo info) {
        if (RoseClient.placeCamera) {
            field_18719 = RoseClient.showBody;
            method_19325(RoseClient.cameraYaw, RoseClient.cameraPitch);
            method_19322(new class_243(RoseClient.cameraX, RoseClient.cameraY, RoseClient.cameraZ));
        }
    }
}
