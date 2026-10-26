package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.EndregaWarriorEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class EndregaWarriorModel extends GeoModel<EndregaWarriorEntity>{
 @Override public Identifier getModelResource(EndregaWarriorEntity e){return Identifier.of("witchersbestiary","geo/endrega_warrior.geo.json");}
 @Override public Identifier getTextureResource(EndregaWarriorEntity e){return Identifier.of("witchersbestiary","textures/entity/endrega_warrior.png");}
 @Override public Identifier getAnimationResource(EndregaWarriorEntity e){return Identifier.of("witchersbestiary","animations/endrega_warrior.animation.json");}
}
