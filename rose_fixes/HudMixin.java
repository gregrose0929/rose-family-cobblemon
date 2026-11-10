package rose.fixes.mixin;

import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rose.fixes.RoseClient;

/** Hearts, hunger and experience mean nothing in a Pokemon battle, so they step aside. The hotbar stays for Poke Balls. */
@Mixin(value = class_329.class, remap = false)
public abstract class HudMixin {
    @Inject(method = "method_1760", at = @At("HEAD"), cancellable = true)
    private void rose$noHearts(class_332 context, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }

    @Inject(method = "method_1741", at = @At("HEAD"), cancellable = true)
    private void rose$noMountHearts(class_332 context, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }

    @Inject(method = "method_1754", at = @At("HEAD"), cancellable = true)
    private void rose$noExperienceBar(class_332 context, int x, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }

    @Inject(method = "method_56136", at = @At("HEAD"), cancellable = true)
    private void rose$noExperienceLevel(class_332 context, class_9779 tick, CallbackInfo info) {
        if (RoseClient.cleanScreen) {
            info.cancel();
        }
    }
}
