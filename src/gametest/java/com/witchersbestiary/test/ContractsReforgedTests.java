package com.witchersbestiary.test;

import com.witchersbestiary.board.BoardScreenHandler;
import com.witchersbestiary.board.NoticeBoardBlock;
import com.witchersbestiary.board.VillageOffers;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.SwampLord;
import com.witchersbestiary.contracts.hunt.VampireHunt;
import com.witchersbestiary.contracts.procedural.*;
import com.witchersbestiary.contracts.tracking.Investigations;
import com.witchersbestiary.economy.Currency;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ContractsReforgedTests implements FabricGameTest {
    private static List<String> legends() {
        return Contracts.DEFINITIONS.all().keySet().stream().filter(LegendaryHunts::offered)
                .map(Object::toString).sorted().toList();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void uniformLegendRotationAndLegacyNbt(TestContext test) {
        var pool = legends();
        test.assertTrue(pool.size() == 13 && !pool.contains(SwampLord.ID.toString()), "Twelve variants plus vampire");
        var lookup = test.getWorld().getRegistryManager();
        var old = new VillageOffers();
        String key = "fixed-seed-721/legends";
        var oldGeneration = old.current(key, 0, List.of(SwampLord.ID.toString(), "witchersbestiary:grave_devourer"));
        var saved = VillageOffers.read(old.writeNbt(new NbtCompound(), lookup), lookup);
        test.assertTrue(saved.current(key, VillageOffers.INTERVAL - 1, pool).equals(oldGeneration)
                && oldGeneration.offers().stream().allMatch(o -> o.bonus() == null), "Old cycle remains until due");
        var rotated = saved.current(key, VillageOffers.INTERVAL, pool);
        test.assertTrue(rotated.offers().stream().allMatch(o -> pool.contains(o.definition()) && o.bonus() != null),
                "Due rotation replaces old rows with offers containing bonuses");
        test.assertTrue(VillageOffers.read(saved.writeNbt(new NbtCompound(), lookup), lookup)
                .current(key, VillageOffers.INTERVAL, pool).equals(rotated), "Saved offers and stacks are identical");

        var counts = new TreeMap<String, Integer>();
        var simulation = new VillageOffers();
        for (int i = 0; i < 2600; i++) {
            var generation = simulation.current(key, i * VillageOffers.INTERVAL, pool);
            var seen = new HashSet<String>();
            for (var offer : generation.offers()) {
                test.assertTrue(seen.add(offer.definition()), "Distinct species in the same rotation");
                counts.merge(offer.definition(), 1, Integer::sum);
            }
        }
        test.assertTrue(counts.size() == 13 && counts.values().stream().allMatch(n -> n >= 520 && n <= 680),
                "All species near uniform expected count 600: " + counts);
        var vampire=Contracts.DEFINITIONS.find(VampireHunt.ID);
        var repeat=VampireHunt.repeat(vampire);
        test.assertTrue(repeat.reward().items().stream().map(grant -> grant.item().getPath()).toList()
                .equals(List.of("witcher_crown", "vampire_fang")), "Repeat vampire uses existing repeatable payout");
        System.out.println("REFORGED LEGENDS 2600 rotations: " + counts);
        test.complete();
    }

    @GameTest(templateName = "witchersbestiary:village_board_arena")
    public void boardBonusSnapshotAndAtomicClaim(TestContext test) {
        var world = test.getWorld();
        var lookup = world.getRegistryManager();
        var p = test.createMockCreativeServerPlayerInWorld();
        var root = test.getAbsolutePos(new BlockPos(2, 2, 46));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)world.setBlockState(root.add(x,-1,z),Blocks.GRASS_BLOCK.getDefaultState());
        test.assertTrue(NoticeBoardBlock.place(world, root, Direction.NORTH), "Board placed");
        p.refreshPositionAndAngles(root.getX()+.5, root.getY(), root.getZ()-1.5,0,0);
        var board = new BoardScreenHandler(751, p.getInventory(), root);
        p.currentScreenHandler = board;
        board.onButtonClick(p,65);
        var rows = board.view(p,"").getAsJsonArray("offers");
        test.assertTrue(rows.size() == 3, "Real board presents three legend rows");
        int row = 0;
        while (rows.get(row).getAsJsonObject().get("id").getAsString().equals(VampireHunt.ID.toString())) row++;
        var offer = rows.get(row).getAsJsonObject();
        var preview = ItemStack.CODEC.parse(lookup.getOps(JsonOps.INSTANCE), offer.get("bonus")).getOrThrow();
        test.assertTrue(board.onButtonClick(p,row), "Accepted from actual board");
        var store = ContractState.get(world.getServer());
        var accepted = store.ownedBy(p.getUuid()).getFirst();
        test.assertTrue(ItemStack.areEqual(preview, ContractBonus.decode(accepted.bonus(), lookup)),
                "Acceptance snapshots the displayed item");

        var definition = accepted.definition();
        UUID death = UUID.randomUUID();
        var book = java.util.stream.LongStream.range(0, 100).mapToObj(seed -> ContractBonus.roll(definition, seed, lookup))
                .filter(nbt -> ContractBonus.decode(nbt, lookup).isOf(Items.ENCHANTED_BOOK)).findFirst().orElseThrow();
        var payout = new ContractInstance(UUID.randomUUID(), p.getUuid(), definition, System.currentTimeMillis());
        payout.procedural=accepted.procedural;
        payout.setBonus(book);
        payout.transition(ContractInstance.Status.ACCEPTED);
        payout.credit(death, p.getUuid(), definition.target(), System.currentTimeMillis());
        var copy = ContractInstance.fromNbt(payout.toNbt());
        store.add(copy);
        var inventory = p.getInventory();
        for (int i=0;i<inventory.main.size();i++) inventory.main.set(i,new ItemStack(Items.COBBLESTONE,64));
        denied(() -> Contracts.claim(p, copy.id()));
        test.assertTrue(copy.status()==ContractInstance.Status.COMPLETED && inventory.count(Currency.CROWN)==0,
                "Full inventory leaves the full payout waiting");
        inventory.clear();
        Contracts.claim(p,copy.id());
        var expected = ContractBonus.decode(book, lookup);
        test.assertTrue(inventory.main.stream().anyMatch(stack -> ItemStack.areEqual(stack, expected))
                && inventory.count(Currency.CROWN)>0, "Saved enchantments, count and crowns delivered");
        denied(() -> Contracts.claim(p,copy.id()));
        test.assertTrue(inventory.count(Items.ENCHANTED_BOOK)==1 && copy.status()==ContractInstance.Status.REWARDED,
                "Second claim is rejected");
        var legacyNbt=copy.toNbt();
        legacyNbt.remove("Bonus");
        var legacy=ContractInstance.fromNbt(legacyNbt);
        test.assertTrue(legacy.bonus()==null && legacy.status()==ContractInstance.Status.REWARDED
                && legacy.progress()==copy.progress(), "Old-format payment stays rewarded");

        Contracts.cancel(p, accepted.id());
        p.currentScreenHandler=p.playerScreenHandler;
        p.discard();
        test.complete();
    }

    @GameTest(templateName = "witchersbestiary:village_board_arena", tickLimit = 180)
    public void everyNamedSpeciesCanTrackSpawnAndComplete(TestContext test) {
        var world = test.getWorld();
        var origin = test.getAbsolutePos(new BlockPos(144, 2, 128));
        for (int x=0;x<=104;x++) for (int z=-10;z<=10;z++) {
            var at=origin.add(x,0,z);
            world.getChunk(at);
            world.setBlockState(at.down(),Blocks.GRASS_BLOCK.getDefaultState());
            for (int dy=0;dy<13;dy++) world.setBlockState(at.up(dy),Blocks.AIR.getDefaultState());
        }
        var p = test.createMockCreativeServerPlayerInWorld();
        p.addExperienceLevels(30);
        for (var id : legends().stream().map(Identifier::of).filter(LegendaryHunts::variant).toList()) {
            move(p,origin);
            var hunt=Contracts.assignPrepared(p,id,721L,origin);
            test.assertTrue(SwampLord.prepareAcceptance(p,hunt,origin),"Loaded safe route for "+id);
            var route=hunt.investigation;
            test.assertTrue(route.clues.size()==2 && hunt.hunt.target==null && Investigations.view(p).has("area"),
                    "Two clues and tracking area for "+id);
            Investigations.toggle(p,true);
            for(var clue:route.clues){
                move(p,clue.position);
                test.assertTrue(Investigations.view(p).getAsJsonObject("point").get("id").getAsString().equals(clue.id.toString()),
                        "Witcher Senses highlights current clue for "+id);
                test.assertTrue(Investigations.inspect(p,clue.id),"Clue accepted in order for "+id);
            }
            move(p,route.lair.north(2));
            test.assertTrue(Investigations.trySpawn(p,hunt) && !Investigations.trySpawn(p,hunt),"One target for "+id);
            var target=(MobEntity)world.getEntity(hunt.hunt.target);
            var restored=ContractInstance.fromNbt(hunt.toNbt());
            test.assertTrue(target!=null && target.getType()==ContractSpecies.type(hunt.procedural.species())
                    && hunt.procedural.abilities().isEmpty() && restored.procedural.equals(hunt.procedural)
                    && restored.hunt.target.equals(hunt.hunt.target),"Bound species, profile and UUID survive NBT: "+id);
            target.setHealth(0);
            target.onDeath(world.getDamageSources().playerAttack(p));
            test.assertTrue(hunt.status()==ContractInstance.Status.COMPLETED,"Contract kill credited for "+id);
            target.discard();
        }
        p.discard();
        test.complete();
    }

    private static void move(ServerPlayerEntity player, BlockPos at) {
        player.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    }

    private static void denied(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException expected) { return; }
        throw new IllegalStateException("Expected the server to deny the claim");
    }
}
