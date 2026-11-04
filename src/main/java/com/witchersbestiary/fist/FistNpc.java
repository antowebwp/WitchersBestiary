package com.witchersbestiary.fist;
import net.minecraft.entity.*;import net.minecraft.entity.mob.PathAwareEntity;import net.minecraft.entity.data.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.nbt.NbtCompound;import net.minecraft.world.World;import net.minecraft.util.*;
import software.bernie.geckolib.animatable.GeoEntity;import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;import software.bernie.geckolib.animation.*;
public class FistNpc extends PathAwareEntity implements GeoEntity{
 private static final TrackedData<String> POSE=DataTracker.registerData(FistNpc.class,TrackedDataHandlerRegistry.STRING);public final int tier;public boolean paid;private int poseTimer;private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
 public FistNpc(EntityType<? extends PathAwareEntity> type,World w,int tier){super(type,w);this.tier=tier;setPersistent();}
 protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(POSE,"idle");}
 protected void initGoals(){goalSelector.add(1,new net.minecraft.entity.ai.goal.SwimGoal(this));goalSelector.add(6,new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this,.55){public boolean canStart(){return getTarget()==null&&poseTimer==0&&!FistFights.invited(FistNpc.this)&&!FistFights.busy(FistNpc.this)&&super.canStart();}});}
 protected ActionResult interactMob(PlayerEntity player,Hand hand){if(player instanceof net.minecraft.server.network.ServerPlayerEntity p){if(p.isSneaking())FistFights.confirm(p,this);else FistFights.invite(p,this);}return ActionResult.SUCCESS;}
 public void configureBout(FistFightSession fight){fight.opponent.health=70+tier*25;fight.opponent.maxStamina=75+tier*12;fight.opponent.stamina=fight.opponent.maxStamina;}
 public String fightPose(FistFightSession.Action action){return action.name().toLowerCase(java.util.Locale.ROOT).replace("idle","stance");}
 public int winReward(){return new int[]{12,28,60}[tier];}
 public void pose(String value,int ticks){if(!dataTracker.get(POSE).equals(value))dataTracker.set(POSE,value);poseTimer=ticks;}
 public String pose(){return dataTracker.get(POSE);}
 public void tick(){super.tick();if(!getWorld().isClient&&!FistFights.busy(this)&&poseTimer>0&&--poseTimer==0)pose("idle",0);}
 public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("FistFirstWinPaid",paid);}
 public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);paid=n.getBoolean("FistFirstWinPaid");pose("idle",0);}
 public void registerControllers(AnimatableManager.ControllerRegistrar r){r.add(new AnimationController<>(this,"body",2,event->{String state=pose();if(this instanceof RockTrollEntity&&isDead())return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.fighter.death"));if((state.equals("idle")||state.equals("stance"))&&event.isMoving())state=state.equals("idle")?"walk":"combat_walk";return event.setAndContinue((state.startsWith("wild_")||state.equals("throw")||state.equals("stomp")||state.equals("alert")||state.equals("death"))?RawAnimation.begin().thenPlayAndHold("animation.fighter."+state):state.equals("knockout")?RawAnimation.begin().thenPlayAndHold("animation.fighter.knockout"):RawAnimation.begin().thenLoop("animation.fighter."+state));}));}
 public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
