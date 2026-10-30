package com.witchersbestiary.contracts.procedural;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;
/** Generic profile/lifecycle adapter on existing entities, scheduler, contract store and reward service. */
public final class ProceduralHunts {
 private static final Set<Identifier> IDS=ContractSpecies.ENTITIES.keySet().stream().map(ProceduralHunts::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
 private static final Identifier FRENZY=Identifier.of("witchersbestiary:procedural_frenzy");
 public static Identifier id(String species){return Identifier.of("witchersbestiary:procedural_"+species);}
 public static boolean isDefinition(Identifier id){return IDS.contains(id);}
 public static ProceduralCombat combat(Entity e){return e instanceof DrownerEntity d?d.procedural:e instanceof GhoulEntity g?g.procedural:e instanceof FogletEntity f?f.procedural:e instanceof ContractTargetAccess a?a.witcher$combat():null;}
 public static UUID binding(Entity e){return e instanceof DrownerEntity d?d.huntContract:e instanceof GhoulEntity g?g.huntContract:e instanceof FogletEntity f?f.huntContract:e instanceof ContractTargetAccess a?a.witcher$contract():null;}
 public static void bind(MobEntity e,ContractInstance c){var data=new ProceduralCombat(c.procedural);if(e instanceof DrownerEntity d){d.huntContract=c.id();d.procedural=data;d.setBaby(false);}else if(e instanceof GhoulEntity g){g.huntContract=c.id();g.procedural=data;}else if(e instanceof FogletEntity f){f.huntContract=c.id();f.procedural=data;}else {((ContractTargetAccess)e).witcher$bind(c.id(),data);if(e instanceof WerewolfEntity wolf)wolf.prepareContractForm();}}
 public static ContractDefinition snapshot(ContractDefinition d,ProceduralProfile p){int ordinal=p.tier().ordinal();int threat=Math.clamp((int)Math.ceil(p.health()/50),1,6);int emeralds=ContractSpecies.legacy(p.species())?switch(p.tier()){case COMMON->3;case RARE->6;case EPIC->10;}:switch(p.tier()){case COMMON->4+threat/2;case RARE->14+threat;case EPIC->24+2*threat;};return new ContractDefinition(d.id(),d.name(),d.description(),d.target(),1,ordinal+1,new ContractDefinition.Reward((ordinal+1)*30,List.of(new ContractDefinition.ItemReward(Identifier.of("minecraft:emerald"),emeralds))),d.minimumLevel(),d.dimension(),d.durationMillis());}
 public static boolean safe(ServerWorld w,ContractInstance c,BlockPos at){
 boolean habitat=c.procedural.species().equals("drowner")?SwampLord.safe(w,at,true):GraveDevourer.safe(w,at,ContractSpecies.FOREST.contains(c.procedural.species()));if(!habitat)return false;
 var type=ContractSpecies.type(c.procedural.species());double half=type.getWidth()/2.0;var box=new net.minecraft.util.math.Box(at.getX()+.5-half,at.getY(),at.getZ()+.5-half,at.getX()+.5+half,at.getY()+type.getHeight(),at.getZ()+.5+half);for(int x=net.minecraft.util.math.MathHelper.floor(box.minX)>>4;x<=net.minecraft.util.math.MathHelper.floor(box.maxX)>>4;x++)for(int z=net.minecraft.util.math.MathHelper.floor(box.minZ)>>4;z<=net.minecraft.util.math.MathHelper.floor(box.maxZ)>>4;z++)if(w.getChunkManager().getWorldChunk(x,z)==null)return false;return !w.isOutOfHeightLimit(net.minecraft.util.math.BlockPos.ofFloored(box.maxX,box.maxY,box.maxZ))&&w.isSpaceEmpty(box);
 }

 public static boolean spawn(ServerPlayerEntity p,ContractInstance c,BlockPos at){
  var w=p.getServerWorld();var h=c.hunt;if(c.procedural==null||h==null||h.target!=null||c.status()!=ContractInstance.Status.ACCEPTED||!h.dimension.equals(w.getRegistryKey().getValue().toString())||!safe(w,c,at))return false;
  MobEntity e=switch(c.procedural.species()){case "drowner"->ModEntities.DROWNER.create(w);case "ghoul"->ModEntities.GHOUL.create(w);default->ContractSpecies.type(c.procedural.species()).create(w);};if(e==null)return false;
  bind(e,c);e.setPersistent();e.setCustomName(c.procedural.displayName());e.setCustomNameVisible(true);
  e.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(c.procedural.health());e.setHealth((float)c.procedural.health());e.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(c.procedural.damage());
  com.witchersbestiary.combat.ProgressiveDifficulty.freshSpawn(e);
  e.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);var box=e.getBoundingBox();for(int x=net.minecraft.util.math.MathHelper.floor(box.minX)>>4;x<=net.minecraft.util.math.MathHelper.floor(box.maxX)>>4;x++)for(int z=net.minecraft.util.math.MathHelper.floor(box.minZ)>>4;z<=net.minecraft.util.math.MathHelper.floor(box.maxZ)>>4;z++)if(w.getChunkManager().getWorldChunk(x,z)==null)return false;if(!w.isSpaceEmpty(e)||!w.doesNotIntersectEntities(e))return false;
  h.target=e.getUuid();h.position=at.toImmutable();h.status=SwampHunt.Status.RESERVED;ContractState.get(w.getServer()).markDirty();
  if(!w.spawnEntity(e)){h.target=null;h.position=null;h.status=SwampHunt.Status.WAITING;ContractState.get(w.getServer()).markDirty();return false;}h.status=SwampHunt.Status.ACTIVE;com.witchersbestiary.contracts.tracking.Investigations.targetSpawned(c,at);ContractState.get(w.getServer()).markDirty();return true;
 }
 public static void cleanup(MobEntity e,ServerWorld w){if(e instanceof DrownerEntity d)SwampLord.cleanup(d,w);if(e instanceof FogletEntity f)f.clearContractCopies(w);SwampLord.clearBar(e.getUuid());}
 public static void tick(MobEntity e){
  var state=combat(e);if(state==null||!(e.getWorld() instanceof ServerWorld w))return;
  if(!e.isAlive()){cleanup(e,w);return;}
  var c=ContractState.get(w.getServer()).find(binding(e));
  if(c==null||c.procedural==null||!c.procedural.equals(state.profile)||c.hunt==null||!e.getUuid().equals(c.hunt.target)||c.status()!=ContractInstance.Status.ACCEPTED){cleanup(e,w);e.discard();return;}
  if(e.age%10==0)SwampLord.updateBar(e,state.displayName,switch(state.profile.tier()){case COMMON->BossBar.Color.WHITE;case RARE->BossBar.Color.BLUE;case EPIC->BossBar.Color.PURPLE;});
  if(state.poison>0)state.poison--;
  if(e instanceof GhoulEntity g)GraveDevourer.combatTick(g,w,state.profile.has(ContractAbility.FRENZY));
  if(e instanceof DrownerEntity d){
   if(state.profile.has(ContractAbility.FRENZY)){if(!state.rageUsed&&d.getHealth()<d.getMaxHealth()*.4){state.rageUsed=true;state.rage=300;w.playSound(null,d.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_DROWNED_SHOOT,net.minecraft.sound.SoundCategory.HOSTILE,.8f,.5f);}var speed=d.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);if(state.rage>0){state.rage--;if(!speed.hasModifier(FRENZY))speed.addTemporaryModifier(new EntityAttributeModifier(FRENZY,.2,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}else speed.removeModifier(FRENZY);}
   if(!com.witchersbestiary.combat.Stun.active(d)&&!d.isAiDisabled())SwampLord.drownerAbilities(d,w,c,state.profile.has(ContractAbility.CHARGE),state.profile.has(ContractAbility.SUMMONER));
  }
 }
 public static float healthFraction(LivingEntity e){var state=combat(e);if(state!=null&&e instanceof KatakanEntity k&&k.isBatForm())return (float)(k.getStoredBeastHealth()/state.profile.health());return e.getMaxHealth()>0?e.getHealth()/e.getMaxHealth():0;}
 public static boolean frenzy(Entity e){var s=combat(e);return s!=null&&(e instanceof GhoulEntity g?g.huntRage>0:s.rage>0);}
 /** Called only after actual health/absorption damage; rejected attacks never proc. */
 public static void hit(MobEntity source,LivingEntity target){var s=combat(source);if(s==null||source.getWorld().isClient||!s.profile.has(ContractAbility.POISONOUS)||s.poison>0)return;s.poison=100;if(source.getRandom().nextFloat()<.35f)target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,60,0));}
 public static boolean death(LivingEntity victim){
  if(victim instanceof FogletEntity f&&f.huntIllusionParent!=null)return true;
  if(!(victim instanceof MobEntity e)||combat(e)==null||!(e.getWorld() instanceof ServerWorld w))return false;
  var store=ContractState.get(w.getServer());var c=store.find(binding(e));if(c!=null&&c.procedural!=null&&c.hunt!=null&&e.getUuid().equals(c.hunt.target)&&!e.isAlive()&&c.status()==ContractInstance.Status.ACCEPTED){c.hunt.status=SwampHunt.Status.DEAD;c.credit(e.getUuid(),c.owner(),c.definition().target(),System.currentTimeMillis());store.markDirty();}cleanup(e,w);return true;
 }
 public static void loaded(Entity e,ServerWorld w){if(combat(e)==null)return;var store=ContractState.get(w.getServer());var c=store.find(binding(e));if(c!=null&&c.hunt!=null&&e.getUuid().equals(c.hunt.target)&&c.hunt.status==SwampHunt.Status.RESERVED){c.hunt.status=SwampHunt.Status.ACTIVE;store.markDirty();}}
}
