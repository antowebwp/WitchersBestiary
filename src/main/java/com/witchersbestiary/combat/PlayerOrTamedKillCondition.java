package com.witchersbestiary.combat;

import com.mojang.serialization.MapCodec;
import com.witchersbestiary.registry.ModLoot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import java.util.Set;

/** Проверяет именно источник последнего удара, без зачёта случайного урона среды. */
public record PlayerOrTamedKillCondition() implements LootCondition {
    public static final MapCodec<PlayerOrTamedKillCondition> CODEC = MapCodec.unit(new PlayerOrTamedKillCondition());
    @Override public LootConditionType getType() { return ModLoot.PLAYER_OR_TAMED_KILL; }
    @Override public Set<LootContextParameter<?>> getRequiredParameters() { return Set.of(LootContextParameters.ATTACKING_ENTITY); }
    @Override public boolean test(LootContext context) {
        Entity attacker = context.get(LootContextParameters.ATTACKING_ENTITY);
        return attacker instanceof PlayerEntity || attacker instanceof TameableEntity pet && pet.isTamed() && pet.getOwnerUuid() != null;
    }
}
