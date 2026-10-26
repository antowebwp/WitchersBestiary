package com.witchersbestiary.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public final class AttackInterruptions {
    private AttackInterruptions() { }
    public static boolean attempt(LivingEntity victim, DamageSource source, float actualLoss) {
        if (victim.getWorld().isClient || actualLoss < 6 || !(source.getAttacker() instanceof PlayerEntity)
                || !(victim instanceof InterruptibleAttack attack) || !attack.canInterruptAttack()) return false;
        if (victim.getRandom().nextFloat() >= .35f) return false;
        attack.interruptAttack();return true;
    }
}
