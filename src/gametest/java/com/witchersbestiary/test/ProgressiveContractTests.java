package com.witchersbestiary.test;
import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.procedural.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import java.util.*;
public final class ProgressiveContractTests implements FabricGameTest {
 private ProceduralProfile profile(String kind,ProceduralProfile.Tier tier){for(int seed=0;seed<1000;seed++){var p=ProceduralProfile.generate(seed,kind);if(p.tier()==tier)return p;}throw new IllegalStateException("No profile");}
 private MobEntity target(TestContext c,String kind,ProceduralProfile.Tier tier,UUID owner){var p=profile(kind,tier);var d=ProceduralHunts.snapshot(Contracts.DEFINITIONS.find(ProceduralHunts.id(kind)),p);var contract=new ContractInstance(UUID.randomUUID(),owner,d,System.currentTimeMillis());contract.procedural=p;var mob=ContractSpecies.type(kind).create(c.getWorld());ProceduralHunts.bind(mob,contract);mob.setCustomName(p.displayName());mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(p.health());mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(p.damage());mob.setHealth((float)p.health());ProgressiveDifficulty.freshSpawn(mob);mob.setHealth(mob.getMaxHealth());return mob;}
 @GameTest(templateName="witchersbestiary:notice_board_arena",batchId="progressive-contracts",tickLimit=60)
 public void allTwelveNaturalAndAllRaritiesUseSharedScalingAndFrozenReload(TestContext c){
  var world=c.getWorld().getServer().getOverworld();long old=world.getTimeOfDay();var owners=List.of(UUID.randomUUID(),UUID.randomUUID());
  try{for(int phase:new int[]{0,2}){world.setTimeOfDay((long)phase*ModConfig.progressive().interval()*24000+18000);
   for(var kind:ContractSpecies.ENTITIES.keySet()){
    var natural=ContractSpecies.type(kind).create(c.getWorld());natural.initialize(c.getWorld(),c.getWorld().getLocalDifficulty(c.getAbsolutePos(new net.minecraft.util.math.BlockPos(4,2,4))),SpawnReason.NATURAL,null);
    if(natural instanceof com.witchersbestiary.entity.monster.WerewolfEntity)natural.tick();ProgressiveDifficulty.freshSpawn(natural);
    double naturalBase=natural.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getBaseValue();double expectedNatural=ProgressiveDifficulty.scaledHealth(natural,naturalBase);
    c.assertTrue(Math.abs(natural.getMaxHealth()-expectedNatural)<.01,"Natural initialization "+kind+" phase"+phase);
    for(var tier:ProceduralProfile.Tier.values()){
     var mob=target(c,kind,tier,owners.get(tier.ordinal()%2));var profile=ProceduralHunts.combat(mob).profile;
     c.assertTrue(ProgressiveDifficulty.eligible(mob)&&ProgressiveDifficulty.steps(mob)==phase,"Contract participates in progression "+kind);
     double expected=ModConfig.progressive().health(profile.health(),phase,ModConfig.progressive().elites().contains("witchersbestiary:"+ContractSpecies.ENTITIES.get(kind)));
     c.assertTrue(Math.abs(mob.getMaxHealth()-expected)<.01&&mob.getHealth()==mob.getMaxHealth(),"Rarity health plus progression initialized once "+kind+" "+tier);
     float damage=ProgressiveDifficulty.damage(c.getWorld().getDamageSources().mobAttack(mob),10);c.assertTrue(Math.abs(damage-10*ModConfig.progressive().damage(phase))<.001,"One outgoing multiplier "+kind);
     mob.setHealth(mob.getMaxHealth()*.6f);float hp=mob.getHealth();var n=new NbtCompound();mob.saveNbt(n);
     world.setTimeOfDay((long)(phase+5)*ModConfig.progressive().interval()*24000+18000);var loaded=(MobEntity)EntityType.loadEntityWithPassengers(n,c.getWorld(),e->e);
     for(int r=0;r<5;r++)ProgressiveDifficulty.refresh(loaded);
     c.assertTrue(ProgressiveDifficulty.steps(loaded)==phase&&Math.abs(loaded.getMaxHealth()-expected)<.01&&Math.abs(loaded.getHealth()-hp)<.02,"Reload neither heals nor stacks nor changes frozen phase "+kind+" tier="+tier+" phase="+ProgressiveDifficulty.steps(loaded)+" expectedMax="+expected+" actualMax="+loaded.getMaxHealth()+" hp="+hp+" loadedHp="+loaded.getHealth());
     c.assertTrue(ProgressiveDifficulty.damage(c.getWorld().getDamageSources().mobAttack(loaded),10)==damage,"Frozen outgoing damage after reload "+kind);
     var saved=new NbtCompound();loaded.saveNbt(saved);c.assertTrue(saved.getLong("WitcherContractProgressivePhase")==phase,"Server phase stored "+kind);
     world.setTimeOfDay((long)phase*ModConfig.progressive().interval()*24000+18000);
    }
   }
  }
  var cow=EntityType.COW.create(c.getWorld());c.assertTrue(!ProgressiveDifficulty.eligible(cow)&&ProgressiveDifficulty.damage(c.getWorld().getDamageSources().mobAttack(cow),10)==10,"No vanilla bystander scaling");
  }finally{world.setTimeOfDay(old);}c.complete();
 }
 @GameTest(templateName="witchersbestiary:notice_board_arena",batchId="progressive-contract-legacy",tickLimit=40)
 public void legacyTargetsKeepZeroPhaseAndKatakanBatKeepsBeastPool(TestContext c){
  var world=c.getWorld().getServer().getOverworld();long old=world.getTimeOfDay();try{
   world.setTimeOfDay((long)2*ModConfig.progressive().interval()*24000+18000);var mob=target(c,"katakan",ProceduralProfile.Tier.EPIC,UUID.randomUUID());double max=mob.getMaxHealth();
   var n=new NbtCompound();mob.saveNbt(n);n.remove("WitcherContractProgressivePhase");mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).removeModifier(ProgressiveDifficulty.HEALTH);mob.setHealth(mob.getMaxHealth());mob.saveNbt(n);n.remove("WitcherContractProgressivePhase");
   var legacy=(MobEntity)EntityType.loadEntityWithPassengers(n,c.getWorld(),e->e);ProgressiveDifficulty.refresh(legacy);c.assertTrue(ProgressiveDifficulty.steps(legacy)==0&&legacy.getMaxHealth()==ProceduralHunts.combat(legacy).profile.health(),"Old unscaled saved targets migrate without surprise buff");
   var katakan=(com.witchersbestiary.entity.monster.KatakanEntity)target(c,"katakan",ProceduralProfile.Tier.EPIC,UUID.randomUUID());katakan.setHealth(25);c.assertTrue(katakan.beginBatForm()&&katakan.getMaxHealth()==8,"Bat decoy remains fixed8 HP");
   var bat=new NbtCompound();katakan.saveNbt(bat);var copy=(com.witchersbestiary.entity.monster.KatakanEntity)EntityType.loadEntityWithPassengers(bat,c.getWorld(),e->e);ProgressiveDifficulty.refresh(copy);c.assertTrue(copy.isBatForm()&&copy.getMaxHealth()==8&&ProgressiveDifficulty.steps(copy)==2&&Math.abs(ProgressiveDifficulty.scaledHealth(copy,ProceduralHunts.combat(copy).profile.health())-max)<.01,"Bat reload preserves scaled beast pool and frozen phase");
  }finally{world.setTimeOfDay(old);}c.complete();
 }
}
