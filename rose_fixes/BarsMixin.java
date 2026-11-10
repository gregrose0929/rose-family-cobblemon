package rose.fixes.mixin;

import net.minecraft.class_332;
import net.minecraft.class_337;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rose.fixes.RoseClient;

/** The bars at the top of the screen would sit on the battle's own name plates. */
@Mixin(value = class_337.class, remap = false)
public abstract class BarsMixin {
    @Inject(method = "method_1796", at = @At("HEAD"), cancellable = true)
    private void rose$noBarsInBattle(class_332 context, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }
}
