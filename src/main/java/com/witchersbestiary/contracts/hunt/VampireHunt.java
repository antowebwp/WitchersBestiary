package com.witchersbestiary.contracts.hunt;

import com.witchersbestiary.contracts.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.HigherVampireEntity;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.List;

public final class VampireHunt extends Item {
    public static final Identifier ID = Identifier.of("witchersbestiary", "higher_vampire");
    private static final int CHANNEL = 100;
    private static final int REPLAY_COOLDOWN = 12000;
    private record Channel(HigherVampireEntity boss, ContractInstance contract, ItemStack stack) {}
    private final java.util.Map<ServerPlayerEntity, Channel> channels = new java.util.WeakHashMap<>();
    private static final int EMPTY = 0, SEALED = 1, REPLAY_ACTIVE = 2;

    public VampireHunt() { super(new Settings().maxCount(1).rarity(Rarity.EPIC)); }

    public static ContractInstance validate(ServerPlayerEntity player, Identifier id, ContractState store) {
        if (!ID.equals(id)) return null;
        var hunts = store.ownedBy(player.getUuid()).stream().filter(c -> ID.equals(c.definition().id())).toList();
        if (hunts.stream().anyMatch(c -> c.status() == ContractInstance.Status.ACCEPTED
                || c.status() == ContractInstance.Status.COMPLETED))
            throw new IllegalStateException("vampire_active");
        var original = claimedOriginal(player, store);
        if (original == null) {
            if (player.getInventory().getEmptySlot() < 0) throw new IllegalStateException("inventory_full");
            return null;
        }
        if (original.vampire != null) throw new IllegalStateException("vampire_active");
        var inventory = player.getInventory();
        if (java.util.stream.Stream.concat(inventory.main.stream(), inventory.offHand.stream())
                .noneMatch(stack -> stack.isOf(ModItems.CRIMSON_PHYLACTERY) && isSealed(stack) && contract(player, stack) == original))
            throw new IllegalStateException("vampire_phylactery");
        return original;
    }

    public static void confirmLegacyPayment(ContractState store, ContractInstance hunt) {
        store.writable();
        if (hunt == null || !ID.equals(hunt.definition().id()) || isRepeat(hunt)
                || hunt.replayGeneration < 1 || hunt.vampire != null)
            throw new IllegalStateException("A sealed original vampire contract is required");
        if (hunt.status() == ContractInstance.Status.REWARDED) return;
        if (hunt.status() != ContractInstance.Status.COMPLETED)
            throw new IllegalStateException("The original hunt must be completed");
        hunt.transition(ContractInstance.Status.REWARDED);
        if (hunt.id().equals(store.tracked(hunt.owner()))) store.track(hunt.owner(), null);
        store.markDirty();
    }

    public static ContractInstance claimedOriginal(ServerPlayerEntity player, ContractState store) {
        return store.ownedBy(player.getUuid()).stream()
                .filter(c -> ID.equals(c.definition().id()) && c.status() == ContractInstance.Status.REWARDED
                        && !isRepeat(c))
                .findFirst().orElse(null);
    }

    private static boolean isRepeat(ContractInstance c) {
        return "hunt.witchersbestiary.higher_vampire.repeat".equals(c.definition().description());
    }

    public static ContractDefinition repeat(ContractDefinition d) {
        return new ContractDefinition(d.id(), d.name(), "hunt.witchersbestiary.higher_vampire.repeat",
                d.target(), 1, d.difficulty(), new ContractDefinition.Reward(50, List.of(
                new ContractDefinition.ItemReward(Identifier.of("witchersbestiary:witcher_crown"), 180),
                new ContractDefinition.ItemReward(Identifier.of("witchersbestiary:vampire_fang"), 2))),
                d.minimumLevel(), d.dimension(), d.durationMillis());
    }

    public static boolean place(ServerPlayerEntity player, ContractInstance contract) {
        if (contract.hunt == null || contract.vampire != null || !first(contract)
                || !contract.hunt.dimension.equals(player.getServerWorld().getRegistryKey().getValue().toString())
                || contract.investigation != null && contract.investigation.next() != null) return false;
        boolean ready = prepare(player, contract, contract.hunt.origin);
        if (ready) com.witchersbestiary.contracts.tracking.Investigations.accepted(player, contract);
        else {
            contract.hunt.status = SwampHunt.Status.WAITING;
            if (contract.investigation == null)
                com.witchersbestiary.contracts.tracking.Investigations.accepted(player, contract);
        }
        ContractState.get(player.getServer()).markDirty();
        return ready;
    }

    public static boolean prepare(ServerPlayerEntity player, ContractInstance c, BlockPos board) {
        var world=player.getServerWorld();
        var tombs=net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.STRUCTURE,
                com.witchersbestiary.world.graveyard.VampireTomb.ID);
        BlockPos located=world.locateStructure(tombs,board,36,false);
        if(located==null)return false;
        var chunk=world.getChunk(located.getX()>>4,located.getZ()>>4);
        var chamber=chunk.getStructureStarts().values().stream().flatMap(start->start.getChildren().stream())
                .filter(com.witchersbestiary.world.graveyard.VampireTomb.Chamber.class::isInstance)
                .map(com.witchersbestiary.world.graveyard.VampireTomb.Chamber.class::cast).findFirst().orElse(null);
        if(chamber==null)return false;
        var entry=chamber.entry();
        c.hunt=new SwampHunt(world.getRegistryKey().getValue().toString(),board);
        c.vampireArena=chamber.arena();
        // Only the immediate approach is loaded for the existing two-clue route planner.
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) world.getChunk((entry.getX()>>4)+dx,(entry.getZ()>>4)+dz);
        return com.witchersbestiary.contracts.tracking.Investigations.prepare(player,c,entry.north(3));
    }

    public static boolean awaken(ServerPlayerEntity owner, ContractInstance c) {
        var world=owner.getServerWorld();var at=c.vampireArena;
        if(at==null||c.hunt==null||c.vampire!=null||!first(c)||!c.owner().equals(owner.getUuid())
                ||!c.hunt.dimension.equals(world.getRegistryKey().getValue().toString())
                ||!owner.isAlive()||owner.isSpectator()||owner.squaredDistanceTo(at.toCenterPos())>12*12
                ||!world.isChunkLoaded(at))return false;
        var center=Vec3d.ofBottomCenter(at);
        var box=net.minecraft.entity.EntityDimensions.changing(1.32f,3.2f).getBoxAt(center);
        if(!world.isSpaceEmpty(box)||world.containsFluid(box)||!world.getOtherEntities(null,box).isEmpty())return false;
        var boss=ModEntities.HIGHER_VAMPIRE.create(world);
        if(boss==null)return false;
        boss.refreshPositionAndAngles(center.x,center.y,center.z,180,0);
        boss.markHuntBinding();boss.setTarget(owner);
        if(!world.spawnEntity(boss))return false;
        c.vampire=boss.getUuid();c.hunt.target=c.vampire;c.hunt.position=at;c.hunt.status=SwampHunt.Status.ACTIVE;
        com.witchersbestiary.contracts.tracking.Investigations.targetSpawned(c,at);
        ContractState.get(owner.getServer()).markDirty();
        return true;
    }

    public static void issue(ServerPlayerEntity player, ContractInstance contract) {
        if (!ID.equals(contract.definition().id())) return;
        var stack = new ItemStack(ModItems.CRIMSON_PHYLACTERY);
        save(stack, contract, EMPTY);
        if(contract.investigation!=null)com.witchersbestiary.contracts.tracking.Investigations.accepted(player,contract);
        if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
    }

    private static void save(ItemStack stack, ContractInstance contract, int state) {
        var nbt = new NbtCompound();
        nbt.putUuid("Contract", contract.id());
        nbt.putInt("State", state);
        nbt.putInt("Generation", contract.replayGeneration);
        if (contract.vampire != null) nbt.putUuid("Boss", contract.vampire);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static boolean isSealed(ItemStack stack) {
        return data(stack).getInt("State") == SEALED;
    }

    private static NbtCompound data(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    private static ContractInstance contract(ServerPlayerEntity player, ItemStack stack) {
        var nbt = data(stack);
        if (!nbt.containsUuid("Contract")) return null;
        var store = ContractState.get(player.getServer());
        if (store.readOnly()) return null;
        var c = store.find(nbt.getUuid("Contract"));
        return c != null && ID.equals(c.definition().id()) && c.owner().equals(player.getUuid())
                && c.replayGeneration == nbt.getInt("Generation") ? c : null;
    }

    // Operator fixture until the tomb has its own encounter activation.
    public static boolean bind(ServerPlayerEntity owner, HigherVampireEntity boss) {
        var store = ContractState.get(owner.getServer());
        if (store.readOnly() || boss.isDefeated() || boss.isHybrid() || boss.getWorld()!=owner.getWorld()) return false;
        var c = store.ownedBy(owner.getUuid()).stream().filter(h -> ID.equals(h.definition().id())
                && h.status()==ContractInstance.Status.ACCEPTED && h.vampire==null).findFirst().orElse(null);
        if (c==null || isRepeat(c) || c.vampireArena!=null || boss.hasHuntBinding()) return false;
        c.vampire=boss.getUuid();
        boss.markHuntBinding();
        store.markDirty();
        return true;
    }

    private static boolean first(ContractInstance c) { return c.status()==ContractInstance.Status.ACCEPTED; }

    private static HigherVampireEntity aimed(ServerPlayerEntity player) {
        var eye = player.getEyePos();
        var end = eye.add(player.getRotationVec(1).multiply(12));
        HigherVampireEntity nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var boss : player.getServerWorld().getEntitiesByClass(HigherVampireEntity.class,
                new Box(eye, end).expand(1), HigherVampireEntity::isAlive)) {
            var box = boss.getBoundingBox().expand(.18);
            var hit = box.contains(eye) ? eye : box.raycast(eye, end).orElse(null);
            if (hit != null && eye.squaredDistanceTo(hit) < distance) {
                distance = eye.squaredDistanceTo(hit);
                nearest = boss;
            }
        }
        return nearest;
    }

    private static void feedback(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery." + key), true);
    }

    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user.isSpectator() || !user.isAlive()) return TypedActionResult.fail(stack);
        var binding = data(stack);
        if (binding.getInt("State") == SEALED) {
            if (!binding.containsUuid("Contract")) {
                if (user instanceof ServerPlayerEntity player) feedback(player, "unsuitable");
                return TypedActionResult.fail(stack);
            }
            if (world.isClient) return TypedActionResult.success(stack);
            return user instanceof ServerPlayerEntity player && summon(player,stack)
                    ? TypedActionResult.consume(stack) : TypedActionResult.fail(stack);
        }
        if (user instanceof ServerPlayerEntity player) {
            var boss = aimed(player);
            if (boss == null || !boss.canBeSealed()) {
                feedback(player, "not_ready");
                return TypedActionResult.fail(stack);
            }
            if (player.squaredDistanceTo(boss) > 25 || !player.canSee(boss)) {
                feedback(player, "range");
                return TypedActionResult.fail(stack);
            }
            var c = contract(player, stack);
            if (boss.hasHuntBinding() && c == null && binding.isEmpty()) {
                var store = ContractState.get(player.getServer());
                if (!store.readOnly()) {
                    var linked = store.all().stream().filter(hunt -> boss.getUuid().equals(hunt.vampire)).findFirst().orElse(null);
                    if (linked != null && linked.owner().equals(player.getUuid()) && first(linked)) c = linked;
                }
            }
            boolean suitable = c != null
                    ? boss.getUuid().equals(c.vampire) && maySeal(c, stack)
                    : !boss.hasHuntBinding() && binding.isEmpty();
            if (!suitable) {
                feedback(player, "unsuitable");
                return TypedActionResult.fail(stack);
            }
            channels.put(player, new Channel(boss, c, stack));
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    public static boolean needsFreshPress(ItemStack stack) {
        return stack.isOf(ModItems.CRIMSON_PHYLACTERY) && isSealed(stack);
    }

    private static boolean maySeal(ContractInstance c, ItemStack stack) {
        return first(c) && data(stack).getInt("State")==EMPTY
                || c.status()==ContractInstance.Status.REWARDED && data(stack).getInt("State")==REPLAY_ACTIVE;
    }

    @Override public int getMaxUseTime(ItemStack stack, LivingEntity user) { return CHANNEL+1; }
    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.BLOCK; }

    @Override public void usageTick(World world, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld server)) return;
        var channel = channels.get(player);
        if (channel == null || stack != channel.stack() || player.getStackInHand(player.getActiveHand()) != stack
                || !player.isAlive() || !channel.boss().isAlive() || !channel.boss().canBeSealed()
                || channel.boss().getWorld() != world || player.squaredDistanceTo(channel.boss()) > 25
                || !player.canSee(channel.boss()) || aimed(player) != channel.boss()) {
            if (channels.remove(player) != null) feedback(player, "interrupted");
            player.stopUsingItem();
            return;
        }
        var boss = channel.boss();
        int elapsed=CHANNEL+1-remaining;
        if (elapsed%5==0) {
            player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.progress", elapsed),true);
            server.spawnParticles(ParticleTypes.CRIMSON_SPORE,boss.getX(),boss.getBodyY(.5),boss.getZ(),5,.5,.35,.5,.04);
        }
        if (elapsed>=CHANNEL) {
            channels.remove(player);
            seal(player, stack, channel.contract(), boss);
            player.stopUsingItem();
        }
    }

    @Override public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remaining) {
        if (user instanceof ServerPlayerEntity player && channels.remove(player) != null)
            feedback(player, "interrupted");
    }

    private static void seal(ServerPlayerEntity player, ItemStack stack, ContractInstance c, HigherVampireEntity boss) {
        if (c == null) {
            var sealed = new NbtCompound();
            sealed.putInt("State", SEALED);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(sealed));
            boss.discard();
            player.getInventory().markDirty();
            feedback(player, "success");
            return;
        }
        if (!boss.getUuid().equals(c.vampire) || !maySeal(c, stack)) {
            feedback(player, "interrupted");
            return;
        }
        var store=ContractState.get(player.getServer());
        boolean initial=first(c);
        if (initial && !c.credit(boss.getUuid(),player.getUuid(),ID,System.currentTimeMillis())) {
            feedback(player, "interrupted");
            return;
        }
        if(initial&&c.hunt!=null){
            c.hunt.status=SwampHunt.Status.DEAD;
            c.investigation.stage=com.witchersbestiary.contracts.tracking.ContractInvestigation.Stage.COMPLETED;
        }
        c.vampire=null;
        c.replayGeneration++;
        c.vampireReplayReady = player.getServer().getOverworld().getTime() + REPLAY_COOLDOWN;
        save(stack,c,SEALED);
        store.markDirty();
        boss.discard();
        player.getInventory().markDirty();
        feedback(player, "success");
        var payout = initial ? c : store.ownedBy(player.getUuid()).stream()
                .filter(hunt -> hunt.status() == ContractInstance.Status.ACCEPTED
                        && ID.equals(hunt.definition().id())
                        && isRepeat(hunt))
                .filter(hunt -> hunt.credit(boss.getUuid(), player.getUuid(), ID, System.currentTimeMillis()))
                .findFirst().orElse(null);
        if (payout != null) {
            try { Contracts.claim(player,payout.id()); }
            catch (IllegalStateException full) { player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.claim"),false); }
        }
    }

    private static boolean summon(ServerPlayerEntity player, ItemStack stack) {
        var c=contract(player,stack);
        if (c==null || c.status()!=ContractInstance.Status.REWARDED || c.vampire!=null) return false;
        long remaining = c.vampireReplayReady - player.getServer().getOverworld().getTime();
        if (remaining > 0) {
            player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.cooldown", (remaining + 19) / 20), true);
            return false;
        }
        var world=player.getServerWorld();
        var at=BlockPos.ofFloored(player.getPos().add(Vec3d.fromPolar(0,player.getYaw()).multiply(5)));
        var feet=Vec3d.ofBottomCenter(at);
        var clearance=net.minecraft.entity.EntityDimensions.changing(1.32f,3.2f).getBoxAt(feet);
        if (!world.isChunkLoaded(at) || !world.isChunkLoaded(at.add(-1,0,-1)) || !world.isChunkLoaded(at.add(1,0,1))
                || !world.isChunkLoaded(at.add(-1,0,1)) || !world.isChunkLoaded(at.add(1,0,-1))
                || !world.getWorldBorder().contains(clearance) || !world.isSpaceEmpty(clearance)
                || world.containsFluid(clearance) || !world.getOtherEntities(null,clearance).isEmpty()
                || !world.getBlockState(at.down()).isSideSolidFullSquare(world,at.down(),Direction.UP)) return false;
        var boss=ModEntities.HIGHER_VAMPIRE.create(world);
        if (boss==null) return false;
        boss.refreshPositionAndAngles(feet.x,feet.y,feet.z,player.getYaw()+180,0);
        boss.markHuntBinding();
        if (!world.spawnEntity(boss)) return false;
        c.vampire=boss.getUuid();
        save(stack,c,REPLAY_ACTIVE);
        ContractState.get(player.getServer()).markDirty();
        return true;
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> lines, TooltipType type) {
        var binding = data(stack);
        String state = binding.getInt("State") == SEALED && !binding.containsUuid("Contract")
                ? "captured" : Integer.toString(binding.getInt("State"));
        lines.add(Text.translatable("item.witchersbestiary.crimson_phylactery.state." + state));
    }
}
