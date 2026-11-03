package com.witchersbestiary.economy;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;
import net.minecraft.text.Text;
import net.minecraft.entity.player.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.screenhandler.v1.*;
public final class Currency {
 public static final Item CROWN=Registry.register(Registries.ITEM,Identifier.of("witchersbestiary:witcher_crown"),new Item(new Item.Settings()));
 public static final Item WALLET=Registry.register(Registries.ITEM,Identifier.of("witchersbestiary:witcher_wallet"),new Item(new Item.Settings().maxCount(1).component(WalletComponents.BALANCE,0L)){
  public TypedActionResult<ItemStack> use(net.minecraft.world.World w,PlayerEntity p,Hand hand){if(p instanceof ServerPlayerEntity server){int slot=hand==Hand.MAIN_HAND?p.getInventory().selectedSlot:40;server.openHandledScreen(new ExtendedScreenHandlerFactory<Integer>(){public Integer getScreenOpeningData(ServerPlayerEntity s){return slot;}public Text getDisplayName(){return Text.translatable("item.witchersbestiary.witcher_wallet");}public net.minecraft.screen.ScreenHandler createMenu(int sync,PlayerInventory inv,PlayerEntity viewer){return new WalletHandler(sync,inv,slot);}});}return TypedActionResult.success(p.getStackInHand(hand),w.isClient);}
 });
 public static final net.minecraft.screen.ScreenHandlerType<WalletHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,Identifier.of("witchersbestiary:wallet"),new ExtendedScreenHandlerType<>(WalletHandler::new,net.minecraft.network.codec.PacketCodecs.VAR_INT));
 public static void initialize(){net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e->{e.add(CROWN);e.add(WALLET);});WalletPackets.initialize();}
}
