package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class Stage22RCombatTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, tickLimit=125)
    public void unicornCombatHealingIsFractional(TestContext c) {
        var world = c.getWorld();
        var owner = UnicornTests.player(c);
        var horse = Unicorns.TYPE.create(world);
        var enemy = EntityType.ZOMBIE.create(world);
        horse.bondWithPlayer(owner);
        horse.setAiDisabled(true);
        horse.setNoGravity(true);
        horse.setHealth(16);
        var source = world.getDamageSources().mobAttack(enemy);
        float[] damaged = new float[1];
        c.runAtTick(1, () -> {
            c.assertTrue(horse.damage(source,1), "Hostile hit starts combat");
            damaged[0] = horse.getHealth();
        });
        c.runAtTick(61, () -> {
            horse.tick();
            c.assertTrue(horse.getHealth() == damaged[0], "100-tick interval still applies after the delay");
        });
        c.runAtTick(101, () -> {
            horse.tick();
            c.assertTrue(Math.abs(horse.getHealth() - damaged[0] - .28f) < .001, "1% of 28 HP is restored");
            horse.discard();
            enemy.discard();
            owner.discard();
            c.complete();
        });
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, tickLimit=205)
    public void followStartsAtEightAndWaitStopsMovement(TestContext c) {
        var world=c.getWorld();
        var at=c.getAbsolutePos(new BlockPos(2,2,2));
        for (int x=-3;x<=15;x++) for(int z=-4;z<=4;z++)
            world.setBlockState(at.add(x,-1,z),Blocks.STONE.getDefaultState());
        var horse=Unicorns.TYPE.create(world);
        var owner=UnicornTests.player(c);
        owner.setNoGravity(true);
        owner.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        horse.refreshPositionAndAngles(owner.getX(),owner.getY(),owner.getZ()+.9,0,0);
        horse.bondWithPlayer(owner);
        world.spawnEntity(horse);
        c.runAtTick(3, () -> owner.setPosition(owner.getPos().add(10,0,0)));
        c.runAtTick(115, () -> {
            c.assertTrue(horse.squaredDistanceTo(owner) <= 25, "Follow distance=" + Math.sqrt(horse.squaredDistanceTo(owner)) + ", owner=" + (horse.getOwner() == owner) + ", navigationIdle=" + horse.getNavigation().isIdle() + ", path=" + horse.getNavigation().findPathTo(owner,3));
            owner.setSneaking(true);
            horse.interactMob(owner, Hand.MAIN_HAND);
            owner.setSneaking(false);
            c.assertTrue(horse.waiting(), "Mode switches to wait");
            owner.setPosition(owner.getPos().add(-10,0,0));
        });
        c.runAtTick(174, () -> {
            c.assertTrue(horse.squaredDistanceTo(owner) > 36, "Wait prevents following");
            horse.discard();
            owner.discard();
            c.complete();
        });
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void steelAddsMaxHealthDamageOnce(TestContext c) {
        var world=c.getWorld();
        var player=UnicornTests.player(c);
        var sword = new ItemStack(ModItems.STEEL_HUNTER_SWORD);
        player.setStackInHand(Hand.MAIN_HAND,sword);
        for (int hp : new int[]{60, 180}) {
            var target=ModEntities.DROWNER.create(world);
            target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(hp);
            target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
            target.setHealth(hp);
            var source=world.getDamageSources().playerAttack(player);
            sword.getItem().getBonusAttackDamage(target,4,source);
            target.damage(source,4);
            c.assertTrue(Math.abs(hp-target.getHealth()-(4+hp*.0035))<.002,
                    "Primary hit includes 0.35% of maximum HP at " + hp + " HP");
            target.setHealth(hp);
            target.timeUntilRegen=0;
            target.damage(source,4);
            c.assertTrue(Math.abs(hp-target.getHealth()-4)<.002, "The same damage source cannot apply the bonus twice");
            target.discard();
        }
        player.discard();
        c.complete();
    }
}
