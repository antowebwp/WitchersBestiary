package com.witchersbestiary.alchemy;

import com.mojang.serialization.Codec;
import com.witchersbestiary.combat.MonsterCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class MutagenItem extends Item {
    private static final int SLOT_COUNT = 3;
    private static final int[] FAMILY_PERCENT = {3, 6, 10};
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<List<Identifier>> EQUIPPED =
            AttachmentRegistry.create(Identifier.of("witchersbestiary", "equipped_mutagens"),
                    builder -> builder.persistent(Identifier.CODEC.sizeLimitedListOf(SLOT_COUNT)).copyOnDeath()
                            .syncWith(Identifier.PACKET_CODEC.collect(net.minecraft.network.codec.PacketCodecs.toList(SLOT_COUNT)),
                                    net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly()));
    private final MonsterCategory first;
    private final MonsterCategory second;
    private final int tier;

    private MutagenItem(MonsterCategory first, MonsterCategory second, int tier) {
        super(new Settings().maxCount(16));
        this.first = first;
        this.second = second;
        this.tier = tier;
    }

    public static List<Item> registerAll() {
        var registered = new ArrayList<Item>();
        var families = MonsterCategory.values();
        for (var first : families) {
            for (int tier = 1; tier <= 3; tier++) registered.add(register(first, null, tier));
            for (var second : families) {
                if (first.name().compareTo(second.name()) >= 0) continue;
                for (int tier = 1; tier <= 3; tier++) registered.add(register(first, second, tier));
            }
        }
        return List.copyOf(registered);
    }

    private static Item register(MonsterCategory first, MonsterCategory second, int tier) {
        String family = first.name().toLowerCase(Locale.ROOT);
        if (second != null) family += "_" + second.name().toLowerCase(Locale.ROOT);
        return Registry.register(Registries.ITEM, Identifier.of("witchersbestiary", "mutagen_" + family + "_" + tier),
                new MutagenItem(first, second, tier));
    }

    public static float multiplier(PlayerEntity player, MonsterCategory target) {
        float bonus = 0;
        for (Identifier id : player.getAttachedOrElse(EQUIPPED, List.of())) {
            if (Registries.ITEM.get(id) instanceof MutagenItem mutagen) {
                bonus += mutagen.tier;
                if (target == mutagen.first || target != null && target == mutagen.second)
                    bonus += FAMILY_PERCENT[mutagen.tier - 1] * (mutagen.second == null ? 1 : .5f);
            }
        }
        return 1 + bonus / 100;
    }

    public static ItemStack equipped(PlayerEntity player, int slot) {
        var current = player.getAttachedOrElse(EQUIPPED, List.of());
        return slot < current.size() ? new ItemStack(Registries.ITEM.get(current.get(slot))) : ItemStack.EMPTY;
    }

    public static boolean equip(PlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof MutagenItem) || stack.isEmpty()) return false;
        var slots = new ArrayList<>(player.getAttachedOrElse(EQUIPPED, List.of()));
        if (slots.size() >= SLOT_COUNT) return false;
        slots.add(Registries.ITEM.getId(stack.getItem()));
        stack.decrementUnlessCreative(1, player);
        player.setAttached(EQUIPPED, List.copyOf(slots));
        return true;
    }

    public static boolean remove(PlayerEntity player, int slot) {
        var equipped = new ArrayList<>(player.getAttachedOrElse(EQUIPPED, List.of()));
        if (slot < 0 || slot >= equipped.size()) return false;
        var stack = new ItemStack(Registries.ITEM.get(equipped.get(slot)));
        if (stack.isEmpty() || !player.getInventory().insertStack(stack)) return false;
        equipped.remove(slot);
        player.setAttached(EQUIPPED, List.copyOf(equipped));
        player.sendMessage(Text.translatable("item.witchersbestiary.mutagen.equipped", equipped.size()), true);
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        var held = player.getStackInHand(hand);
        if (player.isSpectator()) return TypedActionResult.fail(held);
        if (world.isClient) return TypedActionResult.success(held);
        var slots = player.getAttachedOrElse(EQUIPPED, List.of());
        boolean changed = player.isSneaking() ? remove(player, slots.size() - 1) : equip(player, held);
        if (!changed) return TypedActionResult.fail(held);
        return TypedActionResult.consume(held);
    }
}
