package com.witchersbestiary.test;

import com.witchersbestiary.client.vfx.VfxManager;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class ArcaneFxCapture implements ClientModInitializer {
    private static final String[] NAMES = {"igni_normal", "igni_breath_side", "igni_breath_45"};
    private boolean started;
    private int ticks, shot, frames;
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
            if (++shot == NAMES.length) {
                client.scheduleStop();
                return;
            }
            ticks = 59;
        }
        if (shot >= NAMES.length || ++ticks < 60) return;
        client.player.setYaw(0);
        client.player.setPitch(13);
        var center = client.player.getEyePos().add(0, -.6, 6);
        boolean diagonal = shot == 2;
        var axis = diagonal ? new Vec3d(1, 0, 1).normalize() : new Vec3d(1, 0, 0);
        var from = center.subtract(axis.multiply(2.5));
        var to = center.add(axis.multiply(2.5));
        boolean breath = shot != 0;
        VfxManager.igni(new SignPackets.Visual(shot, SignType.IGNI.ordinal(), breath ? 2 : 0,
                breath ? 8 : 9, from, to, breath ? .45f : 27f), client.world.getTime() - 4);
        frames = 1;
    }

    private void capture(MinecraftClient client) {
        if (frames == 0 || write != null) return;
        frames = 0;
        var path = Path.of("..", NAMES[shot] + ".png");
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
