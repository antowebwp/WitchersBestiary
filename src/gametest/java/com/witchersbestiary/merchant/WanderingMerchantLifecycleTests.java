package com.witchersbestiary.merchant;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public final class WanderingMerchantLifecycleTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 45)
    public void naturalArrivalDepartureAndReturn(TestContext c) {
        var world = c.getWorld();
        var server = world.getServer();
        var clock = server.getSaveProperties().getMainWorldProperties();
        var ledger = MerchantLedger.get(server);
        var origin = c.getAbsolutePos(new BlockPos(1, 2, 1));

var id = java.util.UUID.randomUUID();
var profile = new com.mojang.authlib.GameProfile(id, "trade" + id.toString().substring(0, 8));
var data = net.minecraft.server.network.ConnectedClientData.createDefault(profile, false);
var player = new net.minecraft.server.network.ServerPlayerEntity(world.getServer(), world, profile, data.syncedOptions());
player.networkHandler = new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
        new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND), player, data);
        player.setNoGravity(true);
        player.refreshPositionAndAngles(origin.getX() + .5, origin.getY(), origin.getZ() + .5, 0, 0);
        c.runAtTick(10, () -> {
            long initialTime = world.getTime();
            long initialNext = ledger.nextWander;
            long initialDeparture = ledger.wanderDeparture;
            var initialMerchant = ledger.wanderer;
            try {
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                    world.getChunk(player.getChunkPos().x + x, player.getChunkPos().z + z);
                }
                world.onPlayerConnected(player);
                ledger.nextWander = initialTime;
                ledger.wanderer = null;
                ledger.wanderDeparture = 0;
                MerchantSpawns.wander(server);
                c.assertTrue(ledger.wanderer != null && world.getEntity(ledger.wanderer) instanceof ArcaneMerchant,
                    "Natural arrival on loaded terrain");
                var first = (ArcaneMerchant) world.getEntity(ledger.wanderer);
                c.assertTrue(ledger.wanderDeparture - world.getTime() == 96000, "Four-day stay");
                first.offers().stream().filter(o -> o.id.endsWith(":venom_gland")).findFirst().orElseThrow().stock = 0;
                clock.setTime(ledger.wanderDeparture - 1);
                first.tick();
                c.assertTrue(!first.isRemoved(), "Merchant stays until the fourth day ends");
                clock.setTime(ledger.wanderDeparture);
                first.tick();
                long delay = ledger.nextWander - world.getTime();
                c.assertTrue(first.isRemoved() && delay >= 24000 && delay <= 48000 && ledger.wanderer == null,
                    "Departure schedules return in one to two days");
                clock.setTime(ledger.nextWander - 1);
                MerchantSpawns.wander(server);
                c.assertTrue(ledger.wanderer == null, "No premature replacement");
                clock.setTime(ledger.nextWander);
                MerchantSpawns.wander(server);
                c.assertTrue(ledger.wanderer != null && world.getEntity(ledger.wanderer) instanceof ArcaneMerchant,
                    "Natural return");
                var replacement = (ArcaneMerchant) world.getEntity(ledger.wanderer);
                c.assertTrue(!replacement.getUuid().equals(first.getUuid())
                    && replacement.offers().stream().anyMatch(o -> o.id.endsWith(":venom_gland") && o.stock == 4)
                    && replacement.offers().stream().map(o -> o.id).distinct().count() == replacement.offers().size(),
                    "Natural replacement has fresh stock without duplicate offers");
            } finally {
                if (ledger.wanderer != null && !ledger.wanderer.equals(initialMerchant)
                    && world.getEntity(ledger.wanderer) instanceof ArcaneMerchant merchant) merchant.discard();
                ledger.wanderer = initialMerchant;
                ledger.wanderDeparture = initialDeparture;
                ledger.nextWander = initialNext;
                ledger.markDirty();
                clock.setTime(initialTime);
                world.removePlayer(player, net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
            c.complete();
        });
    }
}
