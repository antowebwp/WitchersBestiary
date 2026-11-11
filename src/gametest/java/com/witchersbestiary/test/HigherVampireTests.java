package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public final class HigherVampireTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 110)
    public void transformsInPlaceWithClearance(TestContext context) {
        var world = context.getWorld();
        var pos = context.getAbsolutePos(new BlockPos(3, 2, 3));
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        var vampire = ModEntities.HIGHER_VAMPIRE.create(world);
        vampire.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        vampire.setAiDisabled(true);
        vampire.setHealth(52);
        world.spawnEntity(vampire);
        var uuid = vampire.getUuid();

        world.setBlockState(pos.up(3), Blocks.STONE.getDefaultState());
        context.assertTrue(!vampire.beginTransformation(), "Ceiling must prevent transformation");
        world.setBlockState(pos.up(3), Blocks.AIR.getDefaultState());
        context.assertTrue(vampire.beginTransformation(), "Clear space permits transformation");
        context.assertTrue(!vampire.beginTransformation(), "Transformation cannot restart");

        context.runAtTick(85, () -> {
            context.assertTrue(vampire.isHybrid(), "Hybrid form active");
            context.assertTrue(world.getEntity(uuid) == vampire, "Original entity and UUID retained");
            context.assertTrue(vampire.getHealth() == 52, "Health retained");
            context.assertTrue(vampire.getHeight() == 3.2f && vampire.getWidth() == 1.32f, "Hybrid hitbox active");
            context.assertTrue(Math.abs(vampire.getY() - pos.getY()) < .01, "Feet stay on the floor");
            context.complete();
        });
    }
}
