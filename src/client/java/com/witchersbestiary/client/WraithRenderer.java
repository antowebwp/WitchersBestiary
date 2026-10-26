package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.WraithEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class WraithRenderer extends GeoEntityRenderer<WraithEntity>{public WraithRenderer(EntityRendererFactory.Context c){super(c,new WraithModel());shadowRadius=.35f;}    @Override public software.bernie.geckolib.util.Color getRenderColor(WraithEntity entity,float partial,int light){return software.bernie.geckolib.util.Color.ofARGB(Math.round(entity.phaseAlpha(partial)*255),255,255,255);}
    @Override public net.minecraft.client.render.RenderLayer getRenderType(WraithEntity entity,net.minecraft.util.Identifier texture,net.minecraft.client.render.VertexConsumerProvider buffers,float partial){
        if(entity.getPhase()==com.witchersbestiary.entity.monster.NoonwraithEntity.Phase.DISSOLVING||entity.getPhase()==com.witchersbestiary.entity.monster.NoonwraithEntity.Phase.REAPPEARING)return net.minecraft.client.render.RenderLayer.getEntityTranslucent(texture);
        return super.getRenderType(entity,texture,buffers,partial);
    }
}
