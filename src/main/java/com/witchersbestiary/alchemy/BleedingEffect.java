package com.witchersbestiary.alchemy;

import com.witchersbestiary.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

public final class BleedingEffect extends StatusEffect {
    public BleedingEffect() { super(StatusEffectCategory.HARMFUL, 0xa01c32); }

    @Override public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 20 == 1;
    }

    @Override public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!entity.getWorld().isClient) entity.damage(entity.getDamageSources().generic(), 1 + Math.min(2, amplifier) * .5f);
        return true;
    }

    public static void inflict(LivingEntity victim, int level) {
        var current = victim.getStatusEffect(ModEffects.BLEEDING);
        if (current == null || current.getAmplifier() < level - 1) {
            victim.removeStatusEffect(ModEffects.BLEEDING);
            victim.addStatusEffect(new StatusEffectInstance(ModEffects.BLEEDING, 80, level - 1));
        }
    }
}
