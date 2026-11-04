package com.witchersbestiary.contracts;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;

/** One world-wide store in the overworld, shared across dimensions. */
public final class ContractState extends PersistentState {
    public static final int SCHEMA = 1;
    public static final Type<ContractState> TYPE = new Type<>(ContractState::new, ContractState::read, null);
    private final Map<UUID, ContractInstance> contracts = new LinkedHashMap<>();
    private final Map<UUID, List<ContractInstance>> owners = new HashMap<>();
    private final Map<UUID,UUID> tracking=new HashMap<>();
    private final List<NbtCompound> quarantined = new ArrayList<>();
    private NbtCompound futureSchema;
    public static ContractState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "witchersbestiary_contracts");
    }
    public Collection<ContractInstance> all() { return Collections.unmodifiableCollection(contracts.values()); }
    public List<ContractInstance> ownedBy(UUID player) { return Collections.unmodifiableList(owners.getOrDefault(player, List.of())); }
    public ContractInstance find(UUID id) { return contracts.get(id); }
    public UUID tracked(UUID owner){return tracking.get(owner);}
    public void track(UUID owner,UUID instance){writable();if(instance==null){if(tracking.remove(owner)!=null)markDirty();return;}var c=find(instance);if(c==null||!c.owner().equals(owner)||c.status()!=ContractInstance.Status.ACCEPTED&&c.status()!=ContractInstance.Status.COMPLETED)throw new IllegalArgumentException("Invalid tracking owner/status");if(!instance.equals(tracking.put(owner,instance)))markDirty();}
    public boolean readOnly() { return futureSchema != null; }
    public void writable() { if (futureSchema != null) throw new IllegalStateException("Unsupported future contract schema; state preserved read-only"); }
    public void add(ContractInstance c) {
        writable(); if (contracts.putIfAbsent(c.id(), c) != null) throw new IllegalArgumentException("Duplicate instance id"); owners.computeIfAbsent(c.owner(), key -> new ArrayList<>()).add(c); markDirty();
    }
    /** Keep live work, current-cycle receipts and permanent legendary receipts; bound old terminal detail. */
    public void compact(java.util.function.Predicate<UUID> liveOffer) {
        writable();boolean changed=false;
        for(var list:owners.values()) {int retained=0;var iterator=list.listIterator(list.size());while(iterator.hasPrevious()){
            var c=iterator.previous();boolean terminal=c.status()==ContractInstance.Status.REWARDED||c.status()==ContractInstance.Status.CANCELLED||c.status()==ContractInstance.Status.EXPIRED;
            boolean legendary=c.definition().id().equals(com.witchersbestiary.contracts.hunt.SwampLord.ID)||c.definition().id().equals(com.witchersbestiary.contracts.hunt.GraveDevourer.ID);
            if(!terminal||legendary||liveOffer.test(c.offer))continue;
            if(retained++>=128){iterator.remove();contracts.remove(c.id());changed=true;}
        }}if(changed)markDirty();
    }
    @Override public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
        if (futureSchema != null) return futureSchema.copy();
        n.putInt("Schema", SCHEMA); var list = new NbtList();
        contracts.values().forEach(c -> list.add(c.toNbt())); quarantined.forEach(c -> list.add(c.copy())); n.put("Contracts", list);var selected=new NbtList();tracking.forEach((owner,id)->{var item=new NbtCompound();item.putUuid("Owner",owner);item.putUuid("Contract",id);selected.add(item);});n.put("Tracking",selected);return n;
    }
    public static ContractState read(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
        var state = new ContractState();
        if (n.getInt("Schema") != SCHEMA) {
            state.futureSchema = n.copy();
            com.witchersbestiary.WitchersBestiary.LOGGER.error("Unsupported contract schema {}; preserving state read-only", n.getInt("Schema")); return state;
        }
        var list = n.getList("Contracts", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            try { var c = ContractInstance.fromNbt(entry); if (state.contracts.putIfAbsent(c.id(), c) != null) throw new IllegalArgumentException("Duplicate instance"); state.owners.computeIfAbsent(c.owner(), key -> new ArrayList<>()).add(c); }
            catch (RuntimeException e) { state.quarantined.add(entry.copy()); com.witchersbestiary.WitchersBestiary.LOGGER.error("Quarantined unreadable contract record: {}", e.getMessage()); }
        }
        var selected=n.getList("Tracking",NbtElement.COMPOUND_TYPE);for(int i=0;i<selected.size();i++){var item=selected.getCompound(i);if(!item.containsUuid("Owner")||!item.containsUuid("Contract"))continue;var c=state.find(item.getUuid("Contract"));if(c!=null&&c.owner().equals(item.getUuid("Owner"))&&(c.status()==ContractInstance.Status.ACCEPTED||c.status()==ContractInstance.Status.COMPLETED))state.tracking.put(c.owner(),c.id());}
        return state;
    }
}
