package com.witchersbestiary.fist;

import net.minecraft.entity.*;
import com.witchersbestiary.entity.ai.CompanionDuty;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.*;
import java.util.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** Neutral territorial fighter. All combat effects are resolved on the server. */
public final class RockTrollEntity extends FistNpc implements Tameable {
 private static final TrackedData<Optional<UUID>> OWNER=DataTracker.registerData(RockTrollEntity.class,TrackedDataHandlerRegistry.OPTIONAL_UUID);
 private final Map<UUID,Long> victories=new HashMap<>();
 private final CompanionDuty duty = new CompanionDuty();
 private java.util.UUID alertedTarget;
 private int attackTicks, recovery, lastAttack=-1, attack=-1;
 public RockTrollEntity(EntityType<? extends net.minecraft.entity.mob.PathAwareEntity> type,World world){super(type,world,2);}
 @Override public void configureBout(FistFightSession fight){super.configureBout(fight);fight.player.maxStamina=140;fight.player.stamina=140;fight.opponent.quickContact=8;fight.opponent.heavyContact=18;fight.opponent.quickLength=20;fight.opponent.heavyLength=36;fight.opponent.health=180;fight.opponent.maxStamina=140;fight.opponent.stamina=140;fight.opponent.heavyDamageMultiplier=1.4;}
 @Override public String fightPose(FistFightSession.Action action){return (action==FistFightSession.Action.QUICK||action==FistFightSession.Action.HEAVY?"wild_":"")+super.fightPose(action);}
 @Override public int winReward(){return 90;}
 @Override protected void initDataTracker(DataTracker.Builder builder) {
  super.initDataTracker(builder);
  builder.add(OWNER,Optional.empty());
 }
 @Override public UUID getOwnerUuid(){return dataTracker.get(OWNER).orElse(null);}
 @Override public boolean canDuel(){return getOwnerUuid()==null;}
 @Override protected boolean canWander(){return canDuel();}
 @Override protected boolean shouldDropLoot(){return canDuel()&&super.shouldDropLoot();}
 @Override protected void initGoals(){
  super.initGoals();
  targetSelector.add(1,new net.minecraft.entity.ai.goal.RevengeGoal(this){
   @Override public boolean canStart(){return canDuel()&&super.canStart();}
   @Override public boolean shouldContinue(){return canDuel()&&super.shouldContinue();}
  });
 }
 void recognizeWinner(ServerPlayerEntity player){
  if(!canDuel())return;
  victories.values().removeIf(expiry->expiry<=getWorld().getTime());
  victories.put(player.getUuid(),getWorld().getTime()+6000);
  player.sendMessage(Text.translatable("troll.witchersbestiary.recognized"),false);
 }
    private final com.witchersbestiary.entity.PetFeeding feedingCooldown = new com.witchersbestiary.entity.PetFeeding();
 @Override protected ActionResult interactMob(PlayerEntity player,Hand hand){
  if(!FistFights.busy(player)&&!FistFights.busy(this)){
   var feeding=feedingCooldown.interact(this,player,hand,getOwnerUuid(),
       com.witchersbestiary.entity.PetFeeding::meat);
   if(feeding!=null)return feeding;
  }
  if(hand!=Hand.MAIN_HAND||!player.getStackInHand(hand).isEmpty()||!player.canInteractWithEntity(this,0)
      ||player.isSpectator()||!isAlive())return ActionResult.PASS;
  if(getWorld().isClient)return ActionResult.SUCCESS;
  if(FistFights.busy(player)||FistFights.busy(this))return ActionResult.PASS;
  if(canDuel()){
   if(getTarget()!=null)return ActionResult.PASS;
   long expiry=victories.getOrDefault(player.getUuid(),0L);
   if(expiry>getWorld().getTime()){
    if(player.isSneaking()){
     dataTracker.set(OWNER,Optional.of(player.getUuid()));
     victories.clear();
     resetAttack();
     ((ServerWorld)getWorld()).spawnParticles(net.minecraft.particle.ParticleTypes.HEART,getX(),getBodyY(.9),getZ(),7,.45,.25,.45,.02);
     player.sendMessage(Text.translatable("troll.witchersbestiary.tamed"),true);
    }else player.sendMessage(Text.translatable("troll.witchersbestiary.recognized"),true);
    return ActionResult.CONSUME;
   }
   return super.interactMob(player,hand);
  }
  if(!player.getUuid().equals(getOwnerUuid()))return ActionResult.PASS;
  if(!player.isSneaking()){
   duty.cycle(this);
   setTarget(null);
   resetAttack();
  }
  var mode=CompanionDuty.label(duty.mode());
  player.sendMessage(player.isSneaking()?Text.translatable("troll.witchersbestiary.status",getDisplayName(),player.getDisplayName(),mode):mode,true);
  return ActionResult.CONSUME;
 }
 private void resetAttack(){
  attack=-1;attackTicks=0;alertedTarget=null;getNavigation().stop();pose("idle",0);
 }
 private boolean companionTarget(LivingEntity target){
  if(duty.guarding())return duty.guardTarget(this,getOwner() instanceof PlayerEntity p?p:null,target);
  return !duty.retreating() && !duty.waiting() && getOwner() instanceof PlayerEntity owner && owner.isAlive() && !owner.isSpectator()
      && squaredDistanceTo(owner)<=256 && owner.squaredDistanceTo(target)<=256
      && target instanceof HostileEntity && target.isAlive()
      && !(target instanceof Tameable pet && pet.getOwnerUuid()!=null)
      && !isTeammate(target) && !owner.isTeammate(target) && !FistFights.busy(owner) && !FistFights.busy(target);
 }
 private void companionTick(ServerWorld world){
  if(getTarget()!=null&&!companionTarget(getTarget())){setTarget(null);resetAttack();}
  if(!(getOwner() instanceof PlayerEntity owner)||!owner.isAlive()||owner.isSpectator()||duty.waiting()){
   getNavigation().stop();
   return;
  }
  if(getTarget()==null&&!FistFights.busy(owner)){
   var attacker=owner.getAttacker();
   var victim=owner.getAttacking();
   if(attacker!=null&&owner.age-owner.getLastAttackedTime()<=100&&companionTarget(attacker))setTarget(attacker);
   else if(victim!=null&&owner.age-owner.getLastAttackTime()<=100&&companionTarget(victim))setTarget(victim);
  }
  if(getTarget()!=null||age%10!=0)return;
  double distance=squaredDistanceTo(owner);
  if(distance>=256&&catchUp(world,owner))return;
  if(distance>25)getNavigation().startMovingTo(owner,1.1);
  else getNavigation().stop();
 }
 private boolean catchUp(ServerWorld world,PlayerEntity owner){
  for(int i=0;i<12;i++){
   int x=random.nextBetween(-5,5),z=random.nextBetween(-5,5);
   if(Math.abs(x)<3&&Math.abs(z)<3)continue;
   var pos=owner.getBlockPos().add(x,random.nextBetween(-1,1),z);
   if(!world.isChunkLoaded(pos.add(-2,0,-2))||!world.isChunkLoaded(pos.add(2,0,2))
       ||!world.isChunkLoaded(pos.add(-2,0,2))||!world.isChunkLoaded(pos.add(2,0,-2))
       ||LandPathNodeMaker.getLandNodeType(this,pos)!=PathNodeType.WALKABLE)continue;
   var destination=Vec3d.ofBottomCenter(pos);
   var box=getBoundingBox().offset(destination.subtract(getPos()));
   if(!world.getWorldBorder().contains(box)||!world.isSpaceEmpty(this,box)||world.containsFluid(box)
       ||!world.getOtherEntities(this,box).isEmpty())continue;
   refreshPositionAfterTeleport(destination);getNavigation().stop();return true;
  }
  return false;
 }
 @Override public void writeCustomDataToNbt(NbtCompound n){
  super.writeCustomDataToNbt(n);
  if(!canDuel()){n.putUuid("TrollOwner",getOwnerUuid());duty.write(n,"Troll");}
  else {
   var rights=new NbtCompound();
   victories.forEach((id,expiry)->{if(expiry>getWorld().getTime())rights.putLong(id.toString(),expiry);});
   n.put("TrollVictories",rights);
  }
 }
 @Override public void readCustomDataFromNbt(NbtCompound n){
  super.readCustomDataFromNbt(n);
  dataTracker.set(OWNER,n.containsUuid("TrollOwner")?Optional.of(n.getUuid("TrollOwner")):Optional.empty());
  duty.read(n,"Troll");
  victories.clear();
  if(canDuel()){
   var rights=n.getCompound("TrollVictories");
   for(String key:rights.getKeys())try{victories.put(UUID.fromString(key),rights.getLong(key));}catch(IllegalArgumentException ignored){}
  }
 }
 @Override public boolean damage(DamageSource source,float amount){if(!getWorld().isClient&&!FistFights.busy(this)&&pose().equals("block")&&source.getAttacker()!=null){var direction=source.getAttacker().getPos().subtract(getPos()).normalize();if(getRotationVec(1).dotProduct(direction)>.1)amount*=.3f;}return super.damage(source,amount);}
 @Override public void tick(){
  super.tick();if(!(getWorld() instanceof ServerWorld world)||!isAlive())return;
  if(FistFights.busy(this)){attack=-1;attackTicks=0;recovery=30;setTarget(null);return;}
  if(!canDuel()){
   if(isAiDisabled()||com.witchersbestiary.signs.SignManager.calmed(this)){setTarget(null);resetAttack();return;}
   var owner=getOwner() instanceof PlayerEntity p?p:null;
   if(duty.emergency(world,this,owner,this::resetAttack))return;
   if(duty.guarding())duty.guardTick(world,this,owner,this::resetAttack);
   else companionTick(world);
  }
  var target=getTarget();if(target==null||!target.isAlive()||squaredDistanceTo(target)>1024){setTarget(null);alertedTarget=null;if(attack>=0)pose("idle",0);attack=-1;attackTicks=0;return;}
  if(!target.getUuid().equals(alertedTarget)){alertedTarget=target.getUuid();recovery=canDuel()?20:5;getNavigation().stop();pose("alert",20);}
  getLookControl().lookAt(target,25,25);
  if(recovery>0){recovery--;if(!canDuel()&&squaredDistanceTo(target)>16&&age%5==0)duty.pursue(this,target,1.1);return;}
  if(attack<0){double d=squaredDistanceTo(target);if(d>144||d>16&&(lastAttack==2||!canSee(target))){duty.pursue(this,target,.85);return;}int choices=canDuel()?5:4;int choice=random.nextInt(choices);if(choice==lastAttack)choice=(choice+1)%choices;attack=d>16?2:choice;lastAttack=attack;attackTicks=0;getNavigation().stop();pose(new String[]{"wild_quick","wild_heavy","throw","stomp","block"}[attack],0);}
  attackTicks++;
  int contact=attack==0?8:attack==1?18:attack==2?20:attack==3?22:30;
  if(attackTicks==contact&&attack!=4){
   if(attack==2){if(canSee(target)){var rock=new net.minecraft.entity.projectile.thrown.SnowballEntity(getWorld(),this){@Override protected void onEntityHit(net.minecraft.util.hit.EntityHitResult hit){if(getWorld().isClient||!(getOwner() instanceof RockTrollEntity troll))return;
    if(!troll.canDuel()&&(!(hit.getEntity() instanceof LivingEntity victim)||!troll.companionTarget(victim)))return;
    if(troll.canDuel())super.onEntityHit(hit);
    hit.getEntity().damage(getDamageSources().thrown(this,troll),troll.canDuel()?8:5);}};rock.setItem(new net.minecraft.item.ItemStack(net.minecraft.item.Items.COBBLESTONE));var v=target.getEyePos().subtract(rock.getPos());rock.setVelocity(v.x,v.y+v.horizontalLength()*.15,v.z,1.2f,2);getWorld().spawnEntity(rock);}}
   else if(attack==3){for(var e:getWorld().getEntitiesByClass(LivingEntity.class,getBoundingBox().expand(4,1,4),e->e!=this&&!(e instanceof RockTrollEntity)&&(canDuel()||companionTarget(e)))){if(e.isOnGround()&&canSee(e)&&(e.damage(getDamageSources().mobAttack(this),canDuel()?8:5)||canDuel())){e.takeKnockback(.8,getX()-e.getX(),getZ()-e.getZ());}}}
   else if(squaredDistanceTo(target)<16&&canSee(target)){target.damage(getDamageSources().mobAttack(this),attack==0?6:canDuel()?14:9);target.takeKnockback(attack==0?.3:1,getX()-target.getX(),getZ()-target.getZ());}
  }
  if(attackTicks>=contact+16){attack=-1;recovery=canDuel()?20+random.nextInt(20):10+random.nextInt(8);pose("stance",0);}
 }
}
