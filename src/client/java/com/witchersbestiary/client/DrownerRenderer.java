package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.DrownerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class DrownerRenderer extends GeoEntityRenderer<DrownerEntity> {
    public DrownerRenderer(EntityRendererFactory.Context context){super(context,new DrownerModel());shadowRadius=.45f;}
}
