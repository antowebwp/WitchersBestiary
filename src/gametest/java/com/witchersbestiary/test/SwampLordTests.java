package com.witchersbestiary.test;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.DrownerEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import java.util.*;
public final class SwampLordTests implements FabricGameTest {
 private static final String ARENA="witchersbestiary:notice_board_arena";
 private BlockPos field(TestContext c){for(int x=0;x<16;x++)for(int z=0;z<16;z++)c.setBlockState(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);var at=c.getAbsolutePos(new BlockPos(7,2,7));c.getWorld().setBlockState(at.add(0,-1,2),Blocks.WATER.getDefaultState());return at;}
 private net.minecraft.server.network.ServerPlayerEntity player(TestContext c,BlockPos at){var p=c.createMockCreativeServerPlayerInWorld();p.teleport(c.getWorld(),at.getX(),at.getY(),at.getZ()-4,0,0);return p;}
 private void reject(TestContext c,Runnable r){boolean no=false;try{r.run();}catch(IllegalStateException e){no=true;}c.assertTrue(no,"Rejected operation");}
 @GameTest(templateName=ARENA,batchId="swamp-hunt",tickLimit=30)
 public void fullContractIdentityDeathAndRewardCycle(TestContext c){var at=field(c);var p=player(c,at);p.getInventory().clear();var contract=Contracts.assign(p,SwampLord.ID);
  reject(c,()->Contracts.assign(p,SwampLord.ID));c.assertTrue(SwampLord.safe(c.getWorld(),at,true),"Bank habitat");c.assertTrue(SwampLord.spawn(p,contract,at),"Unique spawn");var boss=(DrownerEntity)c.getWorld().getEntity(contract.hunt.target);boss.setAiDisabled(true);c.assertTrue(boss.getMaxHealth()==108&&boss.huntContract.equals(contract.id()),"Boss stats/binding");
  c.assertTrue(!SwampLord.spawn(p,contract,at),"No duplicate");var ordinary=c.spawnEntity(ModEntities.DROWNER,3,2,3);ordinary.setCustomName(boss.getCustomName());ordinary.damage(c.getWorld().getDamageSources().playerAttack(p),10000);c.assertTrue(contract.progress()==0,"Same name ordinary kill ignored");
  var helper=c.createMockCreativeServerPlayerInWorld();boss.damage(c.getWorld().getDamageSources().playerAttack(helper),10000);c.assertTrue(contract.status()==ContractInstance.Status.COMPLETED&&contract.hunt.status==SwampHunt.Status.DEAD,"Helping player completes exact owner target");SwampLord.death(boss);c.assertTrue(contract.progress()==1,"Completion once");
  reject(c,()->Contracts.claim(helper,contract.id()));int xp=p.totalExperience;Contracts.claim(p,contract.id());c.assertTrue(p.totalExperience==xp+120&&p.getInventory().count(Items.EMERALD)==12,"Existing reward service");reject(c,()->Contracts.claim(p,contract.id()));
  var store=ContractState.read(ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());c.assertTrue(store.find(contract.id()).status()==ContractInstance.Status.REWARDED,"Claim survives state reload");reject(c,()->ContractRewards.claim(store,store.find(contract.id()),p));p.discard();helper.discard();c.complete();}
 @GameTest(templateName=ARENA,batchId="swamp-hunt",tickLimit=30)
 public void persistedEntityIdentityCooldownsAndMinionLimit(TestContext c){var at=field(c);var p=player(c,at);var contract=Contracts.assign(p,SwampLord.ID);c.assertTrue(SwampLord.spawn(p,contract,at),"Spawn");var boss=(DrownerEntity)c.getWorld().getEntity(contract.hunt.target);boss.setAiDisabled(true);var dummy=c.spawnEntity(EntityType.COW,7,2,3);dummy.setAiDisabled(true);boss.setTarget(dummy);boss.huntSummon=0;boss.huntDash=40;SwampLord.tick(boss);
  c.assertTrue(Arrays.stream(boss.huntHelpers).filter(Objects::nonNull).count()==2,"Helper cap and two successful summons");var first=(DrownerEntity)c.getWorld().getEntity(boss.huntHelpers[0]);first.damage(c.getWorld().getDamageSources().genericKill(),10000);c.assertTrue(contract.progress()==0,"Minion death never completes named hunt");boss.huntSummon=0;SwampLord.tick(boss);
  var ids=boss.huntHelpers.clone();boss.huntSummon=0;SwampLord.tick(boss);c.assertTrue(Arrays.equals(ids,boss.huntHelpers),"No extra helpers");
  var n=new NbtCompound();boss.saveNbt(n);try{var file=java.nio.file.Files.createTempFile(java.nio.file.Path.of("."),"swamp-entity-",".nbt");NbtIo.writeCompressed(n,file);n=NbtIo.readCompressed(file,NbtSizeTracker.ofUnlimitedBytes());java.nio.file.Files.delete(file);}catch(java.io.IOException e){throw new RuntimeException(e);}
  var copy=EntityType.loadEntityWithPassengers(n,c.getWorld(),e->e);c.assertTrue(copy instanceof DrownerEntity&&copy.getUuid().equals(boss.getUuid()),"Vanilla entity NBT UUID");var restored=(DrownerEntity)copy;c.assertTrue(restored.huntContract.equals(contract.id())&&restored.huntDash==boss.huntDash&&Arrays.equals(restored.huntHelpers,boss.huntHelpers),"Binding/combat/helper IDs survive NBT");
  var store=ContractState.read(ContractState.get(p.getServer()).writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());var saved=store.find(contract.id());c.assertTrue(saved.hunt.target.equals(boss.getUuid())&&saved.hunt.position.equals(at),"Persistent UUID and location");
  boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);c.assertTrue(!SwampLord.hasBossBar(boss.getUuid()),"Unloaded bar removed");c.assertTrue(contract.hunt.status==SwampHunt.Status.ACTIVE&&!SwampLord.spawn(p,contract,at),"Absence/unload is not death or permission to spawn");
  c.runAtTick(3,()->{c.assertTrue(c.getWorld().spawnEntity(restored),"Reload same saved UUID into entity manager");SwampLord.tick(restored);c.assertTrue(!restored.isRemoved()&&contract.hunt.target.equals(restored.getUuid()),"Restored target reconciles without replacement");Contracts.cancel(p,contract.id());SwampLord.tick(restored);p.discard();c.complete();});}
 @GameTest(templateName=ARENA,batchId="swamp-hunt",tickLimit=30)
 public void cancellationCleanupDashAndUnsuitableTerrain(TestContext c){var at=field(c);var p=player(c,at);c.assertTrue(!SwampLord.safe(c.getWorld(),at.up(4),true),"Air platform rejected");var contract=Contracts.assign(p,SwampLord.ID);SwampLord.spawn(p,contract,at);var boss=(DrownerEntity)c.getWorld().getEntity(contract.hunt.target);boss.setAiDisabled(true);var dummy=c.spawnEntity(EntityType.COW,7,2,3);dummy.setAiDisabled(true);boss.setTarget(dummy);boss.huntDash=0;boss.huntSummon=0;SwampLord.tick(boss);c.assertTrue(boss.huntWindup==15,"Telegraphed dash windup="+boss.huntWindup+" dash="+boss.huntDash+" target="+boss.getTarget()+" distance="+boss.squaredDistanceTo(p));for(int i=0;i<15;i++)SwampLord.tick(boss);c.assertTrue(boss.huntDash==120&&boss.getVelocity().horizontalLengthSquared()>.1,"Dash cooldown/impulse");var helpers=boss.huntHelpers.clone();Contracts.cancel(p,contract.id());SwampLord.tick(boss);c.assertTrue(boss.isRemoved()&&!SwampLord.hasBossBar(boss.getUuid()),"Cancelled target and bar removed");for(var id:helpers)if(id!=null)c.assertTrue(c.getWorld().getEntity(id)==null||c.getWorld().getEntity(id).isRemoved(),"Loaded helpers cleaned");p.discard();c.complete();}
}
