package com.witchersbestiary.test;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.GhoulEntity;
import com.witchersbestiary.entity.ai.GravePounceGoal;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
public final class GraveDevourerTests implements FabricGameTest {
 private static final String ARENA="witchersbestiary:notice_board_arena";
 private BlockPos field(TestContext c){
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){c.setBlockState(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);for(int y=2;y<14;y++){var q=c.getAbsolutePos(new BlockPos(x,y,z));if(c.getWorld().getBlockState(q).isOf(Blocks.BARRIER))c.getWorld().setBlockState(q,Blocks.AIR.getDefaultState());}}
  var at=c.getAbsolutePos(new BlockPos(7,2,7));var w=c.getWorld();var forest=w.getRegistryManager().get(RegistryKeys.BIOME).getEntry(net.minecraft.world.biome.BiomeKeys.FOREST).orElseThrow();
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=w.getChunkManager().getWorldChunk((at.getX()>>4)+x,(at.getZ()>>4)+z);if(chunk!=null)chunk.populateBiomes((a,b,d,s)->forest,w.getChunkManager().getNoiseConfig().getMultiNoiseSampler());}return at;
 }
 private net.minecraft.server.network.ServerPlayerEntity player(TestContext c,BlockPos at){var p=c.createMockCreativeServerPlayerInWorld();p.teleport(c.getWorld(),at.getX(),at.getY(),at.getZ()-4,0,0);return p;}
 private void reject(TestContext c,Runnable r){boolean no=false;try{r.run();}catch(IllegalStateException e){no=true;}c.assertTrue(no,"Rejected operation");}
 @GameTest(templateName=ARENA,batchId="grave-hunt",tickLimit=40)
 public void identityDeathRewardAndSwampExclusion(TestContext c){var at=field(c);var p=player(c,at);p.getInventory().clear();var contract=Contracts.assign(p,GraveDevourer.ID);
  reject(c,()->Contracts.assign(p,SwampLord.ID));c.assertTrue(GraveDevourer.safe(c.getWorld(),at,true),"Forest habitat");c.assertTrue(SwampLord.spawn(p,contract,at),"Common spawn dispatch");var g=(GhoulEntity)c.getWorld().getEntity(contract.hunt.target);g.setAiDisabled(true);
  c.assertTrue(g.getMaxHealth()==120&&g.huntContract.equals(contract.id()),"120HP and binding");c.assertTrue(!GraveDevourer.spawn(p,contract,at),"No duplicate");
  var ordinary=c.spawnEntity(ModEntities.GHOUL,3,2,3);ordinary.setCustomName(g.getCustomName());ordinary.damage(c.getWorld().getDamageSources().playerAttack(p),10000);c.assertTrue(contract.progress()==0,"Ordinary same-name ghoul cannot complete");
  g.damage(c.getWorld().getDamageSources().genericKill(),10000);c.assertTrue(contract.status()==ContractInstance.Status.COMPLETED&&contract.progress()==1,"Exact target death only");GraveDevourer.death(g);c.assertTrue(contract.progress()==1,"Death idempotent");
  Contracts.claim(p,contract.id());c.assertTrue(p.getInventory().count(com.witchersbestiary.economy.Currency.CROWN)==140,"Reward service");reject(c,()->Contracts.claim(p,contract.id()));var store=ContractState.read(ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());reject(c,()->ContractRewards.claim(store,store.find(contract.id()),p));
  var swamp=Contracts.assign(p,SwampLord.ID);c.assertTrue(swamp.hunt!=null,"Swamp still assignable after completed hunt");Contracts.cancel(p,swamp.id());p.discard();c.complete();
 }
 @GameTest(templateName=ARENA,batchId="grave-hunt",tickLimit=40)
 public void persistenceUnloadAndCancellation(TestContext c){var at=field(c);var p=player(c,at);var contract=Contracts.assign(p,GraveDevourer.ID);c.assertTrue(GraveDevourer.spawn(p,contract,at),"Spawn");var g=(GhoulEntity)c.getWorld().getEntity(contract.hunt.target);g.setAiDisabled(true);g.huntLeap=160;g.huntFeed=70;g.huntHealed=8;g.huntRageUsed=true;g.huntRage=123;
  var n=new NbtCompound();g.saveNbt(n);try{var file=java.nio.file.Files.createTempFile(java.nio.file.Path.of("."),"grave-entity-",".nbt");NbtIo.writeCompressed(n,file);n=NbtIo.readCompressed(file,NbtSizeTracker.ofUnlimitedBytes());java.nio.file.Files.delete(file);}catch(java.io.IOException e){throw new RuntimeException(e);}
  var copy=(GhoulEntity)EntityType.loadEntityWithPassengers(n,c.getWorld(),e->e);c.assertTrue(copy.getUuid().equals(g.getUuid())&&copy.huntContract.equals(contract.id())&&copy.huntHealed==8&&copy.huntRageUsed&&copy.huntRage==123&&copy.huntLeap==160&&copy.huntFeed==70,"Entity UUID and bounded abilities survive disk NBT");
  var store=ContractState.read(ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());c.assertTrue(store.find(contract.id()).hunt.target.equals(g.getUuid()),"Contract UUID survives reload");GraveDevourer.tick(g);c.assertTrue(SwampLord.hasBossBar(g.getUuid()),"Shared BossBar");g.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);c.assertTrue(!SwampLord.hasBossBar(g.getUuid())&&!GraveDevourer.spawn(p,contract,at),"Unload removes bar but never permits replacement");
  c.runAtTick(3,()->{c.assertTrue(c.getWorld().spawnEntity(copy),"Reload same UUID");GraveDevourer.tick(copy);c.assertTrue(!copy.isRemoved(),"Reconcile exact target");Contracts.cancel(p,contract.id());GraveDevourer.tick(copy);c.assertTrue(copy.isRemoved()&&!SwampLord.hasBossBar(copy.getUuid()),"Cancel cleanup");p.discard();c.complete();});
 }
 @GameTest(templateName=ARENA,batchId="grave-hunt",tickLimit=40)
 public void telegraphLeapCooldownAndObstacle(TestContext c){var at=field(c);var p=player(c,at);var contract=Contracts.assign(p,GraveDevourer.ID);GraveDevourer.spawn(p,contract,at);var g=(GhoulEntity)c.getWorld().getEntity(contract.hunt.target);g.setAiDisabled(true);g.setOnGround(true);var cow=c.spawnEntity(EntityType.COW,7,2,3);cow.setAiDisabled(true);g.setTarget(cow);var goal=new GravePounceGoal(g);
  c.assertTrue(goal.canStart(),"Clear loaded trajectory");goal.start();for(int i=0;i<13;i++)goal.tick();c.assertTrue(g.getVelocity().horizontalLengthSquared()==0,"14 tick telegraph");goal.tick();c.assertTrue(g.getVelocity().horizontalLengthSquared()>.05&&g.huntLeap==200,"Impulse and ten second cooldown");goal.stop();c.assertTrue(!goal.canStart(),"No repeated leap");g.huntLeap=0;g.setVelocity(Vec3d.ZERO);c.getWorld().setBlockState(at.add(0,1,-2),Blocks.STONE.getDefaultState());c.assertTrue(!goal.safeTrajectory(),"Obstacle rejects leap");Contracts.cancel(p,contract.id());GraveDevourer.tick(g);p.discard();c.complete();
 }
 @GameTest(templateName=ARENA,batchId="grave-hunt",tickLimit=40)
 public void onceOnlyRageAndActualDamageHealingCap(TestContext c){var at=field(c);var p=player(c,at);var contract=Contracts.assign(p,GraveDevourer.ID);GraveDevourer.spawn(p,contract,at);var g=(GhoulEntity)c.getWorld().getEntity(contract.hunt.target);g.setAiDisabled(true);g.setHealth(40);GraveDevourer.tick(g);c.assertTrue(g.huntRageUsed&&g.huntRage==299,"Below 40 percent triggers 15 second rage");for(int i=0;i<301;i++)GraveDevourer.tick(g);c.assertTrue(g.huntRage==0&&g.huntRageUsed,"Rage does not retrigger");
  var cow=c.spawnEntity(EntityType.COW,7,2,4);cow.setAiDisabled(true);cow.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);cow.setInvulnerable(true);g.tryAttack(cow);c.assertTrue(g.huntHealed==0,"No heal on rejected damage");cow.setInvulnerable(false);
  c.assertTrue(g.tryAttack(cow)&&g.huntHealed==2&&g.huntFeed==80,"Only real attack damage heals two HP");g.tryAttack(cow);c.assertTrue(g.huntHealed==2,"Feed cooldown");
  for(int i=0;i<10;i++){g.huntFeed=0;cow.timeUntilRegen=0;g.tryAttack(cow);}c.assertTrue(g.huntHealed==12&&g.getHealth()==52,"Twelve HP lifetime cap");Contracts.cancel(p,contract.id());GraveDevourer.tick(g);p.discard();c.complete();
 }
 @GameTest(templateName=ARENA,batchId="grave-hunt",tickLimit=40)
 public void habitatAndForgedBindingAreRejected(TestContext c){var at=field(c);var p=player(c,at);var contract=Contracts.assign(p,GraveDevourer.ID);
  c.assertTrue(!GraveDevourer.safe(c.getWorld(),at.up(4),true),"No aerial spawn");
  c.assertTrue(!GraveDevourer.safe(c.getWorld(),new BlockPos(20000000,at.getY(),20000000),true),"Unloaded region rejected without loading");
  c.assertTrue(c.getWorld().getChunkManager().getWorldChunk(20000000>>4,20000000>>4)==null,"No chunk forced by habitat check");
  c.assertTrue(GraveDevourer.spawn(p,contract,at),"Real target");var boss=(GhoulEntity)c.getWorld().getEntity(contract.hunt.target);boss.setAiDisabled(true);
  var forged=c.spawnEntity(ModEntities.GHOUL,3,2,3);forged.huntContract=contract.id();forged.damage(c.getWorld().getDamageSources().genericKill(),10000);c.assertTrue(contract.progress()==0,"Contract ID alone cannot credit a different UUID");
  var other=player(c,at.add(0,0,3));var swamp=Contracts.assign(other,SwampLord.ID);reject(c,()->Contracts.assign(other,GraveDevourer.ID));Contracts.cancel(other,swamp.id());Contracts.cancel(p,contract.id());GraveDevourer.tick(boss);p.discard();other.discard();c.complete();
 }

}
