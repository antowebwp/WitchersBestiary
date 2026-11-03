package com.witchersbestiary.economy;
import net.minecraft.entity.player.*;import net.minecraft.item.ItemStack;import net.minecraft.server.network.ServerPlayerEntity;
public final class WalletHandler extends net.minecraft.screen.ScreenHandler {
 private final PlayerInventory inventory;private final int index;private final ItemStack bound;private long sequence;public long balance,nonce;public String status="";
 public WalletHandler(int sync,PlayerInventory inv,int slot){super(Currency.SCREEN,sync);inventory=inv;index=slot;bound=(slot>=0&&slot<36||slot==40)?inv.getStack(slot):ItemStack.EMPTY;}
 public boolean canUse(PlayerEntity p){return p==inventory.player&&(index>=0&&index<36||index==40)&&inventory.getStack(index)==bound&&bound.isOf(Currency.WALLET)&&!p.isRemoved()&&p.isAlive();}
 public ItemStack quickMove(PlayerEntity p,int slot){return ItemStack.EMPTY;}
 public void act(ServerPlayerEntity p,WalletPackets.Action a){if(a.sync()!=syncId||a.sequence()!=sequence||!canUse(p))return;sequence++;try{long amount=a.amount();if(amount==-1)amount=a.deposit()?inventory.main.stream().filter(s->s.isOf(Currency.CROWN)).mapToLong(ItemStack::getCount).sum():WalletTransactions.balance(bound);WalletTransactions.transfer(p,bound,a.deposit(),amount);status="ok";}catch(IllegalArgumentException|ArithmeticException e){status="rejected";}send(p);}
 private void send(ServerPlayerEntity p){net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new WalletPackets.State(syncId,sequence,WalletTransactions.balance(bound),status));}
 public void sendContentUpdates(){super.sendContentUpdates();if(inventory.player instanceof ServerPlayerEntity p){if(!canUse(p)){p.closeHandledScreen();return;}send(p);}}
}
