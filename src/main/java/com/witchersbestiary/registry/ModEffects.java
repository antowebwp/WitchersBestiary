package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.alchemy.RelictHunterEffect;
import com.witchersbestiary.alchemy.MonsterHunterEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
    public static final RegistryEntry<StatusEffect> RELICT_HUNTER = Registry.registerReference(Registries.STATUS_EFFECT,
            Identifier.of(WitchersBestiary.MOD_ID, "relict_hunter"), new RelictHunterEffect());
    public static final RegistryEntry<StatusEffect> NECROPHAGE_HUNTER = Registry.registerReference(Registries.STATUS_EFFECT,
            Identifier.of(WitchersBestiary.MOD_ID,"necrophage_hunter"),new MonsterHunterEffect());
    private ModEffects() { }
    public static final RegistryEntry<StatusEffect> SPECTER_HUNTER = Registry.registerReference(Registries.STATUS_EFFECT,
            Identifier.of(WitchersBestiary.MOD_ID, "specter_hunter"), new MonsterHunterEffect());
    public static final RegistryEntry<StatusEffect> CURSED_HUNTER=Registry.registerReference(Registries.STATUS_EFFECT,Identifier.of(WitchersBestiary.MOD_ID,"cursed_hunter"),new MonsterHunterEffect());
    public static final RegistryEntry<StatusEffect> VAMPIRE_HUNTER=Registry.registerReference(Registries.STATUS_EFFECT,Identifier.of(WitchersBestiary.MOD_ID,"vampire_hunter"),new MonsterHunterEffect());
    public static final RegistryEntry<StatusEffect> ROOT_BOUND=Registry.registerReference(Registries.STATUS_EFFECT,Identifier.of(WitchersBestiary.MOD_ID,"root_bound"),new StatusEffect(net.minecraft.entity.effect.StatusEffectCategory.HARMFUL,0x59432b){});
    public static final RegistryEntry<StatusEffect> STUNNED=Registry.registerReference(Registries.STATUS_EFFECT,Identifier.of(WitchersBestiary.MOD_ID,"stunned"),new StatusEffect(net.minecraft.entity.effect.StatusEffectCategory.HARMFUL,0xada6cd){});
    public static final RegistryEntry<StatusEffect> CLEAR_MIND=Registry.registerReference(Registries.STATUS_EFFECT,Identifier.of(WitchersBestiary.MOD_ID,"clear_mind"),new StatusEffect(net.minecraft.entity.effect.StatusEffectCategory.BENEFICIAL,0x7fb5c3){});
    public static void initialize() { }
}
