package com.witchersbestiary.alchemy;

import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class WitcherBombItem extends Item {
    public static final List<String> IDS = List.of("grapeshot", "moon_dust", "samum", "dragons_dream");
    private final int type;

    public WitcherBombItem(int type) {
        super(new Settings().maxCount(16));
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        var stack = player.getStackInHand(hand);
        if (player.isSpectator() || com.witchersbestiary.fist.FistFights.busy(player)
                || player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity owner) {
            var hit = player.raycast(8, 1, false);
            var center = hit.getPos().subtract(player.getRotationVec(1).multiply(.4));
            for (var target : server.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(2.5),
                    entity -> entity.squaredDistanceTo(center) <= 6.25
                            && com.witchersbestiary.signs.SignManager.enemy(owner, entity) && owner.canSee(entity))) {
                switch (type) {
                    case 0 -> target.damage(server.getDamageSources().playerAttack(owner), 6);
                    case 1 -> {
                        target.removeStatusEffect(StatusEffects.INVISIBILITY);
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120));
                    }
                    case 2 -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
                    case 3 -> target.setOnFireFor(4);
                    default -> throw new IllegalStateException("Unknown bomb type");
                }
            }
            server.spawnParticles(ParticleTypes.POOF, center.x, center.y, center.z, 18, .65, .4, .65, .04);
            player.getItemCooldownManager().set(this, 40);
            stack.decrementUnlessCreative(1, player);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
