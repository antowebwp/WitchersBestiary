package com.witchersbestiary.test;

import com.witchersbestiary.entity.projectile.WitcherBombEntity;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import com.witchersbestiary.signs.SignType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;

final class BombReview implements ClientModInitializer {
    private static final String[] NAMES = {"grapeshot", "moon-dust", "samum", "dragon-gas"};
    private int ticks, index, shotTick, impactTick, phase;
    private boolean flew;
    private volatile int prepared = -1;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        try {
            if (++ticks > 1500) throw new IllegalStateException("Bomb review timeout at index " + index);
            if (phase == 0) {
                if (client.getOverlay() != null) return;
                phase = 1;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(4);
                client.createIntegratedServerLoader().createAndStart("BombReview",
                        new LevelInfo("Bomb VFX Review", GameMode.CREATIVE, false, Difficulty.NORMAL, true,
                                new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.currentScreen != null) return;
            if (phase == 1) {
                int shot = index;
                phase = 2;
                client.getServer().execute(() -> {
                    var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                    var world = player.getServerWorld();
                    var feet = player.getBlockPos();
                    for (int x = -2; x <= 2; x++) for (int y = 0; y < 3; y++)
                        world.setBlockState(feet.add(x, y, 5), net.minecraft.block.Blocks.STONE_BRICKS.getDefaultState());
                    world.setTimeOfDay(13000);
                    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.BOMBS.get(shot)));
                    prepared = shot;
                });
            }
            if (phase == 2 && prepared == index && client.player.getMainHandStack().isOf(ModItems.BOMBS.get(index))) {
                client.player.setYaw(0);
                client.player.setPitch(8);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                shotTick = ticks;
                flew = false;
                impactTick = 0;
                phase = 3;
                return;
            }
            if (phase == 3) {
                boolean flying = false;
                for (var entity : client.world.getEntities()) if (entity instanceof WitcherBombEntity) flying = true;
                if (flying && !flew) capture(client, NAMES[index] + "-flight");
                flew |= flying;
                if (ticks - shotTick > 60 && !flew) throw new IllegalStateException("No tracked projectile " + NAMES[index]);
                if (flew && !flying && impactTick == 0) impactTick = ticks;
                if (impactTick == 0 || ticks < impactTick + (index == 3 ? 16 : 1)) return;
                if (index == 3) {
                    AreaEffectCloudEntity gas = null;
                    for (var entity : client.world.getEntities())
                        if (entity instanceof AreaEffectCloudEntity cloud) gas = cloud;
                    if (gas == null) throw new IllegalStateException("No tracked cloud");
                    capture(client, NAMES[index]);
                    var aim = gas.getPos().add(0, .3, 0).subtract(client.player.getEyePos());
                    float pitch = (float)-Math.toDegrees(Math.atan2(aim.y, aim.horizontalLength()));
                    float yaw = (float)Math.toDegrees(Math.atan2(-aim.x, aim.z));
                    client.player.setPitch(pitch);
                    client.player.setYaw(yaw);
                    phase = 4;
                    impactTick = ticks;
                    client.getServer().execute(() -> {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        player.setPitch(pitch);
                        player.setYaw(yaw);
                        var state = SignManager.state(player);
                        state.learn(SignType.IGNI);
                        state.select(SignType.IGNI);
                        SignManager.cast(player, 1);
                    });
                } else {
                    capture(client, NAMES[index++]);
                    phase = 1;
                }
            }
            if (phase == 4 && ticks >= impactTick + 3) {
                for (var entity : client.world.getEntities()) if (entity instanceof AreaEffectCloudEntity) return;
                capture(client, "dragon-ignition");
                java.nio.file.Files.writeString(java.nio.file.Path.of("../result.txt"), "Four projectiles tracked and removed. Dragon gas appeared and was removed by a normal Igni cast.");
                phase = 5;
                client.scheduleStop();
            }
        } catch (Exception error) {
            error.printStackTrace();
            phase = 5;
            client.scheduleStop();
        }
    }

    private static void capture(MinecraftClient client, String name) throws java.io.IOException {
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(java.nio.file.Path.of("..", name + ".png"));
        }
    }
}
