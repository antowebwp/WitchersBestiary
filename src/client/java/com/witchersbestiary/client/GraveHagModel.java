package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.GraveHagEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class GraveHagModel extends GeoModel<GraveHagEntity>{
 @Override public Identifier getModelResource(GraveHagEntity e){return Identifier.of("witchersbestiary","geo/grave_hag.geo.json");}
 @Override public Identifier getTextureResource(GraveHagEntity e){return Identifier.of("witchersbestiary","textures/entity/grave_hag.png");}
 @Override public Identifier getAnimationResource(GraveHagEntity e){return Identifier.of("witchersbestiary","animations/grave_hag.animation.json");}
}
