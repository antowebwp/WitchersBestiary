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
    private final String save = "Alchemy-RC3-" + System.currentTimeMillis();
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
                client.createIntegratedServerLoader().createAndStart(save,
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
                        for (String family : java.util.List.of("necrophage", "specter", "relict")) {
                            var item = net.minecraft.registry.Registries.ITEM.get(
                                    net.minecraft.util.Identifier.of("witchersbestiary:mutagen_" + family + "_1"));
                            if (!com.witchersbestiary.alchemy.MutagenItem.equip(p, new net.minecraft.item.ItemStack(item)))
                                throw new IllegalStateException("Could not equip " + family);
                        }
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
                            var stack = new net.minecraft.item.ItemStack(item);
                            if (i == 1) stack.addEnchantment(world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                                    .entryOf(net.minecraft.enchantment.Enchantments.PROTECTION), 2);
                            handler.getSlot(i).setStack(stack);
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
                phase = 6;
                ticks = 0;
                client.getServer().execute(() -> {
                    try {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var smithing = player.currentScreenHandler;
                        var armor = smithing.getCursorStack().copy();
                        if (!armor.hasEnchantments()) throw new IllegalStateException("Protection missing after upgrade");
                        smithing.setCursorStack(net.minecraft.item.ItemStack.EMPTY);
                        String[] inputs = {"cat_mastercrafted_template", "cat_enhanced_chestplate", "gravebound_steel_fragment"};
                        for (int i = 0; i < inputs.length; i++) {
                            var item = net.minecraft.registry.Registries.ITEM.get(
                                    net.minecraft.util.Identifier.of("witchersbestiary", inputs[i]));
                            smithing.getSlot(i).setStack(i == 1 ? armor : new net.minecraft.item.ItemStack(item));
                        }
                        smithing.sendContentUpdates();
                    } catch (Throwable error) { failure = error; }
                });
            }
            if (phase == 6 && ticks >= 15) {
                var handler = client.player.currentScreenHandler;
                capture(client, "mastercrafted-preview");
                client.interactionManager.clickSlot(handler.syncId, 3, 0,
                        net.minecraft.screen.slot.SlotActionType.PICKUP, client.player);
                phase = 7;
                ticks = 0;
            }
            if (phase == 7 && ticks >= 15) {
                var handler = client.player.currentScreenHandler;
                var armor = handler.getCursorStack();
                var id = net.minecraft.registry.Registries.ITEM.getId(armor.getItem());
                if (!id.getPath().equals("cat_mastercrafted_chestplate")
                        || !handler.getSlot(0).getStack().isEmpty() || !armor.hasEnchantments())
                    throw new IllegalStateException("Mastercrafted pickup, consumption or enchantment failed: " + id);
                capture(client, "mastercrafted-result");
                for (int slot = 0; slot < 3; slot++)
                    if (com.witchersbestiary.alchemy.MutagenItem.equipped(client.player, slot).isEmpty())
                        throw new IllegalStateException("Missing synced mutagen before rejoin");
                client.player.closeHandledScreen();
                client.world.disconnect();
                client.disconnect();
                client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
                phase = 8;
                ticks = 0;
            }
            if (phase == 8 && client.world == null && client.getServer() == null && ticks >= 20) {
                phase = 9;
                ticks = 0;
                client.createIntegratedServerLoader().start(save, () -> failure = new IllegalStateException("Rejoin cancelled"));
            }
            if (phase == 9 && client.player != null && client.currentScreen == null && ticks >= 40) {
                var expected = java.util.List.of("necrophage", "specter", "relict");
                for (int slot = 0; slot < 3; slot++) {
                    var equipped = com.witchersbestiary.alchemy.MutagenItem.equipped(client.player, slot);
                    var id = net.minecraft.registry.Registries.ITEM.getId(equipped.getItem());
                    if (!id.getPath().equals("mutagen_" + expected.get(slot) + "_1"))
                        throw new IllegalStateException("Mutagen changed after rejoin: " + id);
                }
                phase = 10;
                ticks = 0;
                client.getServer().execute(() -> {
                    try {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        float before = mutagenAttack(player, com.witchersbestiary.entity.ModEntities.GHOUL);
                        var removed = com.witchersbestiary.alchemy.MutagenItem.equipped(player, 2).getItem();
                        int stock = player.getInventory().count(removed);
                        if (!com.witchersbestiary.alchemy.MutagenItem.remove(player, 2)
                                || player.getInventory().count(removed) != stock + 1)
                            throw new IllegalStateException("Removal did not return the mutagen");
                        var returned = player.getInventory().main.stream().filter(stack -> stack.isOf(removed))
                                .findFirst().orElseThrow();
                        if (!com.witchersbestiary.alchemy.MutagenItem.equip(player, returned)
                                || Math.abs(mutagenAttack(player, com.witchersbestiary.entity.ModEntities.GHOUL) - before) > .001f)
                            throw new IllegalStateException("Replacement changed the equipped bonus");
                        while (com.witchersbestiary.alchemy.MutagenItem.remove(player, 0)) {}
                        var ghoul = com.witchersbestiary.entity.ModEntities.GHOUL;
                        var wraith = com.witchersbestiary.entity.ModEntities.WRAITH;
                        float base = mutagenAttack(player, ghoul);
                        float wraithBase = mutagenAttack(player, wraith);
                        float small = attackWith(player, ghoul, "necrophage_1");
                        float superior = attackWith(player, ghoul, "necrophage_3");
                        float offFamily = attackWith(player, wraith, "necrophage_3");
                        float hybridGhoul = attackWith(player, ghoul, "necrophage_specter_3");
                        float hybridWraith = attackWith(player, wraith, "necrophage_specter_3");
                        verifyDamage(base, small, 1.04f);
                        verifyDamage(base, superior, 1.13f);
                        verifyDamage(wraithBase, offFamily, 1.03f);
                        verifyDamage(base, hybridGhoul, 1.08f);
                        verifyDamage(wraithBase, hybridWraith, 1.08f);
                        java.nio.file.Files.writeString(java.nio.file.Path.of("../rc3-result.txt"),
                                "Both Cat templates consumed; enchanted mastercrafted chestplate produced; three mutagens persisted on rejoin.\n"
                                + "Damage: ghoul base=" + base + ", small=" + small + ", superior=" + superior
                                + ", hybrid=" + hybridGhoul + "; wraith base=" + wraithBase + ", off-family="
                                + offFamily + ", hybrid=" + hybridWraith);
                        client.execute(client::scheduleStop);
                    } catch (Throwable error) { failure = error; }
                });
            }
        } catch (Throwable error) {
            error.printStackTrace();
            client.scheduleStop();
            phase = 99;
        }
    }

    private static float attackWith(net.minecraft.server.network.ServerPlayerEntity player,
                                    net.minecraft.entity.EntityType<? extends net.minecraft.entity.LivingEntity> type,
                                    String mutagen) {
        var item = net.minecraft.registry.Registries.ITEM.get(
                net.minecraft.util.Identifier.of("witchersbestiary:mutagen_" + mutagen));
        if (!com.witchersbestiary.alchemy.MutagenItem.equip(player, new net.minecraft.item.ItemStack(item)))
            throw new IllegalStateException("Cannot equip validation mutagen " + mutagen);
        try {
            return mutagenAttack(player, type);
        } finally {
            if (!com.witchersbestiary.alchemy.MutagenItem.remove(player, 0))
                throw new IllegalStateException("Cannot remove validation mutagen " + mutagen);
        }
    }

    private static float mutagenAttack(net.minecraft.server.network.ServerPlayerEntity player,
                                        net.minecraft.entity.EntityType<? extends net.minecraft.entity.LivingEntity> type) {
        var world = player.getServerWorld();
        var target = type.create(world);
        target.setPosition(player.getPos().add(0, 0, 2));
        target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
        world.spawnEntity(target);
        try {
            float before = target.getHealth();
            if (!target.damage(world.getDamageSources().playerAttack(player), 4))
                throw new IllegalStateException("Damage rejected for " + type);
            return before - target.getHealth();
        } finally {
            target.discard();
        }
    }

    private static void verifyDamage(float base, float actual, float ratio) {
        if (Math.abs(base * ratio - actual) > .015f)
            throw new IllegalStateException("Damage expected=" + base * ratio + ", actual=" + actual);
    }

    private static void capture(MinecraftClient client, String name) throws java.io.IOException {
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(java.nio.file.Path.of("..", name + ".png"));
        }
    }
}
