package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.combat.*;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MobEntity.class)
public abstract class MonsterAiMixin implements com.witchersbestiary.lure.LureGoalAccess {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final protected net.minecraft.entity.ai.goal.GoalSelector goalSelector;
    @org.spongepowered.asm.mixin.Unique private boolean witchersbestiary$lureGoalAdded;
    @Override public void witchersbestiary$addLureGoal(net.minecraft.entity.ai.goal.Goal goal){if(!witchersbestiary$lureGoalAdded){witchersbestiary$lureGoalAdded=true;goalSelector.add(2,goal);}}

    @Inject(method="initialize",at=@At("RETURN"))
    private void progressiveSpawn(net.minecraft.world.ServerWorldAccess world,net.minecraft.world.LocalDifficulty difficulty,net.minecraft.entity.SpawnReason reason,net.minecraft.entity.EntityData data,CallbackInfoReturnable<net.minecraft.entity.EntityData> cir){
        ProgressiveDifficulty.freshSpawn((MobEntity)(Object)this);
    }
    @Inject(method="tickNewAi", at=@At("HEAD"), cancellable=true)
    private void blockStunnedAi(CallbackInfo ci) {
        var mob=(MobEntity)(Object)this;
        if(Stun.active(mob)) {Stun.stop(mob);ci.cancel();}
    }
    @Inject(method="tryAttack", at=@At("HEAD"), cancellable=true)
    private void blockStunnedAttack(net.minecraft.entity.Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(Stun.active((MobEntity)(Object)this)) cir.setReturnValue(false);
    }
    @org.spongepowered.asm.mixin.Unique private long witchersbestiary$despawnGraceUntil=Long.MIN_VALUE;
    @Inject(method="checkDespawn",at=@At("HEAD"),cancellable=true)
    private void protectActiveMonster(CallbackInfo ci){
        var mob=(MobEntity)(Object)this;
        if(!MonsterDespawnPolicy.applies(mob)||mob.getWorld().isClient||mob.getWorld().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||mob.isPersistent()||mob.cannotDespawn())return;
        long now=mob.getWorld().getTime();
        if(witchersbestiary$despawnGraceUntil==Long.MIN_VALUE)witchersbestiary$despawnGraceUntil=now+MonsterDespawnPolicy.idleTicks(mob);
        if(mob.hurtTime>0||MonsterDespawnPolicy.engaged(mob))witchersbestiary$despawnGraceUntil=Math.max(witchersbestiary$despawnGraceUntil,now+MonsterDespawnPolicy.COMBAT_GRACE);
        if(now<witchersbestiary$despawnGraceUntil){mob.setDespawnCounter(0);ci.cancel();}
    }
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/entity/SpawnGroup;getImmediateDespawnRange()I"))
    private int monsterHardRange(int original){return MonsterDespawnPolicy.applies((MobEntity)(Object)this)?MonsterDespawnPolicy.HARD_DISTANCE:original;}
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/entity/SpawnGroup;getDespawnStartRange()I"))
    private int monsterSoftRange(int original){return MonsterDespawnPolicy.applies((MobEntity)(Object)this)?MonsterDespawnPolicy.SOFT_DISTANCE:original;}
    @ModifyConstant(method="checkDespawn",constant=@Constant(intValue=600))
    private int monsterIdleDelay(int original){var mob=(MobEntity)(Object)this;return MonsterDespawnPolicy.applies(mob)?MonsterDespawnPolicy.idleTicks(mob):original;}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void saveDespawnGrace(net.minecraft.nbt.NbtCompound n,CallbackInfo ci){
        var mob=(MobEntity)(Object)this;if(!MonsterDespawnPolicy.applies(mob))return;
        long left=witchersbestiary$despawnGraceUntil==Long.MIN_VALUE?MonsterDespawnPolicy.idleTicks(mob):witchersbestiary$despawnGraceUntil-mob.getWorld().getTime();
        n.putLong("WitcherDespawnGrace",Math.clamp(left,0,MonsterDespawnPolicy.idleTicks(mob)));
    }
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void loadDespawnGrace(net.minecraft.nbt.NbtCompound n,CallbackInfo ci){
        var mob=(MobEntity)(Object)this;if(!MonsterDespawnPolicy.applies(mob))return;
        long left=n.contains("WitcherDespawnGrace")?Math.clamp(n.getLong("WitcherDespawnGrace"),0,MonsterDespawnPolicy.idleTicks(mob)):MonsterDespawnPolicy.idleTicks(mob);
        witchersbestiary$despawnGraceUntil=mob.getWorld().getTime()+left;
    }

}
