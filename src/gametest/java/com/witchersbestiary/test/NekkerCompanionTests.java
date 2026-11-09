package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.NekkerEntity;
import com.witchersbestiary.nekker.NekkerHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public final class NekkerCompanionTests implements FabricGameTest {
    private static final String ARENA = "witchersbestiary:village_board_arena";

    private void place(net.minecraft.entity.Entity entity, BlockPos pos) {
        entity.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
    }

    private ServerPlayerEntity player(TestContext c, BlockPos pos) {
        var player = c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        place(player, pos);
        return player;
    }

    private NekkerEntity tame(TestContext c, ServerPlayerEntity player, BlockPos pos) {
        var nekker = ModEntities.NEKKER.create(c.getWorld());
        place(nekker, pos);
        nekker.setTarget(player);
        var food = new ItemStack(Items.BEEF, 64);
        player.setStackInHand(Hand.MAIN_HAND, food);
        nekker.getRandom().setSeed(22);
        for (int i = 0; i < 24 && !nekker.isTamed(); i++) nekker.interactMob(player, Hand.MAIN_HAND);
        c.assertTrue(nekker.isTamed() && nekker.getTarget() == null && (player.isCreative() || food.getCount() < 64), "Survival feeding tames a wild attacker");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        return nekker;
    }

    @GameTest(templateName = ARENA)
    public void nineSlotsOwnerRightsAndNbt(TestContext c) {
        var at = c.getAbsolutePos(new BlockPos(5, 3, 5));
        var a = player(c, at);
        var b = player(c, at.east(2));
        var first = tame(c, a, at.east());
        var second = tame(c, b, at.east(3));
        var menu = new NekkerHandler(1, a.getInventory(), first);
        var other = new NekkerHandler(2, b.getInventory(), second);
        c.assertTrue(!com.witchersbestiary.signs.SignManager.enemy(a, first)
            && !com.witchersbestiary.signs.SignManager.enemy(a, second), "Signs ignore both companions");
        a.currentScreenHandler = menu;
        b.currentScreenHandler = other;
        c.assertTrue(menu.slots.size() == 45 && first.storage().size() == 9
            && menu.canUse(a) && !menu.canUse(b) && other.canUse(b) && !other.canUse(a), "Nine companion slots and owner-only access");
        a.getInventory().setStack(9, new ItemStack(Items.EMERALD, 17));
        b.getInventory().setStack(9, new ItemStack(Items.DIAMOND, 5));
        c.assertTrue(menu.quickMove(a, 9).getCount() == 17 && other.quickMove(b, 9).getCount() == 5,
            "Shift-click moves each owner's stack independently");
        c.assertTrue(menu.quickMove(b, 0).isEmpty() && first.storage().getStack(0).isOf(Items.EMERALD)
            && second.storage().getStack(0).isOf(Items.DIAMOND), "Foreign shift-click cannot mutate either inventory");
        menu.onSlotClick(0, 1, SlotActionType.PICKUP, a);
        c.assertTrue(menu.getCursorStack().getCount() == 9 && first.storage().getStack(0).getCount() == 8,
            "Right-click splits the stack once");
        menu.onSlotClick(8, 0, SlotActionType.PICKUP, a);
        c.assertTrue(menu.getCursorStack().isEmpty() && first.storage().getStack(8).getCount() == 9, "Left-click deposits cursor stack");
        c.assertTrue(menu.onButtonClick(a, 0) && !menu.onButtonClick(b, 0) && first.collecting(), "Only owner can toggle pickup");
        first.interactMob(a, Hand.MAIN_HAND);
        c.assertTrue(first.waiting(), "Owner switches to wait");
        var saved = new NbtCompound();
        first.writeCustomDataToNbt(saved);
        var loaded = ModEntities.NEKKER.create(c.getWorld());
        place(loaded, at.east());
        loaded.readCustomDataFromNbt(saved);
        c.assertTrue(loaded.canAccess(a) && !loaded.canAccess(b) && loaded.waiting() && loaded.collecting()
            && loaded.storage().getStack(8).getCount() == 9 && loaded.storage().getStack(0).getCount() == 8,
            "Saved slot positions, owner and settings restore");
        a.currentScreenHandler = a.playerScreenHandler;
        b.currentScreenHandler = b.playerScreenHandler;
        first.discard(); second.discard(); loaded.discard(); a.discard(); b.discard();
        c.complete();
    }

    @GameTest(templateName = ARENA, tickLimit = 100)
    public void pickupFollowDefenseAndDeath(TestContext c) {
        var world = c.getWorld();
        var at = c.getAbsolutePos(new BlockPos(12, 10, 12));
        for (int x = -6; x <= 19; x++) for (int z = -6; z <= 6; z++) {
            var pos = at.add(x, 0, z);
            world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
        }
        var owner = player(c, at);
        var other = player(c, at.west(2));
        var nekker = tame(c, owner, at.east());
        world.spawnEntity(nekker);
        nekker.setCollecting(true);
        var loose = new ItemEntity(world, at.getX() + 2.5, at.getY() + .1, at.getZ() + .5, new ItemStack(Items.EMERALD, 11));
        var protectedDrop = new ItemEntity(world, at.getX() + 3.5, at.getY() + .1, at.getZ() + .5, new ItemStack(Items.DIAMOND, 7));
        protectedDrop.setOwner(other.getUuid());
        world.spawnEntity(loose);
        world.spawnEntity(protectedDrop);
        c.runAtTick(15, () -> {
            c.assertTrue(loose.isRemoved() && !protectedDrop.isRemoved() && nekker.storage().count(Items.EMERALD) == 11
                && nekker.storage().count(Items.DIAMOND) == 0, "Collect loose items exactly once and respect foreign pickup ownership");
            nekker.interactMob(owner, Hand.MAIN_HAND);
            place(owner, at.east(14));
        });
        c.runAtTick(27, () -> {
            c.assertTrue(nekker.waiting() && nekker.squaredDistanceTo(owner) > 100, "Waiting pet remains behind");
            place(owner, nekker.getBlockPos().west());
            nekker.interactMob(owner, Hand.MAIN_HAND);
            place(owner, at.east(14));
        });
        c.runAtTick(50, () -> {
            c.assertTrue(!nekker.waiting() && nekker.squaredDistanceTo(owner) <= 25, "Following pet catches up on safe ground");
            var zombie = EntityType.ZOMBIE.create(world);
            place(zombie, owner.getBlockPos().west(2));
            owner.setAttacker(zombie);
            nekker.tick();
            c.assertTrue(nekker.getTarget() == zombie, "Pet defends owner against a nearby hostile attacker");
            var villager = EntityType.VILLAGER.create(world);
            place(villager, owner.getBlockPos().west());
            nekker.setTarget(null);
            owner.setAttacker(villager);
            nekker.tick();
            c.assertTrue(nekker.getTarget() == null, "Pet does not attack peaceful NPCs");
            nekker.damage(world.getDamageSources().genericKill(), 1000);
            int dropped = world.getEntitiesByClass(ItemEntity.class, nekker.getBoundingBox().expand(4),
                item -> item.getStack().isOf(Items.EMERALD)).stream().mapToInt(item -> item.getStack().getCount()).sum();
            c.assertTrue(nekker.storage().isEmpty() && dropped == 11, "Death drains storage and drops each item once");
            protectedDrop.discard(); nekker.discard(); zombie.discard(); villager.discard(); owner.discard(); other.discard();
            c.complete();
        });
    }
}
