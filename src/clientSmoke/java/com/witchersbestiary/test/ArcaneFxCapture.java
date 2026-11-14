package com.witchersbestiary.test;

import com.witchersbestiary.client.signs.SignGeometry;
import com.witchersbestiary.client.signs.QuenShellRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import com.witchersbestiary.signs.SignPackets;
import com.witchersbestiary.signs.SignType;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.*;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class ArcaneFxCapture implements ClientModInitializer {
    private static final SignType[] SIGNS = {
            SignType.IGNI, SignType.AARD, SignType.YRDEN, SignType.QUEN, SignType.AXII
    };
    private ArmorStandEntity target;
    private boolean started;
    private int ticks, shot;
    private long captureAt = Long.MAX_VALUE;
    private CompletableFuture<Void> write;

    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, counter) -> capture(MinecraftClient.getInstance()));
    }

    private void tick(MinecraftClient client) {
        if (!started) {
            if (client.getOverlay() != null) return;
            started = true;
            client.options.pauseOnLostFocus = false;
            client.options.getViewDistance().setValue(5);
            client.options.getSimulationDistance().setValue(5);
            client.createIntegratedServerLoader().createAndStart("ArcaneFX-" + System.currentTimeMillis(),
                    new LevelInfo("Arcane FX Capture", GameMode.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), DataConfiguration.SAFE_MODE),
                    new GeneratorOptions(42L, false, false),
                    registries -> registries.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT)
                            .createDimensionsRegistryHolder(), null);
            return;
        }
        if (client.player == null || client.world == null || client.currentScreen != null) return;
        if (write != null) {
            if (!write.isDone()) return;
            write.join();
            write = null;
            if (++shot == SIGNS.length) {
                client.scheduleStop();
                return;
            }
            ticks = 48;
        }
        if (captureAt != Long.MAX_VALUE || shot >= SIGNS.length || ++ticks < 60) return;
        var sign = SIGNS[shot];
        int form = 0;
        client.player.setYaw(0);
        client.player.setPitch(sign == SignType.YRDEN ? 26 : 8);
        var feet = client.player.getPos().add(0, 0, 4);
        if (target != null) {
            client.world.removeEntity(target.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            target = null;
        }
        if (sign == SignType.AXII || sign == SignType.QUEN) {
            target = new ArmorStandEntity(EntityType.ARMOR_STAND, client.world);
            target.setId(-100 - shot);
            target.setPosition(feet);
            target.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,
                    new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_HELMET));
            client.world.addEntity(target);
        }
        switch (sign) {
            case QUEN -> QuenShellRenderer.accept(new SignPackets.QuenShell(target.getId(), form, 12));
            case AXII -> SignGeometry.accept(new SignPackets.Visual(target.getId(), sign.ordinal(), form, 12,
                    feet, feet, .42f));
            case YRDEN -> SignGeometry.accept(new SignPackets.Visual(shot, sign.ordinal(), form, 12,
                    feet, feet, form == 2 ? 1.85f : 3));
            default -> {
                var from = sign == SignType.AARD ? feet.add(0, 1, -1.5) : feet.add(-2.5, 1, 1);
                var to = sign == SignType.AARD ? from.add(0, 0, 5) : from.add(5, 0, 0);
                SignGeometry.accept(new SignPackets.Visual(shot, sign.ordinal(), form, 9,
                        from, to, form == 1 ? 180 : sign == SignType.IGNI ? 37.5f : 45));
            }
        }
        captureAt = client.world.getTime() + 4;
    }

    private void capture(MinecraftClient client) {
        if (client.world == null || client.world.getTime() < captureAt || write != null) return;
        captureAt = Long.MAX_VALUE;
        var path = Path.of("..", SIGNS[shot].name().toLowerCase(java.util.Locale.ROOT) + "-" + 0 + ".png");
        var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
        write = CompletableFuture.runAsync(() -> {
            try (image) {
                image.writeTo(path);
            } catch (java.io.IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        });
    }
}
