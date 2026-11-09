package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.FiendEntity;
import com.witchersbestiary.entity.monster.GriffinEntity;
import com.witchersbestiary.fist.FistRegistry;
import com.witchersbestiary.merchant.ArcaneMerchants;
import com.witchersbestiary.world.NaturalSpawns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;

public final class Stage22CTests implements FabricGameTest {
    private static final String ARENA = "witchersbestiary:village_board_arena";

    private BlockPos floor(TestContext c) {
        var origin = c.getAbsolutePos(new BlockPos(8, 10, 12));
        var world = c.getWorld();
        for (int x = -5; x <= 32; x++) for (int z = -5; z <= 5; z++) {
            var at = origin.add(x, 0, z);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
            for (int y = 0; y < 5; y++) world.setBlockState(at.up(y), Blocks.AIR.getDefaultState());
        }
        return origin;
    }

    private void place(Entity entity, BlockPos position) {
        entity.refreshPositionAndAngles(position.getX() + .5, position.getY(), position.getZ() + .5, -90, 0);
    }

    private net.minecraft.server.network.ServerPlayerEntity player(TestContext c, BlockPos pos) {
        var player = c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        place(player, pos);
        return player;
    }

    @GameTest(templateName = ARENA, tickLimit = 110)
    public void griffinChasesThroughAir(TestContext c) {
        var start = floor(c);
        var victim = player(c, start.east(25));
        var griffin = ModEntities.GRIFFIN.create(c.getWorld());
        place(griffin, start);
        c.getWorld().spawnEntity(griffin);
        griffin.setTarget(victim);
        var flew = new boolean[1];
        c.runAtEveryTick(() -> flew[0] |= griffin.getAction() == GriffinEntity.Action.AIR_CHASE);
        c.runAtTick(68, () -> {
            c.assertTrue(flew[0] && griffin.getX() > start.getX() + 9,
                "Airborne pursuit closes long distance");
            griffin.discard(); victim.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 140)
    public void griffinUsesWingsInCombat(TestContext c) {
        var start = floor(c);
        var victim = player(c, start.east(2));
        var griffin = ModEntities.GRIFFIN.create(c.getWorld());
        place(griffin, start);
        c.getWorld().spawnEntity(griffin);
        griffin.setTarget(victim);
        var wingHit = new boolean[1];
        var health = new float[]{20};
        c.runAtEveryTick(() -> {
            var action = griffin.getAction();
            if ((action == GriffinEntity.Action.WING_ATTACK || action == GriffinEntity.Action.WING_SPIN)
                    && victim.getHealth() < health[0]) wingHit[0] = true;
            victim.setHealth(20);
            health[0] = victim.getHealth();
            place(victim, start.east(2).south(1));
            if (griffin.age % 12 == 0) griffin.damage(c.getWorld().getDamageSources().playerAttack(victim), 1);
        });
        c.runAtTick(120, () -> {
            c.assertTrue(wingHit[0], "Wing impact damages survival player under counterattack");
            griffin.discard(); victim.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA, tickLimit = 100)
    public void fiendCommitsToChargeAndRecovers(TestContext c) {
        var start = floor(c);
        var world = c.getWorld();
        var victim = player(c, start.east(12));
        var fiend = ModEntities.FIEND.create(world);
        place(fiend, start);
        world.spawnEntity(fiend);
        var saved = new NbtCompound();
        fiend.writeCustomDataToNbt(saved);
        saved.putInt("FiendChargeCooldown", 0);
        fiend.readCustomDataFromNbt(saved);
        fiend.setTarget(victim);
        var charged = new boolean[1];
        var recovered = new boolean[1];
        c.runAtEveryTick(() -> {
            charged[0] |= fiend.getAction() == FiendEntity.Action.CHARGE;
            recovered[0] |= charged[0] && fiend.getAction() == FiendEntity.Action.NONE;
            victim.setHealth(20);
            if (fiend.age % 12 == 0) fiend.damage(world.getDamageSources().playerAttack(victim), 1);
        });
        c.runAtTick(55, () -> {
            c.assertTrue(charged[0] && recovered[0] && fiend.getX() > start.getX() + 3,
                "Charge advances and returns to combat under hits");
            fiend.discard(); victim.discard(); c.complete();
        });
    }

    @GameTest(templateName = ARENA)
    public void encounterTablesAndSurface(TestContext c) {
        var world = c.getWorld();
        var registry = world.getRegistryManager().get(RegistryKeys.BIOME);
        for (String name : java.util.List.of("plains", "forest", "dark_forest")) {
            var biome = registry.get(Identifier.ofVanilla(name));
            c.assertTrue(biome.getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).getEntries().stream()
                .anyMatch(e -> e.type == ModEntities.WRAITH), "Natural wraith entry in " + name);
        }
        c.assertTrue(NaturalSpawns.specterWindow(ModEntities.WRAITH, 17000)
            && !NaturalSpawns.specterWindow(ModEntities.WRAITH, 6000), "Night-only wraith window");
        var pos = c.getAbsolutePos(new BlockPos(6, 8, 6));
        var biome = registry.getEntry(Identifier.ofVanilla("stony_shore")).orElseThrow();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("biomesoplenty")) {
            for (String path : java.util.List.of("crag", "highland", "jade_cliffs")) {
                var bop = registry.getEntry(Identifier.of("biomesoplenty", path)).orElseThrow();
                c.assertTrue(bop.value().getSpawnSettings().getSpawnEntries(SpawnGroup.CREATURE).getEntries().stream()
                    .anyMatch(e -> e.type == FistRegistry.ROCK_TROLL), "Troll entry in " + path);
            }
            biome = registry.getEntry(Identifier.of("biomesoplenty", "crag")).orElseThrow();
        }
        var selected = biome;
        world.getChunk(pos).populateBiomes((x,y,z,sampler) -> selected, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        for (int y=0;y<3;y++) world.setBlockState(pos.up(y), Blocks.AIR.getDefaultState());
        c.assertTrue(SpawnRestriction.canSpawn(FistRegistry.ROCK_TROLL, world, SpawnReason.NATURAL, pos, Random.create(17)),
            "Natural troll admits rocky surface");
        world.setBlockState(pos.down(), Blocks.AIR.getDefaultState());
        c.assertTrue(!SpawnRestriction.canSpawn(FistRegistry.ROCK_TROLL, world, SpawnReason.NATURAL, pos, Random.create(17)),
            "Natural troll rejects unsupported surface");
        c.complete();
    }

    @GameTest(templateName = ARENA)
    public void wanderingMerchantKeepsStockAndExpires(TestContext c) {
        var world = c.getWorld();
        var merchant = ArcaneMerchants.TYPE.create(world);
        place(merchant, c.getAbsolutePos(new BlockPos(3, 2, 3)));
        var offer = merchant.offers().stream().filter(o -> o.id.endsWith(":venom_gland")).findFirst().orElseThrow();
        offer.stock--;
        var saved = new NbtCompound();
        merchant.writeCustomDataToNbt(saved);
        c.assertTrue(saved.getLong("MerchantDeparture") == world.getTime() + 96000, "Four-day stay");
        var loaded = ArcaneMerchants.TYPE.create(world);
        loaded.readCustomDataFromNbt(saved);
        c.assertTrue(loaded.offers().stream().filter(o -> o.id.equals(offer.id)).findFirst().orElseThrow().stock == offer.stock
            && loaded.offers().stream().map(o -> o.id).distinct().count() == loaded.offers().size(), "Stock survives reload without duplicates");
        saved.putLong("MerchantDeparture", world.getTime() - 1);
        loaded.readCustomDataFromNbt(saved);
        loaded.tick();
        c.assertTrue(loaded.isRemoved(), "Expired wanderer leaves even near players");
        merchant.discard();
        c.complete();
    }
}
