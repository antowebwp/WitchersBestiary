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

public final class SchoolArmorDetails extends FeatureRenderer<AbstractClientPlayerEntity,PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final int STEEL=0, LEATHER=16, CLOTH=32, TRIM=48, LINING=64, GLASS=80;
    private final ModelPart[][][] models=new ModelPart[3][2][];

    public SchoolArmorDetails(PlayerEntityRenderer context){
        super(context);
        for(var school:SchoolArmor.School.values())for(int slim=0;slim<2;slim++)
            models[school.ordinal()][slim]=bake(school,slim!=0);
    }

    private static void box(ModelPartBuilder part,int material,float x,float y,float z,float w,float h,float d){
        part.uv(0,material).cuboid(x,y,z,w,h,d);
    }

    private static ModelPartData child(ModelPartData root,String name,ModelPartBuilder part){
        return root.addChild(name,part,ModelTransform.NONE);
    }

    private static void strap(ModelPartData torso,String name,float x,float z,float rotation){
        var strip=ModelPartBuilder.create();
        box(strip,LEATHER,-.65f,0,-.32f,1.3f,8.8f,.64f);
        box(strip,TRIM,-.75f,3.2f,-.52f,1.5f,1.15f,.55f);
        torso.addChild(name,strip,ModelTransform.of(x,.9f,z,0,0,rotation));
    }

    private static ModelPart[] bake(SchoolArmor.School school,boolean slim){
        var data=new ModelData();
        var root=data.getRoot();
        boolean bear=school==SchoolArmor.School.BEAR, cat=school==SchoolArmor.School.CAT;
        var cuirass=ModelPartBuilder.create();
        box(cuirass,LEATHER,-3.9f,1,-3.65f,7.8f,8.6f,7.3f);
        box(cuirass,cat?LEATHER:STEEL,bear?-4:-3.25f,1.4f,bear?-4.75f:-4.2f,bear?8:6.5f,bear?5.8f:4.2f,bear?1.6f:1.05f);
        box(cuirass,cat?LEATHER:STEEL,-3.55f,2,3.22f,7.1f,5.7f,bear?1.4f:.95f);
        box(cuirass,LEATHER,-4.25f,8.8f,-3.9f,8.5f,1.45f,7.8f);
        box(cuirass,TRIM,-1,8.9f,-4.45f,2,1.25f,.85f);
        box(cuirass,LEATHER,bear?-4.75f:2.7f,9.3f,2.5f,2.05f,2.4f,2.25f);
        box(cuirass,TRIM,bear?-4.2f:3.3f,9.6f,4.45f,.65f,.65f,.45f);
        if(bear){
            box(cuirass,LINING,-4.25f,-.25f,1.6f,8.5f,2.8f,3.4f);
            box(cuirass,LINING,-4.25f,-.15f,-4.15f,2.15f,2.5f,5.8f);
            box(cuirass,LINING,2.1f,-.15f,-4.15f,2.15f,2.5f,5.8f);
            box(cuirass,STEEL,-3.55f,6.15f,-4.55f,7.1f,1.8f,1.45f);
            box(cuirass,TRIM,-1.15f,3.1f,-5.3f,2.3f,2.3f,.9f);
            box(cuirass,STEEL,-.55f,3.65f,-5.65f,1.1f,1.2f,.6f);
        }else if(cat){
            box(cuirass,CLOTH,-3.25f,-.1f,-3.9f,6.5f,1.65f,7.8f);
            box(cuirass,CLOTH,1.8f,1,3.2f,1.65f,4.6f,1.15f);
            box(cuirass,TRIM,-.45f,4,-4.75f,.9f,1.45f,.7f);
        }else{
            box(cuirass,TRIM,-1.7f,3.2f,-4.85f,3.4f,.75f,.9f);
            box(cuirass,TRIM,-.4f,2,-4.95f,.8f,3.1f,1);
            box(cuirass,TRIM,-.95f,2.65f,-5.25f,1.9f,1.9f,.8f);
            for(int i=0;i<3;i++){
                float x=1.5f+i*.83f;
                box(cuirass,GLASS,x,8.9f,-4.65f,.62f,2.2f,1.15f);
                box(cuirass,TRIM,x+.07f,8.55f,-4.49f,.48f,.55f,.82f);
            }
        }
        var torso=child(root,"torso",cuirass);
        strap(torso,"front",2.8f,bear?-5.05f:-4.55f,.64f);
        strap(torso,"back",-2.8f,bear?4.8f:4.45f,-.64f);
        if(!cat)strap(torso,"cross",-2.8f,bear?-5.13f:-4.63f,-.64f);

        for(int side=0;side<2;side++){
            boolean right=side==0;
            float axis=right?(slim?-.5f:-1):(slim?.5f:1);
            float shoulder=bear?6.65f:cat?(right?4.65f:5.35f):5.85f;
            float outer=right?-.32f:.32f;
            var arm=ModelPartBuilder.create();
            box(arm,cat?LEATHER:LINING,axis-2.55f,-1.8f,-2.64f,5.1f,8.1f,5.28f);
            box(arm,STEEL,axis+outer-shoulder/2,-2.85f,-3.25f,shoulder,bear?3.3f:2.55f,6.5f);
            box(arm,bear?LINING:TRIM,axis+outer-shoulder/2+.2f,-3.25f,-2.98f,shoulder-.4f,.8f,5.96f);
            box(arm,STEEL,axis+outer-shoulder/2+.28f,-.65f,-3.02f,shoulder-.56f,2.05f,6.04f);
            if(bear)box(arm,STEEL,axis-2.45f,1.05f,-2.85f,4.9f,1.55f,5.7f);
            box(arm,cat?LEATHER:STEEL,axis-2.55f,4.7f,-3.2f,5.1f,3.65f,6.05f);
            box(arm,TRIM,axis-2.63f,7.4f,-3.28f,5.26f,.7f,6.21f);
            box(arm,cat?STEEL:TRIM,axis-.6f,5.7f,-3.45f,1.2f,1.1f,.55f);
            child(root,"arm"+side,arm);

            var thigh=ModelPartBuilder.create();
            float sign=right?-1:1;
            box(thigh,CLOTH,-1.85f,-.35f,-3.02f,3.7f,bear?5.4f:cat?3.3f:6.2f,.85f);
            box(thigh,CLOTH,-1.85f,.15f,2.45f,3.7f,bear?5.1f:cat?3.6f:5.7f,1.0f);
            box(thigh,bear?STEEL:LEATHER,sign<0?-3.04f:1.9f,.45f,-2.65f,1.14f,bear?4.4f:3.4f,5.3f);
            box(thigh,TRIM,sign<0?-3.1f:1.83f,1.4f,-2.78f,1.26f,.66f,5.56f);
            box(thigh,STEEL,-1.8f,4.9f,-3.12f,3.6f,2.6f,1.25f);
            if(cat)for(int j=0;j<3;j++)box(thigh,TRIM,-1.25f+j*.8f,1+j*.21f,-3.18f,.25f,1.2f,.3f);
            child(root,"thigh"+side,thigh);

            var boot=ModelPartBuilder.create();
            float height=cat?4.55f:3.65f;
            box(boot,LEATHER,-2.55f,11.8f-height,-2.67f,5.1f,height,5.34f);
            box(boot,STEEL,-2.43f,10.05f,bear?-3.67f:-3.35f,4.86f,2.12f,bear?2.1f:1.6f);
            box(boot,LEATHER,-2.61f,11.5f,-3.45f,5.22f,.76f,6.22f);
            box(boot,TRIM,-2.59f,8.65f,-2.78f,5.18f,.63f,5.56f);
            child(root,"boot"+side,boot);
        }
        var model=TexturedModelData.of(data,64,96).createModel();
        return java.util.stream.Stream.of("torso","arm0","arm1","thigh0","thigh1","boot0","boot1")
            .map(model::getChild).toArray(ModelPart[]::new);
    }

    @Override public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,
            float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch){
        if(player.isInvisible())return;
        var model=getContextModel();
        int slim=player.getSkinTextures().model()==net.minecraft.client.util.SkinTextures.Model.SLIM?1:0;
        renderSlot(player,EquipmentSlot.CHEST,slim,matrices,buffers,light,0,model.body,model.rightArm,model.leftArm);
        renderSlot(player,EquipmentSlot.LEGS,slim,matrices,buffers,light,3,model.rightLeg,model.leftLeg);
        renderSlot(player,EquipmentSlot.FEET,slim,matrices,buffers,light,5,model.rightLeg,model.leftLeg);
    }

    private void renderSlot(AbstractClientPlayerEntity player,EquipmentSlot slot,int slim,MatrixStack matrices,
            VertexConsumerProvider buffers,int light,int first,ModelPart... parents){
        if(!(player.getEquippedStack(slot).getItem() instanceof SchoolArmor armor))return;
        var school=armor.school();
        var pieces=models[school.ordinal()][slim];
        var texture=Identifier.of("witchersbestiary","textures/models/armor/"+school.name().toLowerCase(java.util.Locale.ROOT)+"_details.png");
        var vertices=buffers.getBuffer(RenderLayer.getEntityCutout(texture));
        for(int i=0;i<parents.length;i++){
            if(!parents[i].visible)continue;
            matrices.push();
            parents[i].rotate(matrices);
            pieces[first+i].render(matrices,vertices,light,OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}
