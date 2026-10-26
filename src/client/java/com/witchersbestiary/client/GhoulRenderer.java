package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.GhoulEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GhoulRenderer extends GeoEntityRenderer<GhoulEntity> {
    public GhoulRenderer(EntityRendererFactory.Context context) {
        super(context, new GhoulModel());
        shadowRadius = .42f;
    }
}
