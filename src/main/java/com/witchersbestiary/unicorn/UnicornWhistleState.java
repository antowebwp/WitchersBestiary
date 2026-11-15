package com.witchersbestiary.unicorn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

final class UnicornWhistleState extends PersistentState {
    private static final Type<UnicornWhistleState> TYPE =
            new Type<>(UnicornWhistleState::new, UnicornWhistleState::read, null);
    record Location(UUID owner, Identifier dimension, ChunkPos chunk) {}
    private final Map<UUID, Location> positions = new HashMap<>();

    static UnicornWhistleState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "witchersbestiary_bound_unicorns");
    }

    Location find(UUID unicorn) { return positions.get(unicorn); }

    void update(UnicornEntity unicorn, boolean bind) {
        var owner = unicorn.getOwnerUuid();
        if (owner == null || !bind && !positions.containsKey(unicorn.getUuid())) return;
        var location = new Location(owner, unicorn.getWorld().getRegistryKey().getValue(), unicorn.getChunkPos());
        if (!location.equals(positions.put(unicorn.getUuid(), location))) markDirty();
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        positions.forEach((uuid, location) -> {
            var entry = new NbtCompound();
            entry.putUuid("Owner", location.owner);
            entry.putString("Dimension", location.dimension.toString());
            entry.putLong("Chunk", location.chunk.toLong());
            nbt.put(uuid.toString(), entry);
        });
        return nbt;
    }

    private static UnicornWhistleState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        var state = new UnicornWhistleState();
        for (String key : nbt.getKeys()) {
            var entry = nbt.getCompound(key);
            var dimension = Identifier.tryParse(entry.getString("Dimension"));
            if (dimension == null || !entry.containsUuid("Owner")) continue;
            try {
                state.positions.put(UUID.fromString(key), new Location(
                        entry.getUuid("Owner"), dimension, new ChunkPos(entry.getLong("Chunk"))));
            } catch (IllegalArgumentException ignored) { }
        }
        return state;
    }
}
