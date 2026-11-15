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

    @Override
    public void render(HigherVampireEntity vampire, float yaw, float delta,
                       net.minecraft.client.util.math.MatrixStack matrices,
                       net.minecraft.client.render.VertexConsumerProvider buffers, int light) {
        super.render(vampire, yaw, delta, matrices, buffers, light);
        if (!vampire.isCocooned()) return;
        var out = buffers.getBuffer(net.minecraft.client.render.RenderLayer.getDebugQuads());
        var matrix = matrices.peek().getPositionMatrix();
        double pulse = 1 + .026 * Math.sin((vampire.age + delta) * .37);
        double[] height = {.02, .46, 1.26, 2.23, 2.99, 3.37};
        double[] width = {.43, .98, 1.13, 1.07, .70, .015};
        int cracks = (int)(12 * (1 - vampire.cocoonIntegrity()));
        for (int band = 0; band < height.length - 1; band++) {
            for (int segment = 0; segment < 12; segment++) {
                int index = (segment * 7 + band * 3) % 12;
                double a = segment * Math.PI / 6;
                double b = (segment + 1) * Math.PI / 6;
                double seam = index < cracks ? .05 + .055*(1-vampire.cocoonIntegrity()) : 0;
                int red = 65 + (segment * 3 + band * 7) % 34, green = 10 + (index * 3) % 12;
                vertex(out, matrix, a + seam, width[band]*pulse, height[band], red, green);
                vertex(out, matrix, a + seam*.6 + .15, width[band+1]*pulse, height[band+1], red + 16, green);
                vertex(out, matrix, b - seam*.6 + .15, width[band+1]*pulse, height[band+1], red + 16, green);
                vertex(out, matrix, b - seam, width[band]*pulse, height[band], red, green);
            }
        }
    }

    private static void vertex(net.minecraft.client.render.VertexConsumer out, org.joml.Matrix4f matrix,
                               double angle, double radius, double height, int red, int green) {
        out.vertex(matrix, (float)(Math.cos(angle)*radius), (float)height, (float)(Math.sin(angle)*radius))
                .color(red, green, green + 13, 249);
    }

    private static final class Model extends DefaultedEntityGeoModel<HigherVampireEntity> {
        private Model() { super(Identifier.of("witchersbestiary", "higher_vampire")); }

        private String form(HigherVampireEntity vampire) {
            return vampire.isHybrid() ? "higher_vampire_hybrid" : "higher_vampire_human";
        }

        @Override public Identifier getModelResource(HigherVampireEntity vampire, GeoRenderer<HigherVampireEntity> renderer) {
            return Identifier.of("witchersbestiary", "geo/" + form(vampire) + ".geo.json");
        }

        @Override
        public void setCustomAnimations(HigherVampireEntity vampire, long id,
                software.bernie.geckolib.animation.AnimationState<HigherVampireEntity> state) {
            getBone("left_wing").ifPresent(bone -> bone.setHidden(vampire.isCocooned()));
            getBone("right_wing").ifPresent(bone -> bone.setHidden(vampire.isCocooned()));
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
