package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.DrownerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class DrownerModel extends DefaultedEntityGeoModel<DrownerEntity> {
    public DrownerModel(){super(Identifier.of("witchersbestiary","drowner"));}
    @Override public Identifier getModelResource(DrownerEntity entity, GeoRenderer<DrownerEntity> renderer) {
        return Identifier.of("witchersbestiary", "geo/drowner.geo.json");
    }
    @Override public Identifier getTextureResource(DrownerEntity entity, GeoRenderer<DrownerEntity> renderer) {
        return Identifier.of("witchersbestiary", "textures/entity/drowner.png");
    }
    @Override public Identifier getAnimationResource(DrownerEntity entity) {
        return Identifier.of("witchersbestiary", "animations/drowner.animation.json");
    }
}
