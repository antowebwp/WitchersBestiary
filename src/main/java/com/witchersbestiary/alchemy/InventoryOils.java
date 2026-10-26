package com.witchersbestiary.alchemy;
import com.witchersbestiary.combat.*;
import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import java.util.*;
/** Prepare is read-only. Commit runs once after real HP loss, within the same damage call. */
public final class InventoryOils {
 public static final int CAPACITY=40;
 public static final float BONUS=.15f;
 private record Attack(long tick,MonsterCategory category){}
 private static final Map<PlayerEntity,Attack> USED=new WeakHashMap<>();
 public record Use(PlayerEntity player,MonsterCategory category,ItemStack stack,int slot,boolean legacy,boolean repeated){}
 private InventoryOils(){}
 public static int charges(ItemStack stack){return Math.clamp(stack.getOrDefault(ModDataComponents.OIL_HITS,CAPACITY),0,CAPACITY);}
 public static boolean enabled(PlayerEntity p){return ((OilPreferences)p).bestiary$autoOils();}
 public static Use prepare(LivingEntity victim,DamageSource source){
  if(victim.getWorld().isClient||!(victim instanceof CategorizedMonster monster)||!(source.getAttacker() instanceof PlayerEntity p)||!enabled(p)
    ||source.getSource()!=p||!source.isOf(DamageTypes.PLAYER_ATTACK)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE))return null;
  ItemStack weapon=source.getWeaponStack();if(weapon==null||!WeaponOilItem.supportsMeleeDamage(weapon))return null;
  MonsterCategory category=monster.getMonsterCategory();var last=USED.get(p);
  // Vanilla sweep and Better Combat's multiple victims share one server-tick attack budget.
  if(last!=null&&last.tick==p.getWorld().getTime())return last.category==category?new Use(p,category,ItemStack.EMPTY,-1,false,true):null;
  var inventory=p.getInventory();
  for(int slot=0;slot<inventory.main.size();slot++){
   ItemStack stack=inventory.getStack(slot);
   if(stack.getItem() instanceof WeaponOilItem oil&&oil.category()==category&&charges(stack)>0)return new Use(p,category,stack,slot,false,false);
  }
  // Old saved weapon coatings become one ordinary partial bottle after their first real hit.
  if(weapon.get(ModDataComponents.OIL_CATEGORY)==category&&weapon.getOrDefault(ModDataComponents.OIL_HITS,0)>0&&weapon.getOrDefault(ModDataComponents.OIL_EXPIRY,Long.MAX_VALUE)>p.getWorld().getTime())return new Use(p,category,weapon,-1,true,false);
  return null;
 }
 public static void commit(Use use){
  if(use==null||use.repeated)return;PlayerEntity p=use.player;
  var last=USED.get(p);long tick=p.getWorld().getTime();if(last!=null&&last.tick==tick)return;
  ItemStack stack=use.stack;
  if(use.legacy){
   int remaining=stack.contains(ModDataComponents.OIL_EXPIRY)?CAPACITY:Math.clamp(stack.getOrDefault(ModDataComponents.OIL_HITS,0),0,CAPACITY);
   if(remaining<=0)return;
   for(var item:com.witchersbestiary.registry.ModItemGroups.ALL_ITEMS)if(item instanceof WeaponOilItem oil&&oil.category()==use.category){
    ItemStack migrated=new ItemStack(item);migrated.set(ModDataComponents.OIL_HITS,remaining-1);migrated.set(DataComponentTypes.MAX_STACK_SIZE,1);
    if(remaining>1&&!p.getInventory().insertStack(migrated))p.dropItem(migrated,false);break;
   }
   stack.remove(ModDataComponents.OIL_CATEGORY);stack.remove(ModDataComponents.OIL_HITS);stack.remove(ModDataComponents.OIL_BONUS);stack.remove(ModDataComponents.OIL_EXPIRY);
  }else{
   if(p.getInventory().getStack(use.slot)!=stack||stack.isEmpty()||charges(stack)<=0)return;
   ItemStack rest=stack.getCount()>1?stack.copyWithCount(stack.getCount()-1):ItemStack.EMPTY;
   int remaining=charges(stack)-1;stack.setCount(1);
   if(remaining==0)stack.decrement(1); // Existing oil use consumed the container; no bottle return was defined.
   else{stack.set(ModDataComponents.OIL_HITS,remaining);stack.set(DataComponentTypes.MAX_STACK_SIZE,1);}
   if(!rest.isEmpty()&&!p.getInventory().insertStack(rest))p.dropItem(rest,false);
  }
  USED.put(p,new Attack(tick,use.category));p.getInventory().markDirty();
 }
 public static void initialize(){
  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(net.minecraft.server.command.CommandManager.literal("wboils")
   .executes(c->report(c.getSource()))
   .then(net.minecraft.server.command.CommandManager.literal("on").executes(c->set(c.getSource(),true)))
   .then(net.minecraft.server.command.CommandManager.literal("off").executes(c->set(c.getSource(),false)))
   .then(net.minecraft.server.command.CommandManager.literal("toggle").executes(c->{var p=c.getSource().getPlayerOrThrow();return set(c.getSource(),!enabled(p));}))));
  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer,newPlayer,alive)->((OilPreferences)newPlayer).bestiary$autoOils(enabled(oldPlayer)));
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->USED.clear());
 }
 private static int set(net.minecraft.server.command.ServerCommandSource s,boolean enabled)throws com.mojang.brigadier.exceptions.CommandSyntaxException{((OilPreferences)s.getPlayerOrThrow()).bestiary$autoOils(enabled);return report(s);}
 private static int report(net.minecraft.server.command.ServerCommandSource s)throws com.mojang.brigadier.exceptions.CommandSyntaxException{boolean enabled=enabled(s.getPlayerOrThrow());s.sendFeedback(()->net.minecraft.text.Text.translatable("message.witchersbestiary.auto_oils."+(enabled?"on":"off")),false);return enabled?1:0;}
}
