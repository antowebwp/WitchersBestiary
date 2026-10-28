package com.witchersbestiary.contracts.procedural;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
/** Immutable roll persisted once at acceptance. Structured translation keys survive language changes. */
public record ProceduralProfile(long seed,String species,Tier tier,int name,int epithet,Set<ContractAbility> abilities,double health,double damage) {
 public enum Tier {COMMON,RARE,EPIC}
 public ProceduralProfile {abilities=Set.copyOf(abilities);if(!Set.of("drowner","ghoul","foglet").contains(species)||name<0||name>=8||epithet<0||epithet>=8||abilities.isEmpty()||abilities.size()>3||abilities.stream().anyMatch(a->!a.supports(species))||!Double.isFinite(health)||!Double.isFinite(damage)||health<1||health>200||damage<1||damage>20)throw new IllegalArgumentException("Invalid procedural profile");}
 public static ProceduralProfile generate(long seed,String species){var random=new Random(seed);int roll=random.nextInt(10);var tier=roll<6?Tier.COMMON:roll<9?Tier.RARE:Tier.EPIC;var pool=new ArrayList<ContractAbility>();for(var a:ContractAbility.values())if(a.supports(species))pool.add(a);Collections.shuffle(pool,random);int count=Math.min(pool.size(),tier==Tier.COMMON?1:tier==Tier.RARE?2:2+random.nextInt(2));var chosen=EnumSet.noneOf(ContractAbility.class);chosen.addAll(pool.subList(0,count));double baseHp=switch(species){case "drowner"->36;case "ghoul"->30;default->45;};double baseDamage=switch(species){case "drowner"->6;case "ghoul"->5;default->7;};double hpMultiplier=switch(tier){case COMMON->1.5;case RARE->2;case EPIC->3;};double damageMultiplier=switch(tier){case COMMON->1.1;case RARE->1.25;case EPIC->1.5;};return new ProceduralProfile(seed,species,tier,random.nextInt(8),random.nextInt(8),chosen,Math.round(baseHp*hpMultiplier*(.95+random.nextDouble()*.10)),baseDamage*damageMultiplier);}
 public String nameKey(){return "hunt.witchersbestiary.proc.name."+species+"."+name;}
 public String epithetKey(){return "hunt.witchersbestiary.proc.epithet."+epithet;}
 public Text displayName(){return Text.translatable("hunt.witchersbestiary.proc.name_format",Text.translatable(nameKey()),Text.translatable(epithetKey()));}
 public boolean has(ContractAbility ability){return abilities.contains(ability);}
 public NbtCompound write(){var n=new NbtCompound();n.putLong("Seed",seed);n.putString("Species",species);n.putString("Tier",tier.name());n.putInt("Name",name);n.putInt("Epithet",epithet);n.putDouble("Health",health);n.putDouble("Damage",damage);var list=new NbtList();abilities.stream().sorted().forEach(a->list.add(NbtString.of(a.name())));n.put("Abilities",list);return n;}
 public static ProceduralProfile read(NbtCompound n){var abilities=EnumSet.noneOf(ContractAbility.class);var list=n.getList("Abilities",NbtElement.STRING_TYPE);for(int i=0;i<list.size();i++)if(!abilities.add(ContractAbility.valueOf(list.getString(i))))throw new IllegalArgumentException("Duplicate ability");return new ProceduralProfile(n.getLong("Seed"),n.getString("Species"),Tier.valueOf(n.getString("Tier")),n.getInt("Name"),n.getInt("Epithet"),abilities,n.getDouble("Health"),n.getDouble("Damage"));}
}
