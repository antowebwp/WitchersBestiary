package com.witchersbestiary.combat;

import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.contracts.procedural.ProceduralHunts;
import com.witchersbestiary.contracts.procedural.ProceduralProfile;
import com.witchersbestiary.entity.monster.HigherVampireEntity;
import com.witchersbestiary.entity.monster.InfernalBasiliskEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;

/** Target classification shared by attacks that opt in to a bounded maximum-health component. */
public final class MonsterHealthScaling {
    private MonsterHealthScaling() {}

    public static float bonus(LivingEntity target, double fraction, float cap) {
        if (!(target instanceof CategorizedMonster)
                || !(target instanceof HostileEntity || target instanceof InfernalBasiliskEntity)
                || target instanceof Tameable tame && tame.getOwnerUuid() != null) return 0;
        return component(target, fraction, cap);
    }

    public static float component(LivingEntity target, double fraction, float cap) {
        if (target instanceof HigherVampireEntity vampire && vampire.isCocooned()) return 0;
        var id = Registries.ENTITY_TYPE.getId(target.getType()).toString();
        var contract = ProceduralHunts.combat(target);
        double coefficient = target instanceof HigherVampireEntity || target instanceof InfernalBasiliskEntity
                ? .5 : ModConfig.progressive().elites().contains(id)
                    || contract != null && contract.profile.tier() != ProceduralProfile.Tier.COMMON ? .75 : 1;
        return (float)Math.min(cap, target.getMaxHealth() * fraction * coefficient);
    }
}
