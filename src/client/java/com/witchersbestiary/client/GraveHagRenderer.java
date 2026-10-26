package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.GraveHagEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class GraveHagRenderer extends GeoEntityRenderer<GraveHagEntity>{public GraveHagRenderer(EntityRendererFactory.Context c){super(c,new GraveHagModel());shadowRadius=.48f;}}
