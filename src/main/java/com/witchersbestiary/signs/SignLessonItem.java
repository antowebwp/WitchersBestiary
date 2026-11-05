package com.witchersbestiary.signs;
import net.minecraft.item.*;import net.minecraft.world.World;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.util.*;import net.minecraft.text.Text;
public final class SignLessonItem extends Item {
 private final SignType sign;public SignLessonItem(SignType sign){super(new Settings().maxCount(1));this.sign=sign;}
 public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){var stack=player.getStackInHand(hand);if(player instanceof ServerPlayerEntity p){if(SignManager.state(p).learn(sign)){SignLedger.get(p.getServer()).markDirty();p.sendMessage(Text.translatable("sign.witchersbestiary.learned",Text.translatable("sign.witchersbestiary."+sign.name().toLowerCase(java.util.Locale.ROOT))),false);SignManager.sync(p);}}return TypedActionResult.success(stack,world.isClient);}
 public static void initialize(){for(var sign:SignType.values())net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.ITEM,Identifier.of("witchersbestiary:sign_lesson_"+sign.name().toLowerCase(java.util.Locale.ROOT)),new SignLessonItem(sign));}
}
