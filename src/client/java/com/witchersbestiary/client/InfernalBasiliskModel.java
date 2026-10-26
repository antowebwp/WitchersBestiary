package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.InfernalBasiliskEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
public final class InfernalBasiliskModel extends GeoModel<InfernalBasiliskEntity> {
    @Override public Identifier getModelResource(InfernalBasiliskEntity e){return Identifier.of("witchersbestiary","geo/infernal_basilisk.geo.json");}
    @Override public Identifier getTextureResource(InfernalBasiliskEntity e){return Identifier.of("witchersbestiary","textures/entity/infernal_basilisk.png");}
    @Override public Identifier getAnimationResource(InfernalBasiliskEntity e){return Identifier.of("witchersbestiary","animations/infernal_basilisk.animation.json");}
    @Override public void setCustomAnimations(InfernalBasiliskEntity entity,long id,AnimationState<InfernalBasiliskEntity> state){
        super.setCustomAnimations(entity,id,state);getAnimationProcessor().getBone("saddle").setHidden(!entity.isSaddled());
    }
}
