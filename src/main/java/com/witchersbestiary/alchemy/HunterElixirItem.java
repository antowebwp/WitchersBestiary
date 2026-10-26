package com.witchersbestiary.alchemy;

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

public final class HunterElixirItem extends Item {
    private final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
    public HunterElixirItem(Settings settings,net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect){super(settings);this.effect=effect;}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.text.Text> lines,net.minecraft.item.tooltip.TooltipType type){if(effect.equals(com.witchersbestiary.registry.ModEffects.CLEAR_MIND)){lines.add(net.minecraft.text.Text.translatable("tooltip.witchersbestiary.clear_mind.1").formatted(net.minecraft.util.Formatting.AQUA));lines.add(net.minecraft.text.Text.translatable("tooltip.witchersbestiary.clear_mind.2").formatted(net.minecraft.util.Formatting.GRAY));return;}AlchemyTooltips.append(lines,AlchemyTooltips.category(effect),false);}
    @Override public UseAction getUseAction(ItemStack stack){return UseAction.DRINK;}
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user){return 32;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){return ItemUsage.consumeHeldItem(world,user,hand);}
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user){
        if(world.isClient)return stack;
        user.addStatusEffect(new StatusEffectInstance(effect,RelictElixirItem.DURATION,0)); user.emitGameEvent(GameEvent.DRINK);
        if(user instanceof PlayerEntity player){
            if(player instanceof ServerPlayerEntity server)Criteria.CONSUME_ITEM.trigger(server,stack);
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if(!player.getAbilities().creativeMode){stack.decrement(1);if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);
                ItemStack bottle=new ItemStack(Items.GLASS_BOTTLE);if(!player.getInventory().insertStack(bottle))player.dropItem(bottle,false);}
        } else {stack.decrement(1);if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);}
        return stack;
    }
}
