package com.witchersbestiary.test;

import com.witchersbestiary.board.VillageOffers;
import com.witchersbestiary.contracts.Contracts;
import com.witchersbestiary.contracts.procedural.ContractSpecies;
import com.witchersbestiary.contracts.procedural.ProceduralHunts;
import com.witchersbestiary.contracts.procedural.ProceduralProfile;
import com.witchersbestiary.merchant.ArcaneMerchants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class Stage22XTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void boardSampling(TestContext test) {
        var state = new VillageOffers();
        var pool = ContractSpecies.ENTITIES.keySet().stream().map(ProceduralHunts::id)
                .filter(id -> Contracts.DEFINITIONS.find(id) != null).map(Object::toString).sorted().toList();
        test.assertTrue(pool.size() == 15, "All fifteen definitions loaded");
        var counts = new TreeMap<String, Integer>();
        var tiers = new TreeMap<String, Integer>();
        int repeats = 0;
        int vampire = 0;
        var legends = List.of("witchersbestiary:swamp_lord", "witchersbestiary:grave_devourer", "witchersbestiary:higher_vampire");
        Set<String> previous = Set.of();
        for (int rotation = 0; rotation < 1000; rotation++) {
            long age = rotation * VillageOffers.INTERVAL;
            var board = state.current("minecraft:overworld/test", age, pool);
            var ids = new HashSet<String>();
            for (var offer : board.offers()) {
                test.assertTrue(ids.add(offer.definition()), "No duplicate species on one board");
                String species = offer.definition().substring("witchersbestiary:procedural_".length());
                counts.merge(species, 1, Integer::sum);
                var profile = ProceduralProfile.generate(offer.id().getMostSignificantBits(), species);
                tiers.merge(profile.tier().name(), 1, Integer::sum);
                if (previous.contains(offer.definition())) repeats++;
            }
            previous = ids;
            if (state.current("minecraft:overworld/test/legends", age, legends).offers().size() == 3) vampire++;
        }
        test.assertTrue(counts.keySet().equals(ContractSpecies.ENTITIES.keySet()), "Every species appears");
        test.assertTrue(repeats < 1400, "Rotations avoid recently offered species");
        System.out.println("STAGE22X boards species=" + counts + " tiers=" + tiers
                + " adjacent_repeats=" + repeats + " vampire_boards=" + vampire);
        test.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void merchantSampling(TestContext test) {
        var merchant = ArcaneMerchants.TYPE.create(test.getWorld());
        merchant.village("stage22x-review", merchant.getBlockPos());
        var counts = new TreeMap<String, Integer>();
        var prices = new TreeMap<String, Long>();
        int equalRotations = 0;
        Set<String> previous = Set.of();
        for (int rotation = 0; rotation < 1000; rotation++) {
            var data = new NbtCompound();
            merchant.writeCustomDataToNbt(data);
            data.putLong("MerchantRotation", test.getWorld().getTime());
            merchant.readCustomDataFromNbt(data);
            merchant.tick();
            var ids = new HashSet<String>();
            for (var offer : merchant.offers()) {
                test.assertTrue(ids.add(offer.id) && !offer.result.isEmpty(), "Unique, registered trade");
                var price = prices.putIfAbsent(offer.id, offer.price);
                test.assertTrue(price == null || price == offer.price, "Stable price for " + offer.id);
                counts.merge(offer.id, 1, Integer::sum);
            }
            test.assertTrue(!(ids.contains("witchersbestiary:necrophage_oil")
                    && ids.contains("witchersbestiary:necrophage_elixir")), "No duplicate necrophage category pair");
            if (ids.equals(previous)) equalRotations++;
            previous = ids;
        }
        test.assertTrue(prices.entrySet().containsAll(Map.of(
                "witchersbestiary:necrophage_oil", 52L, "witchersbestiary:necrophage_elixir", 64L,
                "witchersbestiary:cursed_oil", 72L, "witchersbestiary:vampire_elixir", 90L).entrySet()),
                "Seasonal alchemical prices match original prices");
        test.assertTrue(equalRotations < 10, "Stock changes between neighboring rotations");
        System.out.println("STAGE22X merchant items=" + counts + " prices=" + prices
                + " unchanged_rotations=" + equalRotations);
        merchant.discard();
        test.complete();
    }
}
