package com.witchersbestiary.contracts;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

/** Preflight the entire main inventory; a full bag leaves the completed contract unclaimed. */
public final class ContractRewards {
    private ContractRewards() {}
    public static List<ItemStack> plan(List<ItemStack> inventory, ContractDefinition.Reward reward) {
        return plan(inventory, reward, ItemStack.EMPTY);
    }
    private static List<ItemStack> plan(List<ItemStack> inventory, ContractDefinition.Reward reward, ItemStack bonus) {
        var result = new ArrayList<ItemStack>(); inventory.forEach(s -> result.add(s.copy()));
        var grants = new ArrayList<ItemStack>();
        for (var grant : reward.items()) grants.add(new ItemStack(Registries.ITEM.get(grant.item()), grant.count()));
        if (!bonus.isEmpty()) grants.add(bonus.copy());
        for (var remaining : grants) {
            if(remaining.isOf(com.witchersbestiary.economy.Currency.CROWN)){for(var wallet:result)if(wallet.isOf(com.witchersbestiary.economy.Currency.WALLET)&&!remaining.isEmpty()){long balance=com.witchersbestiary.economy.WalletTransactions.balance(wallet);int n=(int)Math.min(remaining.getCount(),Long.MAX_VALUE-balance);wallet.set(com.witchersbestiary.economy.WalletComponents.BALANCE,balance+n);remaining.decrement(n);}}
            for (var slot : result) if (!slot.isEmpty() && ItemStack.areItemsAndComponentsEqual(slot, remaining)) {
                int amount = Math.min(remaining.getCount(), slot.getMaxCount() - slot.getCount());
                if (amount > 0) { slot.increment(amount); remaining.decrement(amount); }
            }
            for (int i = 0; i < result.size() && !remaining.isEmpty(); i++) if (result.get(i).isEmpty()) {
                int amount = Math.min(remaining.getCount(), remaining.getMaxCount()); result.set(i, remaining.copyWithCount(amount)); remaining.decrement(amount);
            }
            if (!remaining.isEmpty()) throw new IllegalStateException("inventory_full");
        }
        return result;
    }
    public static void claim(ContractState store, ContractInstance contract, ServerPlayerEntity player) {
        store.writable();
        if (store.find(contract.id()) != contract || !contract.owner().equals(player.getUuid()) || contract.status() != ContractInstance.Status.COMPLETED)
            throw new IllegalStateException("Only the owner can claim a completed, unclaimed contract");
        var inventory = player.getInventory(); var input=new ArrayList<ItemStack>(inventory.main);if(inventory.offHand.get(0).isOf(com.witchersbestiary.economy.Currency.WALLET))input.add(inventory.offHand.get(0));var slots = plan(input, contract.definition().reward(), ContractBonus.decode(contract.bonus(), player.getRegistryManager()));
        // No asynchronous callbacks or item spawning: transition and delivery share one server-thread operation.
        contract.transition(ContractInstance.Status.REWARDED); store.markDirty();
        for (int i = 0; i < inventory.main.size(); i++) inventory.main.set(i, slots.get(i));
        if(slots.size()>inventory.main.size())inventory.offHand.set(0,slots.get(inventory.main.size()));
        inventory.markDirty(); player.addExperience(contract.definition().reward().experience()); player.currentScreenHandler.sendContentUpdates();
    }
}
