package com.witchersbestiary.test;

import com.witchersbestiary.combat.MonsterHealthScaling;
import com.witchersbestiary.combat.StageCombatBalance;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.signs.SignType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public final class Stage22QBalanceTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void attributesAndDamageProfiles(TestContext context) {
        var world = context.getWorld();
        var monster = ModEntities.GHOUL.create(world);
        monster.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).addTemporaryModifier(
                new net.minecraft.entity.attribute.EntityAttributeModifier(
                        com.witchersbestiary.combat.ProgressiveDifficulty.HEALTH, 0,
                        net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        double[] expectedRates = {.3, .4, .45, .6};
        int[] expectedBases = {8, 9, 11, 14};
        for (var sword : StageCombatBalance.Sword.values()) {
            context.assertTrue(sword.base == expectedBases[sword.ordinal()]
                    && sword.percent == expectedRates[sword.ordinal()], "Rebalanced profile " + sword.name());
        }
        for (int health : new int[]{100, 500, 1000, 2000}) {
            monster.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(health);
            for (var sword : StageCombatBalance.Sword.values()) {
                close(context, MonsterHealthScaling.bonus(monster, sword.percent / 100, 8),
                        Math.min(8, health * sword.percent / 100), sword.name());
            }
            for (var bomb : StageCombatBalance.Bomb.values()) {
                close(context, bomb.damage(monster), bomb.base + Math.min(24, health * bomb.percent / 100), bomb.name());
            }
            close(context, StageCombatBalance.signDamage(monster, SignType.IGNI, 0, 0, 1),
                    Math.min(16, health * .02), "Igni");
            close(context, StageCombatBalance.signDamage(monster, SignType.IGNI, 0, 1, 1),
                    Math.min(16, health * .025), "Igni rank 1");
            close(context, StageCombatBalance.signDamage(monster, SignType.IGNI, 2, 0, 1),
                    Math.min(16, health * .004), "Fire Breath");
            close(context, StageCombatBalance.signDamage(monster, SignType.AARD, 0, 0, 1),
                    Math.min(16, health * .015), "Aard");
        }
        monster.discard();
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void legendaryMultiplierAndControlOnlyForms(TestContext context) {
        var vampire = ModEntities.HIGHER_VAMPIRE.create(context.getWorld());
        vampire.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(500);
        close(context, StageCombatBalance.Bomb.GRAPESHOT.damage(vampire), 13.5, "Boss bomb");
        close(context, StageCombatBalance.signDamage(vampire, SignType.IGNI, 0, 1, 1), 6.25, "Boss Igni");
        close(context, StageCombatBalance.signDamage(vampire, SignType.QUEN, 3, 3, 1), 0, "Quen aura");
        close(context, StageCombatBalance.signDamage(vampire, SignType.AXII, 0, 3, 1), 0, "Axii");
        vampire.discard();
        context.complete();
    }

    private static void close(TestContext context, float actual, double expected, String label) {
        context.assertTrue(Math.abs(actual - expected) < .001, label + ": expected " + expected + ", got " + actual);
    }
}
