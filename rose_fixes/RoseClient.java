package rose.fixes;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;

/** Battles get a clear screen and a chosen camera. The battle menu frees the cursor, so holding right mouse looks around. */
public class RoseClient implements ClientModInitializer {
    private static final int RIGHT_BUTTON = 1;
    private static final String CAMERA_MESSAGE = "rose-battle-camera:";
    private static final int SIDE = 0, BEHIND = 1, OVERHEAD = 2, FIRST_PERSON = 3, FREE = 4;

    /** Read by the camera each frame. */
    public static boolean placeCamera;
    public static double cameraX, cameraY, cameraZ;
    public static float cameraYaw, cameraPitch;

    private int mode = SIDE;
    private boolean looking;
    private double lastX;
    private double lastY;
    private boolean inBattle;
    private boolean minimapWasOff;
    private boolean aimed;
    private boolean lookedAround;
    private boolean framed;
    private double midX, midY, midZ;
    private double offsetAngle;
    private double separation;
    private float startYaw;

    private Path settingsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("rose_battle_camera.txt");
    }

    private void loadMode() {
        try {
            mode = Math.max(SIDE, Math.min(FREE, Integer.parseInt(Files.readString(settingsFile()).trim())));
        } catch (Exception noSavedChoice) {
            mode = SIDE;
        }
    }

    private void chooseMode(int chosen) {
        mode = chosen;
        framed = false;
        aimed = false;
        try {
            Files.writeString(settingsFile(), Integer.toString(chosen));
        } catch (Exception couldNotSave) {
            // The choice still works until the game closes.
        }
    }

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
        if (battling && !inBattle) {
            framed = false;
            aimed = false;
            lookedAround = false;
        }
        inBattle = battling;
        if (battling) {
            updateCamera(client);
        } else {
            placeCamera = false;
        }
        // The first-person hand makes no sense from across the field.
        client.field_1773.method_35768(!placeCamera);
    }

    private void updateCamera(class_310 client) {
        placeCamera = false;
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        List<PokemonEntity> fighters = new ArrayList<>();
        for (class_1297 entity : client.field_1687.method_18112()) {
            if (entity instanceof PokemonEntity pokemon && pokemon.isBattling() && client.field_1724.method_5858(pokemon) < 40 * 40) {
                fighters.add(pokemon);
            }
        }
        if (fighters.size() >= 2) {
            double x = 0, y = 0, z = 0;
            for (PokemonEntity fighter : fighters) {
                x += fighter.method_23317();
                y += fighter.method_23318();
                z += fighter.method_23321();
            }
            x /= fighters.size();
            y /= fighters.size();
            z /= fighters.size();
            if (!framed) {
                class_243 a = fighters.get(0).method_19538();
                class_243 b = fighters.get(fighters.size() - 1).method_19538();
                double dx = b.field_1352 - a.field_1352;
                double dz = b.field_1350 - a.field_1350;
                separation = Math.sqrt(dx * dx + dz * dz);
                // Film from the far side, so the player isn't standing between the camera and the fight.
                double sideX = -dz, sideZ = dx;
                double toPlayerX = client.field_1724.method_23317() - x, toPlayerZ = client.field_1724.method_23321() - z;
                if (sideX * toPlayerX + sideZ * toPlayerZ > 0) {
                    sideX = -sideX;
                    sideZ = -sideZ;
                }
                // Over the shoulder rather than straight through the player.
                offsetAngle = mode == BEHIND ? Math.atan2(toPlayerZ, toPlayerX) + 0.5 : Math.atan2(sideZ, sideX);
                startYaw = client.field_1724.method_36454();
                midX = x;
                midY = y;
                midZ = z;
                framed = true;
            }
            midX += (x - midX) * 0.05;
            midY += (y - midY) * 0.05;
            midZ += (z - midZ) * 0.05;
        }
        if (!framed || mode == FREE) {
            return;
        }
        if (mode == FIRST_PERSON) {
            if (!aimed && !lookedAround) {
                double dx = midX - client.field_1724.method_23317();
                double dz = midZ - client.field_1724.method_23321();
                client.field_1724.method_36456((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
                client.field_1724.method_36457((float) Math.toDegrees(Math.atan2(client.field_1724.method_23320() - midY - 0.5, Math.sqrt(dx * dx + dz * dz))));
                aimed = true;
            }
            return;
        }
        double distance = mode == OVERHEAD ? 2.5 : mode == BEHIND ? separation * 0.6 + 7 : separation * 0.7 + 6;
        double height = mode == OVERHEAD ? 7 + separation * 0.3 : mode == BEHIND ? 3.5 : 2.5;
        double angle = offsetAngle + Math.toRadians(class_3532.method_15393(client.field_1724.method_36454() - startYaw));
        double outX = Math.cos(angle) * distance, outZ = Math.sin(angle) * distance;
        double lookY = midY + 0.8;
        // Pull in rather than end up inside a wall.
        double reach = 1.0;
        for (double step = 0.1; step <= 1.0; step += 0.05) {
            class_2338 at = class_2338.method_49638(new class_243(midX + outX * step, lookY + (height - 0.8) * step, midZ + outZ * step));
            if (!client.field_1687.method_8320(at).method_26220(client.field_1687, at).method_1110()) {
                reach = Math.max(0.2, step - 0.1);
                break;
            }
        }
        cameraX = midX + outX * reach;
        cameraY = lookY + (height - 0.8) * reach;
        cameraZ = midZ + outZ * reach;
        double lookX = midX - cameraX, lookZ = midZ - cameraZ;
        cameraYaw = (float) Math.toDegrees(Math.atan2(lookZ, lookX)) - 90f;
        cameraPitch = (float) -Math.toDegrees(Math.atan2(lookY - cameraY, Math.sqrt(lookX * lookX + lookZ * lookZ)));
        placeCamera = true;
    }

    @Override
    public void onInitializeClient() {
        loadMode();
        WorldRenderEvents.START.register(context -> clearScreenForBattle(class_310.method_1551()));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String text = message.getString();
            if (!text.startsWith(CAMERA_MESSAGE)) {
                return true;
            }
            try {
                chooseMode(Math.max(SIDE, Math.min(FREE, Integer.parseInt(text.substring(CAMERA_MESSAGE.length()).trim()))));
            } catch (NumberFormatException ignored) {
                // Not a choice this version knows.
            }
            return false;
        });
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
                lookedAround = true;
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
