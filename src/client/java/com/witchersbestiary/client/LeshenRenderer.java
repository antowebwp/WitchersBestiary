package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.LeshenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class LeshenRenderer extends GeoEntityRenderer<LeshenEntity> {
    public LeshenRenderer(EntityRendererFactory.Context context) {
        super(context, new LeshenModel());
        shadowRadius = 0.9f;
        addRenderLayer(new LeshenEyesLayer(this));
    }
}
