package com.witchersbestiary.test;
import com.witchersbestiary.merchant.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.nbt.NbtCompound;
import java.util.*;
public final class MerchantAssortmentTests implements FabricGameTest {
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
 public void newWanderingAssortment(TestContext c){
  var swords=Set.of("witchersbestiary:steel_hunter_sword","witchersbestiary:silver_witcher_sword","witchersbestiary:grave_blade");
  var seen=new HashSet<String>();int armed=0;var seeds=new Random(21021);
  for(int i=0;i<64;i++){
   var e=ArcaneMerchants.TYPE.create(c.getWorld());e.setUuid(new UUID(1,seeds.nextLong()));var offers=e.offers();
   for(var id:List.of("minecraft:bread","minecraft:cooked_beef","minecraft:cooked_porkchop","minecraft:golden_carrot","witchersbestiary:griffin_feather","witchersbestiary:gravebound_steel_fragment"))c.assertTrue(offers.stream().anyMatch(o->o.id.equals(id)&&o.stock>0&&o.price>0),"New food/material registered: "+id);
   var weapons=offers.stream().filter(o->swords.contains(o.id)).toList();c.assertTrue(weapons.size()<=1,"At most one rare sword");
   for(var o:weapons){armed++;seen.add(o.id);c.assertTrue(o.price>=2500&&o.price<=7000&&o.stock==1,"Expensive registered sword with limited stock");o.stock=0;}
   var n=new NbtCompound();e.writeCustomDataToNbt(n);var copy=ArcaneMerchants.TYPE.create(c.getWorld());copy.readCustomDataFromNbt(n);
   c.assertTrue(copy.offers().stream().map(o->o.id+":"+o.price+":"+o.stock).toList().equals(offers.stream().map(o->o.id+":"+o.price+":"+o.stock).toList()),"New stock persists including sold sword");
   e.village("assortment",e.getBlockPos());c.assertTrue(e.offers().stream().noneMatch(o->swords.contains(o.id)||o.id.equals("minecraft:bread")),"Village assortment unchanged");e.discard();copy.discard();
  }
  c.assertTrue(armed>0&&armed<64&&seen.size()==3,"Rare selection covers all three existing swords");c.complete();
 }
}
