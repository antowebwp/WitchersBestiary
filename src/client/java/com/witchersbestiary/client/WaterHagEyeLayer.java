package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.WaterHagEntity;
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
public final class WaterHagEyeLayer extends GeoRenderLayer<WaterHagEntity> {
    private static final RenderLayer EYES = RenderLayer.getEyes(Identifier.of("witchersbestiary","textures/entity/water_hag_glowing.png"));
    public WaterHagEyeLayer(GeoRenderer<WaterHagEntity> renderer) { super(renderer); }
    @Override public void render(MatrixStack matrices, WaterHagEntity entity, BakedGeoModel model, RenderLayer renderType,
            VertexConsumerProvider buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        if(!entity.isAlive())return;
        getRenderer().reRender(model,matrices,buffers,entity,EYES,buffers.getBuffer(EYES),partialTick,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,overlay,0xffffffff);
    }
}
