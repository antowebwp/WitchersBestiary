package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.fist.FistRegistry;
import com.witchersbestiary.merchant.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.SpawnHelper;

public final class Stage22CEncounterTests implements FabricGameTest {
    private static final String ARENA = "witchersbestiary:village_board_arena";

    private BlockPos patch(TestContext c, Identifier id, int offset) {
        var world = c.getWorld();
        var center = c.getAbsolutePos(new BlockPos(23 + offset, 8, 23));
        var biome = world.getRegistryManager().get(RegistryKeys.BIOME).getEntry(id).orElseThrow();
        world.getChunk(center).populateBiomes((x,y,z,sampler) -> biome, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
            var pos = center.add(x, 0, z);
            world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
            for (int y = 0; y < 4; y++) world.setBlockState(pos.up(y), Blocks.AIR.getDefaultState());
        }
        return center;
    }

    private MobEntity naturalSpawn(TestContext c, BlockPos center, EntityType<?> type, Identifier biome) {
        var world = c.getWorld();
        var chunk = world.getChunk(center);
        var found = new java.util.ArrayList<MobEntity>();
        for (int i = 0; i < 1536 && found.isEmpty(); i++) {
            SpawnHelper.spawnEntitiesInChunk(type.getSpawnGroup(), world, chunk, center,
                (candidate, pos, source) -> found.isEmpty() && candidate == type && world.getBiome(pos).matchesId(biome),
                (mob, source) -> found.add(mob));
        }
        c.assertTrue(found.size() == 1 && world.getEntity(found.getFirst().getUuid()) == found.getFirst(),
            "Natural singleton " + EntityType.getId(type));
        return found.getFirst();
    }

    @GameTest(templateName = ARENA, tickLimit = 60)
    public void naturalWraithAndTrollAppear(TestContext c) {
        var world = c.getWorld();
        var plains = Identifier.ofVanilla("plains");
        var rocky = FabricLoader.getInstance().isModLoaded("biomesoplenty")
            ? Identifier.of("biomesoplenty", "crag") : Identifier.ofVanilla("stony_shore");
        var wraith = patch(c, plains, 0);
        var troll = patch(c, rocky, 32);
        var player = c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setNoGravity(true);
        player.refreshPositionAndAngles(wraith.getX() + 16, wraith.getY(), wraith.getZ() - 36, 0, 0);
        long previousTime = world.getTimeOfDay();
        world.setTimeOfDay(18000);
        c.runAtTick(16, () -> {
            try {
                naturalSpawn(c, wraith, ModEntities.WRAITH, plains).discard();
                naturalSpawn(c, troll, FistRegistry.ROCK_TROLL, rocky).discard();
            } finally {
                world.setTimeOfDay(previousTime + 16);
                player.discard();
            }
            c.complete();
        });
    }

    @GameTest(templateName = ARENA)
    public void merchantWaitsForSessionAndSchedulesReturn(TestContext c) {
        var world = c.getWorld();
        var ledger = MerchantLedger.get(world.getServer());
        var oldMerchant = ledger.wanderer;
        long oldNext = ledger.nextWander, oldDeparture = ledger.wanderDeparture;
        var pos = c.getAbsolutePos(new BlockPos(4, 3, 4));
        var player = c.createMockCreativeServerPlayerInWorld();
        player.refreshPositionAndAngles(pos.getX() + 1.5, pos.getY(), pos.getZ() + .5, 0, 0);
        var merchant = ArcaneMerchants.TYPE.create(world);
        merchant.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        var saved = new NbtCompound();
        merchant.writeCustomDataToNbt(saved);
        long expiry = world.getTime() - 48001;
        saved.putLong("MerchantDeparture", expiry);
        merchant.readCustomDataFromNbt(saved);
        world.spawnEntity(merchant);
        ledger.wanderer = merchant.getUuid();
        ledger.wanderDeparture = expiry;
        ledger.nextWander = expiry + 30000;
        try {
            player.currentScreenHandler = new MerchantHandler(1, player.getInventory(), merchant.getId());
            merchant.tick();
            c.assertTrue(!merchant.isRemoved() && merchant.trading(), "Trading session delays departure");
            player.currentScreenHandler = player.playerScreenHandler;
            merchant.tick();
            c.assertTrue(merchant.isRemoved() && ledger.wanderer == null && ledger.nextWander == world.getTime() + 30000,
                "Return interval begins after actual departure");
            var restored = MerchantLedger.read(ledger.writeNbt(new NbtCompound(), world.getRegistryManager()), world.getRegistryManager());
            c.assertTrue(restored.nextWander == ledger.nextWander, "Return schedule survives reload");
        } finally {
            player.currentScreenHandler = player.playerScreenHandler;
            player.discard();
            merchant.discard();
            ledger.wanderer = oldMerchant;
            ledger.nextWander = oldNext;
            ledger.wanderDeparture = oldDeparture;
            ledger.markDirty();
        }
        c.complete();
    }
}
