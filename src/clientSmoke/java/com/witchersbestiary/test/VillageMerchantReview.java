package com.witchersbestiary.test;

import com.witchersbestiary.merchant.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.Path;

final class VillageMerchantReview implements ClientModInitializer {
    private int phase, ticks;
    private volatile Throwable failure;
    private volatile boolean positioned;
    private volatile int merchantId;
    private String previous;
    private boolean turnedPage;

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        try {
            if (failure != null) throw new IllegalStateException(failure);
            if (++ticks > 1800) throw new IllegalStateException("Merchant review timed out in phase " + phase);
            if (phase == 0 && client.getOverlay() == null) {
                phase++;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(6);
                client.options.getSimulationDistance().setValue(5);
                client.createIntegratedServerLoader().createAndStart("Merchant-" + System.currentTimeMillis(),
                        new LevelInfo("Village Merchant Review", GameMode.SURVIVAL, false, Difficulty.PEACEFUL,
                                false, new GameRules(), DataConfiguration.SAFE_MODE),
                        new GeneratorOptions(220719L, true, false),
                        r -> r.get(net.minecraft.registry.RegistryKeys.WORLD_PRESET).get(WorldPresets.DEFAULT)
                                .createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null) return;
            if (phase == 1) {
                phase++;
                client.getServer().execute(() -> {
                    try {
                        var world = client.getServer().getOverworld();
                        var village = world.locateStructure(StructureTags.VILLAGE, world.getSpawnPos(), 32, false);
                        if (village == null) throw new IllegalStateException("No generated village");
                        world.getChunk(village);
                        var at = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, village);
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        player.teleport(world, at.getX()+.5, at.getY(), at.getZ()+.5, 0, 0);
                        positioned = true;
                    } catch (Throwable error) { failure = error; }
                });
                return;
            }
            if (phase == 2 && positioned) {
                for (var entity : client.world.getEntities()) {
                    if (!(entity instanceof ArcaneMerchant) || merchantId != 0) continue;
                    int id = entity.getId();
                    client.getServer().execute(() -> {
                        var world = client.getServer().getOverworld();
                        if (!(world.getEntityById(id) instanceof ArcaneMerchant merchant)
                                || merchant.mode() != ArcaneMerchant.Mode.VILLAGE) return;
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        merchant.setAiDisabled(true);
                        player.teleport(world, merchant.getX(), merchant.getY()+.1, merchant.getZ()-2.5, 0, 0);
                        merchantId = id;
                    });
                }
                if (merchantId == 0) return;
                phase++;
                ticks = 0;
            }
            if (phase == 3 && ticks >= 25) {
                var entity = client.world.getEntityById(merchantId);
                if (entity == null) return;
                var direction = entity.getEyePos().subtract(client.player.getEyePos());
                client.player.setYaw((float)Math.toDegrees(Math.atan2(-direction.x, direction.z)));
                client.player.setPitch((float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalLength())));
                if (ticks == 29) capture(client, "village");
                if (ticks >= 35 && ticks % 5 == 0)
                    client.interactionManager.interactEntity(client.player, entity, Hand.MAIN_HAND);
                if (client.player.currentScreenHandler instanceof MerchantHandler) phase++;
            }
            if (phase >= 4 && client.player.currentScreenHandler instanceof MerchantHandler handler
                    && handler.snapshot.has("offers")) {
                String offers = handler.snapshot.get("offers").toString();
                if (phase == 4) {
                    if (!(client.currentScreen instanceof com.witchersbestiary.client.merchant.MerchantScreen screen)) return;
                    if (!turnedPage) {
                        for (var child : screen.children()) {
                            if (child instanceof net.minecraft.client.gui.widget.ButtonWidget button
                                    && button.getMessage().getString().equals(">")) {
                                button.onPress();
                                turnedPage = true;
                                break;
                            }
                        }
                        return;
                    }
                    previous = offers;
                    capture(client, "before");
                    phase = 5;
                    client.getServer().execute(() -> {
                        var world = client.getServer().getOverworld();
                        ((net.minecraft.world.level.ServerWorldProperties)world.getLevelProperties())
                                .setTime(world.getTime() + ArcaneMerchant.VILLAGE_ROTATION);
                    });
                } else if (!offers.equals(previous)) {
                    if (phase++ == 5) return;
                    capture(client, "after");
                    java.nio.file.Files.writeString(Path.of("../result.txt"), previous + "\n" + offers);
                    client.scheduleStop();
                }
            }
        } catch (Throwable error) {
            error.printStackTrace();
            client.scheduleStop();
        }
    }

    private static void capture(MinecraftClient client, String name) throws java.io.IOException {
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(Path.of("..", name + ".png"));
        }
    }
}
