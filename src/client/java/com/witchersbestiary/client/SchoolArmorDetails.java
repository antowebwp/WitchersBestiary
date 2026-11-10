package com.witchersbestiary.client;

import com.witchersbestiary.equipment.armory.SchoolArmor;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/** School-specific relief follows the vanilla armor pose and preserves its render layers. */
public final class SchoolArmorDetails extends FeatureRenderer<AbstractClientPlayerEntity,PlayerEntityModel<AbstractClientPlayerEntity>> {
    private final ModelPart[][] details=new ModelPart[3][3];

    public SchoolArmorDetails(PlayerEntityRenderer context){
        super(context);
        for(var school:SchoolArmor.School.values()){
            var data=new ModelData();
            var chest=ModelPartBuilder.create();
            switch(school){
                case GRIFFIN -> chest.uv(0,0).cuboid(-2,2,-3.18f,4,4,1);
                case BEAR -> chest.uv(0,0).cuboid(-3,1,-3.26f,6,5,1)
                    .uv(0,10).cuboid(-3.5f,0,2.16f,7,3,1);
                case CAT -> chest.uv(0,0).cuboid(-3,.25f,-3.12f,6,2,1)
                    .uv(0,10).cuboid(-1,6,-3.2f,2,2,1);
            }
            data.getRoot().addChild("chest",chest,ModelTransform.NONE);
            float depth=school==SchoolArmor.School.BEAR?5.6f:5.25f;
            float height=school==SchoolArmor.School.CAT?2.2f:3.2f;
            for(int side=0;side<2;side++){
                var plate=ModelPartBuilder.create().uv(0,20)
                    .cuboid(side==0?-3.65f:-1.65f,-2.7f,-depth/2,5.3f,height,depth)
                    .uv(24,20).cuboid(side==0?-3.7f:-1.7f,5,-2.7f,5.4f,2,5.4f);
                data.getRoot().addChild("arm"+side,plate,ModelTransform.NONE);
            }
            var model=TexturedModelData.of(data,64,64).createModel();
            details[school.ordinal()]=new ModelPart[]{model.getChild("chest"),model.getChild("arm0"),model.getChild("arm1")};
        }
    }

    @Override public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,
            float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch){
        if(player.isInvisible()||!(player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof SchoolArmor armor))return;
        var model=getContextModel();
        var pieces=details[armor.school().ordinal()];
        var texture=Identifier.of("witchersbestiary","textures/models/armor/"+armor.school().name().toLowerCase(java.util.Locale.ROOT)+"_details.png");
        var vertices=buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        draw(pieces[0],model.body,matrices,vertices,light);
        draw(pieces[1],model.rightArm,matrices,vertices,light);
        draw(pieces[2],model.leftArm,matrices,vertices,light);
    }

    private static void draw(ModelPart detail,ModelPart parent,MatrixStack matrices,VertexConsumer vertices,int light){
        if(!parent.visible)return;
        matrices.push();
        parent.rotate(matrices);
        detail.render(matrices,vertices,light,OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
