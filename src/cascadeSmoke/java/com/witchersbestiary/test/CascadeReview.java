package com.witchersbestiary.test;

import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.client.vfx.GrapeshotVolume;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
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

public final class CascadeReview {
    private enum Angle {
        PREVIOUS("previous", new Vec3d(0, 1.44, -5.5)),
        FRONT("front", new Vec3d(0, 1.44, -5.5)),
        DIAGONAL("45deg", new Vec3d(3.89, 1.44, -3.89)),
        SIDE("side", new Vec3d(5.5, 1.44, 0));

        final String filePrefix;
        final Vec3d cameraOffset;

        Angle(String filePrefix, Vec3d cameraOffset) {
            this.filePrefix = filePrefix;
            this.cameraOffset = cameraOffset;
        }
    }

    private static final Angle[] SHOTS = Angle.values();
    private static volatile Angle armed;
    private static volatile Vec3d impactCue;

    private final GrapeshotVolume volume = new GrapeshotVolume();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final List<CompletableFuture<Void>> writes = new ArrayList<>();
    private int ticks, phase, shot, launched, impactTick, lastCapture = -1;
    private Vec3d impact;
    private volatile boolean sceneReady;

    public static boolean useCascade() {
        return armed != null;
    }

    public static boolean usePreviousRecipe() {
        return armed == Angle.PREVIOUS;
    }

    public static void impact(Vec3d position) {
        impactCue = position;
    }

    public static void start() {
        var review = new CascadeReview();
        ClientTickEvents.END_CLIENT_TICK.register(review::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (review.impact != null && review.phase == 4 && SHOTS[review.shot] != Angle.PREVIOUS) {
                double age = review.ticks - review.impactTick + context.tickCounter().getTickDelta(false);
                review.volume.render(context, review.impact.add(0, .45, 0), age);
            }
        });
        WorldRenderEvents.END.register(context -> review.capture());
    }

    private void tick(MinecraftClient client) {
        if (phase == 6) return;
        try {
            if (++ticks > 2000) throw new IllegalStateException("Grapeshot review timed out at " + phase);
            if (phase == 0) {
                if (client.getOverlay() != null) return;
                phase = 1;
                client.options.pauseOnLostFocus = false;
                client.options.hudHidden = true;
                client.options.getViewDistance().setValue(4);
                client.options.getMaxFps().setValue(120);
                client.createIntegratedServerLoader().createAndStart("CascadeVolumeReview",
                        new LevelInfo("Grapeshot Volume Review", GameMode.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.currentScreen != null) return;
            if (phase == 1) {
                phase = 2;
                client.getServer().execute(() -> {
                    var server = client.getServer();
                    var player = server.getPlayerManager().getPlayerList().getFirst();
                    var world = player.getServerWorld();
                    var feet = player.getBlockPos();
                    server.getTickManager().setTickRate(10);
                    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                    world.setTimeOfDay(11000);
                    for (int x = -9; x <= 9; x++) {
                        for (int z = -6; z <= 12; z++) {
                            var block = (x + z) % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
                            world.setBlockState(feet.add(x, -1, z), block.getDefaultState());
                        }
                    }
                    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.BOMBS.getFirst()));
                    sceneReady = true;
                });
            }
            if (phase == 2 && sceneReady && client.player.getMainHandStack().isOf(ModItems.BOMBS.getFirst())) {
                if (shot > 0 && ticks - launched < 50) return;
                armed = SHOTS[shot];
                impactCue = null;
                client.player.setYaw(0);
                client.player.setPitch(22);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                launched = ticks;
                lastCapture = -1;
                phase = 3;
                return;
            }
            if (phase == 3) {
                var cue = impactCue;
                if (cue != null) {
                    impact = cue;
                    impactTick = ticks;
                    setCamera(client, cue, SHOTS[shot]);
                    phase = 4;
                } else if (ticks - launched > 50) {
                    throw new IllegalStateException("No real Grapeshot impact for " + SHOTS[shot]);
                }
            }
            if (phase == 4 && ticks - impactTick >= 29) {
                client.setCameraEntity(client.player);
                impact = null;
                armed = null;
                phase = ++shot == SHOTS.length ? 5 : 2;
            }
            if (phase == 5 && writes.stream().allMatch(CompletableFuture::isDone)) {
                writes.forEach(CompletableFuture::join);
                Files.writeString(Path.of("../volume-result.txt"), "Four real Grapeshot collisions, three camera angles.");
                finish(client);
            }
        } catch (Exception error) {
            error.printStackTrace();
            finish(client);
        }
    }

    private static void setCamera(MinecraftClient client, Vec3d impact, Angle angle) {
        var eye = impact.add(angle.cameraOffset);
        var focus = impact.add(0, 1.02, 0);
        var direction = focus.subtract(eye);
        var camera = new ArmorStandEntity(client.world, eye.x, eye.y, eye.z);
        float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalLength()));
        camera.refreshPositionAndAngles(eye.x, eye.y - camera.getStandingEyeHeight(), eye.z, yaw, pitch);
        // Camera reads interpolated head yaw for living camera entities.
        camera.setHeadYaw(yaw);
        camera.prevHeadYaw = yaw;
        camera.prevPitch = pitch;
        client.setCameraEntity(camera);
    }

    private void capture() {
        if (phase != 4) return;
        int age = ticks - impactTick;
        if (age == lastCapture || age > 26) return;
        lastCapture = age;
        var path = Path.of("..", SHOTS[shot].filePrefix + "-" + age + ".png");
        var image = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
        writes.add(CompletableFuture.runAsync(() -> {
            try (image) {
                image.writeTo(path);
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        }, writer));
    }

    private void finish(MinecraftClient client) {
        phase = 6;
        writer.shutdown();
        client.scheduleStop();
    }
}
