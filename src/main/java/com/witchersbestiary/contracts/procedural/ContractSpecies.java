package com.witchersbestiary.contracts.procedural;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
/** Approved species IDs are independent from existing entity registry aliases. */
public final class ContractSpecies {
 private static final Set<String> LEGACY=Set.of("drowner","ghoul","foglet");
 public static final Set<String> FOREST=Set.of("ghoul","foglet","leshen","endrega","werewolf","katakan");
 public static final Map<String,String> ENTITIES=Map.ofEntries(Map.entry("leshen","leshen"),Map.entry("drowner","drowner"),Map.entry("ghoul","ghoul"),Map.entry("noonwraith","noonwraith"),Map.entry("foglet","foglet"),Map.entry("fiend","fiend"),Map.entry("endrega","endrega_warrior"),Map.entry("katakan","katakan"),Map.entry("werewolf","werewolf"),Map.entry("specter","wraith"),Map.entry("griffin","griffin"),Map.entry("grave_hag","grave_hag"));
 @SuppressWarnings("unchecked") public static EntityType<? extends net.minecraft.entity.mob.MobEntity> type(String species){var id=ENTITIES.get(species);if(id==null)throw new IllegalArgumentException("Unapproved hunt species");return (EntityType<? extends net.minecraft.entity.mob.MobEntity>)Registries.ENTITY_TYPE.get(Identifier.of("witchersbestiary",id));}
 public static String species(String entity){return ENTITIES.entrySet().stream().filter(e->e.getValue().equals(entity)).map(Map.Entry::getKey).findFirst().orElseThrow();}
 public static boolean legacy(String species){return LEGACY.contains(species);}
}
