package com.witchersbestiary.test;

import com.witchersbestiary.entity.projectile.WitcherBombEntity;
import com.witchersbestiary.registry.ModItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

public final class CascadeReview {
    private static volatile boolean cascadeShot;
    private int ticks, phase, shot, launched, impact;
    private boolean flew;
    private String pending;
    private volatile boolean sceneReady;

    public static boolean useCascade() {
        return cascadeShot;
    }

    public static void start() {
        var review = new CascadeReview();
        ClientTickEvents.END_CLIENT_TICK.register(review::tick);
        WorldRenderEvents.END.register(context -> review.capture());
    }

    private void tick(MinecraftClient client) {
        if (phase == 5) return;
        try {
            if (++ticks > 1500) throw new IllegalStateException("Cascade comparison timed out at " + phase);
            if (phase == 0) {
                if (client.getOverlay() != null) return;
                phase = 1;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(4);
                client.createIntegratedServerLoader().createAndStart("CascadeReview",
                        new LevelInfo("Cascade A/B Review", GameMode.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.currentScreen != null) return;
            if (phase == 1) {
                phase = 2;
                client.getServer().execute(() -> {
                    var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                    var world = player.getServerWorld();
                    var feet = player.getBlockPos();
                    for (int x = -4; x <= 4; x++) for (int y = 0; y < 4; y++)
                        world.setBlockState(feet.add(x, y, 6), Blocks.STONE_BRICKS.getDefaultState());
                    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
                    world.setTimeOfDay(12800);
                    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.BOMBS.getFirst()));
                    sceneReady = true;
                });
            }
            if (phase == 2 && sceneReady && client.player.getMainHandStack().isOf(ModItems.BOMBS.getFirst())) {
                if (shot > 0 && ticks - launched < 65) return;
                cascadeShot = shot == 1;
                client.player.setYaw(0);
                client.player.setPitch(8);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                launched = ticks;
                impact = 0;
                flew = false;
                phase = 3;
                return;
            }
            if (phase == 3) {
                boolean flying = false;
                for (var entity : client.world.getEntities())
                    if (entity instanceof WitcherBombEntity) flying = true;
                flew |= flying;
                if (flew && !flying && impact == 0) impact = ticks;
                if (impact == 0 && ticks - launched > 60) throw new IllegalStateException("Missing tracked collision");
                if (impact > 0 && ticks - impact < 12) pending = (shot == 0 ? "arcane-" : "cascade-") + (ticks - impact);
                if (impact > 0 && ticks - impact >= 30) {
                    shot++;
                    phase = shot == 1 ? 2 : 4;
                }
            }
            if (phase == 4) {
                Files.writeString(Path.of("../result.txt"), "Two real throws tracked through impact, two render sequences captured.");
                phase = 5;
                client.scheduleStop();
            }
        } catch (Exception error) {
            fail(client, error);
        }
    }

    private void capture() {
        if (pending == null) return;
        String name = pending;
        pending = null;
        var client = MinecraftClient.getInstance();
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(Path.of("..", name + ".png"));
        } catch (IOException error) {
            fail(client, error);
        }
    }

    private void fail(MinecraftClient client, Exception error) {
        error.printStackTrace();
        phase = 5;
        client.scheduleStop();
    }
}
