package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.LeshenEntity;
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
public final class LeshenEyesLayer extends GeoRenderLayer<LeshenEntity> {
    private static final RenderLayer EYES = RenderLayer.getEyes(Identifier.of("witchersbestiary","textures/entity/leshen_glowing.png"));
    public LeshenEyesLayer(GeoRenderer<LeshenEntity> renderer) { super(renderer); }
    @Override public void render(MatrixStack matrices, LeshenEntity entity, BakedGeoModel model, RenderLayer renderType,
            VertexConsumerProvider buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        getRenderer().reRender(model,matrices,buffers,entity,EYES,buffers.getBuffer(EYES),partialTick,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,overlay,0xffffffff);
    }
}
