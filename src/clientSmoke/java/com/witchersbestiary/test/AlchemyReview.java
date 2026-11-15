package com.witchersbestiary.test;

import com.witchersbestiary.workshop.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;

final class AlchemyReview implements ClientModInitializer {
    private int ticks, phase, recipeIndex;
    private volatile boolean ready;
    private volatile Throwable failure;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        try {
            if (failure != null) throw new IllegalStateException(failure);
            if (++ticks > 1600) throw new IllegalStateException("Alchemy review timeout at phase " + phase);
            if (phase == 0 && client.getOverlay() == null) {
                phase++;
                client.options.pauseOnLostFocus = false;
                client.options.getGuiScale().setValue(2);
                client.options.getViewDistance().setValue(4);
                client.createIntegratedServerLoader().createAndStart("Alchemy-" + System.currentTimeMillis(),
                        new LevelInfo("Alchemy review", GameMode.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), DataConfiguration.SAFE_MODE),
                        new GeneratorOptions(22L, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
            }
            if (phase == 1 && client.player != null && client.currentScreen == null) {
                phase++;
                client.getServer().execute(() -> {
                    try {
                        var world = client.getServer().getOverworld();
                        var p = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        BlockPos at = p.getBlockPos().south(2);
                        world.setBlockState(at, ModWorkshop.BLOCKS.get(WorkshopKind.ALCHEMY_WORKBENCH).getDefaultState());
                        var table = (WorkshopInventory)world.getBlockEntity(at);
                        var catalog = AlchemyRecipes.catalog(world);
                        var recipe = catalog.stream().filter(r -> r.id().getPath().equals("mutagen_necrophage_1"))
                                .findFirst().orElseThrow();
                        recipeIndex = catalog.indexOf(recipe);
                        for (var ingredient : recipe.value().getIngredients())
                            p.getInventory().insertStack(ingredient.getMatchingStacks()[0].copy());
                        p.openHandledScreen(table);
                        ready = true;
                    } catch (Throwable error) { failure = error; }
                });
                ticks = 0;
            }
            if (phase == 2 && ready && ticks >= 25 && client.currentScreen instanceof
                    com.witchersbestiary.client.workshop.AlchemyScreen screen) {
                capture(client, "alchemy");
                var category = screen.getClass().getDeclaredField("category");
                category.setAccessible(true);
                category.setInt(screen, 3);
                var query = screen.getClass().getDeclaredField("query");
                query.setAccessible(true);
                query.set(screen, "Small Mutagen · Necrophage");
                screen.init(client, screen.width, screen.height);
                client.interactionManager.clickButton(client.player.currentScreenHandler.syncId, 100 + recipeIndex);
                phase++;
                ticks = 0;
            }
            if (phase == 3 && ticks >= 15) {
                var output = client.player.currentScreenHandler.getSlot(3).getStack();
                if (!(output.getItem() instanceof com.witchersbestiary.alchemy.MutagenItem))
                    throw new IllegalStateException("Mutagen crafting packet produced no result");
                capture(client, "mutagen");
                client.player.closeHandledScreen();
                ready = false;
                phase = 4;
                ticks = 0;
                client.getServer().execute(() -> {
                    try {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var world = player.getServerWorld();
                        var at = player.getBlockPos().south(2);
                        world.setBlockState(at, net.minecraft.block.Blocks.SMITHING_TABLE.getDefaultState());
                        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory((id, inv, owner) ->
                                new net.minecraft.screen.SmithingScreenHandler(id, inv,
                                        net.minecraft.screen.ScreenHandlerContext.create(world, at)),
                                net.minecraft.text.Text.translatable("container.upgrade")));
                        var handler = player.currentScreenHandler;
                        String[] inputs = {"cat_enhanced_template", "cat_chestplate", "venom_gland"};
                        for (int i = 0; i < inputs.length; i++) {
                            var item = net.minecraft.registry.Registries.ITEM.get(
                                    net.minecraft.util.Identifier.of("witchersbestiary", inputs[i]));
                            handler.getSlot(i).setStack(new net.minecraft.item.ItemStack(item));
                        }
                        handler.sendContentUpdates();
                        ready = true;
                    } catch (Throwable error) { failure = error; }
                });
            }
            if (phase == 4 && ready && ticks >= 20 &&
                    client.player.currentScreenHandler instanceof net.minecraft.screen.SmithingScreenHandler smithing) {
                if (smithing.getSlot(3).getStack().isEmpty()) throw new IllegalStateException("Smithing result missing");
                capture(client, "smithing-template");
                client.interactionManager.clickSlot(smithing.syncId, 3, 0,
                        net.minecraft.screen.slot.SlotActionType.PICKUP, client.player);
                phase = 5;
                ticks = 0;
            }
            if (phase == 5 && ticks >= 15) {
                var handler = client.player.currentScreenHandler;
                var result = net.minecraft.registry.Registries.ITEM.getId(handler.getCursorStack().getItem());
                if (!handler.getSlot(0).getStack().isEmpty() || !result.getPath().equals("cat_enhanced_chestplate"))
                    throw new IllegalStateException("Smithing pickup or template consumption failed: " + result);
                capture(client, "smithing-result");
                client.scheduleStop();
                phase++;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            client.scheduleStop();
            phase = 99;
        }
    }

    private static void capture(MinecraftClient client, String name) throws java.io.IOException {
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(java.nio.file.Path.of("..", name + ".png"));
        }
    }
}
