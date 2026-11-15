package com.witchersbestiary.test;

import com.witchersbestiary.client.signs.SignsClient;
import com.witchersbestiary.entity.projectile.WitcherBombEntity;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import com.witchersbestiary.signs.SignPlayerState;
import com.witchersbestiary.signs.SignType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class ArcaneV2Review {
    private enum Shot {
        IGNI, BREATH_SIDE, BREATH_45, AARD, GRAPESHOT, MOON_DUST, SAMUM, GAS, GAS_BREATH
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<CompletableFuture<Void>> writes = new ArrayList<>();
    private static final Shot[] SHOTS = Shot.values();
    private int ticks, phase, index, launched, started, captured = -1, resume;
    private boolean flying, ignitionRequested, ended;
    private volatile int ready = -1;
    private volatile boolean castOk = true;
    private net.minecraft.entity.mob.ZombieEntity reviewTarget;
    private volatile boolean suppressed;
    private Vec3d cloudPosition;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.END.register(context -> capture());
    }

    private void tick(MinecraftClient client) {
        if (phase == 7) return;
        try {
            if (++ticks > 2200) throw new IllegalStateException("Arcane FX capture timeout at " + index + "/" + phase);
            if (phase == 0) {
                if (client.getOverlay() != null) return;
                phase = 1;
                client.options.pauseOnLostFocus = false;
                client.options.hudHidden = true;
                client.options.getViewDistance().setValue(4);
                client.options.getMaxFps().setValue(120);
                client.createIntegratedServerLoader().createAndStart("ArcaneV2Review",
                        new LevelInfo("Arcane FX V2 Review", GameMode.CREATIVE, false, Difficulty.NORMAL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(41, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.world == null || client.currentScreen != null) return;
            if (!castOk) throw new IllegalStateException("Real cast rejected for " + SHOTS[index]);
            if (phase == 1) {
                if (ticks < resume) return;
                if (index == SHOTS.length) {
                    phase = 6;
                    return;
                }
                prepare(client, SHOTS[index]);
                phase = 2;
            }
            if (phase == 2 && ticks >= resume && ready == index && SignsClient.view != null) {
                var shot = SHOTS[index];
                int item = bomb(shot);
                float pitch = item < 0 ? 3 : 32;
                client.player.setYaw(0);
                client.player.setPitch(pitch);
                if (item >= 0 && !client.player.getMainHandStack().isOf(ModItems.BOMBS.get(item))) return;
                launched = ticks;
                captured = -1;
                flying = false;
                ignitionRequested = false;
                ended = false;
                cloudPosition = null;
                if (item < 0) {
                    var focus = client.player.getPos().add(0,1.35,3.2);
                    setCamera(client, focus, switch (shot) {
                        case BREATH_SIDE -> new Vec3d(7.0,1.0,0);
                        case BREATH_45 -> new Vec3d(4.25,1.15,-4.25);
                        case AARD -> new Vec3d(4.4,2.6,-5.25);
                        default -> new Vec3d(4.5,1.4,-4.6);
                    });
                    if (breath(shot)) SignsClient.send(10, SignType.IGNI.ordinal());
                    else cast(client, shot == Shot.AARD ? SignType.AARD : SignType.IGNI, 3, 0);
                    started = ticks;
                    phase = 4;
                } else {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    phase = 3;
                }
                return;
            }
            if (phase == 3) trackImpact(client);
            if (phase == 4) advance(client, SHOTS[index]);
            if (phase == 6 && writes.stream().allMatch(CompletableFuture::isDone)) {
                writes.forEach(CompletableFuture::join);
                Files.writeString(Path.of("../result.txt"),
                        "Real sign casts and bomb throws. Moon Dust marked its target. Both Igni forms consumed Dragon gas.");
                stop(client);
            }
        } catch (Exception error) {
            error.printStackTrace();
            stop(client);
        }
    }

    private void prepare(MinecraftClient client, Shot shot) {
        int current = index;
        client.getServer().execute(() -> {
            var server = client.getServer();
            var player = server.getPlayerManager().getPlayerList().getFirst();
            var world = player.getServerWorld();
            var feet = player.getBlockPos();
            if (current == 0) {
                server.getTickManager().setTickRate(10);
                world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, server);
                world.setTimeOfDay(12300);
                for (int x = -9; x <= 9; x++) for (int z = -6; z <= 12; z++) {
                    var block = (x + z) % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
                    world.setBlockState(feet.add(x, -1, z), block.getDefaultState());
                }
                resume = ticks + 18;
            }
            var state = SignManager.state(player);
            state.learn(SignType.IGNI);
            state.learn(SignType.AARD);
            if (current == 0) {
                state.award(100);
                for (var node : SignPlayerState.Upgrade.values()) state.upgrade(SignType.IGNI, node);
                if (!state.unlock(SignType.IGNI, 2) || !state.equip(SignType.IGNI, 1, 2)) castOk = false;
            }
            state.select(shot == Shot.AARD ? SignType.AARD : SignType.IGNI);
            int bomb = bomb(shot);
            player.setPitch(bomb < 0 ? 3 : 32);
            player.setYaw(0);
            player.setStackInHand(Hand.MAIN_HAND, bomb < 0 ? ItemStack.EMPTY : new ItemStack(ModItems.BOMBS.get(bomb)));
            if (shot == Shot.GAS && reviewTarget != null) reviewTarget.discard();
            if (shot == Shot.MOON_DUST) {
                var target = EntityType.ZOMBIE.create(world);
                if (target == null) throw new IllegalStateException("Unable to spawn suppression target");
                target.setAiDisabled(true);
                target.setPersistent();
                target.setPosition(player.getPos().add(.92,0,2.5));
                world.spawnEntity(target);
                reviewTarget = target;
            }
            SignManager.sync(player);
            ready = current;
        });
    }

    private void trackImpact(MinecraftClient client) {
        boolean present = false;
        for (var entity : client.world.getEntities()) if (entity instanceof WitcherBombEntity) present = true;
        if (present) {
            flying = true;
            return;
        }
        if (!flying) {
            if (ticks - launched > 45) throw new IllegalStateException("Projectile was not tracked");
            return;
        }
        var focus = client.player.getPos().add(0,.80,2.65);
        setCamera(client, focus, new Vec3d(2.35,1.45,-5.65));
        started = ticks;
        phase = 4;
    }

    private void advance(MinecraftClient client, Shot shot) {
        int age = ticks - started;
        if (breath(shot)) {
            if (age % 6 == 0 && age < 23) SignsClient.send(8, SignType.IGNI.ordinal());
            if (age == 23) SignsClient.send(7, SignType.IGNI.ordinal());
        }
        if (shot == Shot.MOON_DUST && age >= 6 && age < 24) {

            for (var entity : client.world.getEntities()) {
                if (entity instanceof net.minecraft.entity.mob.ZombieEntity zombie &&
                        zombie.isGlowing()) suppressed = true;
            }

        }
        if (shot == Shot.MOON_DUST && age == 24 && !suppressed)
            throw new IllegalStateException("Moon Dust did not mark the review target");
        if (shot == Shot.GAS || shot == Shot.GAS_BREATH) {
            AreaEffectCloudEntity gas = null;
            for (var entity : client.world.getEntities()) if (entity instanceof AreaEffectCloudEntity cloud) gas = cloud;
            if (cloudPosition == null && gas != null) {
                cloudPosition = gas.getPos();
                setCamera(client, cloudPosition.add(0,.73,0), new Vec3d(2.7,1.75,-5.95));
            }
            if (age >= 31 && !ignitionRequested && cloudPosition != null) {
                ignitionRequested = true;
                var aim = cloudPosition.add(0,.38,0).subtract(client.player.getEyePos());
                float pitch = pitch(aim), yaw = yaw(aim);
                client.player.setYaw(yaw);
                client.player.setPitch(pitch);
                if (shot == Shot.GAS) cast(client, SignType.IGNI, pitch, yaw);
                else SignsClient.send(10, SignType.IGNI.ordinal());
            }
            if (age == 42 && shot == Shot.GAS_BREATH) SignsClient.send(7, SignType.IGNI.ordinal());
            if (ignitionRequested && gas == null) ended = true;
            if (age == 55 && !ended) throw new IllegalStateException("Dragon's Dream did not ignite for " + shot);
        }
        int length = switch (shot) {
            case GAS, GAS_BREATH -> 69;
            case MOON_DUST -> 51;
            case BREATH_SIDE, BREATH_45 -> 32;
            default -> 27;
        };
        if (age < length) return;
        client.setCameraEntity(client.player);
        index++;
        phase = 1;
        resume = ticks + 63;
    }

    private void cast(MinecraftClient client, SignType type, float pitch, float yaw) {
        client.getServer().execute(() -> {
            var server = client.getServer();
            var player = server.getPlayerManager().getPlayerList().getFirst();
            player.setYaw(yaw);
            player.setPitch(pitch);
            SignManager.state(player).select(type);
            castOk = SignManager.cast(player, server.getTicks());
        });
    }

    private static int bomb(Shot shot) {
        return switch (shot) {
            case GRAPESHOT -> 0;
            case MOON_DUST -> 1;
            case SAMUM -> 2;
            case GAS, GAS_BREATH -> 3;
            default -> -1;
        };
    }

    private static boolean breath(Shot shot) {
        return shot == Shot.BREATH_SIDE || shot == Shot.BREATH_45;
    }

    private static void setCamera(MinecraftClient client, Vec3d focus, Vec3d offset) {
        var eye = focus.add(offset);
        var dir = focus.subtract(eye);
        var camera = new ArmorStandEntity(client.world, eye.x, eye.y, eye.z);
        float yaw = yaw(dir), pitch = pitch(dir);
        camera.refreshPositionAndAngles(eye.x, eye.y - camera.getStandingEyeHeight(), eye.z, yaw, pitch);
        camera.setHeadYaw(yaw);
        camera.prevHeadYaw = yaw;
        camera.prevPitch = pitch;
        client.setCameraEntity(camera);
    }

    private static float yaw(Vec3d dir) {
        return (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
    }

    private static float pitch(Vec3d dir) {
        return (float)-Math.toDegrees(Math.atan2(dir.y, dir.horizontalLength()));
    }

    private void capture() {
        if (phase != 4) return;
        int age = ticks - started;
        if (age == captured) return;
        captured = age;
        var path = Path.of("..", SHOTS[index].name().toLowerCase(java.util.Locale.ROOT) + "-" + age + ".png");
        var image = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
        writes.add(CompletableFuture.runAsync(() -> {
            try (image) {
                image.writeTo(path);
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        }, io));
    }

    private void stop(MinecraftClient client) {
        phase = 7;
        io.shutdown();
        client.scheduleStop();
    }
}
