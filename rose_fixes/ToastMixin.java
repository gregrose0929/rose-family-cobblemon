package rose.fixes.mixin;

import net.minecraft.class_332;
import net.minecraft.class_374;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rose.fixes.RoseClient;

/** Pop-ups wait until the battle is over. They aren't lost: their timers only run while they are drawn. */
@Mixin(value = class_374.class, remap = false)
public abstract class ToastMixin {
    @Inject(method = "method_1996", at = @At("HEAD"), cancellable = true)
    private void rose$noPopUpsInBattle(class_332 context, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }
}
