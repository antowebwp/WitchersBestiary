package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.contracts.hunt.VampireHunt;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class PhylacteryUseMixin {
    @Unique private boolean wb$phylacteryPressSpent;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void wb$releasePhylacteryPress(CallbackInfo callback) {
        var client = (MinecraftClient)(Object)this;
        if (!client.options.useKey.isPressed()) wb$phylacteryPressSpent = false;
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void wb$onePhylacterySummonPerPress(CallbackInfo callback) {
        var client = (MinecraftClient)(Object)this;
        if (client.player == null) return;
        var main = client.player.getMainHandStack();
        var off = client.player.getOffHandStack();
        if (!main.isOf(ModItems.CRIMSON_PHYLACTERY) && !off.isOf(ModItems.CRIMSON_PHYLACTERY)) return;
        if (wb$phylacteryPressSpent && (VampireHunt.needsFreshPress(main) || VampireHunt.needsFreshPress(off))) {
            callback.cancel();
            return;
        }
        wb$phylacteryPressSpent = true;
    }
}
