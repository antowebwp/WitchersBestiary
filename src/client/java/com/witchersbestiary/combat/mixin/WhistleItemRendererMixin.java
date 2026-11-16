package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.unicorn.Unicorns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
abstract class WhistleItemRendererMixin {
    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"), argsOnly = true)
    private BakedModel witcher$whistleGuiIcon(BakedModel model, ItemStack stack, ModelTransformationMode mode) {
        if (mode != ModelTransformationMode.GUI || !stack.isOf(Unicorns.WHISTLE)) return model;
        var manager = MinecraftClient.getInstance().getBakedModelManager();
        var icon = manager.getModel(Identifier.of("witchersbestiary", "item/unicorn_whistle_icon"));
        return icon == manager.getMissingModel() ? model : icon;
    }
}
