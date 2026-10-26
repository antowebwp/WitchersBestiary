package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.BonecrusherEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class BonecrusherModel extends GeoModel<BonecrusherEntity>{
 @Override public Identifier getModelResource(BonecrusherEntity e){return Identifier.of("witchersbestiary","geo/bonecrusher.geo.json");}
 @Override public Identifier getTextureResource(BonecrusherEntity e){return Identifier.of("witchersbestiary","textures/entity/bonecrusher.png");}
 @Override public Identifier getAnimationResource(BonecrusherEntity e){return Identifier.of("witchersbestiary","animations/bonecrusher.animation.json");}
}
