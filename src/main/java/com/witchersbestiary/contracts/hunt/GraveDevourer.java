package com.witchersbestiary.contracts.hunt;

import com.witchersbestiary.contracts.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.GhoulEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Named profile on the existing ghoul; lifecycle uses the common contract and search scheduler. */
public final class GraveDevourer {
 public static final Identifier ID=Identifier.of("witchersbestiary:grave_devourer");
 private static final Identifier RAGE=Identifier.of("witchersbestiary:devourer_rage");
 public static boolean safe(ServerWorld w,BlockPos at,boolean habitat){
  if(!SwampLord.safe(w,at,false,false))return false;
  // Forest canopy is permitted; buildings and underground cavities are not.
  int top=w.getChunkManager().getWorldChunk(at.getX()>>4,at.getZ()>>4).sampleHeightmap(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,at.getX()&15,at.getZ()&15);
  if(top>at.getY()+2&&!w.getBlockState(new BlockPos(at.getX(),top,at.getZ())).isIn(BlockTags.LEAVES))return false;
  return !habitat||w.getBiome(at).isIn(BiomeTags.IS_FOREST);
 }
 public static boolean spawn(ServerPlayerEntity p,ContractInstance c,BlockPos at){
  var w=p.getServerWorld();var h=c.hunt;
  if(!c.definition().id().equals(ID)||h==null||h.target!=null||c.status()!=ContractInstance.Status.ACCEPTED||!w.getRegistryKey().getValue().toString().equals(h.dimension)||!safe(w,at,true))return false;
  var g=ModEntities.GHOUL.create(w);if(g==null)return false;
  g.huntContract=c.id();g.setPersistent();g.setCustomName(Text.translatable("hunt.witchersbestiary.grave_devourer"));g.setCustomNameVisible(true);
  g.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(120);g.setHealth(120);
  g.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
  g.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(com.witchersbestiary.config.ModConfig.speed(.43));
  g.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(.65);
  com.witchersbestiary.combat.ProgressiveDifficulty.freshSpawn(g);
  g.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
  if(!w.doesNotIntersectEntities(g))return false;
  h.target=g.getUuid();h.position=at.toImmutable();h.status=SwampHunt.Status.RESERVED;ContractState.get(w.getServer()).markDirty();
  if(!w.spawnEntity(g)){h.target=null;h.position=null;h.status=SwampHunt.Status.WAITING;ContractState.get(w.getServer()).markDirty();return false;}
  h.status=SwampHunt.Status.ACTIVE;com.witchersbestiary.contracts.tracking.Investigations.targetSpawned(c,at);ContractState.get(w.getServer()).markDirty();return true;
 }
 public static void tick(GhoulEntity g){
  if(g.procedural!=null){com.witchersbestiary.contracts.procedural.ProceduralHunts.tick(g);return;}
  if(g.huntContract==null||!(g.getWorld() instanceof ServerWorld w))return;
  var c=ContractState.get(w.getServer()).find(g.huntContract);
  if(!g.isAlive()){SwampLord.clearBar(g.getUuid());return;}
  if(c==null||!c.definition().id().equals(ID)||c.hunt==null||!g.getUuid().equals(c.hunt.target)||c.status()!=ContractInstance.Status.ACCEPTED){SwampLord.clearBar(g.getUuid());g.discard();return;}
  SwampLord.updateBar(g,"hunt.witchersbestiary.grave_devourer",BossBar.Color.RED);
  combatTick(g,w,true);
 }
 public static void combatTick(GhoulEntity g,ServerWorld w,boolean frenzy){
  if(g.huntLeap>0)g.huntLeap--;if(g.huntFeed>0)g.huntFeed--;
  if(frenzy&&!g.huntRageUsed&&g.getHealth()<g.getMaxHealth()*.4f){g.huntRageUsed=true;g.huntRage=300;w.playSound(null,g.getBlockPos(),SoundEvents.ENTITY_RAVAGER_ROAR,SoundCategory.HOSTILE,.8f,.7f);}
  var speed=g.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
  if(g.huntRage>0){g.huntRage--;if(!speed.hasModifier(RAGE))speed.addTemporaryModifier(new EntityAttributeModifier(RAGE,.20,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
  else speed.removeModifier(RAGE);
 }
 public static void feed(GhoulEntity g){
  if(g.procedural!=null&&!g.procedural.profile.has(com.witchersbestiary.contracts.procedural.ContractAbility.LIFESTEAL))return;
  if(g.huntContract==null||g.getWorld().isClient||!g.isAlive()||g.huntFeed>0||g.huntHealed>=12)return;
  float amount=Math.min(2,Math.min(12-g.huntHealed,g.getMaxHealth()-g.getHealth()));
  if(amount>0){g.heal(amount);g.huntHealed+=amount;g.huntFeed=80;}
 }
 public static boolean death(LivingEntity e){
  if(!(e instanceof GhoulEntity g)||g.huntContract==null||!(g.getWorld() instanceof ServerWorld w))return false;
  var store=ContractState.get(w.getServer());var c=store.find(g.huntContract);
  if(c!=null&&c.definition().id().equals(ID)&&c.hunt!=null&&g.getUuid().equals(c.hunt.target)&&!g.isAlive()&&c.status()==ContractInstance.Status.ACCEPTED){c.hunt.status=SwampHunt.Status.DEAD;c.credit(g.getUuid(),c.owner(),c.definition().target(),System.currentTimeMillis());store.markDirty();}
  SwampLord.clearBar(g.getUuid());return true;
 }
 public static void loaded(Entity e,ServerWorld w){if(e instanceof GhoulEntity g&&g.huntContract!=null){var store=ContractState.get(w.getServer());var c=store.find(g.huntContract);if(c!=null&&c.hunt!=null&&g.getUuid().equals(c.hunt.target)&&c.hunt.status==SwampHunt.Status.RESERVED){c.hunt.status=SwampHunt.Status.ACTIVE;store.markDirty();}}}
 public static void write(GhoulEntity g,NbtCompound n){if(g.huntContract==null)return;n.putUuid("WbGraveHunt",g.huntContract);n.putInt("WbGraveLeap",g.huntLeap);n.putInt("WbGraveRage",g.huntRage);n.putBoolean("WbGraveRageUsed",g.huntRageUsed);n.putInt("WbGraveFeed",g.huntFeed);n.putFloat("WbGraveHealed",g.huntHealed);}
 public static void read(GhoulEntity g,NbtCompound n){g.huntContract=n.containsUuid("WbGraveHunt")?n.getUuid("WbGraveHunt"):null;g.huntLeap=Math.clamp(n.getInt("WbGraveLeap"),0,240);g.huntRage=Math.clamp(n.getInt("WbGraveRage"),0,300);g.huntRageUsed=n.getBoolean("WbGraveRageUsed");g.huntFeed=Math.clamp(n.getInt("WbGraveFeed"),0,80);g.huntHealed=Math.max(0,Math.min(12,n.getFloat("WbGraveHealed")));}
}
