package com.witchersbestiary.lure;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;

public final class LureBlockEntity extends BlockEntity {
 private UUID nonce;
 public LureBlockEntity(BlockPos pos,BlockState state){super(ModLures.BLOCK_ENTITY,pos,state);}
 public UUID nonce(){return nonce;}
 public void arm(LureState.Entry e){nonce=e.nonce;markDirty();}
 private LureState.Entry entry(ServerWorld w){var e=nonce==null?null:LureState.get(w).get(nonce);return e!=null&&e.matches(w,pos)&&e.kind==((LureBlock)getCachedState().getBlock()).kind?e:null;}
 public static void tick(World world,BlockPos pos,BlockState state,LureBlockEntity be){if(!(world instanceof ServerWorld w)||w.getTime()%20!=0||be.nonce==null)return;
  var e=be.entry(w);if(e==null||e.status!=0){w.removeBlock(pos,false);return;}long now=LureState.now(w);
  if(now<e.due){be.atmosphere(w,e,now);return;}var owner=w.getPlayerByUuid(e.owner);if(owner==null||!owner.isAlive()||owner.isSpectator()||owner.squaredDistanceTo(Vec3d.ofCenter(pos))>64*64)return;
  be.atmosphere(w,e,now);String error=e.kind.habitatError(w,pos);if(error!=null||!LurePlacement.support(w,pos)){be.refund(w,true);w.removeBlock(pos,false);return;}
  var nearby=w.getEntitiesByClass(PathAwareEntity.class,new Box(pos).expand(96),m->m.getType()==e.kind.type()&&m.isAlive()&&!m.isAiDisabled());nearby.sort(Comparator.comparingDouble(m->m.squaredDistanceTo(Vec3d.ofCenter(pos))));
  if(!nearby.isEmpty()){
   var beast=nearby.getFirst();if(beast.getTarget()!=null&&beast.getTarget()!=owner&&beast.getTarget().isAlive()){be.refund(w,true);w.removeBlock(pos,false);return;}
   // A nearby but sealed-off monster is not a guaranteed encounter.
   if(!(beast instanceof com.witchersbestiary.entity.monster.GriffinEntity g&&g.isFlying())){
    var path=beast.getNavigation().findPathTo(pos,2);
    if(path==null||!path.reachesTarget()){be.refund(w,true);w.removeBlock(pos,false);return;}
   }
   if(LureState.get(w).finish(e,beast,now)){be.arrival(w,e);w.removeBlock(pos,false);}return;
  }
  var spawn=LurePlacement.find(w,pos,e.kind);if(spawn==null){be.refund(w,true);w.removeBlock(pos,false);return;}
  var beast=e.kind.type().create(w);if(beast==null){be.refund(w,true);w.removeBlock(pos,false);return;}
  // One stable UUID per reservation, assigned before insertion. No second attempt after a terminal status.
  beast.setUuid(e.nonce);beast.refreshPositionAndAngles(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);beast.initialize(w,w.getLocalDifficulty(spawn),SpawnReason.TRIGGERED,null);
  if(!beast.canSpawn((net.minecraft.world.WorldView)w)){be.refund(w,true);w.removeBlock(pos,false);return;}
  // The server thread cannot save between these synchronous operations; rollback a rejected insertion.
  if(w.getEntity(e.nonce)!=null){if(w.getEntity(e.nonce).getType()==e.kind.type()){LureState.get(w).finish(e,(PathAwareEntity)w.getEntity(e.nonce),now);be.arrival(w,e);}else be.refund(w,true);w.removeBlock(pos,false);return;}
  if(w.spawnEntity(beast)){LureState.get(w).finish(e,beast,now);be.arrival(w,e);w.removeBlock(pos,false);}else{be.refund(w,true);w.removeBlock(pos,false);}
 }
 public void refund(ServerWorld w,boolean tell){var e=entry(w);if(e==null||!LureState.get(w).refund(e))return;Block.dropStack(w,pos,new ItemStack(ModLures.item(e.kind)));if(tell){var p=w.getPlayerByUuid(e.owner);if(p!=null)p.sendMessage(Text.translatable("message.witchersbestiary.lure.refund"),true);}}
 private void arrival(ServerWorld w,LureState.Entry e){w.playSound(null,pos,switch(e.kind){case LESHEN->SoundEvents.ENTITY_WOLF_GROWL;case FIEND->SoundEvents.ENTITY_RAVAGER_ROAR;case GRIFFIN->SoundEvents.ENTITY_PHANTOM_AMBIENT;},SoundCategory.HOSTILE,1.5f,e.kind==LureKind.GRIFFIN?1.5f:.6f);var p=w.getPlayerByUuid(e.owner);if(p!=null)p.sendMessage(Text.translatable("message.witchersbestiary.lure.arrival"),true);}
 private void atmosphere(ServerWorld w,LureState.Entry e,long now){long age=now-e.created,left=e.due-now;boolean finalPhase=left<=400;int period=finalPhase?20:40;
  if(now%period==0){var particle=e.kind==LureKind.GRIFFIN?ParticleTypes.WHITE_ASH:e.kind==LureKind.LESHEN?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.CRIMSON_SPORE;w.spawnParticles(particle,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,finalPhase?8:2,.35,.7,.35,.015);}
  if(age>=1200&&now%(finalPhase?40:120)==0){SoundEvent sound=switch(e.kind){case LESHEN->finalPhase?SoundEvents.BLOCK_CHEST_OPEN:SoundEvents.BLOCK_WOOD_STEP;case FIEND->finalPhase?SoundEvents.ENTITY_RAVAGER_ROAR:SoundEvents.ENTITY_RAVAGER_STEP;case GRIFFIN->finalPhase?SoundEvents.ENTITY_PHANTOM_AMBIENT:SoundEvents.ENTITY_ENDER_DRAGON_FLAP;};w.playSound(null,pos,sound,SoundCategory.HOSTILE,finalPhase?1.2f:.5f,e.kind==LureKind.GRIFFIN?1.3f:.7f);}
 }
 @Override protected void writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup r){super.writeNbt(n,r);if(nonce!=null)n.putUuid("LureNonce",nonce);}
 @Override protected void readNbt(NbtCompound n,RegistryWrapper.WrapperLookup r){super.readNbt(n,r);nonce=n.containsUuid("LureNonce")?n.getUuid("LureNonce"):null;}
}
