package com.witchersbestiary.contracts.procedural;

import com.witchersbestiary.contracts.ContractDefinition;
import com.witchersbestiary.contracts.hunt.GraveDevourer;
import com.witchersbestiary.contracts.hunt.SwampLord;
import com.witchersbestiary.contracts.hunt.VampireHunt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.util.Identifier;

/** Legendary variants share the procedural routes and entity lifecycle, with a separate board identity. */
public final class LegendaryHunts {
    private static final List<String> SPECIES = List.of("leshen", "griffin", "fiend", "katakan", "werewolf",
            "foglet", "endrega", "noonwraith", "specter", "grave_hag", "bonecrusher", "nekker");
    private static final Set<Identifier> IDS = SPECIES.stream().map(LegendaryHunts::id).collect(Collectors.toUnmodifiableSet());

    private LegendaryHunts() {}

    public static Identifier id(String species) {
        return Identifier.of("witchersbestiary", "legendary_" + species);
    }

    public static boolean variant(Identifier id) { return IDS.contains(id); }

    public static boolean offered(Identifier id) { return variant(id) || VampireHunt.ID.equals(id); }

    public static boolean onLegendsTab(Identifier id) {
        return offered(id) || SwampLord.ID.equals(id) || GraveDevourer.ID.equals(id);
    }

    public static void addDefinitions(Map<Identifier, ContractDefinition> definitions) {
        for (String species : SPECIES) {
            var base = definitions.get(ProceduralHunts.id(species));
            if (base == null || !base.target().equals(Identifier.of("witchersbestiary", ContractSpecies.ENTITIES.get(species)))) continue;
            var id = id(species);
            String key = "hunt.witchersbestiary.legendary." + species;
            definitions.put(id, new ContractDefinition(id, key, key + ".description", base.target(), 1, 4,
                    base.reward(), base.minimumLevel(), base.dimension(), base.durationMillis()));
        }
    }
}
