package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.FogletEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class FogletRenderer extends GeoEntityRenderer<FogletEntity> {
    public FogletRenderer(EntityRendererFactory.Context context) {
        super(context, new FogletModel());
        shadowRadius = .42f;
    }
}
