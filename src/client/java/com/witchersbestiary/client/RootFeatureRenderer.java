package com.witchersbestiary.client;

import com.witchersbestiary.combat.RootBinding;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

/** Roots are visual only; the server's timed binding owns movement. */
public final class RootFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity,PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier TEXTURE=Identifier.of("witchersbestiary","textures/entity/binding_roots.png");
    private final ModelPart roots;
    public RootFeatureRenderer(PlayerEntityRenderer context){super(context);
        ModelData data=new ModelData();data.getRoot().addChild("roots",ModelPartBuilder.create()
            .uv(0,0).cuboid(-6,21,-1,12,2,2).uv(0,8).cuboid(-1,20,-6,2,2,12)
            .uv(0,24).cuboid(-5,18,-5,2,6,2).uv(12,24).cuboid(3,18,3,2,6,2)
            .uv(24,24).cuboid(-5,20,3,2,4,2).uv(36,24).cuboid(3,19,-5,2,5,2),ModelTransform.NONE);
        roots=TexturedModelData.of(data,64,64).createModel();
    }
    @Override public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch){
        if(!RootBinding.active(player))return;
        roots.render(matrices,buffers.getBuffer(RenderLayer.getEntityCutout(TEXTURE)),light,OverlayTexture.DEFAULT_UV);
    }
}
