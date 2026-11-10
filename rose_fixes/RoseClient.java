package rose.fixes;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import java.nio.file.Files;
import java.nio.file.Path;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattleActor;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.UUID;
import kotlin.Unit;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1101;
import net.minecraft.class_1113;
import net.minecraft.class_1297;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3419;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;

/**
 * Battles get a clear screen, a chosen camera and music. Trainers are announced with a banner, and gym leaders and other
 * big names get an opening sweep around the field. The battle menu frees the cursor, so holding right mouse looks around.
 */
public class RoseClient implements ClientModInitializer {
    private static final int RIGHT_BUTTON = 1;
    private static final String CAMERA_MESSAGE = "rose-battle-camera:";
    private static final int SIDE = 0, BEHIND = 1, OVERHEAD = 2, FIRST_PERSON = 3, FREE = 4, GAME = 5;
    private static final long INTRO_MILLIS = 2600, SWEEP_MILLIS = 4200;
    /** The shots Game Style cuts between, and how long each one holds. */
    private static final int IDLE = 0, WIDE = 1, ATTACK = 2, REACT = 3, FAINT = 4, ENTER = 5;
    private static final long[] SHOT_MILLIS = {0, 7000, 1500, 1300, 2200, 1800};
    private static final long IDLE_BEFORE_WIDE = 9000, SHAKE_MILLIS = 380;
    private static final int WILD = 0, TRAINER = 1, BIG = 2;
    private static final String[] MUSIC = {"battle.wild", "battle.trainer", "battle.gym"};

    /** Read by the camera each frame. */
    public static boolean placeCamera;
    public static boolean showBody = true;
    public static double cameraX, cameraY, cameraZ;
    public static float cameraYaw, cameraPitch;
    /** True during a battle: hearts, hunger, experience, the top bars and pop-ups are not drawn. */
    public static boolean cleanScreen;
    /** Below 1 zooms in, for close-ups. */
    public static double fovScale = 1;

    private int mode = GAME;
    private ClientBattle listeningTo;
    /** Shots waiting their turn: the kind, then 1 if it is about the player's own Pokemon. */
    private final Deque<int[]> shots = new ArrayDeque<>();
    private int shot = IDLE;
    private boolean shotOfAlly, shakePending;
    private long shotStart, shotEnd, shakeStart;
    /** Once the player's body gets in the way it stays hidden until the next cut, so it never flickers. */
    private boolean bodyHidden;
    private boolean gliding;
    private long lastFrame;
    private double glideX, glideY, glideZ, lookAtX, lookAtY, lookAtZ;
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
    /** Names of gym leaders and other big opponents, in lower case, with their title and name for the banner. */
    private final Map<String, String[]> bigTrainers = new HashMap<>();
    private int kind;
    private String bannerTitle = "", bannerName = "";
    private long battleStart, sweepStart;
    private boolean sweepDone;

    private Path settingsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("rose_battle_camera.txt");
    }

    private void loadMode() {
        try {
            mode = Math.max(SIDE, Math.min(GAME, Integer.parseInt(Files.readString(settingsFile()).trim())));
        } catch (Exception noSavedChoice) {
            mode = GAME;
        }
    }

    private void chooseMode(int chosen) {
        mode = chosen;
        framed = false;
        aimed = false;
        gliding = false;
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
            gliding = false;
            shots.clear();
            shot = IDLE;
            shakePending = false;
            shakeStart = 0;
            beginBattle(client);
        }
        inBattle = battling;
        cleanScreen = battling;
        if (battling) {
            listenForMoves();
            updateCamera(client);
        } else {
            placeCamera = false;
        }
        if (!placeCamera) {
            fovScale = 1;
        }
        // The first-person hand makes no sense from across the field.
        client.field_1773.method_35768(!placeCamera);
    }

    private void updateCamera(class_310 client) {
        placeCamera = false;
        showBody = true;
        fovScale = 1;
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        // Holding a Poke Ball means the player is aiming a throw, which needs their own eyes.
        if (client.field_1724.method_6047().method_7909() instanceof PokeBallItem || client.field_1724.method_6079().method_7909() instanceof PokeBallItem) {
            aimed = false;
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
        if (kind == BIG && framed && !sweepDone && mode != FREE && mode != FIRST_PERSON) {
            long now = System.currentTimeMillis();
            if (sweepStart == 0) {
                sweepStart = now;
            }
            if (now - sweepStart < SWEEP_MILLIS) {
                sweep(client, (now - sweepStart) / (double) SWEEP_MILLIS);
                return;
            }
            sweepDone = true;
            // Game Style carries on from where the sweep ended instead of cutting.
            glideX = cameraX;
            glideY = cameraY;
            glideZ = cameraZ;
            lookAtX = midX;
            lookAtY = midY + 0.8;
            lookAtZ = midZ;
            lastFrame = now;
            shots.clear();
            beginShot(IDLE, true, now);
            gliding = true;
        }
        if (mode == GAME) {
            gameCamera(client, fighters);
            return;
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
        placeAround(client, offsetAngle + Math.toRadians(class_3532.method_15393(client.field_1724.method_36454() - startYaw)), modeDistance(), modeHeight());
    }

    private double modeDistance() {
        return mode == OVERHEAD ? 2.5 : mode == BEHIND ? separation * 0.6 + 7 : separation * 0.7 + 6;
    }

    private double modeHeight() {
        return mode == OVERHEAD ? 7 + separation * 0.3 : mode == BEHIND ? 3.5 : 2.5;
    }

    /** Puts the camera on a circle around the middle of the fight, looking in. */
    private void placeAround(class_310 client, double angle, double distance, double height) {
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
        showBody = !bodyInTheWay(client, midX, lookY, midZ);
        placeCamera = true;
    }

    /** Works out who the battle is against, then starts its music. */
    private void beginBattle(class_310 client) {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        kind = TRAINER;
        bannerTitle = "";
        bannerName = "";
        battleStart = System.currentTimeMillis();
        sweepStart = 0;
        sweepDone = false;
        if (battle.isPvW()) {
            kind = WILD;
        } else {
            UUID me = client.field_1724 == null ? null : client.field_1724.method_5667();
            for (ClientBattleSide side : battle.getSides()) {
                boolean mine = false;
                for (ClientBattleActor actor : side.getActors()) {
                    mine |= actor.getUuid().equals(me);
                }
                if (mine) {
                    continue;
                }
                for (ClientBattleActor actor : side.getActors()) {
                    String name = actor.getDisplayName().getString();
                    String[] big = bigTrainers.get(name.toLowerCase(Locale.ROOT));
                    if (big != null) {
                        kind = BIG;
                        bannerTitle = big[0];
                        bannerName = big[1];
                    } else if (kind != BIG) {
                        bannerTitle = actor.getType() == ActorType.PLAYER ? "Family Battle" : "Trainer Battle";
                        bannerName = name;
                    }
                }
            }
        }
        // The game's own background music would play over the battle's.
        client.method_1538().method_4859();
        client.method_1483().method_4873(new Music(MUSIC[kind]));
        System.out.println("[Rose] Battle against " + (kind == WILD ? "a wild Pokemon" : bannerTitle + " " + bannerName) + ", music " + MUSIC[kind]
            + (client.method_1483().method_4869(class_2960.method_60655("rose_family", MUSIC[kind])) == null ? " (missing)" : " (found)"));
    }

    /** Battle music that loops until the battle is over, then fades away. */
    private static final class Music extends class_1101 {
        private static final float FULL = 0.55f;

        Music(String name) {
            super(class_3414.method_47908(class_2960.method_60655("rose_family", name)), class_3419.field_15253, class_1113.method_43221());
            field_5446 = true;
            field_5451 = 0;
            field_5442 = 0.05f;
            field_18936 = true;
            field_5440 = class_1113.class_1114.field_5478;
        }

        @Override
        public void method_16896() {
            if (class_310.method_1551().field_1687 == null) {
                method_24876();
            } else if (CobblemonClient.INSTANCE.getBattle() == null) {
                field_5442 -= 0.02f;
                if (field_5442 <= 0) {
                    method_24876();
                }
            } else if (field_5442 < FULL) {
                field_5442 = Math.min(FULL, field_5442 + 0.05f);
            }
        }
    }

    /** A circling shot that opens a battle with a gym leader, coming down to where the chosen camera sits. */
    private void sweep(class_310 client, double progress) {
        double eased = progress * progress * (3 - 2 * progress);
        double far = separation * 0.9 + 12;
        placeAround(client, offsetAngle + (1 - eased) * Math.PI * 1.15, far + (modeDistance() - far) * eased, 7.5 + (modeHeight() - 7.5) * eased);
    }

    /** Names the opponent across the middle of the screen as a trainer battle begins. */
    private void drawBanner(class_310 client, class_332 context) {
        if (!inBattle || kind == WILD || bannerName.isEmpty() || client.field_1690.field_1842) {
            return;
        }
        long age = System.currentTimeMillis() - battleStart;
        long length = kind == BIG ? SWEEP_MILLIS : INTRO_MILLIS;
        if (age > length) {
            return;
        }
        double opening = Math.min(1, age / 300.0);
        double shown = Math.min(opening, Math.min(1, (length - age) / 400.0));
        int alpha = (int) (shown * 255);
        int width = context.method_51421(), middle = (int) (context.method_51443() * 0.36);
        int half = kind == BIG ? 24 : 16;
        int reach = (int) (width / 2 * opening);
        int top = middle - half, bottom = middle + half;
        context.method_25294(width / 2 - reach, top, width / 2 + reach, bottom, (int) (shown * 150) << 24);
        context.method_25294(width / 2 - reach, top, width / 2 + reach, top + 1, alpha << 24 | 0xFFAA00);
        context.method_25294(width / 2 - reach, bottom - 1, width / 2 + reach, bottom, alpha << 24 | 0xFFAA00);
        if (alpha < 16) {
            // Text this faint would be drawn solid.
            return;
        }
        context.method_25300(client.field_1772, bannerTitle.toUpperCase(Locale.ROOT), width / 2, top + (kind == BIG ? 5 : 4), alpha << 24 | 0xFFAA00);
        float scale = kind == BIG ? 2.5f : 1.5f;
        context.method_51448().method_22903();
        context.method_51448().method_46416(width / 2f, top + (kind == BIG ? 18 : 15), 0);
        context.method_51448().method_22905(scale, scale, 1);
        context.method_25300(client.field_1772, bannerName, 0, 0, alpha << 24 | 0xFFFFFF);
        context.method_51448().method_22909();
    }

    /** Reads the battle's messages, so the camera can cut to whoever is attacking, hit, fainting or coming out. */
    private void listenForMoves() {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null || battle == listeningTo) {
            return;
        }
        listeningTo = battle;
        battle.getMessages().subscribe(line -> {
            StringBuilder text = new StringBuilder();
            line.accept((index, style, codePoint) -> {
                text.appendCodePoint(codePoint);
                return true;
            });
            String words = text.toString();
            class_310 client = class_310.method_1551();
            if (client.field_1724 == null) {
                return Unit.INSTANCE;
            }
            int mine = words.startsWith(client.field_1724.method_5477().getString() + "'s") ? 1 : 0;
            if (words.contains(" used ")) {
                shots.add(new int[] {ATTACK, mine});
                shots.add(new int[] {REACT, 1 - mine});
            } else if (words.contains(" fainted")) {
                shots.add(new int[] {FAINT, mine});
            } else if (words.startsWith("Go! ")) {
                shots.add(new int[] {ENTER, 1});
            } else if (words.contains(" sent out ")) {
                shots.add(new int[] {ENTER, 0});
            }
            if (words.contains("super effective") || words.contains("critical hit")) {
                // A big hit shakes the shot of whoever took it.
                if (shot == REACT) {
                    shakeStart = System.currentTimeMillis();
                } else {
                    shakePending = true;
                }
            }
            return Unit.INSTANCE;
        });
    }

    /** True when the player stands in the foreground of the shot, between the camera and what it is looking at. */
    private boolean bodyInTheWay(class_310 client, double aimX, double aimY, double aimZ) {
        double lineX = aimX - cameraX, lineY = aimY - cameraY, lineZ = aimZ - cameraZ;
        double length = Math.max(0.1, Math.sqrt(lineX * lineX + lineY * lineY + lineZ * lineZ));
        double toX = client.field_1724.method_23317() - cameraX, toY = client.field_1724.method_23318() + 0.9 - cameraY, toZ = client.field_1724.method_23321() - cameraZ;
        double along = (toX * lineX + toY * lineY + toZ * lineZ) / length;
        if (along < -1 || along > length + 0.5) {
            // Behind the camera, or in the background where a trainer belongs.
            return false;
        }
        double offX = toX - lineX / length * along, offY = toY - lineY / length * along, offZ = toZ - lineZ / length * along;
        return Math.sqrt(offX * offX + offY * offY + offZ * offZ) < 1.4 + Math.max(0, along) * 0.6;
    }

    private void beginShot(int kind, boolean ofAlly, long now) {
        bodyHidden = false;
        shot = kind;
        shotOfAlly = ofAlly;
        shotStart = now;
        // A long queue means the battle is running ahead of the camera, so hurry.
        shotEnd = now + (long) (SHOT_MILLIS[kind] * (shots.size() > 3 ? 0.6 : 1));
        if (kind == ATTACK) {
            shakePending = false;
        } else if (kind == REACT && shakePending) {
            shakeStart = now + 250;
            shakePending = false;
        }
    }

    /**
     * Films the battle the way the Pokemon games do, in cuts. While you choose, it sits behind your Pokemon, sometimes
     * cutting to a wide shot. A move gets a shot of the attacker, then one over its shoulder at the target. Fainting and
     * coming out get close-ups. The camera stays on one side of the two Pokemon, so left and right never swap.
     */
    private void gameCamera(class_310 client, List<PokemonEntity> fighters) {
        UUID me = client.field_1724.method_5667();
        PokemonEntity ally = null;
        for (PokemonEntity fighter : fighters) {
            if (me.equals(fighter.method_6139()) && (ally == null || client.field_1724.method_5858(fighter) < client.field_1724.method_5858(ally))) {
                ally = fighter;
            }
        }
        if (ally == null) {
            // Ownership hasn't reached this PC yet, so the nearest fighter is the best guess.
            for (PokemonEntity fighter : fighters) {
                if (ally == null || client.field_1724.method_5858(fighter) < client.field_1724.method_5858(ally)) {
                    ally = fighter;
                }
            }
        }
        PokemonEntity foe = null;
        if (ally != null) {
            for (PokemonEntity fighter : fighters) {
                if (fighter != ally && !me.equals(fighter.method_6139()) && (foe == null || ally.method_5858(fighter) < ally.method_5858(foe))) {
                    foe = fighter;
                }
            }
        }
        if (ally == null || foe == null || !framed) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean cut = false;
        boolean idle = shot == IDLE || shot == WIDE;
        if (!gliding && kind == WILD) {
            // Open on the wild Pokemon. Trainers announce theirs, which brings its own shot.
            beginShot(ENTER, false, now);
            shotEnd = now + INTRO_MILLIS;
            cut = true;
        } else if (!gliding) {
            beginShot(IDLE, true, now);
            cut = true;
        } else if ((!idle && now >= shotEnd) || (idle && !shots.isEmpty())) {
            int[] next = shots.poll();
            if (next != null) {
                beginShot(next[0], next[1] == 1, now);
            } else {
                beginShot(IDLE, true, now);
            }
            cut = true;
        } else if (shot == IDLE && !lookedAround && now - shotStart > IDLE_BEFORE_WIDE) {
            beginShot(WIDE, true, now);
            cut = true;
        } else if (shot == WIDE && (now >= shotEnd || lookedAround)) {
            beginShot(IDLE, true, now);
            cut = true;
        }
        double ax = ally.method_23317(), ay = ally.method_23318(), az = ally.method_23321();
        double fx = foe.method_23317(), fy = foe.method_23318(), fz = foe.method_23321();
        double gap = Math.max(0.5, Math.sqrt((fx - ax) * (fx - ax) + (fz - az) * (fz - az)));
        double dirX = (fx - ax) / gap, dirZ = (fz - az) / gap;
        double rightX = -dirZ, rightZ = dirX;
        double allyHeight = ally.method_17682(), foeHeight = foe.method_17682();
        // The subject of the shot, the other Pokemon, and which way the subject faces it.
        double sx = shotOfAlly ? ax : fx, sy = shotOfAlly ? ay : fy, sz = shotOfAlly ? az : fz;
        double ox = shotOfAlly ? fx : ax, oy = shotOfAlly ? fy : ay, oz = shotOfAlly ? fz : az;
        double height = shotOfAlly ? allyHeight : foeHeight, otherHeight = shotOfAlly ? foeHeight : allyHeight;
        double facing = shotOfAlly ? 1 : -1;
        double progress = shotEnd > shotStart ? Math.min(1, (now - shotStart) / (double) (shotEnd - shotStart)) : 0;
        double goalX, goalY, goalZ, aimX, aimY, aimZ;
        if (shot == ATTACK || shot == FAINT || shot == ENTER) {
            // In front of the subject and off to the side. Attacks push in, entrances pull back, and faints rise slowly.
            double near = shot == ATTACK ? (2.4 + height * 1.3) * (1 - 0.12 * progress) : shot == FAINT ? 2.0 + height * 1.1 : (2.0 + height * 1.2) * (1 + 0.25 * progress);
            // Never so far forward that the camera ends up past the other Pokemon.
            near = Math.min(near, gap * 0.62);
            double side = shot == ATTACK ? 1.7 + height * 0.5 : shot == FAINT ? 1.2 + height * 0.4 : 1.0 + height * 0.4;
            double up = shot == ATTACK ? 0.6 + height * 0.5 : shot == FAINT ? 0.35 + height * 0.25 + 0.5 * progress : 0.4 + height * 0.3;
            goalX = sx + dirX * facing * near + rightX * side;
            goalY = sy + up;
            goalZ = sz + dirZ * facing * near + rightZ * side;
            aimX = sx;
            aimY = sy + height * (shot == ATTACK ? 0.55 : shot == FAINT ? 0.35 : 0.6);
            aimZ = sz;
            fovScale = shot == ATTACK ? 0.82 : shot == FAINT ? 0.75 : 0.85;
        } else if (shot == REACT) {
            // Over the attacker's shoulder at the Pokemon being hit.
            double back = (1.6 + otherHeight * 1.2) * (1 - 0.1 * progress);
            goalX = ox + dirX * facing * back + rightX * (0.9 + otherHeight * 0.45);
            goalY = oy + otherHeight * 0.95 + 0.5;
            goalZ = oz + dirZ * facing * back + rightZ * (0.9 + otherHeight * 0.45);
            aimX = sx;
            aimY = sy + height * 0.5;
            aimZ = sz;
            fovScale = 0.8;
        } else if (shot == WIDE) {
            // Both Pokemon from the side, panning slowly along the line between them.
            double pan = -1.5 + 3 * progress;
            goalX = (ax + fx) / 2 + rightX * (gap * 0.8 + 5.5) + dirX * pan;
            goalY = (ay + fy) / 2 + 1.6;
            goalZ = (az + fz) / 2 + rightZ * (gap * 0.8 + 5.5) + dirZ * pan;
            aimX = (ax + fx) / 2;
            aimY = (ay + fy) / 2 + 0.6;
            aimZ = (az + fz) / 2;
            fovScale = 0.85;
        } else {
            // The classic view: behind and beside your Pokemon, looking across at the opponent, swaying a little.
            double back = 2.6 + allyHeight * 1.4;
            double side = 1.4 + allyHeight * 0.45 + 0.25 * Math.sin((now - shotStart) / 2600.0);
            double offsetX = -dirX * back + rightX * side, offsetZ = -dirZ * back + rightZ * side;
            double lookX = dirX * gap * 0.7, lookZ = dirZ * gap * 0.7;
            // Right mouse orbits around your own Pokemon.
            double turn = Math.toRadians(class_3532.method_15393(client.field_1724.method_36454() - startYaw));
            double cos = Math.cos(turn), sin = Math.sin(turn);
            goalX = ax + offsetX * cos - offsetZ * sin;
            goalY = ay + 1.1 + allyHeight * 0.9;
            goalZ = az + offsetX * sin + offsetZ * cos;
            aimX = ax + lookX * cos - lookZ * sin;
            aimY = ay + ((fy - ay) + foeHeight * 0.5 + allyHeight * 0.4) / 2;
            aimZ = az + lookX * sin + lookZ * cos;
            fovScale = 0.9;
        }
        // Pull in toward what the shot is looking at rather than end up inside a wall.
        double reach = 1.0;
        for (double step = 0.15; step <= 1.0; step += 0.05) {
            class_2338 at = class_2338.method_49638(new class_243(aimX + (goalX - aimX) * step, aimY + (goalY - aimY) * step, aimZ + (goalZ - aimZ) * step));
            if (!client.field_1687.method_8320(at).method_26220(client.field_1687, at).method_1110()) {
                reach = Math.max(0.2, step - 0.1);
                break;
            }
        }
        goalX = aimX + (goalX - aimX) * reach;
        goalY = aimY + (goalY - aimY) * reach;
        goalZ = aimZ + (goalZ - aimZ) * reach;
        if (!gliding || cut) {
            glideX = goalX;
            glideY = goalY;
            glideZ = goalZ;
            lookAtX = aimX;
            lookAtY = aimY;
            lookAtZ = aimZ;
            gliding = true;
        } else {
            // Within a shot the camera only eases, which smooths out the Pokemon's own movement.
            double blend = 1 - Math.exp(-Math.min(now - lastFrame, 100) * (shot == IDLE ? 0.0035 : 0.012));
            glideX += (goalX - glideX) * blend;
            glideY += (goalY - glideY) * blend;
            glideZ += (goalZ - glideZ) * blend;
            lookAtX += (aimX - lookAtX) * blend;
            lookAtY += (aimY - lookAtY) * blend;
            lookAtZ += (aimZ - lookAtZ) * blend;
        }
        lastFrame = now;
        cameraX = glideX;
        cameraY = glideY;
        cameraZ = glideZ;
        double toX = lookAtX - glideX, toZ = lookAtZ - glideZ;
        cameraYaw = (float) Math.toDegrees(Math.atan2(toZ, toX)) - 90f;
        cameraPitch = (float) -Math.toDegrees(Math.atan2(lookAtY - glideY, Math.sqrt(toX * toX + toZ * toZ)));
        long shaking = now - shakeStart;
        if (shaking >= 0 && shaking < SHAKE_MILLIS) {
            double strength = 1.4 * (1 - shaking / (double) SHAKE_MILLIS);
            cameraYaw += (float) (strength * Math.sin(shaking * 0.09));
            cameraPitch += (float) (strength * Math.cos(shaking * 0.11));
        }
        // Your own body never blocks the shot: it only shows when it is off to the side or in the background.
        bodyHidden |= client.field_1724.method_5649(glideX, glideY, glideZ) < 16 || bodyInTheWay(client, lookAtX, lookAtY, lookAtZ);
        showBody = !bodyHidden;
        placeCamera = true;
    }

    @Override
    public void onInitializeClient() {
        loadMode();
        try (InputStream list = RoseClient.class.getResourceAsStream("/assets/rose_family/big_trainers.txt")) {
            for (String line : new String(list.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    bigTrainers.put(parts[0], new String[] {parts[1], parts[2]});
                }
            }
        } catch (Exception noList) {
            // Every trainer then gets the ordinary trainer music and banner.
        }
        HudRenderCallback.EVENT.register((context, tick) -> drawBanner(class_310.method_1551(), context));
        WorldRenderEvents.START.register(context -> clearScreenForBattle(class_310.method_1551()));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String text = message.getString();
            if (!text.startsWith(CAMERA_MESSAGE)) {
                return true;
            }
            try {
                chooseMode(Math.max(SIDE, Math.min(GAME, Integer.parseInt(text.substring(CAMERA_MESSAGE.length()).trim()))));
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
