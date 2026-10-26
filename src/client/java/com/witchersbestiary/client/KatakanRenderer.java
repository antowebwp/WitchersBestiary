package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.KatakanEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class KatakanRenderer extends GeoEntityRenderer<KatakanEntity> {
    public KatakanRenderer(EntityRendererFactory.Context context){super(context,new KatakanModel());shadowRadius=.45f;}
}
