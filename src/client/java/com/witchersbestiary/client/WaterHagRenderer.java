package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.WaterHagEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class WaterHagRenderer extends GeoEntityRenderer<WaterHagEntity>{public WaterHagRenderer(EntityRendererFactory.Context c){super(c,new WaterHagModel());shadowRadius=.65f;addRenderLayer(new WaterHagEyeLayer(this));}}
