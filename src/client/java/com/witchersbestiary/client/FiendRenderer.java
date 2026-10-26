package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.FiendEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class FiendRenderer extends GeoEntityRenderer<FiendEntity>{public FiendRenderer(EntityRendererFactory.Context c){super(c,new FiendModel());shadowRadius=1.5f;addRenderLayer(new FiendEyeLayer(this));}}
