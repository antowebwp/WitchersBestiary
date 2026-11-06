package com.witchersbestiary.test;
import com.witchersbestiary.fist.*;import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;import net.minecraft.test.*;import net.minecraft.nbt.NbtCompound;import java.util.UUID;
public final class TournamentTests implements FabricGameTest {
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
 public void progressionPayoutReplayAndRecovery(TestContext c){var ledger=new TournamentProgress();var rewards=new FistRewards();var a=UUID.randomUUID();var b=UUID.randomUUID();long now=100;
  c.assertTrue(!ledger.enter(a,TournamentProgress.Category.CITY),"City locked");
  for(var category:TournamentProgress.Category.values()){
   c.assertTrue(ledger.enter(a,category),"Unlocked category");for(int r=0;r<category.rounds;r++){c.assertTrue(ledger.begin(a)&&!ledger.begin(a),"Only one live bout");c.assertTrue(ledger.finish(a,true,now,rewards)&&!ledger.finish(a,true,now,rewards),"One round per completion");}
  }
  c.assertTrue(rewards.pending(a)==1550&&ledger.entry(a).titles==7&&rewards.pending(b)==0,"Exact personal champion rewards");
  ledger.enter(a,TournamentProgress.Category.VILLAGE);for(int r=0;r<3;r++){ledger.begin(a);ledger.finish(a,true,now,rewards);}c.assertTrue(rewards.pending(a)==1550,"Cooldown suppresses paid training");
  ledger.enter(b,TournamentProgress.Category.VILLAGE);ledger.begin(b);ledger.finish(b,true,now,rewards);
  var restored=TournamentProgress.read(ledger.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());c.assertTrue(restored.entry(b).round==1&&restored.entry(b).active!=null,"Between rounds restored");restored.begin(b);var interrupted=TournamentProgress.read(restored.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());c.assertTrue(interrupted.entry(b).active==null&&interrupted.entry(b).lastRound==1,"Interrupted fight loses tournament, preserves history");
  restored.finish(b,false,now,rewards);c.assertTrue(restored.entry(b).active==null&&rewards.pending(b)==0,"Loss has no payout");c.complete();
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",tickLimit=200)
 public void officialArenaBoutCleanupAndNoRoundMoney(TestContext c){
  var w=c.getWorld();var origin=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(24,3,24));
  for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){var at=origin.add(x,0,z);w.setBlockState(at.down(),net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState());for(int y=0;y<8;y++)w.setBlockState(at.up(y),net.minecraft.block.Blocks.AIR.getDefaultState());}
  var organizer=FistRegistry.NOVICE.create(w);organizer.refreshPositionAndAngles(origin.getX(),origin.getY(),origin.getZ(),0,0);w.spawnEntity(organizer);
  var p=c.createMockCreativeServerPlayerInWorld();p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);p.getInventory().clear();p.refreshPositionAndAngles(origin.getX()+1,origin.getY(),origin.getZ(),0,0);float health=p.getHealth();var ledger=TournamentProgress.get(w.getServer());ledger.abandon(p.getUuid());ledger.enter(p.getUuid(),TournamentProgress.Category.VILLAGE);long before=FistRewards.get(w.getServer()).pending(p.getUuid());
  c.assertTrue(Tournaments.start(p,organizer),"Safe arena and official bout start");var bout=FistFights.active(p);c.assertTrue(bout!=null&&!FistFights.abilitiesAllowed(p),"Existing official restrictions");var q=c.createMockCreativeServerPlayerInWorld();q.getInventory().clear();q.refreshPositionAndAngles(origin.getX()+1,origin.getY(),origin.getZ()+1,0,0);ledger.enter(q.getUuid(),TournamentProgress.Category.VILLAGE);c.assertTrue(Tournaments.start(q,organizer),"Second player gets independent safe arena");var other=FistFights.active(q);c.assertTrue(other.npc()!=bout.npc()&&other.arena().squaredDistanceTo(bout.arena())>=625,"Full bout boundaries do not overlap");FistFights.cancel(q);q.discard();
  bout.fight().stage=FistFightSession.Stage.FINISHED;bout.fight().playerWon=true;FistFights.tick(w.getServer());
  c.assertTrue(ledger.entry(p.getUuid()).round==1&&FistFights.active(p)==null&&bout.npc().isRemoved(),"Win advances once and cleans NPC");c.assertTrue(FistRewards.get(w.getServer()).pending(p.getUuid())==before&&p.getHealth()==health,"No intermediate reward or actual health damage");
  p.refreshPositionAndAngles(origin.getX()+1,origin.getY(),origin.getZ(),0,0);c.assertTrue(Tournaments.start(p,organizer),"Continue is explicit");FistFights.cancel(p);c.assertTrue(ledger.entry(p.getUuid()).active==null&&ledger.entry(p.getUuid()).lastRound==1,"Surrender keeps history and releases session");var wallet=new net.minecraft.item.ItemStack(com.witchersbestiary.economy.Currency.WALLET);p.getInventory().setStack(10,wallet);long money=com.witchersbestiary.merchant.MerchantTransactions.balance(p);
  for(var category:TournamentProgress.Category.values()){c.assertTrue(ledger.enter(p.getUuid(),category),"Category accessible in gameplay ledger");for(int round=0;round<category.rounds;round++){p.getInventory().selectedSlot=8;p.refreshPositionAndAngles(origin.getX()+1,origin.getY(),origin.getZ(),0,0);c.assertTrue(Tournaments.start(p,organizer),"Next official round starts only on request: "+category+"/"+round+" hand="+p.getMainHandStack()+" active="+ledger.entry(p.getUuid()).active);var current=FistFights.active(p);current.fight().stage=FistFightSession.Stage.FINISHED;current.fight().playerWon=true;FistFights.tick(w.getServer());}}
  c.assertTrue(com.witchersbestiary.merchant.MerchantTransactions.balance(p)==money+1550&&ledger.entry(p.getUuid()).titles==7,"All12 actual official sessions pay exact crowns and grant titles");
  p.discard();organizer.discard();c.complete();
 }
}
