package com.witchersbestiary.combat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.witchersbestiary.client.vfx.BloodLegacyFx;
import com.witchersbestiary.equipment.armory.BloodAuraState;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
abstract class BloodLegacyItemRendererMixin {
    @Unique private static final String DRAW = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V";
    @Unique private boolean bestiary$drawBloodAura;

    @WrapOperation(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;" + DRAW))
    private void bestiary$scopeBloodAura(ItemRenderer renderer, ItemStack stack, ModelTransformationMode mode,
            boolean leftHanded, MatrixStack matrices, VertexConsumerProvider buffers, int light, int overlay,
            BakedModel model, Operation<Void> original, LivingEntity holder) {
        boolean previous = bestiary$drawBloodAura;
        boolean held = switch (mode) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> true;
            default -> false;
        };
        bestiary$drawBloodAura = held && stack.isOf(ModItems.BLOOD_LEGACY) && holder instanceof BloodAuraState aura
                && aura.bestiary$bloodAura() && holder.isAlive() && holder.getMainHandStack().isOf(ModItems.BLOOD_LEGACY)
                && leftHanded == (holder.getMainArm() == Arm.LEFT);
        try {
            original.call(renderer, stack, mode, leftHanded, matrices, buffers, light, overlay, model);
        } finally {
            bestiary$drawBloodAura = previous;
        }
    }

    @Inject(method = DRAW, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void bestiary$renderBloodAura(ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
            MatrixStack matrices, VertexConsumerProvider buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (!bestiary$drawBloodAura || !stack.isOf(ModItems.BLOOD_LEGACY)) return;
        var client = MinecraftClient.getInstance();
        if (client.world == null) return;
        matrices.push();
        try {
            // Baked quads are in block coordinates. The blade mesh is centered on the model pivot.
            matrices.translate(.5, .5, .5);
            BloodLegacyFx.renderBlade(matrices, buffers,
                    client.world.getTime() + client.getRenderTickCounter().getTickDelta(false));
        } finally {
            matrices.pop();
        }
    }
}
