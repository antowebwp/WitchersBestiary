package com.witchersbestiary.unicorn;

import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

final class UnicornLanding {
    private UnicornLanding() {}

    static Optional<Vec3d> find(ServerWorld world, UnicornEntity unicorn, BlockPos near) {
        if (unicorn.hasPassengers()) return Optional.empty();
        for (int radius = 3; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                for (int y : new int[]{0, 1, -1, 2, -2}) {
                    var at = near.add(x, y, z);
                    if (!world.isChunkLoaded(at) || world.isOutOfHeightLimit(at)) continue;
                    var below = at.down();
                    var floor = world.getBlockState(below);
                    if (!floor.isSideSolidFullSquare(world, below, Direction.UP)
                            || floor.isOf(Blocks.MAGMA_BLOCK) || floor.isOf(Blocks.CACTUS)
                            || floor.isOf(Blocks.CAMPFIRE) || floor.isOf(Blocks.SOUL_CAMPFIRE)
                            || floor.isOf(Blocks.POWDER_SNOW)) continue;
                    var position = Vec3d.ofBottomCenter(at);
                    var box = unicorn.getBoundingBox().offset(position.subtract(unicorn.getPos()));
                    if (!world.getWorldBorder().contains(box) || !world.isSpaceEmpty(unicorn, box)) continue;
                    boolean hazardous = BlockPos.stream(box).anyMatch(block ->
                            !world.isChunkLoaded(block) || !world.getFluidState(block).isEmpty()
                            || world.getBlockState(block).isOf(Blocks.FIRE)
                            || world.getBlockState(block).isOf(Blocks.SOUL_FIRE)
                            || world.getBlockState(block).isOf(Blocks.POWDER_SNOW));
                    if (!hazardous) return Optional.of(position);
                }
            }
        }
        return Optional.empty();
    }
}
