package com.witchersbestiary.contracts.procedural;
import java.util.*;
/** Small extensible compatibility registry; mutable runtime state belongs to the entity, not this registry. */
public enum ContractAbility {
 CHARGE(Set.of("drowner","ghoul"),200), FRENZY(Set.of("drowner","ghoul"),300),
 LIFESTEAL(Set.of("ghoul"),80), POISONOUS(Set.of("drowner","ghoul"),100),
 SUMMONER(Set.of("drowner"),300), ILLUSIONIST(Set.of("foglet"),160), PHASE_SHIFT(Set.of("foglet"),100);
 public final Set<String> species;public final int cooldown;
 ContractAbility(Set<String> species,int cooldown){this.species=species;this.cooldown=cooldown;}
 public String id(){return name().toLowerCase(Locale.ROOT);}
 public boolean supports(String kind){return species.contains(kind);}
}
