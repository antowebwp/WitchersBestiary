package com.witchersbestiary.test;

import com.witchersbestiary.entity.monster.WraithEntity;
import com.witchersbestiary.equipment.armory.BloodAuraState;
import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.equipment.armory.LegendaryAbilities.HammerInput;
import com.witchersbestiary.registry.ModDataComponents;
import com.witchersbestiary.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class UnifiedWeaponKeyTests implements FabricGameTest {
    @GameTest(templateName = "witchersbestiary:village_board_arena", tickLimit = 70)
    public void weaponKeyDispatchAndHammerRelease(TestContext context) {
        var world = context.getWorld();
        var player = LegendaryAbilityTests.player(context, "WeaponKeyTest");
        player.setOnGround(true);
        var hammer = ModItems.BONECRUSHER_CLUB.getDefaultStack();
        player.setStackInHand(Hand.MAIN_HAND, hammer);
        context.assertTrue(hammer.getItem().use(world, player, Hand.MAIN_HAND).getResult() == ActionResult.PASS,
                "Use is available for ordinary Minecraft interaction");
        context.assertTrue(!LegendaryAbilities.activate(player), "Tap-only sword packet cannot swing hammer");

        player.setStackInHand(Hand.OFF_HAND, Items.STICK.getDefaultStack());
        context.assertTrue(!LegendaryAbilities.hammerInput(player, HammerInput.START), "Offhand must be empty");
        player.setStackInHand(Hand.OFF_HAND, net.minecraft.item.ItemStack.EMPTY);
        context.assertTrue(LegendaryAbilities.hammerInput(player, HammerInput.START), "Valid hold starts");
        context.assertTrue(!LegendaryAbilities.hammerInput(player, HammerInput.RELEASE) && hammer.getDamage() == 0,
                "An immediate release cannot attack");
        context.assertTrue(LegendaryAbilities.hammerInput(player, HammerInput.START), "Short hold does not consume cooldown");

        long start = world.getTime();
        context.runAtEveryTick(() -> {
            player.setOnGround(true);
            long elapsed = world.getTime() - start;
            if (elapsed == 2) player.setStackInHand(Hand.MAIN_HAND, ModItems.BONECRUSHER_CLUB.getDefaultStack());
            if (elapsed == 3) player.setStackInHand(Hand.MAIN_HAND, hammer);
            if (elapsed == 12) {
                context.assertTrue(!LegendaryAbilities.hammerInput(player, HammerInput.RELEASE),
                        "Even switching to an identical hammer and back cancels the windup");
                context.assertTrue(LegendaryAbilities.hammerInput(player, HammerInput.START), "A new hold starts");
            }
            if (elapsed == 24) {
                context.assertTrue(hammer.getDamage() == 0, "Full windup waits for release");
                context.assertTrue(LegendaryAbilities.hammerInput(player, HammerInput.RELEASE), "Release executes slam");
                context.assertTrue(hammer.getDamage() == 3 && hammer.get(ModDataComponents.HAMMER_READY_AT) == world.getTime() + 240,
                        "Slam spends three durability and preserves the twelve-second cooldown");
                context.assertTrue(!LegendaryAbilities.hammerInput(player, HammerInput.RELEASE)
                        && !LegendaryAbilities.hammerInput(player, HammerInput.START), "Release cannot replay or bypass cooldown");

                player.setStackInHand(Hand.MAIN_HAND, ModItems.BLOOD_LEGACY.getDefaultStack());
                var target = EntityType.ZOMBIE.create(world);
                for (int i = 0; i < 8; i++) LegendaryAbilities.bloodHit(player, target, 1);
                context.assertTrue(LegendaryAbilities.activate(player) && ((BloodAuraState) player).bestiary$bloodAura()
                        && !LegendaryAbilities.activate(player), "Blood Legacy activates once through the shared key");
                target.discard();

                player.setStackInHand(Hand.MAIN_HAND, ModItems.GRAVE_BLADE.getDefaultStack());
                context.assertTrue(LegendaryAbilities.activate(player) && !LegendaryAbilities.activate(player),
                        "Grave Blade activates once through the shared key");
                var ghosts = world.getEntitiesByClass(WraithEntity.class, player.getBoundingBox().expand(5),
                        ghost -> ghost.getOwner() == player);
                context.assertTrue(ghosts.size() == 1, "Exactly one allied wraith");
                ghosts.getFirst().discard();
                player.setStackInHand(Hand.MAIN_HAND, hammer);
                context.assertTrue(!LegendaryAbilities.hammerInput(player, HammerInput.START),
                        "Switching abilities does not clear the hammer cooldown");
                player.discard();
                context.complete();
            }
        });
    }
}
