package com.witchersbestiary.workshop;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
public final class WorkshopInventory extends BlockEntity implements SidedInventory,NamedScreenHandlerFactory {
 private final DefaultedList<ItemStack> stacks;
 private final net.minecraft.block.entity.ChestLidAnimator lid=new net.minecraft.block.entity.ChestLidAnimator();
 private final net.minecraft.block.entity.ViewerCountManager viewers=new net.minecraft.block.entity.ViewerCountManager(){
  protected void onContainerOpen(net.minecraft.world.World w,BlockPos p,BlockState s){}
  protected void onContainerClose(net.minecraft.world.World w,BlockPos p,BlockState s){}
  protected void onViewerCountUpdate(net.minecraft.world.World w,BlockPos p,BlockState s,int before,int after){w.addSyncedBlockEvent(p,s.getBlock(),1,after);}
  protected boolean isPlayerViewing(PlayerEntity p){return p.currentScreenHandler instanceof GenericContainerScreenHandler handler&&handler.getInventory()==WorkshopInventory.this;}
 };
 public int viewerCount(){return viewers.getViewerCount();}
 public float lidProgress(float delta){return lid.getProgress(delta);}
 public void tickLid(){lid.step();}
 public void updateViewers(){if(world!=null&&!world.isClient&&!isRemoved())viewers.updateViewerCount(world,pos,getCachedState());}
 @Override public void onOpen(PlayerEntity p){if(kind()==WorkshopKind.WITCHER_CHEST&&!isRemoved()&&!p.isSpectator())viewers.openContainer(p,world,pos,getCachedState());}
 @Override public void onClose(PlayerEntity p){if(kind()==WorkshopKind.WITCHER_CHEST&&!isRemoved()&&!p.isSpectator())viewers.closeContainer(p,world,pos,getCachedState());}
 @Override public boolean onSyncedBlockEvent(int type,int data){if(type==1&&kind()==WorkshopKind.WITCHER_CHEST){lid.setOpen(data>0);return true;}return super.onSyncedBlockEvent(type,data);}
 private boolean hopperStorage(){return kind()==WorkshopKind.WITCHER_CHEST||kind()==WorkshopKind.RITUAL_URN||kind()==WorkshopKind.WITCHER_EQUIPMENT_RACK;}
 public WorkshopInventory(BlockPos p,BlockState s){super(ModWorkshop.INVENTORY,p,s);stacks=DefaultedList.ofSize(((WorkshopDecorBlock)s.getBlock()).kind.slots,ItemStack.EMPTY);}
 public WorkshopKind kind(){return ((WorkshopDecorBlock)getCachedState().getBlock()).kind;}
 public static boolean sword(ItemStack stack){return stack.isIn(ItemTags.SWORDS)||stack.getItem() instanceof SwordItem||stack.isIn(ModWorkshop.RACK_SWORDS);}
 public static boolean weapon(ItemStack stack){return sword(stack)||stack.getItem() instanceof AxeItem||stack.isOf(Items.BOW)||stack.isOf(Items.CROSSBOW)||stack.isOf(Items.TRIDENT)||stack.isOf(Items.MACE)||stack.isIn(ModWorkshop.DISPLAY_WEAPONS);}
 public boolean interact(PlayerEntity player,Hand hand){
  if(world==null||world.isClient||!canPlayerUse(player)||!kind().rack())return false;
  var held=player.getStackInHand(hand);
  if(held.isEmpty()||player.isSneaking()){
   for(int i=size()-1;i>=0;i--)if(!getStack(i).isEmpty()){var out=removeStack(i);if(!player.giveItemStack(out))player.dropItem(out,false);return true;}
   return false;
  }
  if(!isValid(0,held))return false;
  for(int i=0;i<size();i++)if(getStack(i).isEmpty()){setStack(i,held.split(1));return true;}
  return false;
 }
 public boolean process(PlayerEntity player){
  if(!(world instanceof ServerWorld server)||kind()!=WorkshopKind.ALCHEMY_WORKBENCH||!canPlayerUse(player))return false;
  var input=CraftingRecipeInput.create(3,1,java.util.List.of(getStack(0),getStack(1),getStack(2)));
  var entry=server.getRecipeManager().listAllOfType(RecipeType.CRAFTING).stream().filter(e->e.id().getNamespace().equals("witchersbestiary")&&e.id().getPath().startsWith("workshop/")&&e.value().matches(input,server)).findFirst();
  if(entry.isEmpty())return false;
  var result=entry.get().value().craft(input,server.getRegistryManager());var current=getStack(3);
  if(result.isEmpty()||!current.isEmpty()&&(!ItemStack.areItemsAndComponentsEqual(current,result)||current.getCount()+result.getCount()>current.getMaxCount()))return false;
  var remainder=entry.get().value().getRemainder(input);
  for(int i=0;i<3;i++){
   getStack(i).decrement(1);var back=remainder.get(i);
   if(!back.isEmpty()){if(getStack(i).isEmpty())stacks.set(i,back);else if(ItemStack.areItemsAndComponentsEqual(getStack(i),back)&&getStack(i).getCount()+back.getCount()<=getStack(i).getMaxCount())getStack(i).increment(back.getCount());else if(!player.giveItemStack(back))player.dropItem(back,false);}
  }
  if(current.isEmpty())stacks.set(3,result);else current.increment(result.getCount());markDirty();return true;
 }
 @Override public int size(){return stacks.size();}
 @Override public boolean isEmpty(){return stacks.stream().allMatch(ItemStack::isEmpty);}
 @Override public ItemStack getStack(int slot){return stacks.get(slot);}
 @Override public ItemStack removeStack(int slot,int count){var out=Inventories.splitStack(stacks,slot,count);if(!out.isEmpty())markDirty();return out;}
 @Override public ItemStack removeStack(int slot){var out=Inventories.removeStack(stacks,slot);if(!out.isEmpty())markDirty();return out;}
 @Override public void setStack(int slot,ItemStack stack){stacks.set(slot,stack);stack.capCount(kind().rack()?1:stack.getMaxCount());markDirty();}
 @Override public boolean isValid(int slot,ItemStack stack){return kind()==WorkshopKind.WALL_SWORD_RACK?sword(stack):kind()==WorkshopKind.WEAPON_STAND?weapon(stack):kind()!=WorkshopKind.ALCHEMY_WORKBENCH||slot<3;}
 @Override public void clear(){stacks.clear();markDirty();}
 @Override public boolean canPlayerUse(PlayerEntity p){return Inventory.canPlayerUse(this,p);}
 @Override public int[] getAvailableSlots(Direction side){return hopperStorage()?java.util.stream.IntStream.range(0,size()).toArray():new int[0];}
 @Override public boolean canInsert(int slot,ItemStack stack,Direction side){return hopperStorage();}
 @Override public boolean canExtract(int slot,ItemStack stack,Direction side){return hopperStorage();}
 @Override public Text getDisplayName(){return getCachedState().getBlock().getName();}
 @Override public ScreenHandler createMenu(int sync,PlayerInventory player,PlayerEntity owner){return kind()==WorkshopKind.WITCHER_CHEST?new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X5,sync,player,this,5):kind()==WorkshopKind.ALCHEMY_WORKBENCH?new AlchemyScreenHandler(sync,player,this):kind()==WorkshopKind.RITUAL_URN||kind()==WorkshopKind.WITCHER_EQUIPMENT_RACK?new WorkshopContainerScreenHandler(sync,player,this,size()):null;}
 @Override public void markDirty(){super.markDirty();if(world!=null&&!world.isClient&&kind().rack())world.updateListeners(pos,getCachedState(),getCachedState(),Block.NOTIFY_LISTENERS);}
 @Override protected void writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup r){super.writeNbt(n,r);Inventories.writeNbt(n,stacks,r);}
 @Override protected void readNbt(NbtCompound n,RegistryWrapper.WrapperLookup r){super.readNbt(n,r);stacks.clear();Inventories.readNbt(n,stacks,r);}
 @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this);}
 @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup r){return createNbt(r);}
}
