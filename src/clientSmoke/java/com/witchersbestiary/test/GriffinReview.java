package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.GriffinEntity;
import com.witchersbestiary.entity.monster.GriffinEntity.Action;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class GriffinReview {
    private final java.util.concurrent.ExecutorService writer = Executors.newSingleThreadExecutor();
    private final ArrayList<CompletableFuture<Void>> writes = new ArrayList<>();
    private final HashSet<String> captured = new HashSet<>();
    private GriffinEntity serverGriffin;
    private int entityId = -1, ticks, combatTicks, hits, stateAge;
    private double maximumHeight;
    private boolean requested, done;
    private String shot;
    private Action previous = Action.NONE;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.END.register(context -> capture());
    }

    private void tick(MinecraftClient client) {
        if (done) return;
        try {
            if (++ticks > 1550) throw new IllegalStateException("Griffin review timeout");
            if (!requested && client.getOverlay() == null) {
                requested = true;
                client.options.hudHidden = true;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(5);
                client.createIntegratedServerLoader().createAndStart("GriffinReviewAir",
                        new LevelInfo("Griffin Reforged", GameMode.CREATIVE, false, Difficulty.NORMAL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.world == null || client.currentScreen != null) return;
            var server = client.getServer();
            if (entityId == -1) {
                entityId = server.submit(() -> {
                    var p = server.getPlayerManager().getPlayerList().getFirst();
                    var world = p.getServerWorld();
                    world.setTimeOfDay(4500);
                    world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, server);
                    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                    var g = ModEntities.GRIFFIN.create(world);
                    g.refreshPositionAndAngles(.5, -60, .5, 0, 0);
                    g.setAiDisabled(true);
                    world.spawnEntity(g);
                    serverGriffin = g;
                    p.teleport(world,.5,-60,6.5,180,0);
                    return g.getId();
                }).join();
                return;
            }
            if (!(client.world.getEntityById(entityId) instanceof GriffinEntity griffin)) return;
            combatTicks++;
            if (combatTicks < 45) {
                int view = Math.min(3, combatTicks / 10);
                double angle = new double[]{0, Math.PI / 2, Math.PI, Math.PI / 4}[view];
                camera(client, griffin.getPos().add(0,1.4,0), new Vec3d(Math.sin(angle)*8, 2.4, Math.cos(angle)*8));
                if (combatTicks % 10 == 6) shot = "view-" + view;
                return;
            }
            if (combatTicks == 45) server.submit(() -> {
                var p = server.getPlayerManager().getPlayerList().getFirst();
                p.changeGameMode(GameMode.SURVIVAL);
                serverGriffin.setAiDisabled(false);
                serverGriffin.setTarget(p);
            }).join();
            client.options.backKey.setPressed(combatTicks > 50 && combatTicks < 118);
            if (combatTicks == 216) server.submit(() -> {
                var p = server.getPlayerManager().getPlayerList().getFirst();
                p.teleport(p.getServerWorld(), serverGriffin.getX(), -60, serverGriffin.getZ()+21, 180, 0);
                serverGriffin.setTarget(p);
            }).join();
            var result = server.submit(() -> {
                var p = server.getPlayerManager().getPlayerList().getFirst();
                if (p.getHealth() < p.getMaxHealth()) {
                    hits++;
                    p.setHealth(p.getMaxHealth());
                }
                maximumHeight = Math.max(maximumHeight, serverGriffin.getY() + 60);
                return serverGriffin.getAction();
            }).join();
            if (result != previous) {
                previous = result;
                stateAge = 0;
            }
            stateAge++;
            var focus = griffin.getPos().add(0, 1.3, 0);
            camera(client, focus, new Vec3d(7.1,2.7,6.8));
            if (stateAge == (result == Action.NONE ? 2 : result == Action.AIR_CHASE ? 18 : 7))
                shot = result.name().toLowerCase(Locale.ROOT);
            if (combatTicks == 470) {
                client.options.backKey.setPressed(false);
                for (var write : writes) write.join();
                Files.writeString(Path.of("../combat-result.txt"), "Damage samples: " + hits + "\nMaximum height: "
                        + maximumHeight + "\nCaptured: " + captured);
                stop(client);
            }
        } catch (Exception error) {
            error.printStackTrace();
            stop(client);
        }
    }

    private static void camera(MinecraftClient client, Vec3d focus, Vec3d offset) {
        var eye = focus.add(offset);
        var dir = offset.negate();
        float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(dir.y, dir.horizontalLength()));
        var camera = new ArmorStandEntity(client.world, eye.x, eye.y, eye.z);
        camera.refreshPositionAndAngles(eye.x, eye.y-camera.getStandingEyeHeight(), eye.z, yaw, pitch);
        camera.setHeadYaw(yaw);
        camera.prevHeadYaw = yaw;
        camera.prevPitch = pitch;
        client.setCameraEntity(camera);
    }

    private void capture() {
        if (shot == null || done) return;
        String name = shot;
        shot = null;
        if (!captured.add(name)) return;
        var image = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
        writes.add(CompletableFuture.runAsync(() -> {
            try (image) { image.writeTo(Path.of("../",name+".png")); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }, writer));
    }

    private void stop(MinecraftClient client) {
        done = true;
        writer.shutdown();
        client.scheduleStop();
    }
}
