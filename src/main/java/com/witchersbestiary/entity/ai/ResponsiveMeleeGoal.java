package com.witchersbestiary.entity.ai;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import java.util.EnumSet;

/** Bounded path refresh and a telegraphed 6-tick windup, one hit per swing, optional second swing. */
public final class ResponsiveMeleeGoal extends Goal {
    private final MobEntity mob;
    private final boolean combos;
    private int age, end, repath, cooldown;
    private boolean second, firstHit, secondHit;
    private net.minecraft.util.math.Vec3d facing=net.minecraft.util.math.Vec3d.ZERO;
    private CombatState state = CombatState.IDLE;
    private final CombatMotion motion=new CombatMotion();
    public ResponsiveMeleeGoal(MobEntity mob, boolean combos) {
        this.mob=mob; this.combos=combos; setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    public CombatState state() { return state; }
    @Override public boolean shouldRunEveryTick() { return true; }
    @Override public boolean canStart() { return !Stun.active(mob) && mob.getTarget()!=null && mob.getTarget().isAlive(); }
    @Override public boolean shouldContinue() { return canStart(); }
    @Override public void stop() { age=0; state=CombatState.IDLE; mob.getNavigation().stop(); }
    @Override public void tick() {
        var target=mob.getTarget(); if (target==null || Stun.active(mob)) { stop(); return; }
        mob.getLookControl().lookAt(target,40,40);
        motion.sample(mob,target);
        if(age==0)CombatMotion.face(mob,motion.lead(target,3,.55),18);
        if(cooldown>0) cooldown--;
        if(age>0) {
            int current=age++;
            boolean active=current>=6&&current<=8 || second&&current>=17&&current<=19;
            if(current<5||second&&current>10&&current<15)facing=CombatMotion.face(mob,motion.lead(target,2,.4),18);
            else CombatMotion.holdFacing(mob,facing);
            state=active?CombatState.ATTACK_ACTIVE:current<6 || second && current>9 && current<17?CombatState.ATTACK_WINDUP:CombatState.ATTACK_RECOVERY;
            if(active && !(current<10?firstHit:secondHit) && CombatMotion.contact(mob,target,facing,2.3,.1)) {
                mob.tryAttack(target);if(current<10)firstHit=true;else secondHit=true;
            }
            if(second && current==11) ((GeoEntity)mob).triggerAnim("action","attack");
            if(current>=end) {age=0;cooldown=ModConfig.attackTicks(4);state=CombatState.CHASE;}
        } else if(cooldown==0 && CombatMotion.distance(mob,target)<=2.15 && CombatMotion.meleeHeight(mob,target) && mob.canSee(target)) {
            facing=net.minecraft.util.math.Vec3d.fromPolar(0,mob.getYaw());firstHit=secondHit=false; second=combos && mob.getRandom().nextFloat()<.55f; age=1; end=second?24:14;
            state=CombatState.ATTACK_WINDUP;
            ((GeoEntity)mob).triggerAnim("action","attack");
        } else state=cooldown>0?CombatState.ATTACK_RECOVERY:mob.squaredDistanceTo(target)<9&&!mob.canSee(target)?CombatState.POSITIONING:CombatState.CHASE;
        // Keep closing distance during windup/recovery. Active swings lock aim, not an entire second of movement.
        boolean committed=age>5&&age<11||second&&age>15&&age<21;
        if(committed){mob.getNavigation().stop();mob.setVelocity(0,mob.getVelocity().y,0);return;}
        if(--repath<=0) { repath=6; if(mob.squaredDistanceTo(target)>2.25||!CombatMotion.meleeHeight(mob,target)||!mob.canSee(target)) {
            var point=motion.lead(target,4,.65);mob.getNavigation().startMovingTo(point.x,point.y,point.z,mob.isTouchingWater()?1.28:1.2);
        } else mob.getNavigation().stop(); }
    }
}
