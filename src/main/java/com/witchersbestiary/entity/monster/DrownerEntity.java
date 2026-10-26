package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.MonsterCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Утопец использует ванильную навигацию и водные цели утопленника, но свою геометрию и анимации. */
public final class DrownerEntity extends DrownedEntity implements com.witchersbestiary.entity.ai.LocalAggression.Member,GeoEntity, CategorizedMonster, com.witchersbestiary.combat.Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(DrownerEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("animation.drowner.idle");
    private static final RawAnimation WALK=RawAnimation.begin().thenLoop("animation.drowner.walk");
    private static final RawAnimation RUN=RawAnimation.begin().thenLoop("animation.drowner.run");
    private static final RawAnimation SWIM=RawAnimation.begin().thenLoop("animation.drowner.swim");
    private static final RawAnimation DEATH=RawAnimation.begin().thenPlayAndHold("animation.drowner.death");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);

    public DrownerEntity(EntityType<? extends DrownedEntity> type, World world) { super(type,world); experiencePoints=5; navigation=amphibiousNavigation(world); moveControl=new net.minecraft.entity.ai.control.AquaticMoveControl(this,85,10,1.5f,1f,false); setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.WATER,0); }
    private net.minecraft.entity.ai.pathing.EntityNavigation amphibiousNavigation(World world){var nav=new net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation(this,world);nav.setCanSwim(true);return nav;}
    @Override public void updateSwimming(){setSwimming(isTouchingWater()&&!hasVehicle());}
    @Override public void travel(net.minecraft.util.math.Vec3d input){
        if(isLogicalSideForUpdatingMovement()&&isTouchingWater()){
            updateVelocity(.14f,input);move(net.minecraft.entity.MovementType.SELF,getVelocity());setVelocity(getVelocity().multiply(.86));
        }else super.travel(input);
    }
    private com.witchersbestiary.entity.ai.ResponsiveMeleeGoal melee;
    @Override public com.witchersbestiary.combat.CombatState getCombatState(){return com.witchersbestiary.combat.Stun.active(this)?com.witchersbestiary.combat.CombatState.STUNNED:melee==null?com.witchersbestiary.combat.CombatState.IDLE:melee.state();}
    @Override public void cancelCombatForStun(){if(melee!=null)melee.stop();}
    // NaturalSpawns owns habitat/density/geometry. PathAwareEntity's negative
    // daylight path favor must not silently veto these two day-active necrophages.
    @Override public boolean canSpawn(net.minecraft.world.WorldAccess world,net.minecraft.entity.SpawnReason reason){return world.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL;}
    @Override public boolean canSpawn(net.minecraft.world.WorldView world){
        return world.doesNotIntersectEntities(this)&&world.isSpaceEmpty(getBoundingBox());
    }
    @Override protected void initGoals(){
        // Retain Drowned move control/navigation, remove its daylight land-attack restriction and competing wander goals.
        goalSelector.add(1,melee=new com.witchersbestiary.entity.ai.ResponsiveMeleeGoal(this,true));
        goalSelector.add(7,new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this,.9));
        targetSelector.add(1,new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this,net.minecraft.entity.player.PlayerEntity.class,0,true,false,null));
    }
    @Override protected void initEquipment(net.minecraft.util.math.random.Random random,LocalDifficulty difficulty) { }
    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.NECROPHAGE; }
    @Override public boolean isFireImmune() { return true; }
    @Override public boolean canDrownedAttackTarget(net.minecraft.entity.LivingEntity target) {
        return target != null && target.isAlive();
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"movement",4,state -> {
            if (getHealth() <= 0) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            if (isTouchingWater()) return state.setAndContinue(SWIM);
            return state.setAndContinue(getVelocity().horizontalLengthSquared() > .035 ? RUN : WALK);
        }));
        AnimationController<DrownerEntity> action=new AnimationController<>(this,"action",0,state -> PlayState.STOP);
        for(String name:new String[]{"attack","hurt","emerge"}) action.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.drowner."+name));
        controllers.add(action);
    }
    @Override public EntityData initialize(ServerWorldAccess world,LocalDifficulty difficulty,SpawnReason reason,EntityData data){
        EntityData result=super.initialize(world,difficulty,reason,data);
        if(reason==SpawnReason.NATURAL)triggerAnim("action","emerge");
        return result;
    }
    @Override public boolean damage(DamageSource source,float amount) {
        boolean hit=super.damage(source,amount);if(hit&&source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity p)com.witchersbestiary.entity.ai.LocalAggression.alert(this,p);
        if(hit&&!getWorld().isClient&&isAlive()&&(melee==null||melee.state()==com.witchersbestiary.combat.CombatState.CHASE)) triggerAnim("action","hurt");
        return hit;
    }
    public static DefaultAttributeContainer.Builder createDrownerAttributes() {
        // Keep Zombie/Drowned age and scale attributes so vanilla spawn-egg baby rolls
        // can safely initialize the entity; override only the Bestiary's combat stats.
        return DrownedEntity.createDrownedAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,6).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,com.witchersbestiary.config.ModConfig.speed(.25))
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);
    }

    @Override public void tick(){super.tick();com.witchersbestiary.entity.ai.LocalAggression.tick(this);}
    private java.util.UUID sharedTarget;
    public java.util.UUID sharedGroupTarget(){return sharedTarget;}
    public void sharedGroupTarget(java.util.UUID target){sharedTarget=target;}
    @Override public int getLimitPerChunk(){return com.witchersbestiary.config.ModConfig.groupMaximum("drowner",5);}
}
