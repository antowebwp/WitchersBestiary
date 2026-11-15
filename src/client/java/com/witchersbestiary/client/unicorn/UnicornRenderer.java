package com.witchersbestiary.client.unicorn;

import com.witchersbestiary.unicorn.UnicornEntity;
import com.witchersbestiary.unicorn.Unicorns;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class UnicornRenderer extends GeoEntityRenderer<UnicornEntity> {
    public UnicornRenderer(EntityRendererFactory.Context context) {
        super(context, new GeoModel<>() {
            @Override public Identifier getModelResource(UnicornEntity entity) { return asset("geo/unicorn.geo.json"); }
            @Override public Identifier getTextureResource(UnicornEntity entity) {
                String material = armor(entity);
                return asset("textures/entity/unicorn" + (material == null ? "" : "_armor_" + material) + ".png");
            }
            @Override public void setCustomAnimations(UnicornEntity entity, long id,
                software.bernie.geckolib.animation.AnimationState<UnicornEntity> state) {
                getBone("saddle").ifPresent(bone -> bone.setHidden(!entity.isSaddled()));
                getBone("horn").ifPresent(bone -> bone.setHidden(false));
                getBone("armor_shell").ifPresent(bone -> bone.setHidden(armor(entity) == null));
            }
            @Override public Identifier getAnimationResource(UnicornEntity entity) { return asset("animations/unicorn.animation.json"); }
        });
        shadowRadius = .68f;
        addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<UnicornEntity>(this) {
            @Override public void render(net.minecraft.client.util.math.MatrixStack matrices, UnicornEntity entity,
                software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.render.RenderLayer layer,
                net.minecraft.client.render.VertexConsumerProvider buffers, net.minecraft.client.render.VertexConsumer buffer,
                float delta, int light, int overlay) {
                if (entity.healing()) super.render(matrices, entity, model, layer, buffers, buffer, delta, light, overlay);
            }
        });
    }

    @Override protected int getBlockLight(UnicornEntity entity, net.minecraft.util.math.BlockPos pos) {
        return Math.max(8, super.getBlockLight(entity, pos));
    }

    private static String armor(UnicornEntity entity) {
        var stack = entity.getBodyArmor();
        if (stack.isOf(net.minecraft.item.Items.LEATHER_HORSE_ARMOR)) return "leather";
        if (stack.isOf(net.minecraft.item.Items.IRON_HORSE_ARMOR)) return "iron";
        if (stack.isOf(net.minecraft.item.Items.GOLDEN_HORSE_ARMOR)) return "gold";
        if (stack.isOf(net.minecraft.item.Items.DIAMOND_HORSE_ARMOR)) return "diamond";
        return null;
    }

    private static Identifier asset(String path) { return Identifier.of("witchersbestiary", path); }
    public static void initialize() {
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Unicorns.TYPE, UnicornRenderer::new);
    }
}
