package com.witchersbestiary.test;

import com.witchersbestiary.alchemy.DragonGas;
import com.witchersbestiary.alchemy.MoonDust;
import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class BombInteractionTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void gasCanOnlyIgniteOnce(TestContext context) {
        var world = context.getWorld();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BombFixture");
        var player = new net.minecraft.server.network.ServerPlayerEntity(world.getServer(), world, profile,
                net.minecraft.server.network.ConnectedClientData.createDefault(profile, false).syncedOptions());
        var start = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 3, 1)));
        var end = start.add(5, 0, 0);
        DragonGas.release(world, player, start.add(3, 0, 0));
        context.assertTrue(DragonGas.ignite(player, start, end, 37.5) == 1, "Igni cone ignites one cloud");
        context.assertTrue(DragonGas.ignite(player, start, end, 0) == 0, "Consumed cloud cannot ignite again");
        DragonGas.release(world, player, start.add(3, 0, 0));
        context.assertTrue(DragonGas.ignite(player, start, end, 0) == 1, "Breath segment ignites gas");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 150)
    public void moonDustExpiresWithoutDisablingAi(TestContext context) {
        var world = context.getWorld();
        var foglet = ModEntities.FOGLET.create(world);
        var target = net.minecraft.entity.EntityType.ZOMBIE.create(world);
        foglet.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 200));
        MoonDust.apply(foglet);
        context.assertTrue(MoonDust.active(foglet) && !foglet.isAiDisabled()
                && !foglet.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.INVISIBILITY),
                "Reveals target and retains AI");
        context.assertTrue(foglet.summonIllusions(target) == 0, "Illusions blocked during suppression");
        context.runAtTick(126, () -> {
            context.assertTrue(!MoonDust.active(foglet), "Suppression expires");
            context.complete();
        });
    }
}
