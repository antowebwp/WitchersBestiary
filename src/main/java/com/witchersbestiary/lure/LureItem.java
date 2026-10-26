package com.witchersbestiary.lure;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
public final class LureItem extends BlockItem {
 public final LureKind kind;
 public LureItem(LureBlock block,Settings settings){super(block,settings);kind=block.kind;}
 @Override public ActionResult place(ItemPlacementContext context){
  // Habitat, journal and encounter checks are server-only. Predicting BlockItem.place
  // consumes the client stack even when the unchanged server stack is rejected.
  if(!(context.getWorld() instanceof ServerWorld world))return ActionResult.SUCCESS;
  var player=context.getPlayer();if(player==null)return reject(context);var pos=context.getBlockPos();if(context.getSide()!=net.minecraft.util.math.Direction.UP){player.sendMessage(Text.translatable("message.witchersbestiary.lure.ground"),true);return reject(context);}String error=LurePlacement.error(world,pos,kind,player);
  if(error!=null){player.sendMessage(Text.translatable("message.witchersbestiary.lure."+error),true);return reject(context);}
  var journal=LureState.get(world);var entry=journal.reserve(player.getUuid(),world,pos,kind);if(entry==null)return reject(context);var stack=context.getStack();int count=stack.getCount();
  var result=super.place(context);
  if(result.isAccepted()&&world.getBlockEntity(pos) instanceof LureBlockEntity be){be.arm(entry);player.sendMessage(Text.translatable("message.witchersbestiary.lure.wait"),true);return result;}
  journal.refund(entry);if(result.isAccepted()&&stack.getCount()<count)stack.increment(count-stack.getCount());return reject(context);
 }
 private static ActionResult reject(ItemPlacementContext context){
  if(context.getPlayer() instanceof net.minecraft.server.network.ServerPlayerEntity player){
   player.getInventory().markDirty();
   // A delta update cannot repair an unchanged server slot after client prediction.
   player.playerScreenHandler.syncState();
   if(player.currentScreenHandler!=player.playerScreenHandler)player.currentScreenHandler.syncState();
  }
  return ActionResult.FAIL;
 }
}
