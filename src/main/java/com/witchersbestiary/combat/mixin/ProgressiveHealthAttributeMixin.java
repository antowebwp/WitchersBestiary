package com.witchersbestiary.combat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.witchersbestiary.combat.ProgressiveDifficulty;
import net.minecraft.entity.attribute.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only attributes carrying WB's per-instance modifier can exceed vanilla's 1024HP ceiling.
 * Tracked modifiers synchronize this rule to clients; no global attribute/type limit changes. */
@Mixin(EntityAttributeInstance.class)
public abstract class ProgressiveHealthAttributeMixin {
    @WrapOperation(method="computeValue",at=@At(value="INVOKE",target="Lnet/minecraft/entity/attribute/EntityAttribute;clamp(D)D"))
    private double progressiveHealthLimit(EntityAttribute attribute,double value,Operation<Double> original){
        var instance=(EntityAttributeInstance)(Object)this;
        return attribute==EntityAttributes.GENERIC_MAX_HEALTH.value()&&instance.hasModifier(ProgressiveDifficulty.HEALTH)
            ?Math.max(1,value):original.call(attribute,value);
    }
}
