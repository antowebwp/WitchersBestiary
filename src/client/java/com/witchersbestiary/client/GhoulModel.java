package com.witchersbestiary.client;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.monster.GhoulEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class GhoulModel extends DefaultedEntityGeoModel<GhoulEntity> {
    public GhoulModel() { super(Identifier.of(WitchersBestiary.MOD_ID, "ghoul")); }
    @Override public Identifier getModelResource(GhoulEntity entity, GeoRenderer<GhoulEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "geo/ghoul.geo.json");
    }
    @Override public Identifier getTextureResource(GhoulEntity entity, GeoRenderer<GhoulEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "textures/entity/ghoul.png");
    }
    @Override public Identifier getAnimationResource(GhoulEntity entity) {
        return Identifier.of(WitchersBestiary.MOD_ID, "animations/ghoul.animation.json");
    }
}
