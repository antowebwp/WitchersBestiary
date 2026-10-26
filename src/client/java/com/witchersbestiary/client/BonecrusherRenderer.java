package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.BonecrusherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class BonecrusherRenderer extends GeoEntityRenderer<BonecrusherEntity>{public BonecrusherRenderer(EntityRendererFactory.Context c){super(c,new BonecrusherModel());shadowRadius=1;}}
