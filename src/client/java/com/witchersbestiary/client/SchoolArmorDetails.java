package com.witchersbestiary.client;

import com.witchersbestiary.equipment.armory.SchoolArmor;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

public final class SchoolArmorDetails<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T,M> {
    private static final int STEEL=0, LEATHER=16, CLOTH=32, TRIM=48, LINING=64, GLASS=80;
    private final ModelPart[][][][] models=new ModelPart[SchoolArmor.School.values().length][2][3][];

    public SchoolArmorDetails(FeatureRendererContext<T,M> context){
        super(context);
        for(var school:SchoolArmor.School.values())for(int slim=0;slim<2;slim++)
            for(int tier=0;tier<3;tier++)
                models[school.ordinal()][slim][tier]=tier==0?bake(school,slim!=0,tier):bakeAdvanced(school,slim!=0,tier);
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

    private static void helmetShell(ModelPartData helmet, SchoolArmor.School school, int tier){
        boolean cat=school==SchoolArmor.School.CAT, bear=school==SchoolArmor.School.BEAR;
        int shell=cat?LEATHER:STEEL;
        angled(helmet,"crownShell",shell,0,-8.33f,.15f,bear?8.75f:8.35f,.95f,8.35f,0,0);
        angled(helmet,"napeShell",cat?CLOTH:shell,0,-5.14f,4.18f,7.9f,5.55f,.83f,0,0);
        for(int side:new int[]{-1,1}){
            angled(helmet,"sideShell"+side,shell,side*4.06f,-5.52f,.55f,.83f,5.7f,7.22f,0,0);
            if(tier==0)continue;
            angled(helmet,"cheekPlate"+side,bear?STEEL:cat?LEATHER:TRIM,side*3.68f,-2.78f,-4.19f,
                cat?.94f:1.28f,3.11f,1.24f,-.15f,side*.12f);
            if(tier!=2)continue;
            if(bear){
                angled(helmet,"forgedPeak"+side,STEEL,side*2.79f,-8.03f,-3.55f,2.7f,1.85f,2.45f,.09f,side*.12f);
                angled(helmet,"peakRivet"+side,TRIM,side*2.78f,-7.68f,-4.86f,.69f,.69f,.55f,0,.785f);
            }else if(cat){
                angled(helmet,"catEar"+side,STEEL,side*2.84f,-8.77f,-2.59f,1.42f,2.62f,1.32f,-.12f,side*.31f);
                angled(helmet,"maskSweep"+side,STEEL,side*2.01f,-6.44f,-4.71f,4.1f,.86f,.93f,0,-side*.32f);
            }else{
                for(int feather=0;feather<3;feather++)
                    angled(helmet,"wing"+side+"_"+feather,TRIM,side*(4.03f+feather*.31f),-7.78f-feather*.46f,
                        .6f+feather*.92f,.64f,1.72f+feather*.22f,2.55f,-.44f,side*.26f);
            }
        }
    }

    private static ModelPart[] bakeAdvanced(SchoolArmor.School school, boolean slim, int tier){
        var data=new ModelData();
        var root=data.getRoot();
        boolean master=tier==2, bear=school==SchoolArmor.School.BEAR, cat=school==SchoolArmor.School.CAT;
        int armor=cat?LEATHER:STEEL, edge=bear?LINING:TRIM;
        float width=bear?9.2f:cat?8.15f:8.75f;
        var torso=child(root,"torso",ModelPartBuilder.create());
        angled(torso,"foundation",armor,0,4.7f,0,width,8.2f,8.95f,0,0);
        if(bear){
            for(int row=0;row<3;row++){
                angled(torso,"rib"+row,STEEL,0,2.7f+row*2.07f,-4.71f,width-row*.64f,1.95f,1.75f,-.15f,0);
                angled(torso,"backRib"+row,STEEL,0,3.2f+row*2.05f,4.55f,width-.75f-row*.38f,1.76f,1.1f,.17f,0);
            }
            for(int lock=-2;lock<=2;lock++){
                float drop=master?3.65f:2.4f;
                angled(torso,"mantle"+lock,LINING,lock*1.6f,1.05f,4.57f,1.9f,drop-Math.abs(lock)*.32f,2.1f,.23f,lock*.13f);
            }
        }else if(cat){
            for(int row=0;row<4;row++){
                angled(torso,"leatherFold"+row,row==0?CLOTH:LEATHER,0,2.0f+row*1.52f,-4.5f,7.8f-row*.55f,1.72f,.94f,-.19f,.09f);
                angled(torso,"backFold"+row,LEATHER,0,2.3f+row*1.45f,4.43f,7.5f-row*.38f,1.29f,.92f,.11f,-.09f);
            }
            angled(torso,"scarf",CLOTH,0,.84f,0,8.15f,1.75f,9.35f,0,.08f);
        }else{
            for(int side:new int[]{-1,1}){
                angled(torso,"breast"+side,STEEL,side*1.94f,3.45f,-4.81f,4.03f,5.13f,1.35f,-.14f,side*.18f);
                angled(torso,"backShield"+side,STEEL,side*1.97f,4.2f,4.54f,3.91f,5.9f,1.28f,.15f,-side*.10f);
            }
            angled(torso,"keel",TRIM,0,4.45f,-5.6f,1.45f,4.73f,.78f,-.13f,0);
        }
        angled(torso,"girdle",LEATHER,0,8.96f,0,width+.22f,1.44f,9.35f,0,0);
        angled(torso,"clasp",TRIM,0,8.98f,-4.93f,1.86f,1.46f,.86f,0,0);
        if(master){
            float panelWidth=bear?5.48f:cat?2.48f:4.15f;
            float panelX=cat?2.16f:0;
            angled(torso,"rearPanel",CLOTH,panelX,7.62f,5.05f,panelWidth,7.72f,.86f,.12f,cat?-.13f:0);
            angled(torso,"rearPanelTrim",edge,panelX,11.17f,5.61f,panelWidth,.67f,.48f,0,cat?-.13f:0);
            angled(torso,"upperKeel",cat?STEEL:TRIM,0,2.8f,-5.59f,bear?4.12f:cat?2.05f:3.61f,2.1f,1.21f,-.12f,.12f);
            angled(torso,"lumbarShell",armor,0,7.31f,4.97f,width-.39f,2.1f,1.38f,.14f,0);
        }
        var helmet=child(root,"helmet",ModelPartBuilder.create());
        helmetShell(helmet,school,tier);
        angled(helmet,"visor",cat?CLOTH:STEEL,0,-6.52f,-4.97f,bear?8.94f:8.52f,1.66f,1.23f,-.16f,0);
        for(int side=0;side<2;side++){
            float sign=side==0?-1:1;
            float axis=sign*(slim?.5f:1);
            float shoulder=bear?(master?6.9f:6.05f):cat?(side==0&&master?5.32f:4.24f):master?6.18f:5.45f;
            var arm=child(root,"arm"+side,ModelPartBuilder.create());
            for(int layer=0;layer<(master?3:2);layer++)
                angled(arm,"shoulder"+layer,layer==0&&bear?LINING:armor,axis+sign*(.38f+layer*.23f),
                    -2.18f+layer*1.52f,.03f,shoulder-layer*.42f,1.46f,5.88f-layer*.15f,0,sign*(.15f+layer*.08f));
            angled(arm,"vambrace",armor,axis,6.14f,-.02f,cat?4.85f:5.53f,3.68f,5.86f,0,sign*.08f);
            angled(arm,"braceEdge",edge,axis,7.67f,0,cat?5.04f:5.72f,.65f,6.12f,0,sign*.08f);
            if(master)angled(arm,"outerShield",STEEL,axis+sign*1.98f,5.69f,-.53f,1.43f,3.41f,4.64f,-.12f,sign*.14f);

            var thigh=child(root,"thigh"+side,ModelPartBuilder.create());
            angled(thigh,"cuisse",cat?LEATHER:STEEL,0,2.55f,0,4.1f,5.02f,6.14f,0,0);
            angled(thigh,"tasset",armor,sign*.84f,2.52f,-2.78f,2.97f,4.55f,1.31f,-.13f,sign*.13f);
            angled(thigh,"rearGuard",armor,0,4.49f,2.98f,3.78f,3.62f,1.05f,.17f,0);
            angled(thigh,"knee",STEEL,0,5.8f,-3.28f,cat?3.02f:3.82f,2.17f,1.55f,-.13f,0);
            angled(thigh,"kneeStrap",LEATHER,0,5.87f,.19f,4.21f,.65f,6.28f,0,0);
            if(master)angled(thigh,"sideShield",armor,sign*2.05f,4.03f,0,1.36f,4.7f,6.29f,0,sign*.12f);

            var boot=child(root,"boot"+side,ModelPartBuilder.create());
            angled(boot,"bootBody",LEATHER,0,10.42f,-.24f,4.6f,3.61f,6.18f,0,0);
            angled(boot,"heelCup",armor,0,10.39f,2.64f,4.49f,2.62f,1.45f,.10f,0);
            angled(boot,"toeCap",STEEL,0,11.02f,-3.02f,4.52f,2.07f,2.05f,-.12f,0);
            angled(boot,"sole",LEATHER,0,11.82f,-.45f,4.79f,.65f,7.33f,0,0);
            for(int layer=0;layer<(master?3:2);layer++)
                angled(boot,"greave"+layer,layer==0?edge:armor,0,8.28f+layer*.82f,-2.91f+layer*.13f,4.32f-layer*.17f,.84f,1.02f,.15f,0);
            finishLimb(arm,thigh,boot,school,axis,sign,tier);
        }
        finishArmor(torso,helmet,school,tier);
        return parts(data);
    }

    private static void finishArmor(ModelPartData torso, ModelPartData helmet, SchoolArmor.School school, int tier){
        boolean bear=school==SchoolArmor.School.BEAR, cat=school==SchoolArmor.School.CAT;
        boolean master=tier==2;
        float front=tier==0?(bear?-5.6f:-5.15f):bear?-5.82f:-5.64f;
        if(tier>0){
            strap(torso,"harnessFront",2.8f,front-.34f,.61f);
            strap(torso,"harnessBack",-2.7f,5.78f,-.62f);
            angled(torso,"beltStud",TRIM,0,9.05f,-5.61f,2.01f,1.36f,.62f,0,.12f);
        }
        if(bear){
            for(int side:new int[]{-1,1}){
                float span=master?3.5f:tier==1?3.02f:2.12f;
                angled(torso,"furBib"+side,LINING,side*2.98f,1.35f,-4.28f,span,master?3.4f:2.16f,1.9f,-.24f,side*.26f);
                angled(torso,"ribLock"+side,TRIM,side*2.85f,4.78f,front,.77f,1.2f,.84f,-.15f,side*.24f);
                angled(helmet,"ironTemple"+side,STEEL,side*4.54f,-4.14f,1.04f,.82f,master?4.6f:3.45f,master?5.8f:4.9f,.1f,0);
                if(master)angled(helmet,"forgedFlare"+side,STEEL,side*3.6f,-7.94f,-3.34f,2.5f,2.5f,3.05f,-.19f,side*.25f);
            }
            if(master)angled(torso,"bearBoss",TRIM,0,3.83f,front-.72f,2.67f,2.21f,1.22f,0,.785f);
        }else if(cat){
            angled(helmet,"hoodPeak",CLOTH,0,-7.76f,-4.65f,master?7.26f:7.75f,1.83f,2.35f,-.35f,0);
            for(int side:new int[]{-1,1}){
                angled(helmet,"hoodSeam"+side,LEATHER,side*4.27f,-5.47f,-1.16f,.7f,4.13f,6.37f,.2f,side*.15f);
                if(master)angled(helmet,"point"+side,STEEL,side*3.15f,-8.68f,-2.56f,1.33f,3.65f,1.65f,-.20f,side*.29f);
            }
            angled(torso,"scarfTie",CLOTH,2.45f,2.24f,5.04f,1.55f,master?4.85f:3.06f,1.03f,.14f,-.28f);
            angled(torso,"utilityPouch",LEATHER,-3.51f,9.15f,-3.72f,1.92f,2.32f,2.24f,.05f,-.18f);
            angled(torso,"pouchClasp",TRIM,-3.51f,9.16f,-4.94f,.77f,.66f,.47f,0,-.18f);
        }else{
            for(int side:new int[]{-1,1}){
                angled(torso,"gildedRib"+side,TRIM,side*2.23f,2.48f,front,3.45f,.57f,.88f,-.12f,side*.31f);
                angled(helmet,"templeFrame"+side,TRIM,side*4.42f,-4.4f,-2.4f,.62f,3.43f,3.6f,-.18f,0);
                if(master)for(int feather=0;feather<3;feather++)
                    angled(helmet,"crestFeather"+side+"_"+feather,TRIM,side*(4.76f+feather*.26f),
                        -7.59f-feather*.48f,-.66f+feather*.88f,.69f,2.1f+feather*.35f,2.72f,-.53f,side*.34f);
            }
            angled(torso,"sunSeal",TRIM,0,4.16f,front-.61f,master?2.45f:1.62f,master?2.45f:1.62f,.82f,0,.785f);
        }
        if(master){
            float x=cat?-2.3f:0;
            angled(torso,"rearBadge",TRIM,x,5.19f,5.77f,cat?1.45f:2.25f,1.58f,.68f,.14f,.785f);
        }
    }

    private static void finishLimb(ModelPartData arm, ModelPartData thigh, ModelPartData boot,
            SchoolArmor.School school, float axis, float side, int tier){
        boolean bear=school==SchoolArmor.School.BEAR, cat=school==SchoolArmor.School.CAT;
        boolean master=tier==2;
        float cap=bear?4.3f:cat?3.14f:3.85f;
        angled(thigh,"kneecap",STEEL,0,6.01f,-3.98f,cap,1.51f,.89f,-.16f,0);
        angled(boot,"heelStrap",cat?TRIM:STEEL,0,10.17f,3.22f,cat?3.7f:4.45f,.67f,1.08f,.13f,0);
        if(tier==0)return;
        angled(arm,"elbowShell",cat?LEATHER:STEEL,axis+side*.62f,4.29f,2.74f,cat?2.6f:3.69f,2.05f,1.25f,.24f,side*.08f);
        angled(thigh,"outerTasset",cat?LEATHER:STEEL,side*(master?2.72f:2.35f),2.1f,-.51f,
            master?1.6f:1.13f,master?4.7f:3.32f,bear?5.98f:4.65f,.13f,side*.21f);
        if(master){
            float shoulder=bear?5.95f:cat?4.6f:5.56f;
            angled(arm,"pauldronMantle",bear?LINING:STEEL,axis+side*.63f,-2.73f,-.12f,
                shoulder,bear?1.94f:1.39f,bear?6.68f:6.18f,-.09f,side*.29f);
            angled(arm,"pauldronRim",bear?STEEL:TRIM,axis+side*.91f,-1.72f,-3.02f,
                shoulder-.52f,.57f,.89f,.03f,side*.27f);
            angled(boot,"instepRidge",cat?LEATHER:STEEL,0,10.55f,-3.65f,cap,1.62f,1.23f,-.27f,0);
        }
    }

    private static ModelPart[] parts(ModelData data){
        var model=TexturedModelData.of(data,64,96).createModel();
        return java.util.stream.Stream.of("torso","arm0","arm1","thigh0","thigh1","boot0","boot1","helmet")
            .map(model::getChild).toArray(ModelPart[]::new);
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
        helmetShell(helmet,school,tier);
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
            angled(thighPart,"rearCuisse",bear?STEEL:cat?LEATHER:CLOTH,0,4.6f,2.98f,
                bear?3.62f:3.26f,4.2f,.82f,.08f,0);
            angled(bootPart,"heelCounter",LEATHER,0,10.92f,2.62f,4.58f,2.34f,.95f,.05f,0);
            if(tier>0){
                angled(thighPart,"sideLame",cat?LEATHER:STEEL,sign*2.04f,4.9f,0,1.03f,2.42f,5.38f,0,sign*.12f);
                angled(armPart,"outerLame",cat?LEATHER:STEEL,axis+sign*(bear?2.65f:2.35f),1.55f,.05f,1.15f,2.65f,5.2f,0,sign*.23f);
                angled(thighPart,"outerGreave",STEEL,sign*1.74f,6.92f,-2.43f,1.33f,2.45f,2.12f,-.14f,sign*.12f);
                angled(bootPart,"heelGuard",cat?LEATHER:STEEL,0,10.38f,2.58f,4.48f,2.13f,1.04f,.08f,0);
            }
            if(tier==2){
                angled(armPart,"shoulderCrest",bear?LINING:cat?STEEL:TRIM,axis+sign*.47f,-2.7f,.14f,
                    bear?5.55f:cat?3.95f:4.96f,1.35f,5.92f,-.06f,sign*.20f);
                angled(thighPart,"rearLame",cat?LEATHER:STEEL,0,6.75f,3.05f,bear?3.45f:2.96f,1.32f,.85f,.18f,0);
                angled(armPart,"forearmRidge",cat?STEEL:TRIM,axis+sign*1.45f,5.98f,-3.7f,1.25f,2.85f,.84f,-.12f,sign*.10f);
                angled(thighPart,"articulatedLame",cat?LEATHER:STEEL,sign*.73f,3.65f,-3.33f,cat?1.85f:2.75f,1.42f,.83f,-.23f,sign*.15f);
                angled(bootPart,"toeRidge",cat?LEATHER:TRIM,0,11.04f,bear?-4.13f:-3.98f,bear?3.95f:3.25f,.7f,.88f,-.25f,0);
            }
            finishLimb(armPart,thighPart,bootPart,school,axis,sign,tier);
        }
        finishArmor(torso,helmet,school,tier);
        return parts(data);
    }

    @Override public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,T entity,
            float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch){
        if(entity.isInvisible() && !(entity instanceof ArmorStandEntity))return;
        var model=getContextModel();
        int slim=entity instanceof AbstractClientPlayerEntity player && player.getSkinTextures().model()==net.minecraft.client.util.SkinTextures.Model.SLIM?1:0;
        renderSlot(entity,EquipmentSlot.HEAD,slim,matrices,buffers,light,7,model.head);
        renderSlot(entity,EquipmentSlot.CHEST,slim,matrices,buffers,light,0,model.body,model.rightArm,model.leftArm);
        renderSlot(entity,EquipmentSlot.LEGS,slim,matrices,buffers,light,3,model.rightLeg,model.leftLeg);
        renderSlot(entity,EquipmentSlot.FEET,slim,matrices,buffers,light,5,model.rightLeg,model.leftLeg);
    }

    private void renderSlot(T entity,EquipmentSlot slot,int slim,MatrixStack matrices,
            VertexConsumerProvider buffers,int light,int first,ModelPart... parents){
        var stack=entity.getEquippedStack(slot);
        if(!(stack.getItem() instanceof SchoolArmor armor))return;
        var school=armor.school();
        var pieces=models[school.ordinal()][slim][armor.tier()];
        var texture=Identifier.of("witchersbestiary","textures/models/armor/"+school.name().toLowerCase(java.util.Locale.ROOT)+"_details.png");
        var vertices=net.minecraft.client.render.item.ItemRenderer.getArmorGlintConsumer(
                buffers, RenderLayer.getEntityCutout(texture), stack.hasGlint());
        for(int i=0;i<parents.length;i++){
            if(!parents[i].visible && !(entity instanceof ArmorStandEntity))continue;
            matrices.push();
            parents[i].rotate(matrices);
            pieces[first+i].render(matrices,vertices,light,OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}
