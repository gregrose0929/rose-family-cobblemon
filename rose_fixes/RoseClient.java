package rose.fixes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.class_310;
import net.minecraft.class_437;

/** The battle menu frees the cursor, which locks the camera. Holding right mouse looks around, without pressing any menu button. */
public class RoseClient implements ClientModInitializer {
    private static final int RIGHT_BUTTON = 1;
    private boolean looking;
    private double lastX;
    private double lastY;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!screen.getClass().getName().equals("com.cobblemon.mod.common.client.gui.battle.BattleGUI")) {
                return;
            }
            looking = false;
            ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, button) -> {
                if (button != RIGHT_BUTTON) {
                    return true;
                }
                looking = true;
                lastX = client.field_1729.method_1603();
                lastY = client.field_1729.method_1604();
                return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((s, x, y, button) -> {
                if (button != RIGHT_BUTTON) {
                    return true;
                }
                looking = false;
                return false;
            });
            ScreenEvents.afterRender(screen).register((s, context, x, y, delta) -> look(client));
        });
    }

    private void look(class_310 client) {
        if (!looking || client.field_1724 == null) {
            return;
        }
        double mouseX = client.field_1729.method_1603();
        double mouseY = client.field_1729.method_1604();
        double sensitivity = client.field_1690.method_42495().method_41753() * 0.6 + 0.2;
        double scale = sensitivity * sensitivity * sensitivity * 8.0;
        client.field_1724.method_5872((mouseX - lastX) * scale, (mouseY - lastY) * scale);
        lastX = mouseX;
        lastY = mouseY;
    }
}
