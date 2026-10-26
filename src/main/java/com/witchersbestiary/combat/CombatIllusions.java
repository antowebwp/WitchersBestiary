package com.witchersbestiary.combat;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import java.util.*;

/** Bounded persisted slots, reclaimed on death or absolute expiry even across unloaded chunks. */
public final class CombatIllusions {
    public static final int LIFETIME = 400;
    private final int capacity;
    public CombatIllusions(){this(2);}
    public CombatIllusions(int capacity){this.capacity=Math.clamp(capacity,1,4);}
    public void clear(ServerWorld world){for(UUID id:slots.keySet()){var e=world.getEntity(id);if(e!=null)e.discard();}slots.clear();}
    private final Map<UUID, Long> slots = new HashMap<>();
    public int available(ServerWorld world) {
        slots.entrySet().removeIf(e -> {
            Entity entity=world.getEntity(e.getKey());
            return world.getTime()>=e.getValue() || entity!=null && !entity.isAlive();
        });
        return Math.max(0,capacity-slots.size());
    }
    public void add(Entity entity, long expiry) { slots.put(entity.getUuid(),expiry); }
    public void save(NbtCompound nbt) {
        NbtList list=new NbtList();
        slots.forEach((id,end)->{NbtCompound slot=new NbtCompound();slot.putUuid("Id",id);slot.putLong("Expiry",end);list.add(slot);});
        nbt.put("ActiveIllusions",list);
    }
    public void load(NbtCompound nbt) {
        slots.clear();NbtList list=nbt.getList("ActiveIllusions",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<Math.min(capacity,list.size());i++) {var slot=list.getCompound(i);if(slot.containsUuid("Id"))slots.put(slot.getUuid("Id"),slot.getLong("Expiry"));}
    }
}
