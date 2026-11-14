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
    public static void initialize() {
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin()) return;
            var id = key.getValue();
            if (!id.getNamespace().equals("minecraft") || !java.util.Set.of(
                    "chests/abandoned_mineshaft", "chests/simple_dungeon",
                    "chests/woodland_mansion", "chests/stronghold_corridor").contains(id.getPath())) return;
            var pool = net.minecraft.loot.LootPool.builder().rolls(net.minecraft.loot.provider.number.ConstantLootNumberProvider.create(1))
                    .with(net.minecraft.loot.entry.EmptyEntry.builder().weight(20));
            for (int i = 0; i < ModItems.SCHOOL_TEMPLATES.size(); i++)
                pool.with(net.minecraft.loot.entry.ItemEntry.builder(ModItems.SCHOOL_TEMPLATES.get(i)).weight(i % 2 == 0 ? 3 : 1));
            table.pool(pool);
        });
    }
}
