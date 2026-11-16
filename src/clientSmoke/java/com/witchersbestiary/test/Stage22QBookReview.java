package com.witchersbestiary.test;

import com.witchersbestiary.bestiary.BestiaryItem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.gui.GuiBook;

final class Stage22QBookReview {
    private static final boolean STAGE22X = Boolean.getBoolean("witchersbestiary.stage22xReview");
    private static final String[] ENTRIES = STAGE22X ? new String[] {"contract_field_guide"} : new String[] {"grapeshot", "moon_dust", "samum", "dragons_dream",
            "sign_igni", "sign_aard", "sign_yrden", "sign_quen", "sign_axii", "silver_witcher_sword"};
    private final ArrayList<CompletableFuture<Void>> captures = new ArrayList<>();
    private int age, scene, opened;
    private boolean worldRequested, stopped, bookOpened;
    private String language = "en_us";
    private CompletableFuture<Void> reload;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (stopped) return;
        if (++age > 1600) throw new IllegalStateException("Stage 22Q book review timeout");
        if (!worldRequested && client.getOverlay() == null) {
            worldRequested = true;
            client.options.pauseOnLostFocus = false;
            client.options.getViewDistance().setValue(3);
            client.getLanguageManager().setLanguage(language);
            client.createIntegratedServerLoader().createAndStart("Stage22Q",
                    new LevelInfo("Stage 22Q Review", GameMode.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                    r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
            return;
        }
        if (client.player == null || client.getOverlay() != null || reload != null && !reload.isDone()) return;
        if (!bookOpened) {
            PatchouliAPI.get().openBookGUI(BestiaryItem.BOOK_ID);
            bookOpened = true;
            return;
        }
        if (scene == ENTRIES.length) {
            if (language.equals("en_us")) {
                language = "ru_ru";
                scene = 0;
                bookOpened = false;
                client.setScreen(null);
                client.getLanguageManager().setLanguage(language);
                client.options.language = language;
                reload = client.reloadResources();
                return;
            }
            if (!captures.stream().allMatch(CompletableFuture::isDone)) return;
            captures.forEach(CompletableFuture::join);
            stopped = true;
            client.scheduleStop();
            return;
        }
        if (opened == 0) {
            var id = ENTRIES[scene];
            PatchouliAPI.get().openBookEntry(BestiaryItem.BOOK_ID, Identifier.of("witchersbestiary", id),
                    id.startsWith("sign_") ? 4 : 0);
            opened = age;
        } else if (age - opened >= 12) {
            if (!(client.currentScreen instanceof GuiBook gui) || gui.book.getContents().isErrored()) {
                throw new IllegalStateException("Could not open Bestiary entry " + ENTRIES[scene]);
            }
            var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
            var output = Path.of("..", STAGE22X && language.equals("en_us") ? "bestiary.png" : language + "-" + ENTRIES[scene] + ".png");
            captures.add(CompletableFuture.runAsync(() -> {
                try (image) { image.writeTo(output); }
                catch (IOException error) { throw new IllegalStateException(error); }
            }));
            scene++;
            opened = 0;
        }
    }
}
