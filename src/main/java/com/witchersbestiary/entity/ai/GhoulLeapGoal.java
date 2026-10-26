package com.witchersbestiary.entity.ai;

import com.witchersbestiary.entity.monster.GhoulEntity;
import net.minecraft.entity.ai.goal.Goal;
import com.witchersbestiary.combat.*;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

/** One short pounce when the player is near enough; entity cooldown prevents leap spam. */
public final class GhoulLeapGoal extends Goal {
    private final GhoulEntity ghoul;
    private int tick;
    private boolean contact;
    private Vec3d facing=Vec3d.ZERO;

    public GhoulLeapGoal(GhoulEntity ghoul) {
        this.ghoul = ghoul;
        setControls(EnumSet.of(Control.MOVE,Control.LOOK,Control.JUMP));
    }

    @Override public boolean canStart() {
        var target = ghoul.getTarget();
        if (!ghoul.canLeap() || target == null || !target.isAlive() || !ghoul.canSee(target)) return false;
        var state=ghoul.getCombatState();if(state!=CombatState.CHASE&&state!=CombatState.IDLE)return false;
        double distance = ghoul.squaredDistanceTo(target);
        return distance >= 4.0 && distance <= 16.0;
    }

    @Override public boolean shouldRunEveryTick(){return true;}
    @Override public boolean shouldContinue() { return tick<20&&ghoul.isAlive()&&!Stun.active(ghoul)&&ghoul.getTarget()!=null&&ghoul.getTarget().isAlive(); }

    @Override public void start() { tick=0;contact=false;ghoul.getNavigation().stop();ghoul.preparePounce();facing=CombatMotion.face(ghoul,ghoul.getTarget().getPos(),18); }
    @Override public void tick(){
        var target=ghoul.getTarget();if(target==null)return;tick++;ghoul.getNavigation().stop();
        if(tick<3)facing=CombatMotion.face(ghoul,target.getPos(),18);else CombatMotion.holdFacing(ghoul,facing);
        if(tick<4)ghoul.setVelocity(0,ghoul.getVelocity().y,0);
        if(tick==4){ghoul.setVelocity(facing.x*.48,.38,facing.z*.48);ghoul.velocityModified=true;}
        if(tick>=6&&tick<=12&&!contact&&CombatMotion.contact(ghoul,target,facing,1.65,.35)){ghoul.tryAttack(target);contact=true;}
        if(tick>4&&ghoul.horizontalCollision)ghoul.setVelocity(0,ghoul.getVelocity().y,0);
    }
    @Override public void stop(){ghoul.finishPounce();ghoul.getNavigation().stop();}
}
