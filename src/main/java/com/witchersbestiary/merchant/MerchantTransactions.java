package com.witchersbestiary.merchant;
import com.witchersbestiary.economy.*;
import com.witchersbestiary.economy.Currency;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/** Exact copied-inventory payment. No dropped output and no partial debit on rejection. */
public final class MerchantTransactions {
 public static long balance(ServerPlayerEntity p){long total=0;for(int i=0;i<37;i++){var s=i<36?p.getInventory().main.get(i):p.getOffHandStack();long n=s.isOf(Currency.CROWN)?s.getCount():s.isOf(Currency.WALLET)?WalletTransactions.balance(s):0;total=total>Long.MAX_VALUE-n?Long.MAX_VALUE:total+n;}return total;}
 public static boolean buy(ServerPlayerEntity p,ArcaneMerchant merchant,String offerId){return purchase(p,merchant,offerId).equals("bought");}
 public static String purchase(ServerPlayerEntity p,ArcaneMerchant merchant,String offerId){
  if(!merchant.isAlive()||merchant.getWorld()!=p.getWorld()||p.squaredDistanceTo(merchant)>36||!p.isAlive()||p.isSpectator()||com.witchersbestiary.fist.FistFights.busy(p))return "unavailable";
  var offer=merchant.offers().stream().filter(o->o.id.equals(offerId)).findFirst().orElse(null);if(offer==null||offer.price<=0||offer.result.isEmpty())return "unavailable";if(offer.stock<=0)return "stock";
  var copy=new ArrayList<ItemStack>();p.getInventory().main.forEach(s->copy.add(s.copy()));copy.add(p.getOffHandStack().copy());long remaining=offer.price;
  for(var stack:copy)if(stack.isOf(Currency.CROWN)&&remaining>0){int n=(int)Math.min(remaining,stack.getCount());stack.decrement(n);remaining-=n;}
  for(var stack:copy)if(stack.isOf(Currency.WALLET)&&remaining>0){long old=WalletTransactions.balance(stack),n=Math.min(old,remaining);stack.set(WalletComponents.BALANCE,CrownBalance.withdraw(old,n));remaining-=n;}
  if(remaining!=0)return "funds";if(!insert(copy,offer.result.copy()))return "space";
  for(int i=0;i<36;i++)p.getInventory().main.set(i,copy.get(i));p.getInventory().offHand.set(0,copy.get(36));offer.stock--;p.getInventory().markDirty();return "bought";
 }
 private static boolean insert(List<ItemStack> bag,ItemStack result){for(int i=0;i<36;i++){var s=bag.get(i);if(!s.isEmpty()&&ItemStack.areItemsAndComponentsEqual(s,result)){int n=Math.min(result.getCount(),s.getMaxCount()-s.getCount());s.increment(n);result.decrement(n);if(result.isEmpty())return true;}}for(int i=0;i<36;i++)if(bag.get(i).isEmpty()){int n=Math.min(result.getCount(),result.getMaxCount());bag.set(i,result.copyWithCount(n));result.decrement(n);if(result.isEmpty())return true;}return false;}
}
