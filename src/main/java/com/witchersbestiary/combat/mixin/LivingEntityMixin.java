package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.combat.AlchemyDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements com.witchersbestiary.alchemy.OilPreferences, com.witchersbestiary.artifact.ArtifactCooldowns, com.witchersbestiary.accessory.VampireBloodReserve {
    @org.spongepowered.asm.mixin.Unique private boolean witchersbestiary$autoOils=true;
    public boolean bestiary$autoOils(){return witchersbestiary$autoOils;}
    public void bestiary$autoOils(boolean enabled){witchersbestiary$autoOils=enabled;}
    @org.spongepowered.asm.mixin.Unique private long witchersbestiary$rootReady,witchersbestiary$wolfReady;
    public long bestiary$rootsReady(){return witchersbestiary$rootReady;}
    public void bestiary$rootsReady(long t){witchersbestiary$rootReady=t;}
    public long bestiary$wolfReady(){return witchersbestiary$wolfReady;}
    public void bestiary$wolfReady(long t){witchersbestiary$wolfReady=t;}
    @Inject(method="getNextAirUnderwater",at=@At("RETURN"),cancellable=true)
    private void artifactAir(int air,CallbackInfoReturnable<Integer> cir){cir.setReturnValue(com.witchersbestiary.artifact.Artifacts.air((LivingEntity)(Object)this,air,cir.getReturnValue()));}
    @org.spongepowered.asm.mixin.Unique private float witchersbestiary$bloodDrawn;
    public float bestiary$bloodDrawn(){return witchersbestiary$bloodDrawn;}
    public void bestiary$bloodDrawn(float amount){witchersbestiary$bloodDrawn=Float.isFinite(amount)?Math.clamp(amount,0,4):4;}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void saveBlood(net.minecraft.nbt.NbtCompound n,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){n.putBoolean("WitcherAutoOils",witchersbestiary$autoOils);n.putLong("WitcherRootRingReady",witchersbestiary$rootReady);n.putLong("WitcherWolfRingReady",witchersbestiary$wolfReady);if(witchersbestiary$bloodDrawn>0)n.putFloat("WitcherBloodDrawn",witchersbestiary$bloodDrawn);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void loadBlood(net.minecraft.nbt.NbtCompound n,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){bestiary$bloodDrawn(n.getFloat("WitcherBloodDrawn"));witchersbestiary$autoOils=!n.contains("WitcherAutoOils")||n.getBoolean("WitcherAutoOils");witchersbestiary$rootReady=n.getLong("WitcherRootRingReady");witchersbestiary$wolfReady=n.getLong("WitcherWolfRingReady");}
    @Inject(method="tick",at=@At("TAIL"))
    private void rootTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){com.witchersbestiary.combat.ProgressiveDifficulty.tick((LivingEntity)(Object)this);com.witchersbestiary.combat.RootBinding.tick((LivingEntity)(Object)this);com.witchersbestiary.combat.Stun.sync((LivingEntity)(Object)this);if(com.witchersbestiary.combat.Stun.active((LivingEntity)(Object)this))com.witchersbestiary.combat.Stun.stop((LivingEntity)(Object)this);}
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void rootTravel(net.minecraft.util.math.Vec3d input,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var e=(LivingEntity)(Object)this;if(com.witchersbestiary.combat.RootBinding.active(e)||com.witchersbestiary.combat.Stun.active(e)){e.setVelocity(net.minecraft.util.math.Vec3d.ZERO);ci.cancel();}}
    @Inject(method="jump",at=@At("HEAD"),cancellable=true)
    private void rootJump(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(com.witchersbestiary.combat.RootBinding.active((LivingEntity)(Object)this)||com.witchersbestiary.combat.Stun.active((LivingEntity)(Object)this))ci.cancel();}

    @org.spongepowered.asm.mixin.injection.ModifyArgs(method="applyArmorToDamage",at=@At(value="INVOKE",target="Lnet/minecraft/entity/DamageUtil;getDamageLeft(Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/entity/damage/DamageSource;FF)F"))
    private void progressiveArmor(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        args.set(3,com.witchersbestiary.combat.ProgressiveDifficulty.armor(args.get(2),args.get(3)));
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="damage")
    private boolean witchersbestiary$damage(DamageSource source,float amount,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        var victim=(LivingEntity)(Object)this;
        if(source.isOf(net.minecraft.entity.damage.DamageTypes.LAVA)&&com.witchersbestiary.combat.BasiliskRideProtection.active(victim))return original.call(source,0f);
        if(source.getAttacker() instanceof com.witchersbestiary.combat.CategorizedMonster&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE))amount=com.witchersbestiary.config.ModConfig.damage(amount);
        amount=com.witchersbestiary.combat.ProgressiveDifficulty.damage(source,amount);
        amount=com.witchersbestiary.accessory.HunterAccessories.protect(victim,source,com.witchersbestiary.artifact.Artifacts.protection(victim,source,amount));
        amount=com.witchersbestiary.equipment.armory.ArmoryCombat.apply(victim,source,amount);
        var oil=Float.isFinite(amount)&&amount>0?com.witchersbestiary.alchemy.InventoryOils.prepare(victim,source):null;
        float before=victim.getHealth();
        boolean result=original.call(source,AlchemyDamage.withOil(victim,source,amount,oil));
        float actual=Math.max(0,before-victim.getHealth());
        if(result&&actual>0){com.witchersbestiary.alchemy.InventoryOils.commit(oil);com.witchersbestiary.accessory.HunterAccessories.afterDamage(victim,source,actual);}
        return result;
    }
}
