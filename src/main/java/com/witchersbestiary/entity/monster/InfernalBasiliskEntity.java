package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Owner-only six-legged mount; vanilla controlled travel remains responsible for rider packets. */
public final class InfernalBasiliskEntity extends TameableEntity implements GeoEntity, CategorizedMonster, Stunnable {
    // Keep legacy ordinals (especially DEFEND=2) stable for existing saved pets.
    public enum Mode { FOLLOW, WAIT, DEFEND, WANDER }
    private static final TrackedData<Boolean> SADDLED=DataTracker.registerData(InfernalBasiliskEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> STUN=DataTracker.registerData(InfernalBasiliskEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MODE=DataTracker.registerData(InfernalBasiliskEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> EXHAUSTED=DataTracker.registerData(InfernalBasiliskEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final CombatMotion motion=new CombatMotion();
    private int cooldown,actionTick,action,exhaustionCooldown;
    private Vec3d committed=Vec3d.ZERO;
    private java.util.UUID committedTarget;
    private boolean contacted;

    public InfernalBasiliskEntity(EntityType<? extends InfernalBasiliskEntity> type,World world){
        super(type,world);setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.LAVA,0);
        setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.DANGER_FIRE,0);setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.DAMAGE_FIRE,0);
    }
    // AnimalEntity's light/grass pathfinding preference is not a Nether spawn restriction.
    // SpawnRestriction/BasiliskSpawns and the independent collision check enforce habitat safety.
    @Override public boolean canSpawn(net.minecraft.world.WorldAccess world,SpawnReason reason){return true;}
    public static DefaultAttributeContainer.Builder createAttributes(){return createMobAttributes()
        .add(EntityAttributes.GENERIC_MAX_HEALTH,100).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.24)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,10).add(EntityAttributes.GENERIC_FOLLOW_RANGE,24)
        .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.85).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
    @Override protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(SADDLED,false);b.add(STUN,false);b.add(MODE,0);b.add(EXHAUSTED,0);}
    @Override protected void initGoals(){
        goalSelector.add(0,new SitGoal(this));
        goalSelector.add(1,new Goal(){
            {setControls(java.util.EnumSet.of(Control.MOVE,Control.LOOK));}
            public boolean canStart(){return getControllingPassenger()==null&&exhaustedTicks()==0&&!trackedStun()&&mode()!=Mode.WAIT&&getTarget()!=null&&getTarget().isAlive();}
            public boolean shouldContinue(){return canStart();}
            public void tick(){combatTick();}
            public void stop(){getNavigation().stop();cancelCombatForStun();}
        });
        goalSelector.add(3,new FollowOwnerGoal(this,1,6,3){@Override public boolean canStart(){return mode()==Mode.FOLLOW&&getControllingPassenger()==null&&super.canStart();}@Override public boolean shouldContinue(){return mode()==Mode.FOLLOW&&getControllingPassenger()==null&&super.shouldContinue();}});
        goalSelector.add(6,new WanderAroundFarGoal(this,.65){
            private boolean allowed(){return (!isTamed()||mode()==Mode.WANDER)&&getControllingPassenger()==null&&exhaustedTicks()==0&&!trackedStun();}
            @Override public boolean canStart(){return allowed()&&super.canStart();}
            @Override public boolean shouldContinue(){return allowed()&&super.shouldContinue();}
        });
        goalSelector.add(7,new LookAtEntityGoal(this,PlayerEntity.class,10));
        targetSelector.add(1,new TrackOwnerAttackerGoal(this){@Override public boolean canStart(){return mode()==Mode.DEFEND&&getControllingPassenger()==null&&super.canStart();}});
        targetSelector.add(2,new AttackWithOwnerGoal(this){@Override public boolean canStart(){return mode()==Mode.DEFEND&&getControllingPassenger()==null&&super.canStart();}});
        targetSelector.add(3,new RevengeGoal(this){@Override public boolean canStart(){return mode()!=Mode.WAIT&&getControllingPassenger()==null&&super.canStart();}});
        targetSelector.add(4,new ActiveTargetGoal<>(this,PlayerEntity.class,10,true,false,p->!isTamed()&&exhaustedTicks()==0&&p instanceof PlayerEntity hunter&&!hunter.isCreative()&&!hunter.isSpectator()));
    }
    public Mode mode(){return Mode.values()[Math.clamp(dataTracker.get(MODE),0,Mode.values().length-1)];}
    public void setMode(Mode mode){dataTracker.set(MODE,mode.ordinal());setSitting(mode==Mode.WAIT);setInSittingPose(mode==Mode.WAIT);setTarget(null);getNavigation().stop();cancelCombatForStun();}
    public int exhaustedTicks(){return dataTracker.get(EXHAUSTED);}
    public boolean isSaddled(){return dataTracker.get(SADDLED);}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.HYBRID;}
    @Override public PassiveEntity createChild(ServerWorld w,PassiveEntity other){return null;}
    @Override public boolean isBreedingItem(ItemStack stack){return false;}
    @Override public boolean canImmediatelyDespawn(double distance){return !isTamed()&&!hasCustomName();}
    @Override public boolean cannotDespawn(){return isTamed()||super.cannotDespawn();}
    @Override public boolean canTarget(LivingEntity target){return !isOwner(target)&&!(target instanceof TameableEntity pet&&isTamed()&&getOwnerUuid()!=null&&getOwnerUuid().equals(pet.getOwnerUuid()))&&super.canTarget(target);}
    private long lastFeedTick=Long.MIN_VALUE;
    public static int foodHealing(ItemStack stack){
        if(stack.isOf(Items.BLAZE_ROD))return 12;if(stack.isOf(Items.BLAZE_POWDER))return 8;
        if(stack.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.ITEM,net.minecraft.util.Identifier.of("witchersbestiary","basilisk_cooked_meat"))))return 6;
        if(stack.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.ITEM,net.minecraft.util.Identifier.of("witchersbestiary","basilisk_raw_meat"))))return 4;
        return 0;
    }
    @Override public ActionResult interactMob(PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);
        if(isTamed()&&isOwner(player)){
            int healing=foodHealing(stack);
            if(healing>0){
                if(hand==Hand.OFF_HAND&&foodHealing(player.getMainHandStack())>0)return ActionResult.PASS;
                if(!getWorld().isClient&&getHealth()<getMaxHealth()&&lastFeedTick!=getWorld().getTime()){
                    float before=getHealth();heal(healing);
                    if(getHealth()>before){
                        lastFeedTick=getWorld().getTime();if(!player.isCreative())stack.decrement(1);
                        playSound(SoundEvents.ENTITY_GENERIC_EAT,.7f,.75f);
                    }
                }
                return ActionResult.success(getWorld().isClient);
            }
            // Let the normal second-hand pass feed before an empty-main-hand mount/mode action.
            if(hand==Hand.MAIN_HAND&&stack.isEmpty()&&foodHealing(player.getOffHandStack())>0)return ActionResult.PASS;
        }
        if(hand!=Hand.MAIN_HAND)return ActionResult.PASS;
        if(!isTamed()&&stack.isOf(ModItems.MOLTEN_HEART)){
            if(exhaustedTicks()==0){if(!getWorld().isClient)player.sendMessage(Text.translatable("message.witchersbestiary.basilisk_weaken"),true);return ActionResult.FAIL;}
            if(!getWorld().isClient){setTamed(true,true);setOwner(player);setPersistent();setHealth(60);dataTracker.set(EXHAUSTED,0);setMode(Mode.FOLLOW);if(!player.isCreative())stack.decrement(1);getWorld().sendEntityStatus(this,EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);triggerAnim("action","taming");}
            return ActionResult.success(getWorld().isClient);
        }
        if(isTamed()&&isOwner(player)){
            if(stack.isOf(Items.SADDLE)&&!isSaddled()){
                if(!getWorld().isClient){dataTracker.set(SADDLED,true);if(!player.isCreative())stack.decrement(1);playSound(SoundEvents.ENTITY_HORSE_SADDLE,1,.7f);}return ActionResult.success(getWorld().isClient);
            }
            if(stack.isEmpty()&&player.isSneaking()){
                if(!getWorld().isClient){setMode(switch(mode()){case FOLLOW->Mode.WAIT;case WAIT->Mode.WANDER;case WANDER,DEFEND->Mode.FOLLOW;});player.sendMessage(Text.translatable("message.witchersbestiary.basilisk_mode",Text.translatable("mode.witchersbestiary."+mode().name().toLowerCase(java.util.Locale.ROOT))),true);}return ActionResult.success(getWorld().isClient);
            }
            if(stack.isEmpty()&&!hasPassengers()&&exhaustedTicks()==0){if(!getWorld().isClient){setMode(Mode.FOLLOW);player.startRiding(this);}return ActionResult.success(getWorld().isClient);}
        }
        return ActionResult.PASS;
    }
    @Override public LivingEntity getControllingPassenger(){return isTamed()&&getFirstPassenger() instanceof PlayerEntity p&&isOwner(p)?p:null;}
    @Override protected boolean canAddPassenger(Entity e){return !hasPassengers()&&isTamed()&&e instanceof PlayerEntity p&&isOwner(p);}
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger,EntityDimensions dimensions,float scale){return new Vec3d(0,1.65*scale,.15*scale);}
    @Override protected Vec3d getControlledMovementInput(PlayerEntity rider,Vec3d input){return new Vec3d(rider.sidewaysSpeed*.45,0,rider.forwardSpeed*(rider.forwardSpeed<0?.35:1));}
    @Override protected float getSaddledSpeed(PlayerEntity rider){return (float)(getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)*(onLava()?.85:.65));}
    @Override protected void tickControlled(PlayerEntity rider,Vec3d input){
        super.tickControlled(rider,input);getNavigation().stop();setTarget(null);cancelCombatForStun();
        setYaw(rider.getYaw());setPitch(rider.getPitch()*.25f);setBodyYaw(getYaw());setHeadYaw(getYaw());
    }
    @Override public boolean canWalkOnFluid(FluidState fluid){return fluid.isIn(FluidTags.LAVA);}
    public boolean onLava(){return isInLava()||getWorld().getFluidState(BlockPos.ofFloored(getPos().add(0,-.5,0))).isIn(FluidTags.LAVA);}
    @Override public void tickMovement(){
        super.tickMovement();
        // Strider's surface collision/buoyancy, without changing position or evicting the rider.
        if(isInLava()){
            if(net.minecraft.block.ShapeContext.of(this).isAbove(net.minecraft.block.FluidBlock.COLLISION_SHAPE,getBlockPos(),true)&&!getWorld().getFluidState(getBlockPos().up()).isIn(FluidTags.LAVA))setOnGround(true);
            else setVelocity(getVelocity().multiply(.5).add(0,.05,0));
            fallDistance=0;
        }
    }
    @Override public void tick(){
        super.tick();if(getWorld().isClient)return;
        if(cooldown>0)cooldown--;if(exhaustionCooldown>0)exhaustionCooldown--;
        if(exhaustedTicks()>0){dataTracker.set(EXHAUSTED,exhaustedTicks()-1);getNavigation().stop();setTarget(null);setVelocity(0,getVelocity().y,0);}
        else if(!isTamed()&&isAlive()&&getHealth()<=25&&exhaustionCooldown==0){dataTracker.set(EXHAUSTED,160);exhaustionCooldown=600;setTarget(null);getNavigation().stop();cancelCombatForStun();triggerAnim("action","taming");}
        if(getControllingPassenger()!=null){getNavigation().stop();setTarget(null);}
    }
    private void combatTick(){
        var target=getTarget();if(target==null)return;motion.sample(this,target);
        if(action==0){
            if(CombatMotion.distance(this,target)>3.2||!CombatMotion.meleeHeight(this,target)||!canSee(target)){
                if(age%6==0){Vec3d lead=motion.lead(target,5,.8);getNavigation().startMovingTo(lead.x,lead.y,lead.z,1.15);}CombatMotion.face(this,target.getPos(),10);return;
            }
            if(cooldown>0){CombatMotion.face(this,target.getPos(),8);return;}
            Vec3d delta=target.getPos().subtract(getPos()).multiply(1,0,1).normalize();action=delta.dotProduct(Vec3d.fromPolar(0,getYaw()))<-.25?2:random.nextInt(5)==0?3:1;
            actionTick=0;contacted=false;committedTarget=target.getUuid();committed=Vec3d.fromPolar(0,getYaw());getNavigation().stop();triggerAnim("action",action==2?"tail":action==3?"roar":"bite");
        }
        actionTick++;setVelocity(0,getVelocity().y,0);getNavigation().stop();
        if(actionTick<=6&&action!=2)committed=CombatMotion.face(this,motion.lead(target,3,.4),9);else CombatMotion.holdFacing(this,committed);
        int impact=action==2?14:action==3?18:12;
        if(!contacted&&actionTick>=impact&&actionTick<=impact+2&&target.getUuid().equals(committedTarget)){
            Vec3d direction=action==2?committed.negate():committed;
            if(CombatMotion.contact(this,target,direction,action==2?3.6:3.2,action==2?0:.35)){
                contacted=true;boolean hit=target.damage(getDamageSources().mobAttack(this),action==2?8:action==3?3:10);
                if(hit)target.takeKnockback(action==1?.35:.8,getX()-target.getX(),getZ()-target.getZ());
            }
        }
        if(actionTick==impact)playSound(action==3?SoundEvents.ENTITY_RAVAGER_ROAR:SoundEvents.ENTITY_GENERIC_BIG_FALL,.8f,.65f);
        if(actionTick>=impact+14){action=0;cooldown=actionTick+12;}
    }
    @Override public boolean damage(DamageSource source,float amount){boolean result=super.damage(source,amount);if(result&&isAlive()&&action==0&&exhaustedTicks()==0)triggerAnim("action","hurt");return result;}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("BasiliskSaddled",isSaddled());n.putInt("BasiliskMode",mode().ordinal());n.putInt("BasiliskExhausted",exhaustedTicks());n.putInt("BasiliskExhaustionCooldown",exhaustionCooldown);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(SADDLED,n.getBoolean("BasiliskSaddled"));setMode(Mode.values()[Math.clamp(n.getInt("BasiliskMode"),0,Mode.values().length-1)]);dataTracker.set(EXHAUSTED,Math.clamp(n.getInt("BasiliskExhausted"),0,160));exhaustionCooldown=Math.clamp(n.getInt("BasiliskExhaustionCooldown"),0,600);}
    @Override public CombatState getCombatState(){return action==0?getTarget()==null?CombatState.IDLE:CombatState.CHASE:actionTick<12?CombatState.ATTACK_WINDUP:actionTick<17?CombatState.ATTACK_ACTIVE:CombatState.ATTACK_RECOVERY;}
    @Override public void cancelCombatForStun(){action=0;actionTick=0;committedTarget=null;cooldown=Math.max(cooldown,15);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN,value);}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<>(this,"movement",3,state->{
            if(!isAlive())return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.infernal_basilisk.death"));
            String clip=!isAlive()?"death":exhaustedTicks()>0?"taming":onLava()?"lava_swim":hasPassengers()?state.isMoving()?"mounted_move":"mounted_idle":state.isMoving()?getTarget()!=null?"run":"walk":"idle";
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.infernal_basilisk."+clip));
        }));
        var attack=new AnimationController<>(this,"action",0,state->PlayState.STOP);
        for(String name:new String[]{"bite","tail","roar","hurt","taming"})attack.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.infernal_basilisk."+name));controllers.add(attack);
    }
}
