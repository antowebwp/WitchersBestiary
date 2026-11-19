package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.equipment.armory.BloodAuraState;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
abstract class BloodAuraPlayerMixin implements BloodAuraState {
    @Unique private static final TrackedData<Boolean> BESTIARY_BLOOD_AURA =
            DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void bestiary$initBloodAura(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(BESTIARY_BLOOD_AURA, false);
    }

    @Override public boolean bestiary$bloodAura() {
        return ((PlayerEntity)(Object)this).getDataTracker().get(BESTIARY_BLOOD_AURA);
    }

    @Override public void bestiary$setBloodAura(boolean active) {
        ((PlayerEntity)(Object)this).getDataTracker().set(BESTIARY_BLOOD_AURA, active);
    }
}
