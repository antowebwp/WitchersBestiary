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
    private static final int EMPTY = 0, SEALED = 1, REPLAY_ACTIVE = 2;

    public VampireHunt() { super(new Settings().maxCount(1).rarity(Rarity.EPIC)); }

    public static void validate(ServerPlayerEntity player, Identifier id, ContractState store) {
        if (ID.equals(id) && store.ownedBy(player.getUuid()).stream().anyMatch(c -> ID.equals(c.definition().id())
                && c.status() != ContractInstance.Status.CANCELLED && c.status() != ContractInstance.Status.EXPIRED))
            throw new IllegalStateException("This legendary hunt has already been accepted");
    }

    public static void issue(ServerPlayerEntity player, ContractInstance contract) {
        if (!ID.equals(contract.definition().id())) return;
        var stack = new ItemStack(ModItems.CRIMSON_PHYLACTERY);
        save(stack, contract, EMPTY);
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
        if (c==null || boss.hasHuntBinding()) return false;
        c.vampire=boss.getUuid();
        boss.markHuntBinding();
        store.markDirty();
        return true;
    }

    private static boolean first(ContractInstance c) { return c.status()==ContractInstance.Status.ACCEPTED; }

    private static HigherVampireEntity aimed(ServerPlayerEntity player, ContractInstance c) {
        if (c.vampire==null || !(player.getServerWorld().getEntity(c.vampire) instanceof HigherVampireEntity boss)
                || !boss.canBeSealed() || player.squaredDistanceTo(boss)>25 || !player.canSee(boss)) return null;
        var from=player.getEyePos();
        var end=from.add(Vec3d.fromPolar(player.getPitch(), player.getYaw()).multiply(5));
        var box=boss.getBoundingBox().expand(.18);
        return box.contains(from)||box.raycast(from,end).isPresent() ? boss : null;
    }

    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack=user.getStackInHand(hand);
        if (user.isSpectator() || !user.isAlive()) return TypedActionResult.fail(stack);
        if (data(stack).getInt("State")==SEALED) {
            if (world.isClient) return TypedActionResult.success(stack);
            return user instanceof ServerPlayerEntity player && summon(player,stack)
                    ? TypedActionResult.consume(stack) : TypedActionResult.fail(stack);
        }
        if (user instanceof ServerPlayerEntity player) {
            var c=contract(player,stack);
            if (c==null || aimed(player,c)==null || !maySeal(c,stack)) return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    private static boolean maySeal(ContractInstance c, ItemStack stack) {
        return first(c) && data(stack).getInt("State")==EMPTY
                || c.status()==ContractInstance.Status.REWARDED && data(stack).getInt("State")==REPLAY_ACTIVE;
    }

    @Override public int getMaxUseTime(ItemStack stack, LivingEntity user) { return CHANNEL+1; }
    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.BLOCK; }

    @Override public void usageTick(World world, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld server)) return;
        var c=contract(player,stack);
        var boss=c==null ? null : aimed(player,c);
        if (!player.isAlive() || !ItemStack.areItemsAndComponentsEqual(player.getStackInHand(player.getActiveHand()),stack) || boss==null || !maySeal(c,stack)) {
            player.stopUsingItem();
            return;
        }
        int elapsed=CHANNEL+1-remaining;
        if (elapsed%5==0) {
            player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.progress", elapsed),true);
            server.spawnParticles(ParticleTypes.CRIMSON_SPORE,boss.getX(),boss.getBodyY(.5),boss.getZ(),5,.5,.35,.5,.04);
        }
        if (elapsed>=CHANNEL) {
            seal(player, player.getStackInHand(player.getActiveHand()), c, boss);
            player.stopUsingItem();
        }
    }

    private static void seal(ServerPlayerEntity player, ItemStack stack, ContractInstance c, HigherVampireEntity boss) {
        var store=ContractState.get(player.getServer());
        boolean initial=first(c);
        if (initial && !c.credit(boss.getUuid(),player.getUuid(),ID,System.currentTimeMillis())) return;
        c.vampire=null;
        c.replayGeneration++;
        save(stack,c,SEALED);
        store.markDirty();
        boss.discard();
        if (initial) {
            try { Contracts.claim(player,c.id()); }
            catch (IllegalStateException full) { player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.claim"),false); }
        }
    }

    private static boolean summon(ServerPlayerEntity player, ItemStack stack) {
        var c=contract(player,stack);
        if (c==null || c.status()!=ContractInstance.Status.REWARDED || c.vampire!=null) return false;
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
        lines.add(Text.translatable("item.witchersbestiary.crimson_phylactery.state."+data(stack).getInt("State")));
    }
}
