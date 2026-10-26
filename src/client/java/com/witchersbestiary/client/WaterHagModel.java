package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.WaterHagEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class WaterHagModel extends GeoModel<WaterHagEntity>{
 @Override public Identifier getModelResource(WaterHagEntity e){return Identifier.of("witchersbestiary","geo/water_hag.geo.json");}
 @Override public Identifier getTextureResource(WaterHagEntity e){return Identifier.of("witchersbestiary","textures/entity/water_hag.png");}
 @Override public Identifier getAnimationResource(WaterHagEntity e){return Identifier.of("witchersbestiary","animations/water_hag.animation.json");}
}
