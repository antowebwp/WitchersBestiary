package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.WerewolfEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class WerewolfRenderer extends GeoEntityRenderer<WerewolfEntity> {
    private final HumanRenderer human;
    private static final class HumanRenderer extends net.minecraft.client.render.entity.MobEntityRenderer<WerewolfEntity,net.minecraft.client.render.entity.model.VillagerResemblingModel<WerewolfEntity>> {
        HumanRenderer(EntityRendererFactory.Context context){super(context,new net.minecraft.client.render.entity.model.VillagerResemblingModel<>(context.getPart(net.minecraft.client.render.entity.model.EntityModelLayers.VILLAGER)),.4f);}
        @Override public net.minecraft.util.Identifier getTexture(WerewolfEntity entity){return net.minecraft.util.Identifier.ofVanilla("textures/entity/villager/villager.png");}
    }
    @Override public void render(WerewolfEntity entity,float yaw,float tickDelta,net.minecraft.client.util.math.MatrixStack matrices,net.minecraft.client.render.VertexConsumerProvider buffers,int light){
        if(entity.isHumanForm()&&!entity.isTransforming())human.render(entity,yaw,tickDelta,matrices,buffers,light);else super.render(entity,yaw,tickDelta,matrices,buffers,light);
    }

    public WerewolfRenderer(EntityRendererFactory.Context context) {
        super(context, new WerewolfModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        human=new HumanRenderer(context);
        shadowRadius = .42f;
    }
}
