package com.witchersbestiary.client;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.monster.LeshenEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class LeshenModel extends DefaultedEntityGeoModel<LeshenEntity> {
    public LeshenModel() { super(Identifier.of(WitchersBestiary.MOD_ID, "leshen")); }
    @Override public Identifier getModelResource(LeshenEntity entity, GeoRenderer<LeshenEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "geo/leshen.geo.json");
    }
    @Override public Identifier getTextureResource(LeshenEntity entity, GeoRenderer<LeshenEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "textures/entity/leshen.png");
    }
    @Override public Identifier getAnimationResource(LeshenEntity entity) {
        return Identifier.of(WitchersBestiary.MOD_ID, "animations/leshen.animation.json");
    }
}
