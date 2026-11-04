package com.witchersbestiary.workshop;

import com.witchersbestiary.alchemy.*;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

/** RecipeManager is the only recipe database. Transactions stage every inventory change. */
public final class AlchemyRecipes {
 public static boolean consumable(ItemStack stack){return stack.getItem() instanceof HunterElixirItem||stack.getItem() instanceof RelictElixirItem||stack.getItem() instanceof WeaponOilItem;}
 public static List<RecipeEntry<CraftingRecipe>> catalog(World world){return world.getRecipeManager().listAllOfType(RecipeType.CRAFTING).stream().filter(e->(consumable(e.value().getResult(world.getRegistryManager()))||e.id().getNamespace().equals("witchersbestiary")&&e.id().getPath().startsWith("workshop/"))&&!e.value().getIngredients().isEmpty()).sorted(Comparator.comparing(e->e.id().toString())).toList();}
 public static net.minecraft.text.Text description(ItemStack stack){
  if(stack.getItem() instanceof WeaponOilItem oil)return net.minecraft.text.Text.translatable("alchemy.witchersbestiary.oil_summary",Math.round(InventoryOils.BONUS*100),InventoryOils.CAPACITY,net.minecraft.text.Text.translatable("category.witchersbestiary."+oil.category().name().toLowerCase(java.util.Locale.ROOT)));
  if(stack.getItem() instanceof HunterElixirItem h&&h.effect().equals(com.witchersbestiary.registry.ModEffects.CLEAR_MIND))return net.minecraft.text.Text.translatable("alchemy.witchersbestiary.clear_summary");
  if(consumable(stack)){var category=stack.getItem() instanceof HunterElixirItem h?AlchemyTooltips.category(h.effect()):com.witchersbestiary.combat.MonsterCategory.RELICT;return net.minecraft.text.Text.translatable("alchemy.witchersbestiary.elixir_summary",Math.round((com.witchersbestiary.combat.AlchemyDamage.HUNTER_MULTIPLIER-1)*100),RelictElixirItem.DURATION/20,net.minecraft.text.Text.translatable("category.witchersbestiary."+category.name().toLowerCase(java.util.Locale.ROOT)));}
  return net.minecraft.text.Text.translatable("alchemy.witchersbestiary.component_effect");
 }
 public record Plan(List<ItemStack> table,List<ItemStack> player,ItemStack output){}
 public static Plan plan(World world,net.minecraft.inventory.Inventory table,PlayerEntity player,RecipeEntry<CraftingRecipe> entry){
  if(!catalog(world).contains(entry))return null;
  var sources=new ArrayList<ItemStack>();for(int i=0;i<3;i++)sources.add(table.getStack(i).copy());for(var s:player.getInventory().main)sources.add(s.copy());
  var ingredients=entry.value().getIngredients();int[] chosen=new int[ingredients.size()];Arrays.fill(chosen,-1);
  if(!assign(ingredients,0,sources,chosen))return null;
  int width=entry.value() instanceof ShapedRecipe shaped?shaped.getWidth():Math.min(3,ingredients.size());int height=entry.value() instanceof ShapedRecipe shaped?shaped.getHeight():(ingredients.size()+width-1)/width;
  var grid=new ArrayList<ItemStack>(Collections.nCopies(width*height,ItemStack.EMPTY));
  for(int i=0;i<chosen.length;i++)if(chosen[i]>=0){var original=chosen[i]<3?table.getStack(chosen[i]):player.getInventory().main.get(chosen[i]-3);grid.set(i,original.copyWithCount(1));}
  var input=CraftingRecipeInput.create(width,height,grid);if(!entry.value().matches(input,world))return null;
  var result=entry.value().craft(input,world.getRegistryManager());var output=table.getStack(3).copy();
  if(result.isEmpty()||result.getCount()>result.getMaxCount()||!output.isEmpty()&&(!ItemStack.areItemsAndComponentsEqual(output,result)||output.getCount()+result.getCount()>output.getMaxCount()))return null;
  if(output.isEmpty())output=result.copy();else output.increment(result.getCount());
  var bag=new ArrayList<ItemStack>(sources.subList(3,sources.size()));
  for(var remainder:entry.value().getRemainder(input))if(!put(bag,remainder.copy()))return null;
  return new Plan(new ArrayList<>(sources.subList(0,3)),bag,output);
 }
 private static boolean assign(List<Ingredient> ingredients,int ignored,List<ItemStack> sources,int[] chosen){
  var units=new ArrayList<Integer>();for(int i=0;i<sources.size();i++)for(int n=0;n<Math.min(ingredients.size(),sources.get(i).getCount());n++)units.add(i);
  int[] owners=new int[units.size()];Arrays.fill(owners,-1);
  for(int i=0;i<ingredients.size();i++)if(!ingredients.get(i).isEmpty()&&!match(i,ingredients,sources,units,owners,new boolean[owners.length]))return false;
  for(int i=0;i<owners.length;i++)if(owners[i]>=0){chosen[owners[i]]=units.get(i);sources.get(units.get(i)).decrement(1);}return true;
 }
 private static boolean match(int ingredient,List<Ingredient> ingredients,List<ItemStack> sources,List<Integer> units,int[] owners,boolean[] seen){
  for(int i=0;i<units.size();i++)if(!seen[i]&&ingredients.get(ingredient).test(sources.get(units.get(i)))){seen[i]=true;if(owners[i]<0||match(owners[i],ingredients,sources,units,owners,seen)){owners[i]=ingredient;return true;}}return false;
 }
 private static boolean put(List<ItemStack> bag,ItemStack stack){if(stack.isEmpty())return true;for(var slot:bag)if(!slot.isEmpty()&&ItemStack.areItemsAndComponentsEqual(slot,stack)){int n=Math.min(stack.getCount(),slot.getMaxCount()-slot.getCount());slot.increment(n);stack.decrement(n);if(stack.isEmpty())return true;}for(int i=0;i<bag.size();i++)if(bag.get(i).isEmpty()){int n=Math.min(stack.getCount(),stack.getMaxCount());bag.set(i,stack.copyWithCount(n));stack.decrement(n);if(stack.isEmpty())return true;}return false;}
 public static boolean craft(WorkshopInventory table,PlayerEntity player,int index){
  var world=table.getWorld();if(world==null||world.isClient||!player.isAlive()||player.isSpectator()||!table.canPlayerUse(player)||table.kind()!=WorkshopKind.ALCHEMY_WORKBENCH||com.witchersbestiary.fist.FistFights.busy(player))return false;
  var list=catalog(world);if(index<0||index>=list.size())return false;var plan=plan(world,table,player,list.get(index));if(plan==null)return false;
  for(int i=0;i<3;i++)table.setStack(i,plan.table.get(i));for(int i=0;i<plan.player.size();i++)player.getInventory().main.set(i,plan.player.get(i));table.setStack(3,plan.output);player.getInventory().markDirty();return true;
 }
}
