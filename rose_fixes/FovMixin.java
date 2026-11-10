package rose.fixes.mixin;

import net.minecraft.class_4184;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rose.fixes.RoseClient;

/** Zooms the view in for the battle camera's close-ups. */
@Mixin(value = class_757.class, remap = false)
public abstract class FovMixin {
    @Inject(method = "method_3196", at = @At("RETURN"), cancellable = true)
    private void rose$battleZoom(class_4184 camera, float delta, boolean changing, CallbackInfoReturnable<Double> result) {
        if (RoseClient.placeCamera && RoseClient.fovScale != 1) {
            result.setReturnValue(result.getReturnValue() * RoseClient.fovScale);
        }
    }
}
