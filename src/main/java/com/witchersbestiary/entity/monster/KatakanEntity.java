package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;
import software.bernie.geckolib.animatable.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;

/** A controlled bat form of this same persistent entity: no second owner, temporary drops or orphan UUIDs. */
public final class KatakanEntity extends HostileEntity implements GeoEntity,CategorizedMonster, Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(KatakanEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    private static final TrackedData<Boolean> BAT=DataTracker.registerData(KatakanEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private boolean batUsed;
    private long batUntil,nextHeal;
    private float beastHealth;
    private Vec3d batOrigin=Vec3d.ZERO,chargeDirection=Vec3d.ZERO;
    private int attackAge,cooldown,chargeCooldown,stunTicks,batHurtTicks;
    private String attack="";
    private boolean chargeHit,firstHit,secondHit;
    public KatakanEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=20;}
    @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);builder.add(BAT,false);}
    public boolean isBatForm(){return dataTracker.get(BAT);}
    public boolean hasUsedBatForm(){return batUsed;}
    public int getStunTicks(){return stunTicks;}
    public float getStoredBeastHealth(){return beastHealth;}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.VAMPIRE;}
    @Override public EntityDimensions getBaseDimensions(EntityPose pose){return isBatForm()?EntityDimensions.changing(.5f,.65f):super.getBaseDimensions(pose);}
    @Override public void onTrackedDataSet(TrackedData<?> data){super.onTrackedDataSet(data);if(BAT.equals(data))calculateDimensions();}
    @Override protected void initGoals(){
        goalSelector.add(1,new Goal(){
            private int repath;
            {setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
            @Override public boolean shouldRunEveryTick(){return true;}
            @Override public boolean canStart(){return !isBatForm()&&stunTicks==0&&getTarget()!=null&&getTarget().isAlive();}
            @Override public void stop(){getNavigation().stop();}
    @Override public void tick(){
                var target=getTarget();if(target==null)return;getLookControl().lookAt(target,35,35);if(attack.isEmpty())CombatMotion.face(KatakanEntity.this,target.getPos(),20);
                if(!attack.isEmpty()){if(!attack.equals("charge")&&--repath<=0){repath=6;getNavigation().startMovingTo(target,1.15);}return;}
                double distance=squaredDistanceTo(target);
                if(cooldown==0&&canSee(target)&&distance<6){startAttack(random.nextFloat()<.3f?"bite":"claws");return;}
                if(cooldown==0&&chargeCooldown==0&&canSee(target)&&distance>=16&&distance<=100){startAttack("charge");return;}
                if(--repath<=0){repath=6;getNavigation().startMovingTo(target,1.2);}
            }
        });
        goalSelector.add(7,new WanderAroundFarGoal(this,.8));targetSelector.add(1,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }
    public boolean startAttack(String action){
        if(getWorld().isClient||!isAlive()||isBatForm()||stunTicks>0||Stun.active(this)||cooldown>0||!attack.isEmpty()||getTarget()==null||!getTarget().isAlive())return false;
        if(!java.util.List.of("claws","bite","charge").contains(action)||action.equals("charge")&&chargeCooldown>0)return false;
        attack=action;attackAge=0;chargeHit=firstHit=secondHit=false;getNavigation().stop();
        chargeDirection=getTarget().getPos().subtract(getPos()).multiply(1,0,1).normalize();
        if(action.equals("charge"))chargeCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(90);triggerAnim("action",action);return true;
    }
    public boolean beginBatForm(){
        if(!(getWorld() instanceof ServerWorld world)||!isAlive()||batUsed||isBatForm()||Stun.active(this)||getHealth()>=32)return false;
        batUsed=true;beastHealth=getHealth();batOrigin=getPos();batUntil=world.getTime()+160;nextHeal=world.getTime()+20;
        attack="";getNavigation().stop();dataTracker.set(BAT,true);getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(8);com.witchersbestiary.combat.ProgressiveDifficulty.refresh(this);setHealth(8);setNoGravity(true);calculateDimensions();
        triggerAnim("action","transform");world.spawnParticles(ParticleTypes.POOF,getX(),getY()+1,getZ(),16,.4,.5,.4,.02);playSound(SoundEvents.ENTITY_BAT_TAKEOFF,1,.8f);return true;
    }
    private boolean safeBeastPosition(Vec3d pos){
        var world=getWorld();var feet=BlockPos.ofFloored(pos);double half=getType().getWidth()/2;var box=new Box(pos.x-half,pos.y,pos.z-half,pos.x+half,pos.y+getType().getHeight(),pos.z+half);
        return world.isChunkLoaded(feet)&&world.getWorldBorder().contains(box)&&world.isSpaceEmpty(this,box)&&!world.containsFluid(box)
                &&world.getBlockState(feet.down()).isSolidBlock(world,feet.down())&&!world.getBlockState(feet.down()).isOf(net.minecraft.block.Blocks.MAGMA_BLOCK);
    }
    private void endBatForm(boolean killed){
        Vec3d destination=safeBeastPosition(getPos())?getPos():batOrigin;
        if(!safeBeastPosition(destination)){
            search:for(int y=-2;y<=2;y++)for(int i=0;i<8;i++){
                double angle=i*Math.PI/4;Vec3d candidate=batOrigin.add(Math.cos(angle)*3,y,Math.sin(angle)*3);
                if(safeBeastPosition(candidate)){destination=candidate;break search;}
            }
        }
        dataTracker.set(BAT,false);getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(80);com.witchersbestiary.combat.ProgressiveDifficulty.refresh(this);setHealth(Math.clamp(beastHealth,.01f,getMaxHealth()));setNoGravity(false);calculateDimensions();
        if(safeBeastPosition(destination))refreshPositionAndAngles(destination.x,destination.y,destination.z,getYaw(),getPitch());
        setVelocity(Vec3d.ZERO);velocityModified=true;cooldown=20;stunTicks=killed?12:0;if(killed)Stun.apply(this,12);attack="";batHurtTicks=0;
        triggerAnim("action",killed?"hurt":"transform");playSound(SoundEvents.ENTITY_BAT_TAKEOFF,.8f,.6f);
    }
    private float beastHealthCap(){return (float)com.witchersbestiary.config.ModConfig.progressive().health(80,com.witchersbestiary.combat.ProgressiveDifficulty.steps(this),com.witchersbestiary.config.ModConfig.progressive().elites().contains("witchersbestiary:katakan"));}
    private void tickBat(ServerWorld world){
        if(getTarget()==null||!getTarget().isAlive()){endBatForm(false);return;}
        if(world.getTime()>=nextHeal&&world.getTime()<=batUntil){beastHealth=Math.min(beastHealthCap(),beastHealth+1);nextHeal=world.getTime()+20;}
        if(world.getTime()>=batUntil){endBatForm(false);return;}
        if(age%10==0){
            Vec3d desired=getPos().subtract(getTarget().getPos()).multiply(1,0,1).normalize().multiply(.24);
            if(getPos().squaredDistanceTo(batOrigin)>64)desired=batOrigin.subtract(getPos()).normalize().multiply(.24);
            double vertical=getY()<batOrigin.y+2?.12:getY()>batOrigin.y+4?-.12:0;
            var velocity=desired.add(0,vertical,0);var box=getBoundingBox().offset(velocity.multiply(3));
            if(world.isSpaceEmpty(this,box)&&world.getWorldBorder().contains(box)&&!world.containsFluid(box))setVelocity(velocity);else setVelocity(desired.z,.1,-desired.x);
            velocityModified=true;
        }
    }
    private boolean strike(float amount,double reach){
        var target=getTarget();if(target==null||!CombatMotion.contact(this,target,chargeDirection,reach,.1))return false;
        return target.damage(getDamageSources().mobAttack(this),amount);
    }
    public boolean exposedToSun(){long time=Math.floorMod(getWorld().getTimeOfDay(),24000);return getWorld().getDimension().hasSkyLight()&&!getWorld().getDimension().hasFixedTime()&&(time<13000||time>=23000)&&!getWorld().isRaining()&&getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING,getBlockX(),getBlockZ())<=getBlockY()&&getWorld().isSkyVisible(getBlockPos());}
    @Override public CombatState getCombatState(){if(isBatForm())return CombatState.SPECIAL_ABILITY;if(stunTicks>0||Stun.active(this))return CombatState.STUNNED;if(!attack.isEmpty())return attack.equals("charge")?CombatState.SPECIAL_ABILITY:attackAge<6?CombatState.ATTACK_WINDUP:attackAge==6||attack.equals("claws")&&attackAge==16?CombatState.ATTACK_ACTIVE:CombatState.ATTACK_RECOVERY;return getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){attack="";attackAge=0;cooldown=Math.max(cooldown,8);}
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld world)||!isAlive())return;
        if(batHurtTicks>0)batHurtTicks--;if(cooldown>0)cooldown--;if(chargeCooldown>0)chargeCooldown--;
        if(Stun.active(this)){cancelCombatForStun();if(stunTicks>0)stunTicks--;return;}
        if(isBatForm()){tickBat(world);return;}
        if(stunTicks>0){stunTicks--;getNavigation().stop();setVelocity(0,getVelocity().y,0);return;}
        if(age%20==0&&exposedToSun())setOnFireFor(8);
        if(!batUsed&&getHealth()<32&&getTarget()!=null&&getTarget().isAlive()){beginBatForm();return;}
        if(attack.isEmpty())return;
        if(getTarget()==null||!getTarget().isAlive()){attack="";cooldown=20;return;}
        attackAge++;
        if(!attack.equals("charge")&&(attackAge<5||attack.equals("claws")&&attackAge>10&&attackAge<16))chargeDirection=CombatMotion.face(this,getTarget().getPos(),20);
        if(attack.equals("claws")){
            if(!firstHit&&attackAge>=6&&attackAge<=8)firstHit=strike(8,2.6);
            if(!secondHit&&attackAge>=17&&attackAge<=19)secondHit=strike(8,2.6);
        }
        if(attack.equals("bite")&&!firstHit&&attackAge>=6&&attackAge<=8&&strike(10,2.2)){firstHit=true;heal(4);}
        if(attack.equals("charge")&&attackAge>10){
            if(!CombatMotion.safeStep(this,chargeDirection.multiply(.65),true)){attack="";cooldown=35;setVelocity(0,getVelocity().y,0);return;}
            setVelocity(chargeDirection.x*.65,getVelocity().y,chargeDirection.z*.65);velocityModified=true;
            if(!chargeHit)chargeHit=strike(8,2.1);
        }
        if(attackAge>=(attack.equals("claws")?24:attack.equals("bite")?16:26)){attack="";cooldown=com.witchersbestiary.config.ModConfig.attackTicks(6);setVelocity(0,getVelocity().y,0);}
    }
    @Override public boolean damage(DamageSource source,float amount){
        if(isBatForm()&&!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            if(getWorld().isClient||isInvulnerableTo(source)||batHurtTicks>0||amount<=0)return false;
            var oil=com.witchersbestiary.alchemy.InventoryOils.prepare(this,source);float modified=AlchemyDamage.withOil(this,source,com.witchersbestiary.combat.ProgressiveDifficulty.damage(source,amount),oil);batHurtTicks=10;
            if(modified>=getHealth())endBatForm(true);else setHealth(getHealth()-modified);
            if(modified>0)com.witchersbestiary.alchemy.InventoryOils.commit(oil);getWorld().sendEntityStatus(this,(byte)2);return true;
        }
        if(isBatForm())endBatForm(false);
        boolean result=super.damage(source,amount);if(result&&isAlive()&&!getWorld().isClient&&attack.isEmpty())triggerAnim("action","hurt");return result;
    }
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("BatUsed",batUsed);n.putBoolean("BatForm",isBatForm());n.putFloat("BeastHealth",beastHealth);n.putLong("BatUntil",batUntil);n.putDouble("BatX",batOrigin.x);n.putDouble("BatY",batOrigin.y);n.putDouble("BatZ",batOrigin.z);n.putInt("StunTicks",stunTicks);n.putInt("ChargeCooldown",chargeCooldown);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);batUsed=n.getBoolean("BatUsed");dataTracker.set(BAT,n.getBoolean("BatForm"));beastHealth=Math.clamp(n.getFloat("BeastHealth"),.01f,beastHealthCap());batUntil=n.getLong("BatUntil");nextHeal=getWorld().getTime()+20;batOrigin=new Vec3d(n.getDouble("BatX"),n.getDouble("BatY"),n.getDouble("BatZ"));stunTicks=MathHelper.clamp(n.getInt("StunTicks"),0,12);chargeCooldown=MathHelper.clamp(n.getInt("ChargeCooldown"),0,140);attack="";cooldown=20;setNoGravity(isBatForm());calculateDimensions();}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar){
        registrar.add(new AnimationController<>(this,"movement",3,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.katakan."+(isBatForm()?"bat_fly":!isAlive()?"death":state.isMoving()?(getVelocity().horizontalLengthSquared()>.04?"run":"walk"):"idle")))));
        var action=new AnimationController<>(this,"action",0,state->PlayState.STOP);for(String name:new String[]{"claws","bite","charge","leap","transform","hurt"})action.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.katakan."+name));registrar.add(action);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,80).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,8).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,com.witchersbestiary.config.ModConfig.speed(.44)).add(EntityAttributes.GENERIC_FOLLOW_RANGE,36).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
