package com.witchersbestiary.combat.mixin;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class BasiliskLavaContactMixin {
    @Inject(method="setOnFireFromLava",at=@At("HEAD"),cancellable=true)
    private void protectRiderFromContact(CallbackInfo ci){if(com.witchersbestiary.combat.BasiliskRideProtection.active((Entity)(Object)this))ci.cancel();}
}
