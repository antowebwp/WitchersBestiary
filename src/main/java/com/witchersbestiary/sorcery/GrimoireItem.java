package com.witchersbestiary.sorcery;
import net.minecraft.item.*;import net.minecraft.world.World;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.util.*;
public final class GrimoireItem extends Item {
 public static final Identifier BOOK=Identifier.of("witchersbestiary:grimoire");
 public GrimoireItem(){super(new Settings().maxCount(1).rarity(Rarity.UNCOMMON));}
 public TypedActionResult<ItemStack> use(World world,PlayerEntity p,Hand hand){if(p instanceof ServerPlayerEntity player){Sorcery.sync(player);vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(player,BOOK);}return TypedActionResult.success(p.getStackInHand(hand),world.isClient);}
}
