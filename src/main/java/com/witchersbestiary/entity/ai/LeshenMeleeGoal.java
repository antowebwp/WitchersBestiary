package com.witchersbestiary.entity.ai;
import com.witchersbestiary.entity.monster.LeshenEntity;
import com.witchersbestiary.combat.*;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;
/** Entity owns timing; no second vanilla attack timer. */
public final class LeshenMeleeGoal extends Goal {
 private final LeshenEntity mob;private int repath;
 public LeshenMeleeGoal(LeshenEntity mob){this.mob=mob;setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
 @Override public boolean shouldRunEveryTick(){return true;}
 @Override public boolean canStart(){return !Stun.active(mob)&&mob.getTarget()!=null&&mob.getTarget().isAlive();}
 @Override public boolean shouldContinue(){return canStart();}
 @Override public void stop(){mob.getNavigation().stop();}
 @Override public void tick(){var target=mob.getTarget();if(target==null)return;mob.getLookControl().lookAt(target,35,35);if(mob.getCombatState()==CombatState.CHASE)CombatMotion.face(mob,target.getPos(),12);var state=mob.getCombatState();if(state!=CombatState.CHASE&&state!=CombatState.IDLE){mob.getNavigation().stop();return;}if(mob.isInAttackRange(target)&&mob.canSee(target))mob.tryAttack(target);if(--repath<=0){repath=6;mob.getNavigation().startMovingTo(target,1.15);}}
}
