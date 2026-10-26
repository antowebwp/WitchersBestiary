package com.witchersbestiary.lure;
import com.witchersbestiary.entity.monster.GriffinEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;
/** Only idle monsters approach; combat/committed actions keep control. */
public final class LureApproachGoal extends Goal {
 private final PathAwareEntity mob;private LureState.Entry destination;private int repath;
 public LureApproachGoal(PathAwareEntity mob){this.mob=mob;setControls(EnumSet.of(Control.MOVE));}
 @Override public boolean canStart(){if(!(mob.getWorld() instanceof ServerWorld w)||mob.getTarget()!=null||!mob.isAlive()||com.witchersbestiary.combat.Stun.active(mob))return false;if(!(mob instanceof GriffinEntity g&&g.isFlying())&&mob instanceof com.witchersbestiary.combat.Stunnable s&&s.getCombatState()!=com.witchersbestiary.combat.CombatState.IDLE&&s.getCombatState()!=com.witchersbestiary.combat.CombatState.CHASE)return false;destination=LureState.get(w).approach(mob.getUuid(),LureState.now(w));return destination!=null&&destination.dimension.equals(w.getRegistryKey().getValue().toString())&&mob.squaredDistanceTo(Vec3d.ofBottomCenter(destination.pos))>6.25;}
 @Override public boolean shouldContinue(){return canStart();}
 @Override public void start(){repath=0;tick();}
 @Override public void tick(){if(destination==null||mob.getTarget()!=null)return;var at=Vec3d.ofBottomCenter(destination.pos);if(mob instanceof GriffinEntity griffin&&griffin.approachLureLanding(at))return;if(repath--<=0){repath=5;mob.getNavigation().startMovingTo(at.x,at.y,at.z,1.05);}}
 @Override public void stop(){if(mob.getTarget()==null)mob.getNavigation().stop();destination=null;}
}
