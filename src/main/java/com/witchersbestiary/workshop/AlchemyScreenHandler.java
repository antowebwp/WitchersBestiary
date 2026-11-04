package com.witchersbestiary.workshop;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
public final class AlchemyScreenHandler extends ScreenHandler {
 private final Inventory inventory;
 private long lastBrew=Long.MIN_VALUE;private final java.util.List<String> recipeIds;
 public AlchemyScreenHandler(int sync,PlayerInventory player){this(sync,player,new SimpleInventory(4));}
 public AlchemyScreenHandler(int sync,PlayerInventory player,Inventory inventory){super(ModWorkshop.ALCHEMY_SCREEN,sync);checkSize(inventory,4);this.inventory=inventory;recipeIds=AlchemyRecipes.catalog(player.player.getWorld()).stream().map(e->e.id().toString()).toList();
  for(int i=0;i<4;i++){final int slot=i;addSlot(new Slot(inventory,i,i==3?274:170+i*18,122){@Override public boolean canInsert(ItemStack s){return slot<3;} });}
  for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(player,column+row*9+9,72+column*18,156+row*18));
  for(int column=0;column<9;column++)addSlot(new Slot(player,column,72+column*18,214));
 }
 @Override public boolean canUse(PlayerEntity p){return inventory.canPlayerUse(p);}
 @Override public boolean onButtonClick(PlayerEntity p,int id){if(p.currentScreenHandler!=this||!p.isAlive()||p.isSpectator()||!canUse(p)||!(inventory instanceof WorkshopInventory be)||p.getWorld().isClient)return false;long tick=p.getWorld().getTime();if(lastBrew!=Long.MIN_VALUE&&tick-lastBrew<4)return false;lastBrew=tick;if(id>=100&&!recipeIds.equals(AlchemyRecipes.catalog(p.getWorld()).stream().map(e->e.id().toString()).toList()))return false;boolean result=id>=100?AlchemyRecipes.craft(be,p,id-100):id==0&&be.process(p);if(!result)p.sendMessage(net.minecraft.text.Text.translatable("alchemy.witchersbestiary.unavailable"),true);sendContentUpdates();return result;}
 @Override public ItemStack quickMove(PlayerEntity p,int index){if(!canUse(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;var stack=slot.getStack();var old=stack.copy();if(index<4){if(!insertItem(stack,4,40,true))return ItemStack.EMPTY;}else if(!insertItem(stack,0,3,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();return old;}
}
