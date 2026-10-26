package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.NekkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class NekkerRenderer extends GeoEntityRenderer<NekkerEntity>{public NekkerRenderer(EntityRendererFactory.Context c){super(c,new NekkerModel());scaleWidth=.9f;scaleHeight=.9f;shadowRadius=.4f;}}
