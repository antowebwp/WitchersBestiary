package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.GriffinEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class GriffinRenderer extends GeoEntityRenderer<GriffinEntity>{public GriffinRenderer(EntityRendererFactory.Context c){super(c,new GriffinModel());shadowRadius=1.2f;}}
