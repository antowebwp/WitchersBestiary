package com.witchersbestiary.test;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.Identifier;
import com.witchersbestiary.registry.ModItems;
import java.util.*;
public final class BlackDyeRecipeTests implements FabricGameTest {
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20g-recipes",tickLimit=40)
 public void nekkerClawBlackDyeBothGridsAndUnlock(TestContext c){verify(c,ModItems.NEKKER_CLAW,"nekker","drowner");}
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20g-recipes",tickLimit=40)
 public void drownerClawBlackDyeBothGridsAndUnlock(TestContext c){verify(c,ModItems.DROWNER_CLAW,"drowner","nekker");}
 private void verify(TestContext c,Item claw,String species,String other){
  var w=c.getWorld();var id=Identifier.of("witchersbestiary","black_dye_from_"+species+"_claw");var manager=w.getRecipeManager();var entry=manager.get(id).orElseThrow();c.assertTrue(entry.value() instanceof ShapelessRecipe,"Registered vanilla shapeless serializer");
  for(int size:new int[]{2,3})for(int slot=0;slot<size*size;slot++){var stacks=new ArrayList<ItemStack>(Collections.nCopies(size*size,ItemStack.EMPTY));stacks.set(slot,new ItemStack(claw));var input=CraftingRecipeInput.create(size,size,stacks);var match=manager.getFirstMatch(RecipeType.CRAFTING,input,w).orElseThrow();c.assertTrue(match.id().equals(id),"Independent recipe selected in "+size+"x"+size+" slot"+slot);var output=match.value().craft(input,w.getRegistryManager());c.assertTrue(output.isOf(Items.BLACK_DYE)&&output.getCount()==2,"Exactly two black dye");}
  for(Item vanilla:new Item[]{Items.INK_SAC,Items.WITHER_ROSE}){var input=CraftingRecipeInput.create(2,2,List.of(new ItemStack(vanilla),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY));var match=manager.getFirstMatch(RecipeType.CRAFTING,input,w).orElseThrow();var output=match.value().craft(input,w.getRegistryManager());c.assertTrue(match.id().getNamespace().equals("minecraft")&&output.isOf(Items.BLACK_DYE)&&output.getCount()==1,"Vanilla black dye unchanged");}
  var player=c.createMockCreativeServerPlayerInWorld();var otherId=Identifier.of("witchersbestiary","black_dye_from_"+other+"_claw");var advancement=w.getServer().getAdvancementLoader().get(Identifier.of("witchersbestiary","recipes/black_dye_from_"+species+"_claw"));c.assertTrue(advancement!=null&&!player.getRecipeBook().contains(id)&&!player.getRecipeBook().contains(otherId),"Independent recipes initially locked");player.getInventory().setStack(0,new ItemStack(claw));player.playerScreenHandler.sendContentUpdates();
  c.runAtTick(3,()->{c.assertTrue(player.getRecipeBook().contains(id)&&!player.getRecipeBook().contains(otherId),"Obtaining matching claw unlocks only its recipe");c.assertTrue(player.getAdvancementTracker().getProgress(advancement).isDone(),"Inventory advancement and recipe reward applied");player.discard();c.complete();});
 }
}
