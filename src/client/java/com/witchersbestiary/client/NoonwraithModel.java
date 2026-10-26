package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.NoonwraithEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
public final class NoonwraithModel extends DefaultedEntityGeoModel<NoonwraithEntity> {
    public NoonwraithModel() { super(Identifier.of("witchersbestiary", "noonwraith")); }
    @Override public Identifier getModelResource(NoonwraithEntity entity, GeoRenderer<NoonwraithEntity> renderer) { return Identifier.of("witchersbestiary", "geo/noonwraith.geo.json"); }
    @Override public Identifier getTextureResource(NoonwraithEntity entity, GeoRenderer<NoonwraithEntity> renderer) { return Identifier.of("witchersbestiary", "textures/entity/noonwraith.png"); }
    @Override public Identifier getAnimationResource(NoonwraithEntity entity) { return Identifier.of("witchersbestiary", "animations/noonwraith.animation.json"); }
}
