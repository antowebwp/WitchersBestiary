package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;

public final class UnicornHealingTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 110)
    public void exactSharedCooldownInAndOutOfCombat(TestContext c) {
        var world = c.getWorld();
        var at = c.getAbsolutePos(new BlockPos(1, 2, 1));
        var owner = c.createMockCreativeServerPlayerInWorld();
        var stranger = c.createMockCreativeServerPlayerInWorld();
        owner.setNoGravity(true);
        stranger.setNoGravity(true);
        owner.refreshPositionAndAngles(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        stranger.refreshPositionAndAngles(at.getX() + 1.5, at.getY(), at.getZ() + .5, 0, 0);
        var first = Unicorns.TYPE.create(world);
        var second = Unicorns.TYPE.create(world);
        for (var horse : java.util.List.of(first, second)) {
            horse.bondWithPlayer(owner);
            horse.refreshPositionAndAngles(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        }
        c.runAtTick(1, () -> {
            stranger.setHealth(5);
            owner.setHealth(owner.getMaxHealth());
            c.assertTrue(!first.healOwner(world), "Full owner and injured stranger are ignored");
            owner.setHealth(5);
            first.tick();
            c.assertTrue(owner.getHealth() == 9 && stranger.getHealth() == 5 && first.healing(), "Exactly 4 HP outside combat");
            c.assertTrue(!second.healOwner(world), "Two unicorns share the cooldown");
        });
        c.runAtTick(80, () -> {
            owner.setHealth(5);
            second.tick();
            c.assertTrue(owner.getHealth() == 5, "Cooldown still active on tick 79");
        });
        c.runAtTick(81, () -> {
            var attacker = EntityType.ZOMBIE.create(world);
            owner.setAttacker(attacker);
            second.tick();
            c.assertTrue(owner.getHealth() == 9 && second.healing(), "Exactly 4 HP in combat on tick 80");
            attacker.discard(); first.discard(); second.discard(); owner.discard(); stranger.discard();
            c.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void mountedRegenAndReload(TestContext c) {
        var world = c.getWorld();
        var owner = c.createMockCreativeServerPlayerInWorld();
        var original = Unicorns.TYPE.create(world);
        original.bondWithPlayer(owner);
        original.saddle(new ItemStack(Items.SADDLE), null);
        var saved = new NbtCompound();
        original.writeCustomDataToNbt(saved);
        original.discard();
        var horse = Unicorns.TYPE.create(world);
        horse.readCustomDataFromNbt(saved);
        owner.startRiding(horse, true);
        owner.setHealth(8);
        horse.tick();
        var regen = owner.getStatusEffect(StatusEffects.REGENERATION);
        c.assertTrue(regen != null && regen.getAmplifier() == 0 && regen.getDuration() == 100 && owner.getHealth() == 12,
            "Restored owner gets both mounted regeneration and a 4 HP heal");
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 30, 1));
        horse.tick();
        c.assertTrue(owner.getStatusEffect(StatusEffects.REGENERATION).getAmplifier() == 1, "Stronger regeneration remains active");
        owner.removeStatusEffect(StatusEffects.REGENERATION);
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0));
        horse.tick();
        c.assertTrue(owner.getStatusEffect(StatusEffects.REGENERATION).getDuration() == 100, "Short mounted effect renews");
        owner.stopRiding();
        horse.tick();
        var dismounted = owner.getStatusEffect(StatusEffects.REGENERATION);
        // The embedded mock player does not tick effects, so advance vanilla's effect instance.
        for (int i = 0; i < 60; i++) dismounted.update(owner, () -> {});
        horse.tick();
        c.assertTrue(owner.getStatusEffect(StatusEffects.REGENERATION) == dismounted && dismounted.getDuration() == 40,
            "A dismounted owner does not receive a refresh");
        for (int i = 0; i < 39; i++) dismounted.update(owner, () -> {});
        c.assertTrue(!dismounted.update(owner, () -> {}), "Dismounted regeneration expires");
        c.runAtTick(110, () -> {
            owner.startRiding(horse, true);
            owner.removeStatusEffect(StatusEffects.REGENERATION);
            horse.damage(world.getDamageSources().genericKill(), 1000);
            owner.setHealth(8);
            horse.tick();
            c.assertTrue(horse.recovering() && owner.getHealth() == 8
                && !owner.hasStatusEffect(StatusEffects.REGENERATION), "Recovery blocks both abilities");
            horse.discard(); owner.discard();
            c.complete();
        });
    }
}
