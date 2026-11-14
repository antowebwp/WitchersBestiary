package com.witchersbestiary.test;
import com.witchersbestiary.workshop.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;import net.minecraft.item.*;import net.minecraft.util.math.BlockPos;
public final class AlchemyTableTests implements FabricGameTest {
 private static net.minecraft.server.network.ServerPlayerEntity player(TestContext context) {
  var world = context.getWorld();
  var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "AlchemyFixture");
  var data = net.minecraft.server.network.ConnectedClientData.createDefault(profile, false);
  var player = new net.minecraft.server.network.ServerPlayerEntity(world.getServer(), world, profile, data.syncedOptions());
  player.networkHandler = new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
      new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND), player, data);
  return player;
 }
 private WorkshopInventory table(TestContext c){var pos=c.getAbsolutePos(new BlockPos(1,2,1));c.getWorld().setBlockState(pos,ModWorkshop.BLOCKS.get(WorkshopKind.ALCHEMY_WORKBENCH).getDefaultState());return (WorkshopInventory)c.getWorld().getBlockEntity(pos);}
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="alchemy-all")
 public void everyActualRecipeConsumesOnceAndKeepsExistingBlock(TestContext c){var t=table(c);var p=player(c);p.refreshPositionAndAngles(t.getPos().getX()+.5,t.getPos().getY(),t.getPos().getZ()+2,0,0);var recipes=AlchemyRecipes.catalog(c.getWorld());c.assertTrue(recipes.size()>=17,"Real registered consumables and three refinements");for(int n=0;n<recipes.size();n++){p.getInventory().clear();t.clear();var recipe=recipes.get(n);for(var ingredient:recipe.value().getIngredients())if(!ingredient.isEmpty()){var stack=ingredient.getMatchingStacks()[0].copyWithCount(1);p.getInventory().insertStack(stack);}c.assertTrue(AlchemyRecipes.craft(t,p,n),"Craft actual recipe "+recipe.id());var result=recipe.value().getResult(c.getWorld().getRegistryManager());c.assertTrue(ItemStack.areItemsAndComponentsEqual(t.getStack(3),result)&&t.getStack(3).getCount()==result.getCount(),"Exact result "+recipe.id());c.assertTrue(!AlchemyRecipes.craft(t,p,n),"Replay cannot create output without inputs "+recipe.id());}p.discard();c.complete();}
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="alchemy-safety")
 public void fullOutputDistanceAndSharedTableAreAtomic(TestContext c){var t=table(c);var a=player(c);var b=player(c);for(var p:java.util.List.of(a,b)){p.refreshPositionAndAngles(t.getPos().getX()+.5,t.getPos().getY(),t.getPos().getZ()+2,0,0);p.getInventory().clear();}var recipes=AlchemyRecipes.catalog(c.getWorld());int id=0;var entry=recipes.get(id);for(var ingredient:entry.value().getIngredients())if(!ingredient.isEmpty())a.getInventory().insertStack(ingredient.getMatchingStacks()[0].copyWithCount(1));t.setStack(3,new ItemStack(Items.STONE,64));var before=a.getInventory().writeNbt(new net.minecraft.nbt.NbtList()).toString();c.assertTrue(!AlchemyRecipes.craft(t,a,id)&&before.equals(a.getInventory().writeNbt(new net.minecraft.nbt.NbtList()).toString()),"Blocked output never consumes");t.setStack(3,ItemStack.EMPTY);c.assertTrue(AlchemyRecipes.craft(t,a,id)&&!AlchemyRecipes.craft(t,b,id),"Second player cannot reuse first player's inputs");var saved=t.createNbt(c.getWorld().getRegistryManager());var restored=new WorkshopInventory(t.getPos(),t.getCachedState());restored.read(saved,c.getWorld().getRegistryManager());c.assertTrue(ItemStack.areEqual(restored.getStack(3),t.getStack(3)),"Existing four-slot serialization retained");a.refreshPositionAndAngles(t.getPos().getX()+100,t.getPos().getY(),t.getPos().getZ(),0,0);c.assertTrue(!AlchemyRecipes.craft(t,a,id)&&!AlchemyRecipes.craft(t,b,-1),"Distance and forged selection rejected");a.discard();b.discard();c.complete();}
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
 public void familyAlternativesAndMutagenBonus(TestContext context) {
  var world = context.getWorld();
  var player = player(context);
  var table = table(context);
  player.refreshPositionAndAngles(table.getPos().getX()+.5,table.getPos().getY(),table.getPos().getZ()+2,0,0);
  var recipes = AlchemyRecipes.catalog(world);
  var oil = recipes.stream().filter(r -> r.id().getPath().equals("necrophage_oil_from_monsters")).findFirst().orElseThrow();
  for (var material : java.util.List.of(
       com.witchersbestiary.registry.ModItems.GHOUL_CLAW, com.witchersbestiary.registry.ModItems.DROWNER_ESSENCE)) {
   table.clear();
   player.getInventory().clear();
   player.getInventory().insertStack(new ItemStack(material));
   for (int i=1;i<oil.value().getIngredients().size();i++)
    player.getInventory().insertStack(oil.value().getIngredients().get(i).getMatchingStacks()[0].copy());
   context.assertTrue(AlchemyRecipes.craft(table, player, recipes.indexOf(oil)), "Claw or essence crafts the same oil");
  }
  var mutagen = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("witchersbestiary:mutagen_necrophage_3"));
  player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,new ItemStack(mutagen));
  mutagen.use(world,player,net.minecraft.util.Hand.MAIN_HAND);
  context.assertTrue(Math.abs(com.witchersbestiary.alchemy.MutagenItem.multiplier(player,
      com.witchersbestiary.combat.MonsterCategory.NECROPHAGE)-1.13f)<.001f,"Superior specialization gives 13 percent");
  context.assertTrue(com.witchersbestiary.alchemy.MutagenItem.remove(player,0),"Equipped mutagen can be retrieved");
  var template = com.witchersbestiary.registry.ModItems.SCHOOL_TEMPLATES.getFirst();
  var recipe = world.getRecipeManager().get(net.minecraft.util.Identifier.of("witchersbestiary:cat_enhanced_boots"))
      .orElseThrow();
  var transform = (net.minecraft.recipe.SmithingRecipe)recipe.value();
  context.assertTrue(transform.testTemplate(new ItemStack(template)) &&
      !transform.testTemplate(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
      "Only the school template matches the recipe");
  context.complete();
 }
}
