package com.witchersbestiary.client;

import com.witchersbestiary.entity.monster.HigherVampireEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class HigherVampireRenderer extends GeoEntityRenderer<HigherVampireEntity> {
    public HigherVampireRenderer(EntityRendererFactory.Context context) {
        super(context, new Model());
        shadowRadius = .65f;
    }

    private static final class Model extends DefaultedEntityGeoModel<HigherVampireEntity> {
        private Model() { super(Identifier.of("witchersbestiary", "higher_vampire")); }

        private String form(HigherVampireEntity vampire) {
            return vampire.isHybrid() ? "higher_vampire_hybrid" : "higher_vampire_human";
        }

        @Override public Identifier getModelResource(HigherVampireEntity vampire, GeoRenderer<HigherVampireEntity> renderer) {
            return Identifier.of("witchersbestiary", "geo/" + form(vampire) + ".geo.json");
        }

        @Override public Identifier getAnimationResource(HigherVampireEntity vampire) {
            return Identifier.of("witchersbestiary", "animations/higher_vampire.animation.json");
        }

        @Override public Identifier getTextureResource(HigherVampireEntity vampire, GeoRenderer<HigherVampireEntity> renderer) {
            return Identifier.of("witchersbestiary", "textures/entity/" + form(vampire)
                    + (vampire.hasRedEyes() ? "_red" : "") + ".png");
        }
    }
}
