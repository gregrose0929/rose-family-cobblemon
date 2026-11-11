package rose.fixes.mixin;

import net.minecraft.class_1297;
import net.minecraft.class_4604;
import net.minecraft.class_898;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rose.fixes.RoseClient;

/** Anyone standing between the battle camera and the Pokemon it is filming isn't drawn for that shot. */
@Mixin(value = class_898.class, remap = false)
public abstract class HideMixin {
    @Inject(method = "method_3950", at = @At("HEAD"), cancellable = true)
    private void rose$outOfTheShot(class_1297 entity, class_4604 view, double x, double y, double z, CallbackInfoReturnable<Boolean> result) {
        if (RoseClient.placeCamera && RoseClient.hidden.contains(entity.method_5628())) {
            result.setReturnValue(false);
        }
    }
}
