package com.witchersbestiary.alchemy;

import com.witchersbestiary.registry.ModEffects;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.tooltip.TooltipType;
import java.util.List;

public final class RelictElixirItem extends Item {
    public static final int DURATION = 3 * 60 * 20;
    public RelictElixirItem(Settings settings) { super(settings); }
    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        AlchemyTooltips.append(tooltip,com.witchersbestiary.combat.MonsterCategory.RELICT,false);
    }
    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.DRINK; }
    @Override public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 32; }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        return ItemUsage.consumeHeldItem(world, player, hand);
    }
    @Override public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient) return stack;
        user.addStatusEffect(new StatusEffectInstance(ModEffects.RELICT_HUNTER, DURATION, 0));
        user.emitGameEvent(GameEvent.DRINK);
        if (user instanceof PlayerEntity player) {
            if (player instanceof ServerPlayerEntity serverPlayer) Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
                if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().insertStack(bottle)) player.dropItem(bottle, false);
            }
        } else {
            stack.decrement(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        }
        return stack;
    }
}
