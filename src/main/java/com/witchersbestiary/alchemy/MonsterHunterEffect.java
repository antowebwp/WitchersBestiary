package com.witchersbestiary.alchemy;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Ванильный StatusEffect — его длительность, иконка и NBT синхронизируются Minecraft. */
public final class MonsterHunterEffect extends StatusEffect {
    public MonsterHunterEffect() { super(StatusEffectCategory.BENEFICIAL,0x46c95b); }
}
