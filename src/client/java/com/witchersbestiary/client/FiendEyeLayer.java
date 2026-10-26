package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.FiendEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Маска глаз рисуется аддитивно, без изменения базовой текстуры существа. */
public final class FiendEyeLayer extends GeoRenderLayer<FiendEntity> {
    private static final RenderLayer EYES = RenderLayer.getEyes(Identifier.of("witchersbestiary","textures/entity/fiend_glowing.png"));
    public FiendEyeLayer(GeoRenderer<FiendEntity> renderer) { super(renderer); }
    @Override public void render(MatrixStack matrices, FiendEntity entity, BakedGeoModel model, RenderLayer renderType,
            VertexConsumerProvider buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        if(!entity.isHypnotizing())return;
        getRenderer().reRender(model,matrices,buffers,entity,EYES,buffers.getBuffer(EYES),partialTick,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,overlay,0xffffffff);
    }
}
