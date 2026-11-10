package com.witchersbestiary.entity;

import java.util.UUID;
import java.util.function.ToIntFunction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class PetFeeding {
    private static final TagKey<net.minecraft.item.Item> RAW_MEAT =
        TagKey.of(RegistryKeys.ITEM, Identifier.of("witchersbestiary", "basilisk_raw_meat"));
    private static final TagKey<net.minecraft.item.Item> COOKED_MEAT =
        TagKey.of(RegistryKeys.ITEM, Identifier.of("witchersbestiary", "basilisk_cooked_meat"));
    private long nextFeedTick = Long.MIN_VALUE;

    public static int meat(ItemStack stack) {
        return stack.isIn(COOKED_MEAT) ? 6 : stack.isIn(RAW_MEAT) ? 4 : 0;
    }

    /** Returns null when the entity should continue its usual interaction. */
    public ActionResult interact(LivingEntity pet, PlayerEntity player, Hand hand, UUID owner,
            ToIntFunction<ItemStack> nutrition) {
        if (!player.getUuid().equals(owner) || !pet.isAlive() || player.isSpectator()
            || !player.canInteractWithEntity(pet, 0)) return null;
        var stack = player.getStackInHand(hand);
        int healing = nutrition.applyAsInt(stack);
        if (hand == Hand.MAIN_HAND && stack.isEmpty() && nutrition.applyAsInt(player.getOffHandStack()) > 0)
            return ActionResult.PASS;
        if (healing <= 0) return null;
        if (hand == Hand.OFF_HAND && nutrition.applyAsInt(player.getMainHandStack()) > 0)
            return ActionResult.PASS;
        if (pet.getWorld() instanceof ServerWorld world && pet.getHealth() < pet.getMaxHealth()
            && world.getTime() >= nextFeedTick) {
            float health = pet.getHealth();
            pet.heal(healing);
            if (pet.getHealth() > health) {
                nextFeedTick = world.getTime() + 10;
                if (!player.isCreative()) stack.decrement(1);
                pet.playSound(SoundEvents.ENTITY_GENERIC_EAT, .7f, 1f);
                world.spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getBodyY(.8), pet.getZ(),
                    3, .24, .16, .24, .01);
            }
        }
        return ActionResult.success(pet.getWorld().isClient);
    }
}
