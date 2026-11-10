package com.witchersbestiary.entity.ai;

import com.witchersbestiary.fist.FistFights;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;

public final class CompanionDuty {
    public enum Mode { FOLLOW, WAIT, GUARD }
    private static final int RADIUS = 10;
    private Mode mode = Mode.FOLLOW;
    private Vec3d post;
    private boolean retreating;
    private int wetTicks;
    private Vec3d danger;

    public Mode mode() { return mode; }
    public boolean waiting() { return mode == Mode.WAIT; }
    public boolean guarding() { return mode == Mode.GUARD; }
    public boolean retreating() { return retreating; }

    public void cycle(MobEntity pet) {
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        post = guarding() ? pet.getPos() : null;
    }

    public static Text label(Mode mode) {
        return Text.translatable(switch (mode) {
            case FOLLOW -> "nekker.witchersbestiary.follow";
            case WAIT -> "nekker.witchersbestiary.wait";
            case GUARD -> "companion.witchersbestiary.guard";
        });
    }

    private boolean inside(Vec3d position) {
        return post != null && MathHelper.square(position.x-post.x)+MathHelper.square(position.z-post.z)<=RADIUS*RADIUS;
    }

    public boolean guardTarget(MobEntity pet, PlayerEntity owner, LivingEntity target) {
        return guarding() && !retreating && inside(pet.getPos()) && inside(target.getPos())
            && target instanceof HostileEntity && target.isAlive()
            && !(target instanceof Tameable tameable && tameable.getOwnerUuid()!=null)
            && !pet.isTeammate(target) && !FistFights.busy(target)
            && (owner==null || !owner.isTeammate(target)&&!FistFights.busy(owner));
    }

    // True means this tick belongs exclusively to escape or retreat.
    public boolean emergency(ServerWorld world, MobEntity pet, PlayerEntity owner, Runnable cancelAttack) {
        if (pet.getHealth() >= pet.getMaxHealth()*.5f) retreating=false;
        else if (pet.getHealth() < pet.getMaxHealth()*.25f) retreating=true;
        boolean wet=pet.isTouchingWater();
        if(!wet&&wetTicks>0)pet.getNavigation().setCanSwim(false);
        wetTicks=wet?wetTicks+1:0;
        if(!retreating&&!wet)return false;
        if(pet.getTarget()!=null){
            danger=pet.getTarget().getPos();
            pet.setTarget(null);
            cancelAttack.run();
        }
        if(wet){
            pet.getNavigation().setCanSwim(true);
            pet.getJumpControl().setActive();
            if(wetTicks==1||wetTicks%10==0){
                if(wetTicks%60==0)pet.getNavigation().stop();
                seekDry(world,pet,pet.getBlockPos(),danger);
            }
        }else if(pet.age%8==0){
            if(owner!=null&&owner.isAlive()&&!owner.isSpectator()&&!owner.isTouchingWater()
                    &&pet.getNavigation().startMovingTo(owner,1.25))return true;
            var origin=post==null?pet.getBlockPos():BlockPos.ofFloored(post);
            seekDry(world,pet,world.isChunkLoaded(origin)?origin:pet.getBlockPos(),danger);
        }
        return true;
    }

    private void seekDry(ServerWorld world, MobEntity pet, BlockPos origin, Vec3d threat) {
        var candidates=new java.util.ArrayList<BlockPos>();
        for(int r=2;r<=8;r+=2)for(int i=0;i<8;i++)for(int y=-1;y<=3;y++){
            double angle=i*Math.PI/4;
            var pos=origin.add((int)Math.round(Math.cos(angle)*r),y,(int)Math.round(Math.sin(angle)*r));
            if(!world.isChunkLoaded(pos.add(-2,0,-2))||!world.isChunkLoaded(pos.add(-2,0,2))
                ||!world.isChunkLoaded(pos.add(2,0,-2))||!world.isChunkLoaded(pos.add(2,0,2)))continue;
            if(net.minecraft.entity.ai.pathing.LandPathNodeMaker.getLandNodeType(pet,pos)!=PathNodeType.WALKABLE)continue;
            var box=pet.getBoundingBox().offset(Vec3d.ofBottomCenter(pos).subtract(pet.getPos()));
            if(world.getWorldBorder().contains(box)&&world.isSpaceEmpty(pet,box)&&!world.containsFluid(box))candidates.add(pos);
        }
        candidates.sort(java.util.Comparator.comparingDouble(pos->pet.squaredDistanceTo(pos.toCenterPos())
            -(threat==null?0:.75*threat.squaredDistanceTo(pos.toCenterPos()))));
        for(var pos:candidates.stream().limit(12).toList()){
            var path=pet.getNavigation().findPathTo(pos,0);
            if(path!=null&&path.reachesTarget()&&pet.getNavigation().startMovingAlong(path,1.25))return;
        }
        if(wetTicks>0){
            for(var pos:candidates.stream().limit(12).toList()){
                var bank=Vec3d.ofBottomCenter(pos);
                var head=pet.getEyePos();
                if(world.raycast(new net.minecraft.world.RaycastContext(head,bank.add(0,.6,0),
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,pet))
                    .getType()!=net.minecraft.util.hit.HitResult.Type.MISS)continue;
                var direction=bank.subtract(pet.getPos()).multiply(1,0,1).normalize();
                pet.addVelocity(direction.x*.14,0,direction.z*.14);
                return;
            }
        }
    }

    public boolean pursue(MobEntity pet, LivingEntity target, double speed) {
        if(!guarding())return pet.getNavigation().startMovingTo(target,speed);
        var path=pet.getNavigation().findPathTo(target,0);
        if(!bounded(path)){pet.getNavigation().stop();return false;}
        return pet.getNavigation().startMovingAlong(path,speed);
    }

    private boolean bounded(net.minecraft.entity.ai.pathing.Path path) {
        if(path==null||!path.reachesTarget())return false;
        for(int i=path.getCurrentNodeIndex();i<path.getLength();i++){
            if(!inside(Vec3d.ofBottomCenter(path.getNode(i).getBlockPos())))return false;
        }
        return true;
    }

    public void guardTick(ServerWorld world, MobEntity pet, PlayerEntity owner, Runnable cancelAttack) {
        if(pet.getTarget()!=null&&!guardTarget(pet,owner,pet.getTarget())){
            pet.setTarget(null);
            cancelAttack.run();
            pet.getNavigation().stop();
        }
        if(pet.getTarget()!=null||post==null||pet.age%10!=0)return;
        if(inside(pet.getPos())){
            var enemies=world.getEntitiesByClass(HostileEntity.class,new Box(post,post).expand(RADIUS,5,RADIUS),
                target->guardTarget(pet,owner,target));
            enemies.sort(java.util.Comparator.comparingDouble(pet::squaredDistanceTo));
            for(var target:enemies.stream().limit(8).toList()){
                if(pet.canSee(target)&&pet.squaredDistanceTo(target)<=9){
                    pet.setTarget(target);return;
                }
                var path=pet.getNavigation().findPathTo(target,0);
                if(bounded(path)){pet.setTarget(target);return;}
            }
        }
        if(pet.squaredDistanceTo(post)>1)pet.getNavigation().startMovingTo(post.x,post.y,post.z,1);
        else pet.getNavigation().stop();
    }

    public void write(NbtCompound n, String prefix) {
        n.putInt(prefix+"Mode",mode.ordinal());
        n.putBoolean(prefix+"Waiting",waiting());
        n.putInt(prefix+"GuardRadius",RADIUS);
        n.putBoolean(prefix+"Retreating",retreating);
        if(post!=null){n.putDouble(prefix+"PostX",post.x);n.putDouble(prefix+"PostY",post.y);n.putDouble(prefix+"PostZ",post.z);}
    }

    public void read(NbtCompound n,String prefix) {
        int id=n.getInt(prefix+"Mode");
        mode=n.contains(prefix+"Mode")&&id>=0&&id<Mode.values().length ? Mode.values()[id]
            : n.getBoolean(prefix+"Waiting")?Mode.WAIT:Mode.FOLLOW;
        post=guarding()&&n.contains(prefix+"PostX") ? new Vec3d(n.getDouble(prefix+"PostX"),
            n.getDouble(prefix+"PostY"),n.getDouble(prefix+"PostZ")) : null;
        if(guarding()&&(post==null||!Double.isFinite(post.lengthSquared()))){mode=Mode.WAIT;post=null;}
        retreating=n.getBoolean(prefix+"Retreating");
    }
}
