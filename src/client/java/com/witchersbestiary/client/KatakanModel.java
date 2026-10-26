package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.KatakanEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class KatakanModel extends DefaultedEntityGeoModel<KatakanEntity> {
    public KatakanModel(){super(Identifier.of("witchersbestiary","katakan"));}
    @Override public Identifier getModelResource(KatakanEntity entity,GeoRenderer<KatakanEntity> renderer){return Identifier.of("witchersbestiary","geo/"+(entity.isBatForm()?"katakan_bat":"katakan")+".geo.json");}
    @Override public Identifier getTextureResource(KatakanEntity entity,GeoRenderer<KatakanEntity> renderer){return Identifier.of("witchersbestiary","textures/entity/katakan.png");}
    @Override public Identifier getAnimationResource(KatakanEntity entity){return Identifier.of("witchersbestiary","animations/katakan.animation.json");}
}
