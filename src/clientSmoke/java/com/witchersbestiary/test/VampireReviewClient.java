package com.witchersbestiary.test;

import com.witchersbestiary.board.*;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.VampireHunt;
import com.witchersbestiary.client.tracking.WitcherSensesClient;
import com.witchersbestiary.entity.monster.HigherVampireEntity;
import com.witchersbestiary.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.*;

final class VampireReviewClient implements ClientModInitializer {
    private static final boolean STAGE22X = Boolean.getBoolean("witchersbestiary.stage22xReview");
    private boolean windowHidden;
    private final Path output = Path.of("..");
    private volatile int phase, ticks;
    private volatile Throwable failure;
    private BlockPos board;
    private int bossFrames;
    private volatile ContractInstance hunt;

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void next(int value) {
        ticks = 0;
        phase = value;
    }

    private void server(MinecraftClient client, Runnable action) {
        client.getServer().execute(() -> {
            try { action.run(); } catch (Throwable error) { failure = error; }
        });
    }

    private void capture(MinecraftClient client, String name) throws Exception {
        try (var frame = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            frame.writeTo(output.resolve(name + ".png"));
        }
    }

    private void aim(MinecraftClient client, Vec3d point) {
        var delta = point.subtract(client.player.getEyePos());
        client.player.setYaw((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength())));
    }

    private void tick(MinecraftClient client) {
        try {
            if (Boolean.getBoolean("witchersbestiary.stage22xReview") && !windowHidden) {
            org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().getHandle());
            windowHidden = true;
        }
        if (failure != null) throw new IllegalStateException(failure);
            if (++ticks > 900 && phase != 99) throw new IllegalStateException("Timeout at phase " + phase);
            if (phase == 0) {
                if (client.getOverlay() != null) return;
                next(1);
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(5);
                client.options.getSimulationDistance().setValue(5);
                client.createIntegratedServerLoader().createAndStart("Stage22G-" + System.currentTimeMillis(),
                        new LevelInfo("Stage 22G Review", GameMode.SURVIVAL, false, Difficulty.NORMAL,
                                false, new GameRules(), DataConfiguration.SAFE_MODE),
                        new GeneratorOptions(220719L, true, false),
                        registries -> registries.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.DEFAULT)
                                .createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.world == null) return;
            var handler = client.player.currentScreenHandler;
            switch (phase) {
                case 1 -> {
                    next(98);
                    server(client, () -> {
                        var world = client.getServer().getOverworld();
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var tomb = world.locateStructure(TagKey.of(RegistryKeys.STRUCTURE,
                                com.witchersbestiary.world.graveyard.VampireTomb.ID), world.getSpawnPos(), 48, false);
                        if (tomb == null) throw new IllegalStateException("No generated tomb");
                        var chamber = world.getChunk(tomb).getStructureStarts().values().stream()
                                .flatMap(start -> start.getChildren().stream())
                                .filter(com.witchersbestiary.world.graveyard.VampireTomb.Chamber.class::isInstance)
                                .map(com.witchersbestiary.world.graveyard.VampireTomb.Chamber.class::cast)
                                .findFirst().orElseThrow();
                        var approach = chamber.entry().north(90);
                        world.getChunk(approach);
                        board = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, approach);
                        // Build a temporary test pad clear of uneven ground and liquid.
                        board = board.up(8);
                        // Choose a nearby test board whose normal rotation includes the vampire.
                        long age = world.getTime();
                        for (int candidate = 0; candidate < 12 && Math.floorMod(age / VillageOffers.INTERVAL
                                + (VillageOffers.identity(world, board) + "/legends").hashCode(), 3) != 0; candidate++) {
                            board = board.east(1);
                        }
                        for (int x = -2; x <= 2; x++)
                            for (int z = -3; z <= 1; z++)
                                world.setBlockState(board.add(x, -1, z), net.minecraft.block.Blocks.STONE.getDefaultState());
                        if (!NoticeBoardBlock.place(world, board, Direction.NORTH))
                            throw new IllegalStateException("Rotated board placement failed");
                        player.teleport(world, board.getX()+.5, board.getY(), board.getZ()-1.5, 0, 0);
                        next(2);
                    });
                }
                case 2 -> {
                    if (handler instanceof BoardScreenHandler) { next(3); return; }
                    if (ticks < 30 || ticks % 10 != 0 || !client.world.getBlockState(board).isOf(NoticeBoards.BLOCK)) return;
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                            new BlockHitResult(Vec3d.ofBottomCenter(board).add(0, .5, -.5), Direction.NORTH, board, false));
                }
                case 3 -> {
                    if (!(handler instanceof BoardScreenHandler)) return;
                    client.interactionManager.clickButton(handler.syncId, 65);
                    next(4);
                }
                case 4 -> {
                    if (ticks < 15 || !(handler instanceof BoardScreenHandler view)) return;
                    var offers = view.snapshot.getAsJsonArray("offers");
                    for (int i = 0; i < offers.size(); i++) {
                        var offer = offers.get(i).getAsJsonObject();
                        if (!VampireHunt.ID.toString().equals(offer.get("id").getAsString())) continue;
                        for (String name : new String[]{"selected", "page"}) {
                            var field = client.currentScreen.getClass().getDeclaredField(name);
                            field.setAccessible(true);
                            field.setInt(client.currentScreen, name.equals("selected") ? i : i / 3);
                        }
                        ((com.witchersbestiary.client.board.NoticeBoardScreen)client.currentScreen).refresh();
                        next(5);
                        return;
                    }
                    throw new IllegalStateException("Legendary offer missing: " + offers);
                }
                case 5 -> {
                    if (ticks < 8) return;
                    capture(client, "board");
                    var view = (BoardScreenHandler) handler;
                    for (var row : view.snapshot.getAsJsonArray("offers")) {
                        var offer = row.getAsJsonObject();
                        if (VampireHunt.ID.toString().equals(offer.get("id").getAsString()))
                            client.interactionManager.clickButton(handler.syncId, offer.get("button").getAsInt());
                    }
                    next(6);
                }
                case 6 -> {
                    var view = (BoardScreenHandler)handler;
                    var message = view.snapshot.get("message");
                    if (message != null && (message.getAsString().equals("rejected") ||
                            message.getAsString().equals("no_safe_route")))
                        throw new IllegalStateException("Board rejected legendary acceptance: " + message);
                    for (var row : view.snapshot.getAsJsonArray("offers")) {
                        var offer = row.getAsJsonObject();
                        if (VampireHunt.ID.toString().equals(offer.get("id").getAsString())
                                && "ACCEPTED".equals(offer.get("state").getAsString())) {
                            client.player.closeHandledScreen();
                            next(14);
                            break;
                        }
                    }
                }
                case 14 -> {
                    if (ticks < 20) return;
                    if (client.player.getInventory().count(ModItems.CRIMSON_PHYLACTERY) != 1)
                        throw new IllegalStateException("Accepted hunt did not sync exactly one phylactery");
                    capture(client, "empty-phylactery");
                    KeyBinding.onKeyPressed(InputUtil.fromKeyCode(86, 0));
                    approach(client);
                }
                case 7 -> {
                    var snapshot = WitcherSensesClient.snapshot();
                    if (ticks < 12 || !snapshot.has("point")) return;
                    var point = snapshot.getAsJsonObject("point");
                    if (!point.has("id")) return;
                    aim(client, new Vec3d(point.get("x").getAsDouble()+.5, point.get("y").getAsDouble()+.06,
                            point.get("z").getAsDouble()+.5));
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(8);
                }
                case 8 -> {
                    if (ticks == 20) approach(client);
                }
                case 9 -> {
                    if (ticks < 20) return;
                    capture(client, "entrance");
                    next(98);
                    server(client, () -> {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var at = hunt.vampireArena.north(5);
                        player.teleport(player.getServerWorld(), at.getX()+.5, at.getY(), at.getZ()+.5, 0, 0);
                        next(10);
                    });
                }
                case 10 -> {
                    if (hunt.vampire == null) return;
                    for (var entity : client.world.getEntities()) {
                        if (!entity.getUuid().equals(hunt.vampire)) continue;
                        aim(client, entity.getPos().add(0, 1, 0));
                        if (++bossFrames < 15) return;
                        capture(client, "arena");
                        next(98);
                        server(client, () -> {
                            var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                            var world = player.getServerWorld();
                            var boss = (HigherVampireEntity)world.getEntity(hunt.vampire);
                            // Accelerate combat while leaving phase transitions and the sealing channel intact.
                            boss.setHealth(1);
                            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, 900, 4));
                            next(11);
                        });
                        return;
                    }
                }
                case 11 -> {
                    if (ticks % 12 != 0) return;
                    server(client, () -> {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var world = player.getServerWorld();
                        var boss = (HigherVampireEntity)world.getEntity(hunt.vampire);
                        if (!boss.canBeSealed()) {
                            if (!boss.isFinalPhase()) return;
                            boss.damage(world.getDamageSources().playerAttack(player), 1000);
                            return;
                        }
                        player.teleport(world, boss.getX(), boss.getY(), boss.getZ()-2.5, 0, 12);
                        next(12);
                    });
                }
                case 12 -> {
                    if (ticks < 10) return;
                    for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isOf(ModItems.CRIMSON_PHYLACTERY))
                        client.player.getInventory().selectedSlot = i;
                    for (var entity : client.world.getEntities()) if (entity.getUuid().equals(hunt.vampire))
                        aim(client, entity.getPos().add(0, .5, 0));
                    client.options.useKey.setPressed(true);
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(13);
                }
                case 13 -> {
                    if (client.player.getInventory().count(ModItems.BLOOD_LEGACY) != 1) return;
                    int crowns = client.player.getInventory().count(com.witchersbestiary.economy.Currency.CROWN);
                    if (crowns != 600 || hunt.status() != ContractInstance.Status.REWARDED)
                        throw new IllegalStateException("Unexpected reward: " + crowns + " crowns, " + hunt.status());
                    client.options.useKey.setPressed(false);
                    var relic = client.player.getMainHandStack();
                    if (!VampireHunt.isSealed(relic)) return;
                    capture(client, "reward");
                    capture(client, "sealed-phylactery");
                    Files.writeString(output.resolve("result.txt"), "Sealed hunt " + hunt.id() + " status=" + hunt.status() + " crowns=" + crowns);
                    if (STAGE22X) {
                        next(98);
                        server(client, () -> {
                            var server = client.getServer();
                            var world = server.getOverworld();
                            var player = server.getPlayerManager().getPlayerList().getFirst();
                            var key = VillageOffers.identity(world, board) + "/legends";
                            long age = world.getTime() + 3 * VillageOffers.INTERVAL;
                            while (Math.floorMod(age / VillageOffers.INTERVAL + key.hashCode(), 3) != 0)
                                age += VillageOffers.INTERVAL;
                            VillageOffers.get(server).current(key, age, java.util.List.of(
                                    "witchersbestiary:swamp_lord", "witchersbestiary:grave_devourer", VampireHunt.ID.toString()));
                            hunt.vampireReplayReady = 0;
                            player.teleport(world, board.getX()+.5, board.getY(), board.getZ()-1.5, 0, 0);
                            next(20);
                        });
                        return;
                    }
                    var group = net.minecraft.registry.Registries.ITEM_GROUP.get(
                            com.witchersbestiary.registry.ModItemGroups.WITCHERS_BESTIARY_KEY);
                    net.minecraft.item.ItemGroups.updateDisplayContext(client.player.networkHandler.getEnabledFeatures(),
                            false, client.world.getRegistryManager());
                    if (!group.contains(new net.minecraft.item.ItemStack(ModItems.BLOOD_LEGACY))
                            || !group.contains(new net.minecraft.item.ItemStack(ModItems.CRIMSON_PHYLACTERY)))
                        throw new IllegalStateException("Items missing from creative tab");
                    client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
                    next(15);
                }
                case 20 -> {
                    if (handler instanceof BoardScreenHandler) {
                        client.interactionManager.clickButton(handler.syncId, 65);
                        next(21);
                    } else if (ticks > 25 && ticks % 8 == 0) {
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                new BlockHitResult(Vec3d.ofBottomCenter(board).add(0, .5, -.5), Direction.NORTH, board, false));
                    }
                }
                case 21 -> {
                    if (ticks < 12 || !(handler instanceof BoardScreenHandler view)) return;
                    var rows = view.snapshot.getAsJsonArray("offers");
                    for (var row : rows) {
                        var offer = row.getAsJsonObject();
                        if (VampireHunt.ID.toString().equals(offer.get("id").getAsString())) {
                            client.interactionManager.clickButton(handler.syncId, offer.get("button").getAsInt());
                            next(22);
                            return;
                        }
                    }
                }
                case 22 -> {
                    if (!(handler instanceof BoardScreenHandler view)) return;
                    for (var row : view.snapshot.getAsJsonArray("offers")) {
                        var offer = row.getAsJsonObject();
                        if (!VampireHunt.ID.toString().equals(offer.get("id").getAsString())
                                || !"ACCEPTED".equals(offer.get("state").getAsString())) continue;
                        int selected = offer.get("button").getAsInt();
                        for (String name : new String[]{"selected", "page"}) {
                            var field = client.currentScreen.getClass().getDeclaredField(name);
                            field.setAccessible(true);
                            field.setInt(client.currentScreen, name.equals("selected") ? selected : selected / 3);
                        }
                        ((com.witchersbestiary.client.board.NoticeBoardScreen)client.currentScreen).refresh();
                        next(23);
                        return;
                    }
                }
                case 23 -> {
                    if (ticks < 12) return;
                    capture(client, "legendary-repeat");
                    client.player.closeHandledScreen();
                    next(24);
                }
                case 24 -> {
                    if (ticks < 10) return;
                    next(98);
                    server(client, () -> {
                        var server = client.getServer();
                        var player = server.getPlayerManager().getPlayerList().getFirst();
                        var world = player.getServerWorld();
                        var at = board.east(20);
                        for (int x=-4; x<=4; x++) for (int z=-4; z<=8; z++) {
                            world.setBlockState(at.add(x,-1,z), net.minecraft.block.Blocks.STONE.getDefaultState());
                            for (int y=0; y<5; y++)
                                world.setBlockState(at.add(x,y,z), net.minecraft.block.Blocks.AIR.getDefaultState());
                        }
                        player.teleport(world, at.getX()+.5, at.getY(), at.getZ()+.5, 0, 0);
                        next(25);
                    });
                }
                case 25 -> {
                    if (ticks < 20) return;
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(26);
                }
                case 26 -> {
                    if (hunt.vampire == null) return;
                    next(98);
                    server(client, () -> {
                        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                        var world = player.getServerWorld();
                        var boss = (HigherVampireEntity)world.getEntity(hunt.vampire);
                        var state = new net.minecraft.nbt.NbtCompound();
                        boss.writeCustomDataToNbt(state);
                        state.putBoolean("HybridForm", true);
                        state.putBoolean("FinalPhase", true);
                        state.putBoolean("Defeated", true);
                        state.putBoolean("Sealable", true);
                        state.putLong("ReviveAt", world.getTime()+1800);
                        boss.readCustomDataFromNbt(state);
                        player.teleport(world, boss.getX(), boss.getY(), boss.getZ()-2.5, 0, 12);
                        next(27);
                    });
                }
                case 27 -> {
                    if (ticks < 12) return;
                    for (var entity : client.world.getEntities()) if (entity.getUuid().equals(hunt.vampire))
                        aim(client, entity.getPos().add(0, .5, 0));
                    client.options.useKey.setPressed(true);
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    next(28);
                }
                case 28 -> {
                    if (client.player.getInventory().count(com.witchersbestiary.economy.Currency.CROWN) != 780) return;
                    client.options.useKey.setPressed(false);
                    if (!VampireHunt.isSealed(client.player.getMainHandStack())
                            || client.player.getInventory().count(ModItems.BLOOD_LEGACY) != 1
                            || client.player.getInventory().count(ModItems.CRIMSON_PHYLACTERY) != 1)
                        throw new IllegalStateException("Second sealing changed unique items");
                    capture(client, "legendary-resealed");
                    Files.writeString(output.resolve("legendary-result.txt"),
                            "PASS repeat board offer, summon, five-second sealing, 180 additional crowns and no duplicate unique items. Defeated pose was a fixture.");
                    next(99);
                    client.scheduleStop();
                }
                case 15 -> {
                    if (ticks < 15) return;
                    capture(client, "blood-legacy-inventory");
                    client.setScreen(null);
                    for (int i = 0; i < 9; i++) {
                        if (client.player.getInventory().getStack(i).isOf(ModItems.BLOOD_LEGACY))
                            client.player.getInventory().selectedSlot = i;
                    }
                    client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
                    next(16);
                }
                case 16 -> {
                    if (ticks < 15) return;
                    capture(client, "blood-legacy-third-person");
                    next(98);
                    server(client, () -> {
                        var server = client.getServer();
                        var player = server.getPlayerManager().getPlayerList().getFirst();
                        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getStack(slot).isOf(com.witchersbestiary.economy.Currency.CROWN)) player.getInventory().setStack(slot, net.minecraft.item.ItemStack.EMPTY);
                        for (var item : java.util.List.of(ModItems.BLOOD_LEGACY, ModItems.CRIMSON_PHYLACTERY)) {
                            int before = player.getInventory().count(item);
                            var id = net.minecraft.registry.Registries.ITEM.getId(item);
                            server.getCommandManager().executeWithPrefix(
                                    player.getCommandSource().withLevel(2), "give @s " + id);
                            if (player.getInventory().count(item) != before + 1)
                                throw new IllegalStateException("Give failed: " + id);
                        }
                        next(17);
                    });
                }
                case 17 -> {
                    Files.writeString(output.resolve("item-audit.txt"),
                            "PASS creative entries, both give IDs, first reward and sealed state");
                    next(99);
                    client.scheduleStop();
                }
                default -> { }
            }
        } catch (Throwable error) {
            next(99);
            try { Files.writeString(output.resolve("failure.txt"), error.toString()); }
            catch (Exception ignored) { }
            error.printStackTrace();
            client.scheduleStop();
        }
    }

    private void approach(MinecraftClient client) {
        next(98);
        server(client, () -> {
            var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
            hunt = BoardScreenHandler.existing(player, VampireHunt.ID);
            var clue = hunt.investigation.next();
            var at = clue == null ? hunt.investigation.lair : clue.position;
            player.teleport(player.getServerWorld(), at.getX()+.5, at.getY()+.05, at.getZ()-1, 0, 12);
            next(clue == null ? 9 : 7);
        });
    }
}
