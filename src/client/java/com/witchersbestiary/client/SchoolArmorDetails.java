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
    private final ModelPart[][][][] models=new ModelPart[3][2][3][];

    public SchoolArmorDetails(PlayerEntityRenderer context){
        super(context);
        for(var school:SchoolArmor.School.values())for(int slim=0;slim<2;slim++)
            for(int tier=0;tier<3;tier++)
                models[school.ordinal()][slim][tier]=bake(school,slim!=0,tier);
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

    private static void angled(ModelPartData root,String name,int material,float x,float y,float z,
            float w,float h,float d,float pitch,float roll){
        var piece=ModelPartBuilder.create();
        box(piece,material,-w/2,-h/2,-d/2,w,h,d);
        root.addChild(name,piece,ModelTransform.of(x,y,z,pitch,0,roll));
    }

    private static void bearTorso(ModelPartData torso){
        angled(torso,"keel",STEEL,0,3.3f,-4.6f,3.7f,4.7f,1.55f,-.10f,0);
        for(int side=-1;side<=1;side+=2){
            angled(torso,"rib"+side,STEEL,side*2.7f,4,-4.25f,2.45f,4.75f,1.45f,-.12f,side*.20f);
            angled(torso,"lower"+side,STEEL,side*1.95f,7.85f,-4.06f,3.6f,1.05f,1.13f,-.14f,side*.05f);
            angled(torso,"pouch"+side,LEATHER,side*3.55f,10.1f,3.85f,2.05f,2.45f,1.85f,.05f,side*.12f);
            angled(torso,"flap"+side,TRIM,side*3.55f,9.7f,4.8f,1.9f,.6f,.48f,0,side*.12f);
            angled(torso,"collar"+side,LINING,side*2.75f,.54f,-3.03f,2.3f,2.35f,2.15f,-.27f,side*.29f);
        }
        for(int lock=-2;lock<=2;lock++){
            float length=3.1f-Math.abs(lock)*.6f;
            angled(torso,"fur"+lock,LINING,lock*1.53f,1.05f+length*.23f,3.75f,1.72f,length,1.45f,.28f,lock*.12f);
        }
        angled(torso,"medallion",TRIM,0,3.45f,-5.69f,2.15f,2.15f,.58f,0,.785f);
        angled(torso,"medallionRelief",STEEL,0,3.55f,-6.02f,1.1f,1.7f,.45f,0,0);
        angled(torso,"buckle",TRIM,0,9.55f,-4.63f,2.55f,1.55f,.7f,0,0);
        angled(torso,"buckleInset",LEATHER,0,9.55f,-5.04f,1.53f,.73f,.22f,0,0);
    }

    private static void bearArm(ModelPartData arm,float axis,float side){
        angled(arm,"furCap",LINING,axis,-2.7f,-.13f,4.9f,1.6f,5.8f,0,side*.17f);
        angled(arm,"pauldronTop",STEEL,axis+side*.38f,-2.05f,0,6.15f,1.35f,6.5f,0,side*.17f);
        angled(arm,"pauldronEdge",TRIM,axis+side*.68f,-1.54f,-3.24f,5.65f,.4f,.5f,0,side*.17f);
        angled(arm,"pauldronMiddle",STEEL,axis+side*.64f,-.63f,.06f,6.05f,1.15f,6.65f,0,side*.23f);
        angled(arm,"pauldronLower",STEEL,axis+side*.5f,.69f,.08f,5.65f,1.05f,6.32f,0,side*.27f);
        angled(arm,"elbow",STEEL,axis+side*.32f,3.8f,2.84f,3.35f,2.35f,1.15f,.2f,side*.12f);
        angled(arm,"vambrace",STEEL,axis,5.75f,-3.16f,4.5f,3.25f,1.45f,-.12f,0);
        angled(arm,"cuff",TRIM,axis,7.8f,-.12f,5.8f,.72f,6.4f,0,0);
        angled(arm,"knuckles",STEEL,axis,8.8f,-3,3.7f,1.15f,.85f,0,0);
    }

    private static void lightTorso(ModelPartData torso, boolean cat){
        if(cat){
            angled(torso,"scarfFold",CLOTH,0,.95f,-3.75f,6,1.3f,.8f,-.12f,.08f);
            angled(torso,"scarfTail",CLOTH,2.5f,2.5f,4.05f,1.35f,3.9f,.55f,.10f,-.20f);
            for(int i=0;i<3;i++){
                angled(torso,"abdomen"+i,LEATHER,0,5.35f+i*.89f,-3.91f,5.8f-i*.35f,.73f,.92f,-.19f,0);
                angled(torso,"spine"+i,STEEL,0,3.5f+i*1.3f,4.03f,2.1f,.85f,.78f,.12f,0);
            }
            angled(torso,"sideWallet",LEATHER,-3.25f,9.8f,2.8f,1.9f,1.95f,2.05f,0,-.15f);
            angled(torso,"sideWalletClasp",TRIM,-3.25f,9.55f,3.9f,.55f,.6f,.45f,0,-.15f);
        }else{
            for(int side=-1;side<=1;side+=2){
                angled(torso,"giltRib"+side,STEEL,side*2.3f,4.1f,-4.01f,2.4f,3.2f,1.12f,-.11f,side*.24f);
                angled(torso,"shoulderAnchor"+side,TRIM,side*2.5f,1.55f,4.11f,1.5f,1.15f,.68f,0,side*.12f);
            }
            angled(torso,"backplate",STEEL,0,4,3.86f,5.85f,4.35f,1.3f,.12f,0);
            angled(torso,"rearTabard",CLOTH,0,7.2f,4.3f,3.4f,3.7f,.66f,.06f,0);
            angled(torso,"rearTabardHem",TRIM,0,8.47f,4.73f,3.6f,.55f,.36f,0,0);
            for(int spoke=0;spoke<4;spoke++)
                angled(torso,"sunRay"+spoke,TRIM,0,3.55f,-5.14f,3.75f,.32f,.54f,0,spoke*.785f);
            angled(torso,"sunBoss",TRIM,0,3.55f,-5.48f,1.25f,1.25f,.66f,0,.785f);
        }
    }

    private static void lightArm(ModelPartData arm,float axis,float side,boolean cat){
        float width=cat?(side<0?4.3f:4.95f):5.55f;
        angled(arm,"shoulderTop",STEEL,axis+side*.35f,-1.75f,-.04f,width,1.5f,6.15f,0,side*.19f);
        angled(arm,"shoulderHem",cat?LEATHER:TRIM,axis+side*.4f,-.95f,-3.16f,width-.28f,.47f,.52f,0,side*.19f);
        angled(arm,"shoulderScale",STEEL,axis+side*.42f,-.29f,0,width-.35f,.86f,5.85f,0,side*.23f);
        angled(arm,"bracer",cat?LEATHER:STEEL,axis,5.9f,-3.07f,3.95f,3.25f,1.35f,-.14f,side*.07f);
        angled(arm,"bracerStrap",cat?TRIM:LEATHER,axis,7.15f,-3.54f,4.08f,.62f,.54f,0,side*.07f);
        if(!cat)angled(arm,"bracerRune",TRIM,axis,5.75f,-3.85f,1.15f,1.15f,.35f,0,.785f);
    }

    private static ModelPart[] bake(SchoolArmor.School school,boolean slim,int tier){
        var data=new ModelData();
        var root=data.getRoot();
        boolean bear=school==SchoolArmor.School.BEAR, cat=school==SchoolArmor.School.CAT;
        var cuirass=ModelPartBuilder.create();
        box(cuirass,LEATHER,-3.9f,1,-3.65f,7.8f,8.6f,7.3f);
        if(!bear)box(cuirass,cat?LEATHER:STEEL,bear?-4:-3.25f,1.4f,bear?-4.75f:-4.2f,bear?8:6.5f,bear?5.8f:4.2f,bear?1.6f:1.05f);
        box(cuirass,cat?LEATHER:STEEL,-3.55f,2,3.22f,7.1f,5.7f,bear?1.4f:.95f);
        box(cuirass,LEATHER,-4.25f,8.8f,-3.9f,8.5f,1.45f,7.8f);
        box(cuirass,TRIM,-1,8.9f,-4.45f,2,1.25f,.85f);
        box(cuirass,LEATHER,bear?-4.75f:2.7f,9.3f,2.5f,2.05f,2.4f,2.25f);
        box(cuirass,TRIM,bear?-4.2f:3.3f,9.6f,4.45f,.65f,.65f,.45f);
        if(bear){
            // The bearing shell is built from the angled child plates below.
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
        if(bear){
            bearTorso(torso);
            for(int i=0;i<3;i++)angled(torso,"lumbar"+i,STEEL,0,5.4f+i*.9f,4.01f,6.65f-i*.6f,.75f,1.16f,.17f,0);
            angled(torso,"mantleBridge",LINING,0,.36f,3.28f,6.8f,1.4f,2.07f,.35f,0);
            for(int side=-1;side<=1;side+=2){
                angled(torso,"maneDrop"+side,LINING,side*2.55f,1.61f,-3.78f,1.32f,2.42f,1.1f,-.20f,side*.23f);
                angled(torso,"mantleRivet"+side,TRIM,side*2.75f,2.36f,4.22f,.66f,.66f,.46f,0,.785f);
            }
        }else lightTorso(torso,cat);
        strap(torso,"front",2.8f,bear?-5.05f:-4.55f,.64f);
        strap(torso,"back",-2.8f,bear?4.8f:4.45f,-.64f);
        if(!cat)strap(torso,"cross",-2.8f,bear?-5.13f:-4.63f,-.64f);

        var helmet=child(root,"helmet",ModelPartBuilder.create());
        angled(helmet,"brow",cat?LEATHER:STEEL,0,-5.8f,-4.75f,cat?7.9f:8.65f,.8f,.75f,-.13f,0);
        if(tier>0){
            angled(torso,"waistPlate",cat?LEATHER:STEEL,0,7.8f,-4.57f,cat?5.5f:7.6f,1.3f,1.15f,-.18f,0);
            for(int side:new int[]{-1,1}){
                angled(helmet,"temple"+side,bear?LINING:STEEL,side*4.22f,-3.7f,-2.6f,.95f,cat?2:3.1f,2.75f,.12f,side*.12f);
                angled(torso,"flank"+side,cat?LEATHER:STEEL,side*3.96f,6.7f,.8f,1.12f,2.5f,4.8f,.12f,side*.10f);
            }
        }
        if(tier==2){
            angled(helmet,"crest",bear?STEEL:TRIM,0,-8.68f,-.4f,cat?1.12f:2.05f,1.1f,7.3f,-.06f,0);
            angled(torso,"spineGuard",cat?LEATHER:STEEL,0,4.1f,4.77f,bear?4.9f:cat?2.6f:4.1f,4.1f,1.25f,.12f,0);
            for(int side:new int[]{-1,1}){
                angled(torso,"breastRidge"+side,bear?STEEL:TRIM,side*2.15f,5.2f,-5.06f,cat?1.6f:2.6f,.8f,.85f,-.12f,side*.24f);
                if(bear)angled(torso,"furLapel"+side,LINING,side*2.94f,1.46f,-4.06f,1.56f,3.12f,1.48f,-.13f,side*.26f);
            }
        }

        for(int side=0;side<2;side++){
            boolean right=side==0;
            float axis=right?(slim?-.5f:-1):(slim?.5f:1);
            var arm=ModelPartBuilder.create();
            box(arm,cat?LEATHER:LINING,axis-2.55f,-1.8f,-2.64f,5.1f,8.1f,5.28f);
            var armPart=child(root,"arm"+side,arm);
            if(bear)bearArm(armPart,axis,right?-1:1);
            else lightArm(armPart,axis,right?-1:1,cat);

            var thigh=ModelPartBuilder.create();
            float sign=right?-1:1;
            box(thigh,CLOTH,-1.85f,-.35f,-3.02f,3.7f,bear?5.4f:cat?3.3f:6.2f,.85f);
            box(thigh,CLOTH,-1.85f,.15f,2.45f,3.7f,bear?5.1f:cat?3.6f:5.7f,1.0f);
            box(thigh,bear?STEEL:LEATHER,sign<0?-3.04f:1.9f,.45f,-2.65f,1.14f,bear?4.4f:3.4f,5.3f);
            box(thigh,TRIM,sign<0?-3.1f:1.83f,1.4f,-2.78f,1.26f,.66f,5.56f);
            box(thigh,STEEL,-1.8f,4.9f,-3.12f,3.6f,2.6f,1.25f);
            if(cat)for(int j=0;j<3;j++)box(thigh,TRIM,-1.25f+j*.8f,1+j*.21f,-3.18f,.25f,1.2f,.3f);
            var thighPart=child(root,"thigh"+side,thigh);
            if(bear){
                angled(thighPart,"tasset",STEEL,sign*1.95f,2.25f,-2.5f,2.4f,3.6f,1.25f,-.18f,sign*.17f);
                angled(thighPart,"quilt",CLOTH,sign*.62f,3.25f,2.82f,2.5f,4.3f,.78f,.12f,sign*.09f);
                angled(thighPart,"knee",STEEL,0,5.95f,-3.43f,2.8f,2.2f,.95f,-.18f,0);
            }else{
                angled(thighPart,"hipShell",cat?LEATHER:STEEL,sign*2.02f,1.55f,-2.5f,1.9f,cat?2.55f:3.35f,1.18f,-.15f,sign*.24f);
                angled(thighPart,"kneeShell",STEEL,0,5.95f,-3.13f,cat?2.65f:3.3f,1.68f,.92f,-.20f,0);
            }

            var boot=ModelPartBuilder.create();
            float height=cat?4.55f:3.65f;
            box(boot,LEATHER,-2.55f,11.8f-height,-2.67f,5.1f,height,5.34f);
            box(boot,STEEL,-2.43f,10.05f,bear?-3.67f:-3.35f,4.86f,2.12f,bear?2.1f:1.6f);
            box(boot,LEATHER,-2.61f,11.5f,-3.45f,5.22f,.76f,6.22f);
            box(boot,TRIM,-2.59f,8.65f,-2.78f,5.18f,.63f,5.56f);
            var bootPart=child(root,"boot"+side,boot);
            if(bear){
                angled(bootPart,"greave",STEEL,0,9.65f,-2.92f,4.45f,2.95f,1.35f,.13f,0);
                angled(bootPart,"instep",STEEL,0,10.85f,-3.53f,4.55f,1.45f,1.35f,-.24f,0);
            }else{
                angled(bootPart,"ankleBand",LEATHER,0,9.65f,-2.96f,4.85f,.72f,.89f,0,0);
                angled(bootPart,"ankleClasp",TRIM,sign*1.38f,9.65f,-3.51f,.74f,.73f,.38f,0,0);
                if(cat)angled(bootPart,"highShaft",LEATHER,0,8.35f,-2.8f,4.4f,2.38f,.75f,.08f,0);
                else angled(bootPart,"toeShell",STEEL,0,10.91f,-3.41f,4.5f,1.3f,1.3f,-.20f,0);
            }
            if(tier>0){
                angled(armPart,"outerLame",cat?LEATHER:STEEL,axis+sign*(bear?2.65f:2.35f),1.55f,.05f,1.15f,2.65f,5.2f,0,sign*.23f);
                angled(thighPart,"outerGreave",STEEL,sign*1.74f,6.92f,-2.43f,1.33f,2.45f,2.12f,-.14f,sign*.12f);
                angled(bootPart,"heelGuard",cat?LEATHER:STEEL,0,10.38f,2.58f,4.48f,2.13f,1.04f,.08f,0);
            }
            if(tier==2){
                angled(armPart,"forearmRidge",cat?STEEL:TRIM,axis+sign*1.45f,5.98f,-3.7f,1.25f,2.85f,.84f,-.12f,sign*.10f);
                angled(thighPart,"articulatedLame",cat?LEATHER:STEEL,sign*.73f,3.65f,-3.33f,cat?1.85f:2.75f,1.42f,.83f,-.23f,sign*.15f);
                angled(bootPart,"toeRidge",cat?LEATHER:TRIM,0,11.04f,bear?-4.13f:-3.98f,bear?3.95f:3.25f,.7f,.88f,-.25f,0);
            }
        }
        var model=TexturedModelData.of(data,64,96).createModel();
        return java.util.stream.Stream.of("torso","arm0","arm1","thigh0","thigh1","boot0","boot1","helmet")
            .map(model::getChild).toArray(ModelPart[]::new);
    }

    @Override public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,
            float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch){
        if(player.isInvisible())return;
        var model=getContextModel();
        int slim=player.getSkinTextures().model()==net.minecraft.client.util.SkinTextures.Model.SLIM?1:0;
        renderSlot(player,EquipmentSlot.HEAD,slim,matrices,buffers,light,7,model.head);
        renderSlot(player,EquipmentSlot.CHEST,slim,matrices,buffers,light,0,model.body,model.rightArm,model.leftArm);
        renderSlot(player,EquipmentSlot.LEGS,slim,matrices,buffers,light,3,model.rightLeg,model.leftLeg);
        renderSlot(player,EquipmentSlot.FEET,slim,matrices,buffers,light,5,model.rightLeg,model.leftLeg);
    }

    private void renderSlot(AbstractClientPlayerEntity player,EquipmentSlot slot,int slim,MatrixStack matrices,
            VertexConsumerProvider buffers,int light,int first,ModelPart... parents){
        if(!(player.getEquippedStack(slot).getItem() instanceof SchoolArmor armor))return;
        var school=armor.school();
        var pieces=models[school.ordinal()][slim][armor.tier()];
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
