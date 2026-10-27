package com.witchersbestiary.contracts;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/** Preflight the entire main inventory; a full bag leaves the completed contract unclaimed. */
public final class ContractRewards {
    private ContractRewards() {}
    public static List<ItemStack> plan(List<ItemStack> inventory, ContractDefinition.Reward reward) {
        var result = new ArrayList<ItemStack>(); inventory.forEach(s -> result.add(s.copy()));
        for (var grant : reward.items()) {
            var remaining = new ItemStack(Registries.ITEM.get(grant.item()), grant.count());
            for (var slot : result) if (!slot.isEmpty() && ItemStack.areItemsAndComponentsEqual(slot, remaining)) {
                int amount = Math.min(remaining.getCount(), slot.getMaxCount() - slot.getCount());
                if (amount > 0) { slot.increment(amount); remaining.decrement(amount); }
            }
            for (int i = 0; i < result.size() && !remaining.isEmpty(); i++) if (result.get(i).isEmpty()) {
                int amount = Math.min(remaining.getCount(), remaining.getMaxCount()); result.set(i, remaining.copyWithCount(amount)); remaining.decrement(amount);
            }
            if (!remaining.isEmpty()) throw new IllegalStateException("Not enough main inventory space; reward remains unclaimed");
        }
        return result;
    }
    public static void claim(ContractState store, ContractInstance contract, ServerPlayerEntity player) {
        store.writable();
        if (store.find(contract.id()) != contract || !contract.owner().equals(player.getUuid()) || contract.status() != ContractInstance.Status.COMPLETED)
            throw new IllegalStateException("Only the owner can claim a completed, unclaimed contract");
        var inventory = player.getInventory(); var slots = plan(inventory.main, contract.definition().reward());
        // No asynchronous callbacks or item spawning: transition and delivery share one server-thread operation.
        contract.transition(ContractInstance.Status.REWARDED); store.markDirty();
        for (int i = 0; i < slots.size(); i++) inventory.main.set(i, slots.get(i));
        inventory.markDirty(); player.addExperience(contract.definition().reward().experience()); player.currentScreenHandler.sendContentUpdates();
    }
}
