package com.witchersbestiary.test.mixin;

import com.witchersbestiary.test.CascadeReview;

import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Comparison-only hook at the real impact. Production bomb code and mechanics stay untouched. */
@Mixin(targets = "com.witchersbestiary.alchemy.BombVfx", remap = false)
abstract class CascadeGrapeshotMixin {
    @Inject(method = "impact", at = @At("HEAD"), cancellable = true)
    private static void cascadeReviewImpact(ServerWorld world, Vec3d position, int type, CallbackInfo callback) {
        if (type != 0 || !CascadeReview.useCascade()) return;
        callback.cancel();

        var center = position.add(0, .14, -.28);
        Vfx.emitter().shape(ShapeSpec.point()).count(1).lifetime(7)
                .size(.28f, 1.45f, Easings.EASE_OUT_CUBIC).alpha(.95f, 0, Easings.EASE_OUT_QUAD)
                .gradient(Easings.LINEAR, 0xFFF4BA, 0xFFC34F, 0xF36222).play(world, center);
        Vfx.emitter().shape(ShapeSpec.hemisphere(.35f)).count(38).lifetime(12).cube()
                .size(.21f, .055f, Easings.EASE_IN_QUAD).speed(.18f).drag(.11f)
                .gradient(Easings.LINEAR, 0xFFFFCA, 0xFFAB2E, 0xD34416, 0x702019)
                .alpha(1, 0, Easings.EASE_IN_QUAD).play(world, center);
        Vfx.emitter().shape(ShapeSpec.hemisphere(.19f)).count(19).lifetime(20).shard().spin(3)
                .size(.14f, .06f, Easings.LINEAR).speed(.23f).gravity(0, -.022f, 0)
                .gradient(Easings.LINEAR, 0xFFE4AB, 0xA59998, 0x655F66)
                .alpha(1, 0, Easings.EASE_IN_QUAD).play(world, center);
        Vfx.emitter().shape(ShapeSpec.hemisphere(.11f)).count(28).lifetime(15)
                .sprite(SpriteId.SPARK).trail(3).stretch(3).speed(.30f).gravity(0, -.018f, 0)
                .size(.083f, .018f, Easings.LINEAR).gradient(Easings.LINEAR, 0xFFF0A3, 0xF47A24)
                .alpha(1, 0, Easings.EASE_OUT_QUAD).play(world, center);
        Vfx.emitter().shape(ShapeSpec.ring(.32f)).count(24).lifetime(8).cube()
                .size(.10f, .04f, Easings.LINEAR).speed(.23f).blend(BlendMode.ALPHA)
                .gradient(Easings.LINEAR, 0xE6C898, 0x99836C).alpha(.62f, 0, Easings.EASE_OUT_QUAD)
                .play(world, position.add(0, -.12, -.28));
        Vfx.emitter().shape(ShapeSpec.hemisphere(.38f)).count(12).lifetime(25)
                .sprite(SpriteId.SMOKE).animate().blend(BlendMode.ALPHA).spin(1.8f).speed(.09f)
                .size(.15f, .76f, Easings.EASE_OUT_QUAD).alpha(.69f, 0, Easings.EASE_IN_QUAD)
                .turbulence(.015f, .4f).gradient(Easings.LINEAR, 0x876354, 0x454049, 0x2F2B35)
                .play(world, center);
    }
}
