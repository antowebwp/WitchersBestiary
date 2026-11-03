package com.witchersbestiary.combat.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.ItemEntity;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.server.network.ServerPlayerEntity;import com.witchersbestiary.economy.*;
/** Server-only pickup adapter; preserves vanilla owner/delay gates and normal remainder pickup. */
@Mixin(ItemEntity.class)
public abstract class CrownPickupMixin {
 @Shadow private java.util.UUID owner;
 @Inject(method="onPlayerCollision",at=@At("HEAD"),cancellable=true)private void collect(PlayerEntity player,CallbackInfo ci){
  var self=(ItemEntity)(Object)this;if(!(player instanceof ServerPlayerEntity p)||p.isSpectator()||!p.isAlive()||self.isRemoved()||self.cannotPickup()||owner!=null&&!owner.equals(p.getUuid())||!self.getStack().isOf(Currency.CROWN))return;
  var stack=self.getStack();int picked=0;
  for(var wallet:java.util.stream.Stream.concat(p.getInventory().main.stream(),p.getInventory().offHand.stream()).toList())if(wallet.isOf(Currency.WALLET)&&!stack.isEmpty()){long balance=WalletTransactions.balance(wallet);int amount=(int)Math.min(stack.getCount(),Long.MAX_VALUE-balance);if(amount>0){wallet.set(WalletComponents.BALANCE,balance+amount);stack.decrement(amount);picked+=amount;}}
  if(picked>0){p.sendPickup(self,picked);p.increaseStat(net.minecraft.stat.Stats.PICKED_UP.getOrCreateStat(Currency.CROWN),picked);p.getInventory().markDirty();if(stack.isEmpty()){self.discard();ci.cancel();}}
 }
}
