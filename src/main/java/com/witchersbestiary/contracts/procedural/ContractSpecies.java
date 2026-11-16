package com.witchersbestiary.contracts.procedural;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.util.Identifier;
/** Approved species IDs are independent from existing entity registry aliases. */
public final class ContractSpecies {
 private static final Set<String> LEGACY=Set.of("drowner","ghoul","foglet");
 public static final Set<String> FOREST=Set.of("ghoul","foglet","leshen","endrega","werewolf","katakan");
 public static final Map<String,String> ENTITIES=Map.ofEntries(Map.entry("leshen","leshen"),Map.entry("drowner","drowner"),Map.entry("ghoul","ghoul"),Map.entry("noonwraith","noonwraith"),Map.entry("foglet","foglet"),Map.entry("fiend","fiend"),Map.entry("endrega","endrega_warrior"),Map.entry("katakan","katakan"),Map.entry("werewolf","werewolf"),Map.entry("specter","wraith"),Map.entry("griffin","griffin"),Map.entry("grave_hag","grave_hag"),Map.entry("water_hag","water_hag"),Map.entry("nekker","nekker"),Map.entry("bonecrusher","bonecrusher"));
 @SuppressWarnings("unchecked") public static EntityType<? extends net.minecraft.entity.mob.MobEntity> type(String species){var id=ENTITIES.get(species);if(id==null)throw new IllegalArgumentException("Unapproved hunt species");var key=Identifier.of("witchersbestiary",id);if(!Registries.ENTITY_TYPE.containsId(key))throw new IllegalStateException("Missing hunt entity: "+key);return (EntityType<? extends net.minecraft.entity.mob.MobEntity>)Registries.ENTITY_TYPE.get(key);}
 public static String species(String entity){return ENTITIES.entrySet().stream().filter(e->e.getValue().equals(entity)).map(Map.Entry::getKey).findFirst().orElseThrow();}
 public static boolean aquatic(String species) {
  return species.equals("water_hag") || species.equals("drowner");
 }

 public static boolean prefers(String species, RegistryEntry<Biome> biome) {
  return switch (species) {
   case "drowner", "foglet" -> biome.isIn(BiomeTags.IS_RIVER) || biome.matchesKey(BiomeKeys.SWAMP)
       || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP);
   case "leshen", "endrega", "water_hag", "nekker" -> biome.isIn(TagKey.of(RegistryKeys.BIOME,
       Identifier.of("witchersbestiary", species + "_habitats")));
   case "specter" -> biome.isIn(TagKey.of(RegistryKeys.BIOME,
       Identifier.of("witchersbestiary", "wraith_habitats")));
   case "bonecrusher" -> biome.isIn(TagKey.of(RegistryKeys.BIOME,
       Identifier.of("witchersbestiary", "ogroid_ranges")));
   case "fiend", "katakan", "werewolf" -> biome.isIn(BiomeTags.IS_FOREST) || biome.isIn(BiomeTags.IS_TAIGA);
   case "griffin", "noonwraith" -> biome.matchesKey(BiomeKeys.PLAINS)
       || biome.matchesKey(BiomeKeys.SUNFLOWER_PLAINS) || biome.matchesKey(BiomeKeys.SAVANNA)
       || species.equals("griffin") && biome.isIn(BiomeTags.IS_MOUNTAIN);
   default -> false;
  };
 }
 public static boolean legacy(String species){return LEGACY.contains(species);}
}
