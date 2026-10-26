package com.witchersbestiary.bestiary;

import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import vazkii.patchouli.api.PatchouliAPI;

/** Patchouli dispatches the book-open packet; this item never imports client code. */
public final class BestiaryItem extends Item {
    public static final Identifier BOOK_ID = Identifier.of("witchersbestiary", "bestiary");
    public BestiaryItem(Settings settings) { super(settings); }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer) PatchouliAPI.get().openBookGUI(serverPlayer, BOOK_ID);
        return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
    }
}
