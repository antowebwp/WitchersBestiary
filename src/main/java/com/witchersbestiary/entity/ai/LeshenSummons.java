package com.witchersbestiary.entity.ai;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.LeshenEntity;
import com.witchersbestiary.entity.monster.LeshenWolfEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LeshenSummons {
    public static final int MAX_WOLVES = 4;
    private final Map<UUID, Long> leases = new HashMap<>();
    private static final int[][] OFFSETS = {{2,0},{-2,0},{0,2},{0,-2},{2,2},{-2,-2},{2,-2},{-2,2}};

    public void tick(ServerWorld world) {
        leases.entrySet().removeIf(entry -> {
            Entity entity = world.getEntity(entry.getKey());
            return world.getTime() >= entry.getValue() || (entity != null && !entity.isAlive());
        });
    }
    public int size() { return leases.size(); }

    public void summon(ServerWorld world, LeshenEntity owner, int requested) {
        tick(world);
        int remaining = Math.min(requested, MAX_WOLVES - size());
        for (int[] offset : OFFSETS) {
            if (remaining <= 0) break;
            BlockPos pos = owner.getBlockPos().add(offset[0], 0, offset[1]);
            int descent = 0;
            while (descent++ < 4 && world.getBlockState(pos.down()).isAir()) pos = pos.down();
            if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) continue;
            LeshenWolfEntity wolf = ModEntities.LESHEN_WOLF.create(world);
            if (wolf == null) continue;
            wolf.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, owner.getYaw(), 0);
            if (!world.isSpaceEmpty(wolf)) continue;
            wolf.bindTo(owner);
            wolf.setTarget(owner.getTarget());
            if (world.spawnEntity(wolf)) {
                leases.put(wolf.getUuid(), world.getTime() + LeshenWolfEntity.LIFETIME);
                remaining--;
            }
        }
    }

    public void clear(ServerWorld world) {
        for (UUID id : leases.keySet()) if (world.getEntity(id) instanceof LeshenWolfEntity wolf) wolf.discard();
        leases.clear();
    }

    public void writeNbt(NbtCompound nbt) {
        NbtList entries = new NbtList();
        leases.forEach((id, expiry) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Id", id);
            entry.putLong("Expiry", expiry);
            entries.add(entry);
        });
        nbt.put("LeshenSummons", entries);
    }
    public void readNbt(NbtCompound nbt) {
        leases.clear();
        NbtList entries = nbt.getList("LeshenSummons", 10);
        for (int i = 0; i < Math.min(entries.size(), MAX_WOLVES); i++) {
            NbtCompound entry = entries.getCompound(i);
            if (entry.containsUuid("Id")) leases.put(entry.getUuid("Id"), entry.getLong("Expiry"));
        }
    }
}
