package com.witchersbestiary.economy;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
/** Preflight copied inventory, then commit synchronously on the server thread. */
public final class WalletTransactions {
 public static long balance(ItemStack s){return CrownBalance.validate(s.getOrDefault(WalletComponents.BALANCE,0L));}
 public static void transfer(ServerPlayerEntity p,ItemStack wallet,boolean deposit,long amount){
  if(amount<=0||!wallet.isOf(Currency.WALLET)||p.getInventory().main.stream().noneMatch(s->s==wallet)&&p.getInventory().offHand.stream().noneMatch(s->s==wallet))throw new IllegalArgumentException("invalid");
  long old=balance(wallet);var copy=new ArrayList<ItemStack>();p.getInventory().main.forEach(s->copy.add(s.copy()));
  if(deposit){long available=copy.stream().filter(s->s.isOf(Currency.CROWN)).mapToLong(ItemStack::getCount).sum();if(amount>available)throw new IllegalArgumentException("coins");CrownBalance.deposit(old,amount);long left=amount;for(var s:copy)if(s.isOf(Currency.CROWN)){int n=(int)Math.min(left,s.getCount());s.decrement(n);left-=n;}}
  else{CrownBalance.withdraw(old,amount);long capacity=copy.stream().mapToLong(s->s.isEmpty()?64:s.isOf(Currency.CROWN)?64-s.getCount():0).sum();if(amount>capacity)throw new IllegalArgumentException("space");long left=amount;for(int i=0;i<copy.size()&&left>0;i++){var s=copy.get(i);if(s.isEmpty()||s.isOf(Currency.CROWN)){int n=(int)Math.min(left,s.isEmpty()?64:64-s.getCount());if(s.isEmpty())copy.set(i,new ItemStack(Currency.CROWN,n));else s.increment(n);left-=n;}}}
  for(int i=0;i<copy.size();i++)if(p.getInventory().main.get(i)!=wallet)p.getInventory().main.set(i,copy.get(i));
  wallet.set(WalletComponents.BALANCE,deposit?CrownBalance.deposit(old,amount):CrownBalance.withdraw(old,amount));p.getInventory().markDirty();
 }
}
