package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.EndregaWarriorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class EndregaWarriorRenderer extends GeoEntityRenderer<EndregaWarriorEntity>{public EndregaWarriorRenderer(EntityRendererFactory.Context c){super(c,new EndregaWarriorModel());shadowRadius=1.15f;}}
