package com.witchersbestiary.alchemy;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

public final class MoonDust {
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> MARK =
            AttachmentRegistry.createPersistent(Identifier.of("witchersbestiary", "moon_dust"), Codec.LONG);

    private MoonDust() {}

    public static void apply(LivingEntity target) {
        target.setAttached(MARK, target.getWorld().getTime() + 120);
        target.removeStatusEffect(StatusEffects.INVISIBILITY);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120));
    }

    public static boolean active(LivingEntity target) {
        return !target.getWorld().isClient && target.getWorld().getTime() < target.getAttachedOrElse(MARK, 0L);
    }
}
