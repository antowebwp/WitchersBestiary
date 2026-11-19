package com.witchersbestiary.equipment.armory;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.WraithEntity;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import org.joml.Vector3f;

public final class LegendaryAbilities extends PersistentState {
    public static final int FULL_CHARGE = 8;
    public static final int BLOOD_DURATION = 200, GRAVE_DURATION = 600, COOLDOWN = 1800;
    private static final Type<LegendaryAbilities> TYPE =
            new Type<>(LegendaryAbilities::new, LegendaryAbilities::read, null);
    private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(.8f, .04f, .08f), .8f);
    private final Map<UUID, Progress> players = new HashMap<>();

    private static final class Progress {
        int charge;
        long bloodEnd, graveEnd;
        float healed;
        WraithEntity ghost;
    }

    public record Activate() implements CustomPayload {
        public static final Id<Activate> ID = new Id<>(Identifier.of("witchersbestiary", "legendary_activate"));
        public static final Activate INSTANCE = new Activate();
        public static final PacketCodec<RegistryByteBuf, Activate> CODEC = PacketCodec.unit(INSTANCE);
        @Override public Id<Activate> getId() { return ID; }
    }

    public record View(int charge, int blood, int grave) implements CustomPayload {
        public static final Id<View> ID = new Id<>(Identifier.of("witchersbestiary", "legendary_view"));
        public static final PacketCodec<RegistryByteBuf, View> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, View::charge, PacketCodecs.VAR_INT, View::blood,
                PacketCodecs.VAR_INT, View::grave, View::new);
        @Override public Id<View> getId() { return ID; }
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(Activate.ID, Activate.CODEC);
        PayloadTypeRegistry.playS2C().register(View.ID, View.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Activate.ID, (packet, context) -> activate(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            var ledger = server.getOverworld().getPersistentStateManager()
                    .getOrCreate(TYPE, "witchersbestiary_legendary_abilities");
            ledger.players.values().forEach(progress -> ledger.updateGrave(progress, server.getOverworld().getTime()));
            for (var player : server.getPlayerManager().getPlayerList()) {
                var stack = player.getMainHandStack();
                if (stack.isOf(ModItems.BLOOD_LEGACY) || stack.isOf(ModItems.GRAVE_BLADE)) ledger.sync(player);
            }
        });
    }

    private static LegendaryAbilities get(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getPersistentStateManager()
                .getOrCreate(TYPE, "witchersbestiary_legendary_abilities");
    }

    public static boolean activate(ServerPlayerEntity player) {
        if (!SignManager.allowed(player)) return false;
        var stack = player.getMainHandStack();
        if (!stack.isOf(ModItems.BLOOD_LEGACY) && !stack.isOf(ModItems.GRAVE_BLADE)) return false;
        var ledger = get(player);
        var progress = ledger.players.computeIfAbsent(player.getUuid(), id -> new Progress());
        long now = player.getServerWorld().getTime();
        if (stack.isOf(ModItems.BLOOD_LEGACY)) {
            if (progress.charge < FULL_CHARGE || remaining(progress.bloodEnd, now) > 0) return false;
            progress.charge = 0;
            progress.healed = 0;
            progress.bloodEnd = now + BLOOD_DURATION;
            bloodParticles(player);
        } else {
            ledger.updateGrave(progress, now);
            if (remaining(progress.graveEnd, now) > 0) return false;
            progress.ghost = summon(player, now + GRAVE_DURATION);
            if (progress.ghost == null) return false;
            progress.graveEnd = now + GRAVE_DURATION;
        }
        ledger.markDirty();
        ledger.sync(player);
        return true;
    }

    private static WraithEntity summon(ServerPlayerEntity player, long expiry) {
        var world = player.getServerWorld();
        var ghost = ModEntities.WRAITH.create(world);
        if (ghost == null) return null;
        Vec3d forward = Vec3d.fromPolar(0, player.getYaw());
        for (Vec3d direction : new Vec3d[]{forward, forward.negate(),
                forward.rotateY((float) (Math.PI / 2)), forward.rotateY((float) (-Math.PI / 2))}) {
            var at = player.getPos().add(direction.multiply(1.6)).add(0, .25, 0);
            ghost.refreshPositionAndAngles(at.x, at.y, at.z, player.getYaw(), 0);
            var box = ghost.getBoundingBox();
            if (box.maxY >= world.getTopY() || box.minY < world.getBottomY()
                    || !world.isChunkLoaded(BlockPos.ofFloored(at)) || !world.getWorldBorder().contains(box)
                    || !world.isSpaceEmpty(ghost, box) || world.containsFluid(box)) continue;
            ghost.bindSummon(player, expiry);
            return world.spawnEntity(ghost) ? ghost : null;
        }
        return null;
    }

    public static void bloodHit(ServerPlayerEntity player, LivingEntity target, float actualDamage) {
        if (actualDamage <= 0 || !SignManager.allowed(player)
                || player.isTeammate(target) || target.isTeammate(player)) return;
        var ledger = get(player);
        var progress = ledger.players.computeIfAbsent(player.getUuid(), id -> new Progress());
        long now = player.getServerWorld().getTime();
        if (progress.bloodEnd > now) {
            float healing = Math.min(8 - progress.healed, Math.min(2, actualDamage * .2f));
            float before = player.getHealth();
            if (healing <= 0 || before >= player.getMaxHealth()) return;
            player.heal(healing);
            progress.healed += Math.max(0, player.getHealth() - before);
            bloodParticles(player);
        } else if (remaining(progress.bloodEnd, now) == 0 && progress.charge < FULL_CHARGE) {
            ++progress.charge;
            ledger.sync(player);
        } else {
            return;
        }
        ledger.markDirty();
    }

    private static void bloodParticles(ServerPlayerEntity player) {
        player.getServerWorld().spawnParticles(BLOOD, player.getX(), player.getBodyY(.65),
                player.getZ(), 7, .3, .25, .3, .01);
    }

    private static int remaining(long end, long now) {
        return end == 0 ? 0 : (int) Math.clamp(end + COOLDOWN - now, 0, GRAVE_DURATION + COOLDOWN);
    }

    private void updateGrave(Progress progress, long now) {
        if (progress.ghost == null || progress.ghost.isAlive() && progress.graveEnd > now) return;
        if (progress.ghost.isAlive()) progress.ghost.discard();
        progress.ghost = null;
        if (progress.graveEnd > now) {
            progress.graveEnd = now;
            markDirty();
        }
    }

    private void sync(ServerPlayerEntity player) {
        var progress = players.get(player.getUuid());
        long now = player.getServerWorld().getTime();
        if (progress != null) updateGrave(progress, now);
        if (!ServerPlayNetworking.canSend(player, View.ID)) return;
        ServerPlayNetworking.send(player, progress == null ? new View(0, 0, 0)
                : new View(progress.charge, remaining(progress.bloodEnd, now),
                        (progress.ghost == null ? -1 : 1) * remaining(progress.graveEnd, now)));
    }

    @Override public NbtCompound writeNbt(NbtCompound data, RegistryWrapper.WrapperLookup registries) {
        players.forEach((id, progress) -> {
            var entry = new NbtCompound();
            entry.putInt("Charge", progress.charge);
            entry.putLong("BloodEnd", progress.bloodEnd);
            entry.putLong("GraveEnd", progress.graveEnd);
            entry.putFloat("Healed", progress.healed);
            data.put(id.toString(), entry);
        });
        return data;
    }

    private static LegendaryAbilities read(NbtCompound data, RegistryWrapper.WrapperLookup registries) {
        var ledger = new LegendaryAbilities();
        for (String key : data.getKeys()) {
            UUID id;
            try { id = UUID.fromString(key); }
            catch (IllegalArgumentException ignored) { continue; }
            var entry = data.getCompound(key);
            var progress = new Progress();
            progress.charge = Math.clamp(entry.getInt("Charge"), 0, FULL_CHARGE);
            progress.bloodEnd = entry.getLong("BloodEnd");
            progress.graveEnd = entry.getLong("GraveEnd");
            progress.healed = Math.clamp(entry.getFloat("Healed"), 0, 8);
            ledger.players.put(id, progress);
        }
        return ledger;
    }
}
