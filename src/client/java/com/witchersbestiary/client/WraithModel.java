package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.WraithEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class WraithModel extends GeoModel<WraithEntity>{
 @Override public Identifier getModelResource(WraithEntity e){return Identifier.of("witchersbestiary","geo/wraith.geo.json");}
 @Override public Identifier getTextureResource(WraithEntity e){return Identifier.of("witchersbestiary","textures/entity/wraith.png");}
 @Override public Identifier getAnimationResource(WraithEntity e){return Identifier.of("witchersbestiary","animations/wraith.animation.json");}
}
