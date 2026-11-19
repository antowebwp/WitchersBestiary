package com.witchersbestiary.contracts;

import com.witchersbestiary.contracts.procedural.LegendaryHunts;
import com.witchersbestiary.contracts.procedural.ProceduralHunts;
import com.witchersbestiary.contracts.procedural.ProceduralProfile.Tier;
import java.util.Random;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

public final class ContractBonus {
    private ContractBonus() {}

    public static NbtCompound roll(ContractDefinition definition, long seed, RegistryWrapper.WrapperLookup registries) {
        if (definition == null || !LegendaryHunts.offered(definition.id()) && !ProceduralHunts.regular(definition.id())) return null;
        Tier tier = LegendaryHunts.offered(definition.id()) ? Tier.LEGENDARY : ProceduralHunts.profile(definition, seed).tier();
        var random = new Random(seed ^ 0x5c925a319dcf7a4dL);
        int roll = random.nextInt(100);
        ItemStack stack = switch (tier) {
            case COMMON -> switch (roll / 20) {
                case 0 -> stack(Items.IRON_INGOT, random, 2, 4);
                case 1 -> stack(Items.LEATHER, random, 2, 4);
                case 2 -> stack(Items.ARROW, random, 8, 16);
                case 3 -> stack(Items.EMERALD, random, 1, 2);
                default -> stack(Items.COAL, random, 4, 8);
            };
            case RARE -> roll < 6 ? new ItemStack(Items.DIAMOND) : switch (roll % 4) {
                case 0 -> stack(Items.GOLD_INGOT, random, 2, 5);
                case 1 -> stack(Items.REDSTONE, random, 5, 10);
                case 2 -> stack(Items.LAPIS_LAZULI, random, 4, 8);
                default -> stack(Items.EMERALD, random, 2, 4);
            };
            case EPIC -> roll < 30 ? stack(Items.DIAMOND, random, 1, 2)
                    : roll < 55 ? book(registries, roll % 2 == 0 ? Enchantments.PROTECTION : Enchantments.UNBREAKING, 2)
                    : roll < 80 ? stack(Items.BLAZE_ROD, random, 2, 4) : new ItemStack(Items.DIAMOND_PICKAXE);
            case LEGENDARY -> roll < 1 ? new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
                    : roll < 40 ? book(registries, roll % 2 == 0 ? Enchantments.FORTUNE : Enchantments.PROTECTION, 3)
                    : roll < 75 ? stack(Items.DIAMOND, random, 2, 4) : stack(Items.ENDER_PEARL, random, 4, 7);
        };
        return (NbtCompound) stack.encode(registries);
    }

    private static ItemStack stack(Item item, Random random, int min, int max) {
        return new ItemStack(item, random.nextInt(min, max + 1));
    }

    private static ItemStack book(RegistryWrapper.WrapperLookup registries, RegistryKey<Enchantment> key, int level) {
        var enchantments = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        enchantments.add(registries.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key), level);
        var stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments.build());
        return stack;
    }

    public static ItemStack decode(NbtCompound bonus, RegistryWrapper.WrapperLookup registries) {
        return bonus == null ? ItemStack.EMPTY : ItemStack.fromNbt(registries, bonus)
                .orElseThrow(() -> new IllegalStateException("reward_unavailable"));
    }
}
