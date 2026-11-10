package rose.fixes.mixin;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The trainer mod only lets players fight gym leaders in a set order inside one of its "series", and turns away anyone
 * whose team is over a level cap. This family server has neither, so any trainer who is free will battle.
 */
@Mixin(value = TrainerMob.class, remap = false)
public abstract class TrainerMixin {
    @Inject(method = "canBattleAgainst", at = @At("HEAD"), cancellable = true)
    private void rose$anyoneCanBattle(class_1297 challenger, CallbackInfoReturnable<Boolean> result) {
        if (!(challenger instanceof class_1657 player)) {
            return;
        }
        TrainerMob trainer = (TrainerMob) (Object) this;
        RCTMod mod = RCTMod.getInstance();
        result.setReturnValue(trainer.getCooldown() == 0 && !trainer.isInBattle() && !mod.isInBattle(player)
            && mod.getTrainerManager().getActivePokemon(player) > 0 && trainer.couldBattleAgainst(challenger));
    }
}
