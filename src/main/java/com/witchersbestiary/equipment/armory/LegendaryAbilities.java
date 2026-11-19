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
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

public final class LegendaryAbilities extends PersistentState {
    public static final int FULL_CHARGE = 8;
    public static final int BLOOD_DURATION = 200, GRAVE_DURATION = 600;
    public static final int BLOOD_COOLDOWN = 600, GRAVE_COOLDOWN = 1800;
    private static final Type<LegendaryAbilities> TYPE =
            new Type<>(LegendaryAbilities::new, LegendaryAbilities::read, null);
    private final Map<UUID, Progress> players = new HashMap<>();

    private static final class Progress {
        int charge;
        long bloodEnd, graveEnd;
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
        BloodLegacyPulse.initialize();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> endBlood(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> endBlood(player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) endBlood(player);
        });
        ServerPlayNetworking.registerGlobalReceiver(Activate.ID, (packet, context) -> activate(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean sync = server.getTicks() % 10 == 0;
            var ledger = server.getOverworld().getPersistentStateManager()
                    .getOrCreate(TYPE, "witchersbestiary_legendary_abilities");
            if (sync) ledger.players.values().forEach(progress -> ledger.updateGrave(progress, server.getOverworld().getTime()));
            for (var player : server.getPlayerManager().getPlayerList()) {
                var aura = (BloodAuraState) player;
                if (aura.bestiary$bloodAura()) {
                    var progress = ledger.players.get(player.getUuid());
                    if (progress.bloodEnd <= player.getServerWorld().getTime()) {
                        aura.bestiary$setBloodAura(false);
                        ledger.sync(player);
                        continue;
                    }
                }
                if (sync) {
                    var stack = player.getMainHandStack();
                    if (stack.isOf(ModItems.BLOOD_LEGACY) || stack.isOf(ModItems.GRAVE_BLADE)) ledger.sync(player);
                }
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
            if (progress.charge < FULL_CHARGE || remaining(progress.bloodEnd, now, BLOOD_COOLDOWN, BLOOD_DURATION) > 0) return false;
            progress.charge = 0;
            progress.bloodEnd = now + BLOOD_DURATION;
            ledger.markDirty();
            BloodLegacyPulse.fire(player);
            if (SignManager.allowed(player) && progress.bloodEnd > player.getServerWorld().getTime())
                ((BloodAuraState) player).bestiary$setBloodAura(true);
        } else {
            ledger.updateGrave(progress, now);
            if (remaining(progress.graveEnd, now, GRAVE_COOLDOWN, GRAVE_DURATION) > 0) return false;
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
        if (progress.bloodEnd > now && ((BloodAuraState) player).bestiary$bloodAura()) {
            player.heal(actualDamage * .2f);
            return;
        } else if (remaining(progress.bloodEnd, now, BLOOD_COOLDOWN, BLOOD_DURATION) == 0 && progress.charge < FULL_CHARGE) {
            ++progress.charge;
            ledger.sync(player);
        } else {
            return;
        }
        ledger.markDirty();
    }

    private static void endBlood(ServerPlayerEntity player) {
        ((BloodAuraState) player).bestiary$setBloodAura(false);
        var ledger = get(player);
        var progress = ledger.players.get(player.getUuid());
        long now = player.getServerWorld().getTime();
        if (progress != null && progress.bloodEnd > now) {
            progress.bloodEnd = now;
            ledger.markDirty();
        }
    }

    private static int remaining(long end, long now, int cooldown, int duration) {
        return end == 0 ? 0 : (int) Math.clamp(end + cooldown - now, 0, duration + cooldown);
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
        if (progress == null) {
            ServerPlayNetworking.send(player, new View(0, 0, 0));
            return;
        }
        int blood = remaining(progress.bloodEnd, now, BLOOD_COOLDOWN, BLOOD_DURATION);
        if (!((BloodAuraState) player).bestiary$bloodAura()) blood = Math.min(BLOOD_COOLDOWN, blood);
        int grave = remaining(progress.graveEnd, now, GRAVE_COOLDOWN, GRAVE_DURATION);
        ServerPlayNetworking.send(player, new View(progress.charge, blood, progress.ghost == null ? -grave : grave));
    }

    @Override public NbtCompound writeNbt(NbtCompound data, RegistryWrapper.WrapperLookup registries) {
        players.forEach((id, progress) -> {
            var entry = new NbtCompound();
            entry.putInt("Charge", progress.charge);
            entry.putLong("BloodEnd", progress.bloodEnd);
            entry.putLong("GraveEnd", progress.graveEnd);
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
            ledger.players.put(id, progress);
        }
        return ledger;
    }
}
