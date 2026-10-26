package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.NekkerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
public final class NekkerModel extends GeoModel<NekkerEntity>{
 @Override public Identifier getModelResource(NekkerEntity e){return Identifier.of("witchersbestiary","geo/nekker.geo.json");}
 @Override public Identifier getTextureResource(NekkerEntity e){return Identifier.of("witchersbestiary","textures/entity/nekker.png");}
 @Override public Identifier getAnimationResource(NekkerEntity e){return Identifier.of("witchersbestiary","animations/nekker.animation.json");}
}
