package com.witchersbestiary.combat;

import com.witchersbestiary.registry.ModEffects;
import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public final class AlchemyDamage {
    private AlchemyDamage() { }
    private static float elixirs(LivingEntity victim, DamageSource source, float amount) {
        if(source.isOf(com.witchersbestiary.equipment.OgroidHammerItem.SLAM))return amount;
        if (victim.getWorld().isClient||!(victim instanceof CategorizedMonster monster)||!(source.getAttacker() instanceof PlayerEntity player))return amount;
        MonsterCategory target=monster.getMonsterCategory();
        float multiplier=1f;
        if(target==MonsterCategory.RELICT&&player.hasStatusEffect(ModEffects.RELICT_HUNTER))multiplier*=1.25f;
        if(target==MonsterCategory.NECROPHAGE&&player.hasStatusEffect(ModEffects.NECROPHAGE_HUNTER))multiplier*=1.25f;
        if(target==MonsterCategory.SPECTER&&player.hasStatusEffect(ModEffects.SPECTER_HUNTER))multiplier*=1.25f;
        if(target==MonsterCategory.CURSED&&player.hasStatusEffect(ModEffects.CURSED_HUNTER))multiplier*=1.25f;
        if(target==MonsterCategory.VAMPIRE&&player.hasStatusEffect(ModEffects.VAMPIRE_HUNTER))multiplier*=1.25f;
        return amount*multiplier;
    }
    /** Preview also used by tests/tooling; it never consumes a charge. */
    public static float modify(LivingEntity victim, DamageSource source, float amount) {
        return withOil(victim,source,amount,com.witchersbestiary.alchemy.InventoryOils.prepare(victim,source));
    }
    public static float withOil(LivingEntity victim,DamageSource source,float amount,com.witchersbestiary.alchemy.InventoryOils.Use oil){
        return elixirs(victim,source,amount)*(oil==null?1:1+com.witchersbestiary.alchemy.InventoryOils.BONUS);
    }
}
