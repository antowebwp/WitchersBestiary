package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.LeshenWolfEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public final class LeshenPackTests implements FabricGameTest {
    private static final String ARENA = "witchersbestiary:village_board_arena";

    private BlockPos floor(TestContext c) {
        var origin = c.getAbsolutePos(new BlockPos(8, 10, 12));
        var world = c.getWorld();
        for (int x = -5; x <= 32; x++) for (int z = -5; z <= 5; z++) {
            var at = origin.add(x, 0, z);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
            for (int y = 0; y < 5; y++) world.setBlockState(at.up(y), Blocks.AIR.getDefaultState());
        }
        return origin;
    }

    private void place(Entity entity, BlockPos position) {
        entity.refreshPositionAndAngles(position.getX() + .5, position.getY(), position.getZ() + .5, -90, 0);
    }

    private net.minecraft.server.network.ServerPlayerEntity player(TestContext c, BlockPos pos) {
        var player = c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        place(player, pos);
        return player;
    }

    @GameTest(templateName = ARENA, tickLimit = 240)
    public void replacesThreePacksAndCleansUpOnDeath(TestContext c) {
        var start = floor(c);
        var world = c.getWorld();
        var victim = player(c, start.east(9));
        var leshen = ModEntities.LESHEN.create(world);
        place(leshen, start);
        world.spawnEntity(leshen);
        leshen.setTarget(victim);
        var defeated = new java.util.HashSet<java.util.UUID>();
        var destroyed = new int[1];
        var lastDestroyed = new int[1];
        c.runAtEveryTick(() -> {
            c.assertTrue(leshen.getSummonedWolfCount() <= 4, "Bounded pack");
            place(victim, start.east(9));
            victim.setHealth(20);
            var wolves = world.getEntitiesByClass(LeshenWolfEntity.class, new Box(start).expand(18), Entity::isAlive);
            if (wolves.size() < 2) return;
            c.assertTrue(wolves.stream().allMatch(wolf -> wolf.getTarget() == victim && !defeated.contains(wolf.getUuid())),
                "Fresh wolves immediately target the player");
            if (destroyed[0] > 0) {
                int elapsed = world.getServer().getTicks() - lastDestroyed[0];
                c.assertTrue(elapsed >= 39 && elapsed <= 56, "Replacement takes two to three seconds");
            }
            if (destroyed[0] == 3) {
                leshen.kill();
                c.assertTrue(wolves.stream().allMatch(Entity::isRemoved), "Boss death clears the replacement pack");
                victim.discard();
                c.complete();
                return;
            }
            if (destroyed[0] == 1) leshen.setHealth(leshen.getMaxHealth() * .45f);
            for (var wolf : wolves) {
                defeated.add(wolf.getUuid());
                wolf.damage(world.getDamageSources().genericKill(), 1000);
            }
            destroyed[0]++;
            lastDestroyed[0] = world.getServer().getTicks();
        });
        c.runAtTick(225, () -> c.assertTrue(false, "Expected four distinct packs before timeout"));
    }
}
