package com.witchersbestiary.client;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.monster.WerewolfEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class WerewolfModel extends DefaultedEntityGeoModel<WerewolfEntity> {
    public WerewolfModel() { super(Identifier.of(WitchersBestiary.MOD_ID, "werewolf")); }
    @Override public Identifier getModelResource(WerewolfEntity entity, GeoRenderer<WerewolfEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "geo/werewolf.geo.json");
    }
    @Override public Identifier getTextureResource(WerewolfEntity entity, GeoRenderer<WerewolfEntity> renderer) {
        return Identifier.of(WitchersBestiary.MOD_ID, "textures/entity/werewolf.png");
    }
    @Override public Identifier getAnimationResource(WerewolfEntity entity) {
        return Identifier.of(WitchersBestiary.MOD_ID, "animations/werewolf.animation.json");
    }
}
