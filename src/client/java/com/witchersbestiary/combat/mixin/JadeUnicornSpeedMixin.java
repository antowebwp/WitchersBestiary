package com.witchersbestiary.combat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.witchersbestiary.unicorn.UnicornEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "snownee.jade.addon.vanilla.HorseStatsProvider", remap = false)
public abstract class JadeUnicornSpeedMixin {
    @WrapOperation(method = "appendTooltip", require = 0, at = {
            @At(value = "INVOKE", target = "Lsnownee/jade/addon/vanilla/HorseStatsProvider;switchText(Ljava/lang/String;ZDD)Lnet/minecraft/text/Text;", remap = false),
            @At(value = "INVOKE", target = "Lsnownee/jade/addon/vanilla/HorseStatsProvider;switchText(Ljava/lang/String;ZDD)Lnet/minecraft/class_2561;", remap = false)
    })
    private Text wb$unicornSpeed(String key, boolean details, double current, double max, Operation<Text> original,
                                 @Local AbstractHorseEntity horse) {
        if (horse instanceof UnicornEntity unicorn && key.equals("jade.horseStat.speed"))
            return unicorn.ridingSpeedText();
        return original.call(key, details, current, max);
    }
}
