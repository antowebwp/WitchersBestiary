package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.combat.PlayerOrTamedKillCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModLoot {
    public static final LootConditionType PLAYER_OR_TAMED_KILL = Registry.register(Registries.LOOT_CONDITION_TYPE,
            Identifier.of(WitchersBestiary.MOD_ID, "player_or_tamed_kill"), new LootConditionType(PlayerOrTamedKillCondition.CODEC));
    private ModLoot() { }
    public static void initialize() { }
}
