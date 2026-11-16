package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.GriffinEntity;
import com.witchersbestiary.entity.monster.GriffinEntity.Action;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class GriffinReforgedTests implements FabricGameTest {
    @GameTest(templateName = "witchersbestiary:village_board_arena", tickLimit = 280)
    public void aerialSequenceAndCounter(TestContext context) {
        var world = context.getWorld();
        var origin = context.getAbsolutePos(new BlockPos(8, 10, 12));
        for (int x=-6; x<=33; x++) for (int z=-7; z<=7; z++) {
            world.setBlockState(origin.add(x,-1,z), Blocks.STONE.getDefaultState());
            for (int y=0; y<4; y++) world.setBlockState(origin.add(x,y,z), Blocks.AIR.getDefaultState());
        }
        var target = UnicornTests.player(context);
        target.changeGameMode(GameMode.SURVIVAL);
        target.refreshPositionAndAngles(origin.getX()+27.5,origin.getY(),origin.getZ()+.5,90,0);
        var griffin = ModEntities.GRIFFIN.create(world);
        griffin.refreshPositionAndAngles(origin.getX()+.5,origin.getY(),origin.getZ()+.5,-90,0);
        world.spawnEntity(griffin);
        griffin.setTarget(target);
        var observed = EnumSet.noneOf(Action.class);
        context.runAtEveryTick(() -> {
            if (griffin.getTarget() == null && !observed.contains(Action.DIVE)) griffin.setTarget(target);
            var action = griffin.getAction();
            if (observed.add(action)) {
                if (action == Action.TAKEOFF_PREPARE)
                    context.assertFalse(griffin.hasTakeoffMeleeProtection(), "Preparation allows melee");
                if (action == Action.TAKEOFF)
                    context.assertTrue(griffin.hasTakeoffMeleeProtection()
                            && !griffin.damage(world.getDamageSources().playerAttack(target),2),
                            "Only ascent blocks normal melee");
                if (action == Action.DIVE) {
                    context.assertFalse(griffin.hasTakeoffMeleeProtection(), "Ascent protection must end");
                    griffin.interruptDive();
                    context.assertTrue(griffin.getAction() == Action.STAGGER && !griffin.isFlying()
                            && !griffin.hasNoGravity(), "Aard counter restores gravity");
                    griffin.setTarget(null);
                    target.changeGameMode(GameMode.CREATIVE);
                }
            }
        });
        context.runAtTick(245, () -> {
            context.assertTrue(observed.containsAll(EnumSet.of(Action.TAKEOFF_PREPARE, Action.TAKEOFF,
                    Action.AIR_CHASE, Action.DIVE_WARNING, Action.DIVE)), "Server exercised the full flight path: " + observed);
            context.assertTrue(!griffin.isFlying() && griffin.isOnGround(), "Griffin returned to the floor");
            griffin.discard();
            target.discard();
            context.complete();
        });
    }
}
