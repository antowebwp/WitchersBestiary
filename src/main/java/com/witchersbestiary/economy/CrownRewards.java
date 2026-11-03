package com.witchersbestiary.economy;
import com.witchersbestiary.contracts.*;
public final class CrownRewards {
 public static ContractDefinition fresh(ContractDefinition d){var grants=d.reward().items().stream().map(g->g.item().toString().equals("minecraft:emerald")?new ContractDefinition.ItemReward(net.minecraft.util.Identifier.of("witchersbestiary:witcher_crown"),g.count()*10):g).toList();return new ContractDefinition(d.id(),d.name(),d.description(),d.target(),d.required(),d.difficulty(),new ContractDefinition.Reward(d.reward().experience(),grants),d.minimumLevel(),d.dimension(),d.durationMillis());}
}
