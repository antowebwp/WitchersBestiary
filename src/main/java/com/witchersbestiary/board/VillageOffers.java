package com.witchersbestiary.board;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;

/** Lazy village rotation, driven by monotonic world age rather than daylight time. */
public final class VillageOffers extends PersistentState {
    public static final long INTERVAL = 72000;
    public record Offer(UUID id, String definition) {}
    public record Generation(UUID id, long created, List<Offer> offers) {
        public Generation { offers = List.copyOf(offers); }
    }
    public static final Type<VillageOffers> TYPE = new Type<>(VillageOffers::new, VillageOffers::read, null);
    private final Map<String, Generation> villages = new HashMap<>();
    private NbtCompound future;
    public static String identity(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        var chunk=world.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4);
        if(chunk!=null){var candidates=new TreeMap<String,Long>();for(var ref:chunk.getStructureReferences().entrySet()){
            if(!VisitedVillageBoards.supported(world,ref.getKey()))continue;
            for(long packed:ref.getValue()){
                // References already contain the durable structure origin: never fetch its chunk.
                var cp=new net.minecraft.util.math.ChunkPos(packed);long dx=(long)pos.getX()-cp.getCenterX(),dz=(long)pos.getZ()-cp.getCenterZ();candidates.put(world.getRegistryKey().getValue()+"/"+world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE).getId(ref.getKey())+"/"+packed,dx*dx+dz*dz);
            }}if(!candidates.isEmpty())return candidates.entrySet().stream().min(Map.Entry.<String,Long>comparingByValue().thenComparing(Map.Entry.comparingByKey())).orElseThrow().getKey();}
        return world.getRegistryKey().getValue()+"/board/"+pos.asLong();
    }
    public boolean live(UUID offer) {return offer!=null&&villages.values().stream().anyMatch(g->g.offers().stream().anyMatch(o->o.id().equals(offer)));}
    public Generation personal(String village, UUID player, long age, List<String> eligible) {
        var base=current(village,age,eligible);var key=village+"/personal/"+player+"/"+base.id();
        return villages.getOrDefault(key,base);
    }
    public Generation refresh(String village, UUID player, long age, List<String> eligible, java.util.function.Predicate<Offer> claimed) {
        var old=personal(village,player,age,eligible);
        if(age-old.created()<24000 || !old.offers().stream().allMatch(claimed))throw new IllegalStateException("Refresh requires five claimed offers and one day cooldown");
        var base=current(village,age,eligible);String prefix=village+"/personal/"+player+"/";
        villages.keySet().removeIf(k->k.startsWith(prefix));
        return current(prefix+base.id(),age,eligible);
    }

    public static VillageOffers get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "witchersbestiary_village_offers");
    }
    public Generation current(String village, long age, List<String> eligible) {
        if (future != null) throw new IllegalStateException("Future village offer schema preserved read-only");
        var old = villages.get(village);
        if (old != null && (age < old.created() || age - old.created() < INTERVAL)) return old;
        var pool = new ArrayList<>(new LinkedHashSet<>(eligible));
        if (pool.size() < 5) throw new IllegalStateException("Five eligible species required");
        Collections.shuffle(pool);
        var offers = new ArrayList<Offer>(5);
        for (int i = 0; i < 5; i++) offers.add(new Offer(UUID.randomUUID(), pool.get(i)));
        var next = new Generation(UUID.randomUUID(), age, offers);
        if(!village.contains("/personal/"))villages.keySet().removeIf(k->k.startsWith(village+"/personal/"));
        villages.put(village, next); markDirty(); return next;
    }
    @Override public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
        if (future != null) return future.copy();
        n.putInt("Schema", 1); var entries = new NbtList();
        villages.forEach((key, generation) -> {
            var entry = new NbtCompound(); entry.putString("Village", key);
            entry.putUuid("Generation", generation.id()); entry.putLong("Created", generation.created());
            var offers = new NbtList();
            generation.offers().forEach(offer -> { var o = new NbtCompound(); o.putUuid("Id", offer.id()); o.putString("Definition", offer.definition()); offers.add(o); });
            entry.put("Offers", offers); entries.add(entry);
        }); n.put("Villages", entries); return n;
    }
    public static VillageOffers read(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
        var state = new VillageOffers();
        if (n.getInt("Schema") != 1) { state.future = n.copy(); return state; }
        try {
        var entries = n.getList("Villages", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            var offers = new ArrayList<Offer>(); var ids = new HashSet<UUID>();
            var list = entry.getList("Offers", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < list.size(); j++) { var o = list.getCompound(j); if (!o.containsUuid("Id") || !ids.add(o.getUuid("Id"))) throw new IllegalStateException("Invalid saved offer UUID"); offers.add(new Offer(o.getUuid("Id"), o.getString("Definition"))); }
            if (offers.size() != 5 || !entry.containsUuid("Generation")) throw new IllegalStateException("Invalid saved offer generation");
            state.villages.put(entry.getString("Village"), new Generation(entry.getUuid("Generation"), entry.getLong("Created"), offers));
        } }catch(RuntimeException invalid){state.villages.clear();state.future=n.copy();}
        return state;
    }
}
