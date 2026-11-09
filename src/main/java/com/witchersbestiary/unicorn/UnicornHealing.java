package com.witchersbestiary.unicorn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

final class UnicornHealing extends PersistentState {
    private static final Type<UnicornHealing> TYPE = new Type<>(UnicornHealing::new, UnicornHealing::read, null);
    private final Map<UUID, Long> until = new HashMap<>();

    static UnicornHealing get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "witchersbestiary_unicorn_healing");
    }

    boolean reserve(UUID owner, long now) {
        if (until.getOrDefault(owner, 0L) > now) return false;
        until.values().removeIf(end -> end <= now);
        until.put(owner, now + 80);
        markDirty();
        return true;
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        until.forEach((owner, end) -> nbt.putLong(owner.toString(), end));
        return nbt;
    }

    private static UnicornHealing read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        var state = new UnicornHealing();
        for (String key : nbt.getKeys()) {
            try { state.until.put(UUID.fromString(key), nbt.getLong(key)); }
            catch (IllegalArgumentException ignored) { }
        }
        return state;
    }
}
