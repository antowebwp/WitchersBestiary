package com.witchersbestiary.test;

import com.witchersbestiary.equipment.armory.BloodAuraState;
import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;

public final class BloodLegacyReleaseTests implements FabricGameTest {
    @GameTest(templateName = "witchersbestiary:village_board_arena", tickLimit = 830)
    public void bloodlustHealingAndCooldownBoundary(TestContext context) {
        var world = context.getWorld();
        var player = LegendaryAbilityTests.player(context, "BloodRelease");
        player.setInvulnerable(true);
        player.setHealth(5);
        player.setStackInHand(Hand.MAIN_HAND, ModItems.BLOOD_LEGACY.getDefaultStack());
        var target = EntityType.ZOMBIE.create(world);
        target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
        target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
        target.setHealth(1000);

        for (int i = 0; i < 8; i++) hit(player, target);
        long started = world.getTime();
        context.assertTrue(LegendaryAbilities.activate(player), "Eight hits activate Bloodlust");
        var aura = (BloodAuraState) player;

        float before = target.getHealth();
        var usedSource = hit(player, target);
        float expected = 5 + .2f * (before - target.getHealth());
        context.assertTrue(expected > 7 && Math.abs(player.getHealth() - expected) < .001f,
                "Healing is 20 percent of health damage and exceeds the former per-hit cap");
        target.timeUntilRegen = 0;
        target.damage(usedSource, 14);
        target.timeUntilRegen = 0;
        com.witchersbestiary.registry.ModEffects.BLEEDING.value().applyUpdateEffect(target, 0);
        target.timeUntilRegen = 0;
        target.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE)
                .entryOf(SignManager.FIRE), player), 1);
        context.assertTrue(Math.abs(player.getHealth() - expected) < .001f,
                "Reused damage source, bleeding and burn never grant a second heal");

        for (int i = 0; i < 3; i++) hit(player, target);
        context.assertTrue(player.getHealth() > 13 && player.getHealth() < player.getMaxHealth(),
                "Healing exceeds the former per-activation cap");
        for (int i = 0; i < 4; i++) hit(player, target);
        context.assertTrue(player.getHealth() == player.getMaxHealth(), "Healing stops at maximum health");

        context.runAtEveryTick(() -> {
            long elapsed = world.getTime() - started;
            if (elapsed == 199) context.assertTrue(aura.bestiary$bloodAura(), "Buff lasts ten seconds");
            if (elapsed == 201) {
                context.assertTrue(!aura.bestiary$bloodAura(), "Buff ends before the cooldown elapses");
                player.setHealth(5);
                hit(player, target);
                context.assertTrue(player.getHealth() == 5, "Expired Bloodlust cannot heal");
            }
            if (elapsed == 799) {
                for (int i = 0; i < 8; i++) hit(player, target);
                context.assertTrue(!LegendaryAbilities.activate(player), "Still cooling down one tick before 30 seconds");
            }
            if (elapsed == 800) {
                for (int i = 0; i < 7; i++) hit(player, target);
                context.assertTrue(!LegendaryAbilities.activate(player), "Cooldown hits did not count toward charge");
                hit(player, target);
                context.assertTrue(LegendaryAbilities.activate(player), "Eight hits charge again at the 30-second boundary");
                target.discard();
                player.discard();
                context.complete();
            }
        });
    }

    private static DamageSource hit(ServerPlayerEntity player, LivingEntity target) {
        var source = player.getDamageSources().playerAttack(player);
        target.timeUntilRegen = 0;
        ModItems.BLOOD_LEGACY.getBonusAttackDamage(target, 14, source);
        target.damage(source, 14);
        return source;
    }
}
