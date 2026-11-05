package rose.fixes;

import com.cobblemon.mod.common.client.CobblemonClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_2561;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
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

    private boolean inBattle;
    private boolean minimapWasOff;

    /** Battles get a clear screen: no minimap over the opponent's health, and no hints behind the menu. */
    private void clearScreenForBattle(class_310 client) {
        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        boolean battling = CobblemonClient.INSTANCE.getBattle() != null;
        if (session != null) {
            MinimapProcessor minimap = session.getMinimapProcessor();
            if (battling && !inBattle) {
                minimapWasOff = minimap.getNoMinimapMessageReceived();
            }
            if (battling) {
                minimap.setNoMinimapMessageReceived(true);
            } else if (inBattle) {
                minimap.setNoMinimapMessageReceived(minimapWasOff);
            }
        }
        if (battling) {
            client.field_1705.method_1758(class_2561.method_43473(), false);
        }
        inBattle = battling;
    }

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.START.register(context -> clearScreenForBattle(class_310.method_1551()));
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
