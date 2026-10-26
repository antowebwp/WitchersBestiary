package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Ground miniboss. One lifetime wave, finite committed actions, collision-resolved tongue pull. */
public final class GraveHagEntity extends HostileEntity implements CategorizedMonster, GeoEntity, Stunnable {
    public enum Action { NONE, CLAW_ATTACK, TONGUE_ATTACK, TONGUE_PULL, CURSE_CASTING, SUMMON_GHOSTS }
    private static final TrackedData<Boolean> STUN=DataTracker.registerData(GraveHagEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final CombatIllusions ghosts=new CombatIllusions(2);
    private Action action=Action.NONE;
    private int actionTick, recovery, tongueCooldown=60, curseCooldown=100, summonCooldown=160;
    public static final int CLAW_WINDUP=5, CLAW_RECOVERY=10, MAX_CLAW_COMBO=2;
    private int clawCombo;
    private boolean waveUsed;
    private LivingEntity committed;
    private Vec3d direction=Vec3d.ZERO;
    public GraveHagEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=20;}
    @Override protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN);}
    @Override public void setTrackedStun(boolean b){dataTracker.set(STUN,b);}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.NECROPHAGE;}
    public Action getAction(){return action;}
    public boolean hasUsedSummons(){return waveUsed;}
    public int activeGhostSlots(){return getWorld() instanceof ServerWorld w?2-ghosts.available(w):0;}
    @Override protected void initGoals(){
        goalSelector.add(0,new SwimGoal(this));
        targetSelector.add(1,new RevengeGoal(this));
        targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,10,true,false,null));
        goalSelector.add(5,new WanderAroundFarGoal(this,.65){@Override public boolean canStart(){return action==Action.NONE&&getTarget()==null&&super.canStart();}});
        goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,8));
    }
    private boolean valid(LivingEntity t){return t!=null&&t.isAlive()&&!t.isRemoved()&&t.getWorld()==getWorld()&&squaredDistanceTo(t)<1024&&!(t instanceof PlayerEntity p&&(p.isCreative()||p.isSpectator()));}
    public boolean hasActiveCombat(){return isAlive()&&!isAiDisabled()&&valid(getTarget());}
    private boolean ready(){return !getWorld().isClient&&hasActiveCombat()&&!Stun.active(this)&&action==Action.NONE&&recovery==0&&canSee(getTarget());}
    private void start(Action next){action=next;actionTick=0;committed=getTarget();direction=CombatMotion.face(this,committed.getPos(),360);if(next==Action.TONGUE_ATTACK)direction=new Vec3d(committed.getX(),committed.getBodyY(.65),committed.getZ()).subtract(mouth()).normalize();getNavigation().stop();if(next==Action.CLAW_ATTACK)stopTriggeredAnim("action","claw_attack");triggerAnim("action",next.name().toLowerCase(java.util.Locale.ROOT));}
    public boolean beginClaws(){if(!ready()||CombatMotion.distance(this,getTarget())>2.5||!CombatMotion.meleeHeight(this,getTarget()))return false;clawCombo=1;start(Action.CLAW_ATTACK);return true;}
    public boolean beginTongue(){if(!ready()||tongueCooldown>0||squaredDistanceTo(getTarget())<9||squaredDistanceTo(getTarget())>36)return false;tongueCooldown=ModConfig.abilityTicks(180);start(Action.TONGUE_ATTACK);return true;}
    public boolean beginCurse(){if(!ready()||curseCooldown>0||squaredDistanceTo(getTarget())>64)return false;curseCooldown=ModConfig.abilityTicks(260);start(Action.CURSE_CASTING);return true;}
    public boolean beginSummon(){if(!ready()||waveUsed||summonCooldown>0)return false;waveUsed=true;start(Action.SUMMON_GHOSTS);return true;}
    private Vec3d mouth(){return getPos().add(0,1.55,0);}
    private boolean clearRay(Vec3d from,Vec3d to){return getWorld().raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,this)).getType()==HitResult.Type.MISS;}
    private void finish(){boolean claws=action==Action.CLAW_ATTACK;action=Action.NONE;actionTick=0;clawCombo=0;committed=null;recovery=claws?0:ModConfig.attackTicks(24);}
    private boolean contact(){if(valid(committed)&&canSee(committed)&&CombatMotion.meleeHeight(this,committed)&&CombatMotion.distance(this,committed)<=2.5&&committed.getPos().subtract(getPos()).multiply(1,0,1).normalize().dotProduct(direction)>.35){committed.damage(getDamageSources().mobAttack(this),(float)getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));return true;}return false;}
    private void summon(){
        if(!(getWorld() instanceof ServerWorld w))return;
        // Eight local candidates, two lifetime summons. No world scans, natural-spawn call or chunk tickets.
        for(int i=0;i<8&&ghosts.available(w)>0;i++){
            double angle=i*Math.PI/4;Vec3d p=getPos().add(Math.cos(angle)*2.5,.3,Math.sin(angle)*2.5);
            var ghost=ModEntities.WRAITH.create(w);if(ghost==null)continue;ghost.refreshPositionAndAngles(p.x,p.y,p.z,getYaw(),0);
            if(!w.isChunkLoaded(ghost.getBlockPos())||!w.getWorldBorder().contains(ghost.getBoundingBox())||!w.isSpaceEmpty(ghost,ghost.getBoundingBox()))continue;
            ghost.bindGraveHag(this);ghost.setTarget(getTarget());
            if(w.spawnEntity(ghost))ghosts.add(ghost,w.getTime()+CombatIllusions.LIFETIME);
        }
    }
    private void tickAction(){
        actionTick++;getNavigation().stop();setVelocity(0,getVelocity().y,0);CombatMotion.holdFacing(this,direction);
        if(!valid(committed)){finish();return;}
        if(action==Action.CLAW_ATTACK){
            // Track only during the short preparation; contact is a single committed frame.
            if(actionTick<=CLAW_WINDUP)direction=CombatMotion.face(this,committed.getPos(),90);
            if(actionTick==CLAW_WINDUP&&!contact()){finish();recovery=ModConfig.attackTicks(CLAW_RECOVERY);getNavigation().startMovingTo(getTarget(),1);return;}
            if(actionTick>=CLAW_WINDUP+ModConfig.attackTicks(CLAW_RECOVERY)){
                if(clawCombo<MAX_CLAW_COMBO&&committed==getTarget()&&canSee(committed)&&CombatMotion.meleeHeight(this,committed)&&CombatMotion.distance(this,committed)<=2.5){clawCombo++;start(Action.CLAW_ATTACK);}else finish();
            }
            return;
        }
        if(action==Action.TONGUE_ATTACK&&actionTick==14){
            // Aim is locked at windup start. A lateral dodge can leave the narrow segment before contact.
            Vec3d from=mouth(),end=from.add(direction.multiply(6));
            var intersection=committed.getBoundingBox().expand(.15).raycast(from,end);
            if(intersection.isPresent()&&clearRay(from,intersection.get())&&canSee(committed)){
                committed.damage(getDamageSources().mobAttack(this),3);action=Action.TONGUE_PULL;actionTick=0;triggerAnim("action","tongue_pull");
            }
        }
        if(action==Action.TONGUE_PULL){
            if(squaredDistanceTo(committed)>42.25||!canSee(committed)||!clearRay(mouth(),new Vec3d(committed.getX(),committed.getBodyY(.65),committed.getZ()))){finish();return;}
            Vec3d delta=getPos().subtract(committed.getPos()).multiply(1,0,1);
            if(delta.length()>1.6){Vec3d pull=delta.normalize().multiply(.22);Vec3d old=committed.getVelocity();committed.setVelocity(pull.x,Math.clamp(old.y,-.15,.15),pull.z);committed.velocityModified=true;}
        }
        if(action==Action.CURSE_CASTING&&actionTick==18&&squaredDistanceTo(committed)<=64&&canSee(committed)){
            committed.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,80,0));committed.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,0));
            ((ServerWorld)getWorld()).spawnParticles(ParticleTypes.SOUL,committed.getX(),committed.getBodyY(.6),committed.getZ(),12,.4,.5,.4,.015);
        }
        if(action==Action.SUMMON_GHOSTS&&actionTick==22)summon();
        int duration=switch(action){case CLAW_ATTACK->26;case TONGUE_ATTACK->28;case TONGUE_PULL->20;case CURSE_CASTING->32;case SUMMON_GHOSTS->40;default->1;};
        if(actionTick>=duration)finish();
    }
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld w))return;
        // Mob attacking flag is tracked; client getTarget() is not synchronized.
        setAttacking(hasActiveCombat()&&action==Action.NONE);
        if(tongueCooldown>0)tongueCooldown--;if(curseCooldown>0)curseCooldown--;if(summonCooldown>0)summonCooldown--;if(recovery>0)recovery--;
        if(!hasActiveCombat()){ghosts.clear(w);if(action!=Action.NONE)finish();if(!valid(getTarget()))setTarget(null);return;}
        if(Stun.active(this)){cancelCombatForStun();return;}
        if(action!=Action.NONE){tickAction();return;}
        var t=getTarget();getLookControl().lookAt(t,25,25);
        if(beginClaws()||beginSummon()||beginTongue()||beginCurse())return;
        // Vanilla ground A*, step height and jump control. Repath at most twice per second.
        if((age+getId())%10==0)getNavigation().startMovingTo(t,1);
    }
    @Override public boolean damage(DamageSource source,float amount){boolean hit=super.damage(source,amount);if(hit&&isAlive()&&action==Action.NONE)triggerAnim("action","hurt");return hit;}
    @Override public void onDeath(DamageSource s){if(getWorld() instanceof ServerWorld w)ghosts.clear(w);finish();super.onDeath(s);}
    @Override public void cancelCombatForStun(){finish();getNavigation().stop();setVelocity(0,getVelocity().y,0);}
    @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action==Action.CLAW_ATTACK?actionTick<CLAW_WINDUP?CombatState.ATTACK_WINDUP:actionTick==CLAW_WINDUP?CombatState.ATTACK_ACTIVE:CombatState.ATTACK_RECOVERY:action!=Action.NONE?CombatState.SPECIAL_ABILITY:getTarget()!=null?CombatState.CHASE:CombatState.IDLE;}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("GraveWaveUsed",waveUsed);n.putInt("GraveTongueCooldown",tongueCooldown);n.putInt("GraveCurseCooldown",curseCooldown);n.putInt("GraveSummonCooldown",summonCooldown);ghosts.save(n);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);waveUsed=n.getBoolean("GraveWaveUsed");tongueCooldown=Math.clamp(n.getInt("GraveTongueCooldown"),0,1200);curseCooldown=Math.clamp(n.getInt("GraveCurseCooldown"),0,1200);summonCooldown=Math.clamp(n.getInt("GraveSummonCooldown"),0,1200);ghosts.load(n);finish();}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<GraveHagEntity>(this,"movement",3,s->s.setAndContinue(!isAlive()?RawAnimation.begin().thenPlayAndHold("animation.grave_hag.death"):RawAnimation.begin().thenLoop("animation.grave_hag."+(s.isMoving()?isAttacking()?"run":"walk":"idle")))));
        var a=new AnimationController<GraveHagEntity>(this,"action",0,s->PlayState.STOP);for(String name:new String[]{"claw_attack","tongue_attack","tongue_pull","curse_casting","summon_ghosts","hurt"})a.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.grave_hag."+name));c.add(a);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,120).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,9).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,ModConfig.speed(.23)).add(EntityAttributes.GENERIC_FOLLOW_RANGE,28).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.45).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
