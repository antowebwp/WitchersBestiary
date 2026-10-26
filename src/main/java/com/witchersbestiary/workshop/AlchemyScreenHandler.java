package com.witchersbestiary.workshop;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
public final class AlchemyScreenHandler extends ScreenHandler {
 private final Inventory inventory;
 public AlchemyScreenHandler(int sync,PlayerInventory player){this(sync,player,new SimpleInventory(4));}
 public AlchemyScreenHandler(int sync,PlayerInventory player,Inventory inventory){super(ModWorkshop.ALCHEMY_SCREEN,sync);checkSize(inventory,4);this.inventory=inventory;
  for(int i=0;i<4;i++){final int slot=i;addSlot(new Slot(inventory,i,26+i*36,33){@Override public boolean canInsert(ItemStack s){return slot<3;} });}
  for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(player,column+row*9+9,8+column*18,84+row*18));
  for(int column=0;column<9;column++)addSlot(new Slot(player,column,8+column*18,142));
 }
 @Override public boolean canUse(PlayerEntity p){return inventory.canPlayerUse(p);}
 @Override public boolean onButtonClick(PlayerEntity p,int id){if(id!=0||!canUse(p)||!(inventory instanceof WorkshopInventory be))return false;boolean result=be.process(p);sendContentUpdates();return result;}
 @Override public ItemStack quickMove(PlayerEntity p,int index){if(!canUse(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;var stack=slot.getStack();var old=stack.copy();if(index<4){if(!insertItem(stack,4,40,true))return ItemStack.EMPTY;}else if(!insertItem(stack,0,3,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();return old;}
}
