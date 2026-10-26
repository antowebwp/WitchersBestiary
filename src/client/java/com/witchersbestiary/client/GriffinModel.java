package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.GriffinEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class GriffinModel extends GeoModel<GriffinEntity>{
 @Override public Identifier getModelResource(GriffinEntity e){return Identifier.of("witchersbestiary","geo/griffin.geo.json");}
 @Override public Identifier getTextureResource(GriffinEntity e){return Identifier.of("witchersbestiary","textures/entity/griffin.png");}
 @Override public Identifier getAnimationResource(GriffinEntity e){return Identifier.of("witchersbestiary","animations/griffin.animation.json");}
}
