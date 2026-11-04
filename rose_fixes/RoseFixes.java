package rose.fixes;

import com.cobblemon.mod.common.api.item.PokemonSelectingItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.class_1269;
import net.minecraft.class_1792;

/** Cobblemon's potions place as decorations when aimed at the ground. Using one on your team should win unless the player sneaks. */
public class RoseFixes implements ModInitializer {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            class_1792 item = player.method_5998(hand).method_7909();
            boolean opensSomething = world.method_8321(hit.method_17777()) != null;
            if (!(item instanceof PokemonSelectingItem) || player.method_5715() || opensSomething) {
                return class_1269.field_5811;
            }
            if (!world.method_8608()) {
                item.method_7836(world, player, hand);
            }
            return class_1269.field_5812;
        });
    }
}
