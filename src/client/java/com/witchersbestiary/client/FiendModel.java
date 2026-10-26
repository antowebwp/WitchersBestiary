package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.FiendEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class FiendModel extends GeoModel<FiendEntity>{
 @Override public Identifier getModelResource(FiendEntity e){return Identifier.of("witchersbestiary","geo/fiend.geo.json");}
 @Override public Identifier getTextureResource(FiendEntity e){return Identifier.of("witchersbestiary","textures/entity/fiend.png");}
 @Override public Identifier getAnimationResource(FiendEntity e){return Identifier.of("witchersbestiary","animations/fiend.animation.json");}
}
