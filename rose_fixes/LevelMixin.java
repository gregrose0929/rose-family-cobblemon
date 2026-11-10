package rose.fixes.mixin;

import com.gitlab.srcmc.rctmod.api.utils.LevelUtils;
import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No level cap. Outside a series the trainer mod holds every player at level 15, which also keeps stronger trainers from
 * ever appearing. With the cap at the top level, wandering trainers match the player's own team instead.
 */
@Mixin(value = LevelUtils.class, remap = false)
public abstract class LevelMixin {
    @Inject(method = "levelCap(Lnet/minecraft/class_1657;)I", at = @At("HEAD"), cancellable = true)
    private static void rose$noLevelCap(class_1657 player, CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(100);
    }
}
