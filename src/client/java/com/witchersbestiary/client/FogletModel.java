package com.witchersbestiary.client;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.monster.FogletEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class FogletModel extends DefaultedEntityGeoModel<FogletEntity> {
    public FogletModel() { super(Identifier.of(WitchersBestiary.MOD_ID, "foglet")); }
    @Override public Identifier getModelResource(FogletEntity entity, GeoRenderer<FogletEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "geo/foglet.geo.json");
    }
    @Override public Identifier getTextureResource(FogletEntity entity, GeoRenderer<FogletEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "textures/entity/foglet.png");
    }
    @Override public Identifier getAnimationResource(FogletEntity entity) {
        return Identifier.of(WitchersBestiary.MOD_ID, "animations/foglet.animation.json");
    }
}
