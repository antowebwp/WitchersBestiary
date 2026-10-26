package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.ai.CombatMotion;
import com.witchersbestiary.entity.projectile.MudClodEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.control.AquaticMoveControl;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Amphibious local predator. Actions commit to fixed server contact ticks, not render callbacks. */
public final class WaterHagEntity extends HostileEntity implements CategorizedMonster,GeoEntity,Stunnable {
    private static final TrackedData<Boolean> HIDING=DataTracker.registerData(WaterHagEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> STUN=DataTracker.registerData(WaterHagEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    public enum Action { NONE, CLAW_ATTACK, MUD_THROW, AMBUSH }
    private Action action=Action.NONE;
    private int actionTick,attackCooldown,mudCooldown,ambushCooldown;
    private boolean combo;
    private Vec3d direction=Vec3d.ZERO;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public WaterHagEntity(EntityType<? extends HostileEntity> type,World world){
        super(type,world);experiencePoints=8;
        moveControl=new AquaticMoveControl(this,85,10,1.3f,1f,false);
        setPathfindingPenalty(PathNodeType.WATER,0);setPathfindingPenalty(PathNodeType.WATER_BORDER,0);
    }
    @Override protected EntityNavigation createNavigation(World w){var nav=new AmphibiousSwimNavigation(this,w);nav.setCanSwim(true);return nav;}
    @Override public void travel(Vec3d input){
        if(isLogicalSideForUpdatingMovement()&&isTouchingWater()){
            // Fluid acceleration is a fraction of walking speed, not a full land impulse each tick.
            updateVelocity(Math.min(.03f,getMovementSpeed()*.1f),input);move(MovementType.SELF,getVelocity());setVelocity(getVelocity().multiply(.82));
            if(getTarget()!=null&&getTarget().getY()>getY()+.4)setVelocity(getVelocity().add(0,.018,0));
        }else super.travel(input);
    }
    // NaturalSpawns owns habitat/density/geometry. PathAwareEntity's negative
    // daylight path favor must not silently veto these two day-active necrophages.
    @Override public boolean canSpawn(net.minecraft.world.WorldAccess world,net.minecraft.entity.SpawnReason reason){return world.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL;}
    @Override public boolean canSpawn(WorldView world){return world.doesNotIntersectEntities(this)&&world.isSpaceEmpty(getBoundingBox());}
    @Override protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(HIDING,false);b.add(STUN,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN);}
    @Override public void setTrackedStun(boolean b){dataTracker.set(STUN,b);}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.NECROPHAGE;}
    public boolean isHiding(){return dataTracker.get(HIDING);}
    public Action getAction(){return action;}
    public int getActionTick(){return actionTick;}
    public int getMudCooldown(){return mudCooldown;}
    @Override public int getLimitPerChunk(){return 2;}
    @Override protected void initGoals(){
        targetSelector.add(1,new RevengeGoal(this));
        targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,10,true,false,null));
        goalSelector.add(5,new WanderAroundFarGoal(this,.65){
            @Override public boolean canStart(){return !isHiding()&&action==Action.NONE&&getTarget()==null&&super.canStart();}
            @Override protected Vec3d getWanderTarget(){
                // A few local samples only when vanilla wander requests a destination.
                for(int i=0;i<6;i++){var p=getBlockPos().add(random.nextInt(9)-4,random.nextInt(3)-1,random.nextInt(9)-4);
                    if(getWorld().isChunkLoaded(p)&&getWorld().getFluidState(p).isIn(FluidTags.WATER)&&getWorld().isSpaceEmpty(WaterHagEntity.this,getBoundingBox().offset(Vec3d.ofBottomCenter(p).subtract(getPos()))))return Vec3d.ofBottomCenter(p);}
                return super.getWanderTarget();
            }
        });
        goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,8));
    }
    private boolean valid(LivingEntity t){return t!=null&&t.isAlive()&&squaredDistanceTo(t)<=24*24&&!(t instanceof PlayerEntity p&&(p.isCreative()||p.isSpectator()));}
    public static boolean ambushSurface(WorldAccess w,BlockPos pos){
        if(!w.isChunkLoaded(pos))return false;
        if(w.getFluidState(pos).isIn(FluidTags.WATER)||w.getFluidState(pos.down()).isIn(FluidTags.WATER))return true;
        var floor=w.getBlockState(pos.down());
        return floor.isOf(net.minecraft.block.Blocks.MUD)||floor.isOf(net.minecraft.block.Blocks.MUDDY_MANGROVE_ROOTS);
    }
    public boolean enterAmbush(){
        if(getWorld().isClient||!isAlive()||Stun.active(this)||action!=Action.NONE||ambushCooldown>0||!ambushSurface(getWorld(),getBlockPos()))return false;
        dataTracker.set(HIDING,true);getNavigation().stop();return true;
    }
    private void start(Action next){
        action=next;actionTick=0;direction=Vec3d.fromPolar(0,getYaw());getNavigation().stop();dataTracker.set(HIDING,false);
        triggerAnim("action",next==Action.CLAW_ATTACK&&!combo?"claw_single":next.name().toLowerCase(java.util.Locale.ROOT));
        if(next!=Action.MUD_THROW)playSound(SoundEvents.ENTITY_DROWNED_AMBIENT, .65f,.65f);
        if(next==Action.AMBUSH){ambushCooldown=ModConfig.abilityTicks(400);splash();}
    }
    public boolean beginMudThrow(){
        var t=getTarget();
        if(getWorld().isClient||!isAlive()||Stun.active(this)||action!=Action.NONE||attackCooldown>0||mudCooldown>0||!valid(t)||!canSee(t)||squaredDistanceTo(t)<9||squaredDistanceTo(t)>144)return false;
        mudCooldown=ModConfig.abilityTicks(200+random.nextInt(81));start(Action.MUD_THROW);return true;
    }
    public boolean beginClaws(boolean twoHits){
        var t=getTarget();if(getWorld().isClient||!isAlive()||Stun.active(this)||action!=Action.NONE||attackCooldown>0||!valid(t)||!canSee(t)||!CombatMotion.meleeHeight(this,t)||CombatMotion.distance(this,t)>2.7)return false;
        combo=twoHits;start(Action.CLAW_ATTACK);return true;
    }
    private void splash(){if(getWorld() instanceof ServerWorld w){w.spawnParticles(ParticleTypes.SPLASH,getX(),getY()+.4,getZ(),14,.6,.25,.6,.05);w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,net.minecraft.block.Blocks.MUD.getDefaultState()),getX(),getY()+.7,getZ(),12,.5,.3,.5,.06);}playSound(SoundEvents.ENTITY_GENERIC_SPLASH,.7f,.7f);}
    private void contact(){
        var t=getTarget();if(!valid(t)||!canSee(t)||CombatMotion.distance(this,t)>2.7||Math.abs(t.getY()-getY())>1.8)return;
        if(t.getPos().subtract(getPos()).multiply(1,0,1).normalize().dotProduct(direction)<.15)return;
        if(t.damage(getDamageSources().mobAttack(this),(float)getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)))t.takeKnockback(.25,direction.x*-1,direction.z*-1);
    }
    private void actionTick(){
        actionTick++;getNavigation().stop();setVelocity(0,getVelocity().y,0);
        var t=getTarget();int windup=action==Action.MUD_THROW?16:action==Action.AMBUSH?14:10;
        if(valid(t)&&(actionTick<windup-2||action==Action.CLAW_ATTACK&&combo&&actionTick>=17&&actionTick<21))direction=CombatMotion.face(this,t.getPos(),20);
        else CombatMotion.holdFacing(this,direction);
        if(action==Action.MUD_THROW&&actionTick==16&&valid(t)&&canSee(t)&&squaredDistanceTo(t)<=144){
            var clod=new MudClodEntity(getWorld(),this);var aim=t.getEyePos().subtract(clod.getPos());
            clod.setVelocity(aim.x,aim.y+aim.horizontalLength()*.10,aim.z,1.15f,1f);getWorld().spawnEntity(clod);
            playSound(SoundEvents.ENTITY_SNOWBALL_THROW,.8f,.55f);
        }
        if(action==Action.CLAW_ATTACK&&(actionTick==10||combo&&actionTick==24)||action==Action.AMBUSH&&actionTick==14)contact();
        int duration=action==Action.MUD_THROW?28:action==Action.AMBUSH?26:combo?35:18;
        if(actionTick>=duration){action=Action.NONE;attackCooldown=ModConfig.attackTicks(18);}
    }
    @Override public void tick(){
        super.tick();if(getWorld().isClient||!isAlive())return;
        if(attackCooldown>0)attackCooldown--;if(mudCooldown>0)mudCooldown--;if(ambushCooldown>0)ambushCooldown--;
        if(Stun.active(this)){cancelCombatForStun();return;}if(isAiDisabled())return;
        if(action!=Action.NONE){actionTick();return;}
        var t=getTarget();if(!valid(t))setTarget(null);
        if(isHiding()){
            getNavigation().stop();setVelocity(0,getVelocity().y,0);
            if(age%20==0&&!ambushSurface(getWorld(),getBlockPos()))dataTracker.set(HIDING,false);
            if(valid(t)&&squaredDistanceTo(t)<25&&canSee(t))start(Action.AMBUSH);
            return;
        }
        if(!valid(t)){if(age%40==0&&random.nextInt(4)==0)enterAmbush();return;}
        CombatMotion.face(this,t.getPos(),20);getLookControl().lookAt(t,25,25);
        if(CombatMotion.distance(this,t)<=2.7&&beginClaws(random.nextInt(3)==0))return;
        if(beginMudThrow())return;
        if((age+getId())%10==0)getNavigation().startMovingTo(t,1);
    }
    @Override public boolean damage(DamageSource source,float amount){
        boolean hit=super.damage(source,amount);if(hit&&isAlive()){dataTracker.set(HIDING,false);if(action==Action.NONE)triggerAnim("action","hurt");}return hit;
    }
    @Override public void cancelCombatForStun(){action=Action.NONE;dataTracker.set(HIDING,false);attackCooldown=20;getNavigation().stop();setVelocity(0,getVelocity().y,0);}
    @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action==Action.MUD_THROW||action==Action.AMBUSH?CombatState.SPECIAL_ABILITY:action==Action.CLAW_ATTACK?actionTick==10||combo&&actionTick==24?CombatState.ATTACK_ACTIVE:actionTick<10?CombatState.ATTACK_WINDUP:CombatState.ATTACK_RECOVERY:isHiding()?CombatState.IDLE:getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("HagHiding",isHiding());n.putInt("HagMudCooldown",mudCooldown);n.putInt("HagAmbushCooldown",ambushCooldown);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(HIDING,n.getBoolean("HagHiding"));mudCooldown=Math.clamp(n.getInt("HagMudCooldown"),0,840);ambushCooldown=Math.clamp(n.getInt("HagAmbushCooldown"),0,1200);action=Action.NONE;attackCooldown=20;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.ENTITY_DROWNED_AMBIENT_WATER;}
    @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.ENTITY_WITCH_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.ENTITY_DROWNED_DEATH;}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        var move=new AnimationController<WaterHagEntity>(this,"movement",3,s->s.setAndContinue(!isAlive()?RawAnimation.begin().thenPlayAndHold("animation.water_hag.death"):RawAnimation.begin().thenLoop("animation.water_hag."+(isHiding()?"hide":s.isMoving()?isAttacking()||getTarget()!=null?"run":"walk":"idle"))));
        for(String name:new String[]{"idle","walk","run"})move.triggerableAnim(name,RawAnimation.begin().thenLoop("animation.water_hag."+name));c.add(move);
        var a=new AnimationController<WaterHagEntity>(this,"action",0,s->PlayState.STOP);
        for(String name:new String[]{"claw_attack","claw_single","mud_throw","ambush","hurt","death"})a.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.water_hag."+name));c.add(a);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,60).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,8).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,ModConfig.speed(.21)).add(EntityAttributes.GENERIC_FOLLOW_RANGE,20).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.35).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
