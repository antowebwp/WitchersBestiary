package com.witchersbestiary.test;

import com.witchersbestiary.signs.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public final class SignSkillPointTests implements FabricGameTest {
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
 public void adminSkillPoints(TestContext context) throws Exception {
  var server = context.getWorld().getServer();
  var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MasteryFixture");
  var player = new net.minecraft.server.network.ServerPlayerEntity(server, context.getWorld(), profile,
      net.minecraft.network.packet.c2s.common.SyncedClientOptions.createDefault());
  var data = net.minecraft.server.network.ConnectedClientData.createDefault(profile, false);
  player.networkHandler = new net.minecraft.server.network.ServerPlayNetworkHandler(server,
      new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND), player, data);
  var dispatcher = server.getCommandManager().getDispatcher();
  var operator = player.getCommandSource().withLevel(2);
  var state = SignManager.state(player);
  var sign = SignType.IGNI;
  state.learn(sign);
  dispatcher.execute("wb skillpoints add 10", operator);
  context.assertTrue(state.points() == 10, "Admin add updates the player's sign balance");
  context.assertTrue(state.upgrade(sign, SignPlayerState.Upgrade.POWER)
      && state.upgrade(sign, SignPlayerState.Upgrade.COST)
      && state.unlock(sign, 1) && state.equip(sign, 1, 1), "Spend and equip through mastery API");
  var saved = SignPlayerState.read(state.write());
  context.assertTrue(saved.points() == 3 && saved.form(sign, 1) == 1, "Balance and form survive save");
  dispatcher.execute("wb skillpoints set 20", operator);
  context.assertTrue(state.points() == 20 && state.form(sign, 1) == 1, "Set preserves mastery");
  dispatcher.execute("wb skillpoints reset", operator);
  dispatcher.execute("wb skillpoints reset", operator);
  context.assertTrue(state.points() == 27 && state.form(sign, 1) == 0 && state.unlocked(sign) == 1
      && state.level(sign, SignPlayerState.Upgrade.POWER) == 0 && state.knows(sign), "Reset refunds once and preserves learning");
  try {
   dispatcher.execute("wb skillpoints add 1", operator.withLevel(0));
   throw new IllegalStateException("Non-operator command succeeded");
  } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
   context.assertTrue(state.points() == 27, "Denied command cannot change balance");
  }
  context.complete();
 }
}
