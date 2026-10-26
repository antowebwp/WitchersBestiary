package com.witchersbestiary.workshop;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
public final class WorkshopContainerScreenHandler extends ScreenHandler {
 private final Inventory inventory;
 private final int count;
 public WorkshopContainerScreenHandler(int sync,PlayerInventory player,int count){this(sync,player,new SimpleInventory(count),count);}
 public WorkshopContainerScreenHandler(int sync,PlayerInventory player,Inventory inventory,int count){
  super(count==6?ModWorkshop.URN_SCREEN:ModWorkshop.EQUIPMENT_SCREEN,sync);checkSize(inventory,count);this.inventory=inventory;this.count=count;
  int columns=count/2,start=88-columns*9;
  for(int i=0;i<count;i++)addSlot(new Slot(inventory,i,start+i%columns*18,26+i/columns*18));
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,8+col*18,84+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,142));
  inventory.onOpen(player.player);
 }
 @Override public boolean canUse(PlayerEntity player){return inventory.canPlayerUse(player);}
 @Override public void onClosed(PlayerEntity player){super.onClosed(player);inventory.onClose(player);}
 @Override public ItemStack quickMove(PlayerEntity player,int index){
  if(!canUse(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
  var slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;var stack=slot.getStack();var copy=stack.copy();
  if(index<count?!insertItem(stack,count,slots.size(),true):!insertItem(stack,0,count,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();return copy;
 }
}
