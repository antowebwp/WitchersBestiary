package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.NoonwraithEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class NoonwraithRenderer extends GeoEntityRenderer<NoonwraithEntity> {
    public NoonwraithRenderer(EntityRendererFactory.Context context) {
        super(context, new NoonwraithModel()); shadowRadius = .25f;
    }
    @Override public software.bernie.geckolib.util.Color getRenderColor(NoonwraithEntity entity,float partial,int light){return software.bernie.geckolib.util.Color.ofARGB(Math.round(entity.phaseAlpha(partial)*255),255,255,255);}
    @Override public net.minecraft.client.render.RenderLayer getRenderType(NoonwraithEntity entity,net.minecraft.util.Identifier texture,net.minecraft.client.render.VertexConsumerProvider buffers,float partial){
        if(entity.getPhase()==com.witchersbestiary.entity.monster.NoonwraithEntity.Phase.DISSOLVING||entity.getPhase()==com.witchersbestiary.entity.monster.NoonwraithEntity.Phase.REAPPEARING)return net.minecraft.client.render.RenderLayer.getEntityTranslucent(texture);
        return super.getRenderType(entity,texture,buffers,partial);
    }
}
