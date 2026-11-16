package com.witchersbestiary.unicorn;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.chunk.ChunkStatus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

final class UnicornWhistle extends Item {
    private static final ChunkTicketType<UUID> LOAD = ChunkTicketType.create(
            "witchersbestiary_unicorn_call", Comparator.<UUID>naturalOrder(), 200);
    private record Request(UUID entity, ServerWorld world, ChunkPos chunk, Hand hand, long end) {}
    private static final Map<MinecraftServer, Map<UUID, Request>> PENDING = new WeakHashMap<>();

    UnicornWhistle() { super(new Settings().maxCount(1)); }

    static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(UnicornWhistle::tick);
    }

    @Override public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
        if (!(target instanceof UnicornEntity unicorn) || !user.isSneaking()) return ActionResult.PASS;
        if (!unicorn.isTame() || !user.getUuid().equals(unicorn.getOwnerUuid())) return ActionResult.FAIL;
        if (user instanceof ServerPlayerEntity player && player.getServer() != null) {
            var data = new NbtCompound();
            data.putUuid("Unicorn", unicorn.getUuid());
            data.putUuid("Owner", player.getUuid());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
            UnicornWhistleState.get(player.getServer()).update(unicorn, true);
            feedback(player, "bound");
        }
        return ActionResult.success(user.getWorld().isClient);
    }

    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user.isSneaking() || !user.isAlive() || user.isSpectator()) return TypedActionResult.pass(stack);
        var id = bound(stack);
        if (id == null) {
            if (user instanceof ServerPlayerEntity player) feedback(player, "unbound");
            return TypedActionResult.fail(stack);
        }
        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld serverWorld))
            return TypedActionResult.success(stack, true);
        if (player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        var binding = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (binding.containsUuid("Owner") && !player.getUuid().equals(binding.getUuid("Owner"))) {
            feedback(player, "owner");
            return TypedActionResult.fail(stack);
        }
        var location = UnicornWhistleState.get(serverWorld.getServer()).find(id);
        if (location == null || !user.getUuid().equals(location.owner())) {
            feedback(player, "unavailable");
            return TypedActionResult.fail(stack);
        }
        if (!world.getRegistryKey().getValue().equals(location.dimension())) {
            feedback(player, "dimension");
            return TypedActionResult.fail(stack);
        }
        if (serverWorld.getEntity(id) instanceof UnicornEntity unicorn)
            return call(player, unicorn) ? TypedActionResult.consume(stack) : TypedActionResult.fail(stack);
        var pending = PENDING.computeIfAbsent(serverWorld.getServer(), ignored -> new HashMap<>());
        if (pending.size() >= 16 || pending.containsKey(player.getUuid())) {
            feedback(player, "pending");
            return TypedActionResult.fail(stack);
        }
        var chunks = serverWorld.getChunkManager();
        chunks.addTicket(LOAD, location.chunk(), 2, player.getUuid());
        // Queue a FULL request through the off-thread entry point, which hands it
        // back to the server executor. Do not await a chunk on the item-use path.
        CompletableFuture.supplyAsync(() -> chunks.getChunkFutureSyncOnMainThread(
                location.chunk().x, location.chunk().z, ChunkStatus.FULL, true)).thenCompose(future -> future);
        pending.put(player.getUuid(), new Request(id, serverWorld, location.chunk(), hand, serverWorld.getTime() + 180));
        return TypedActionResult.consume(stack);
    }

    static void cancelPending(ServerPlayerEntity player, UnicornEntity unicorn) {
        var server = player.getServer();
        var pending = PENDING.get(server);
        if (pending == null) return;
        var request = pending.get(player.getUuid());
        if (request == null || !request.entity.equals(unicorn.getUuid())) return;
        pending.remove(player.getUuid());
        request.world.getChunkManager().removeTicket(LOAD, request.chunk, 2, player.getUuid());
        if (pending.isEmpty()) PENDING.remove(server);
    }

    private static UUID bound(ItemStack stack) {
        if (!(stack.getItem() instanceof UnicornWhistle)) return null;
        var data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return data.containsUuid("Unicorn") ? data.getUuid("Unicorn") : null;
    }

    private static void tick(MinecraftServer server) {
        var pending = PENDING.get(server);
        if (pending == null) return;
        pending.entrySet().removeIf(entry -> {
            var request = entry.getValue();
            var player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null && player.isAlive() && !player.isSpectator()
                    && player.getServerWorld() == request.world
                    && request.entity.equals(bound(player.getStackInHand(request.hand)))) {
                if (request.world.getEntity(request.entity) instanceof UnicornEntity unicorn) {
                    if (request.world.getTime() < request.end) call(player, unicorn);
                    else feedback(player, "unavailable");
                } else if (request.world.getTime() < request.end) {
                    return false;
                } else {
                    feedback(player, "unavailable");
                }
            }
            request.world.getChunkManager().removeTicket(LOAD, request.chunk, 2, entry.getKey());
            return true;
        });
        if (pending.isEmpty()) PENDING.remove(server);
    }

    private static boolean call(ServerPlayerEntity player, UnicornEntity unicorn) {
        if (!unicorn.isAlive() || unicorn.recovering() || unicorn.hasPassengers()
                || !player.getUuid().equals(unicorn.getOwnerUuid())
                || player.getItemCooldownManager().isCoolingDown(Unicorns.WHISTLE)) {
            feedback(player, "busy");
            return false;
        }
        var world = player.getServerWorld();
        var destination = UnicornLanding.find(world, unicorn, player.getBlockPos()).orElse(null);
        if (destination == null || !unicorn.teleport(destination.x, destination.y, destination.z, false)) {
            feedback(player, "landing");
            return false;
        }
        unicorn.onWhistleArrival();
        world.playSound(null, unicorn.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                net.minecraft.sound.SoundCategory.NEUTRAL, .65f, 1.25f);
        world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, unicorn.getX(), unicorn.getBodyY(.55),
                unicorn.getZ(), 12, .42, .45, .42, .015);
        UnicornWhistleState.get(world.getServer()).update(unicorn, true);
        player.getItemCooldownManager().set(Unicorns.WHISTLE, 300);
        feedback(player, "summoned");
        return true;
    }

    private static void feedback(ServerPlayerEntity player, String suffix) {
        player.sendMessage(Text.translatable("item.witchersbestiary.unicorn_whistle." + suffix), true);
    }
}
