package com.witchersbestiary.nekker;

import com.witchersbestiary.entity.monster.NekkerEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.util.Identifier;

public final class NekkerHandler extends GenericContainerScreenHandler {
    public static final ExtendedScreenHandlerType<NekkerHandler, Integer> TYPE =
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of("witchersbestiary", "nekker"),
            new ExtendedScreenHandlerType<>(NekkerHandler::new, PacketCodecs.VAR_INT));

    private final NekkerEntity pet;
    private final Property pickup = Property.create();
    private final Property waiting = Property.create();

    public static void initialize() {}

    private NekkerHandler(int syncId, PlayerInventory player, int entityId) {
        this(syncId, player, (NekkerEntity) null);
    }

    public NekkerHandler(int syncId, PlayerInventory player, NekkerEntity pet) {
        super(TYPE, syncId, player, pet == null || player.player.getWorld().isClient
            ? new net.minecraft.inventory.SimpleInventory(9) : pet.storage(), 1);
        this.pet = pet;
        if (!player.player.getWorld().isClient && pet != null) {
            pickup.set(pet.collecting() ? 1 : 0);
            waiting.set(pet.mode().ordinal());
        }
        addProperty(pickup);
        addProperty(waiting);
    }

    public boolean collecting() { return pickup.get() != 0; }
    public com.witchersbestiary.entity.ai.CompanionDuty.Mode mode() { return com.witchersbestiary.entity.ai.CompanionDuty.Mode.values()[Math.clamp(waiting.get(),0,2)]; }

    @Override public boolean canUse(PlayerEntity player) {
        return player.getWorld().isClient || pet != null && pet.canAccess(player);
    }

    @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        if (canUse(player)) super.onSlotClick(slot, button, action, player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) {
        return canUse(player) ? super.quickMove(player, slot) : ItemStack.EMPTY;
    }

    @Override public boolean onButtonClick(PlayerEntity player, int button) {
        if (button != 0 || player.getWorld().isClient || player.currentScreenHandler != this || !canUse(player)) return false;
        pet.setCollecting(!pet.collecting());
        pickup.set(pet.collecting() ? 1 : 0);
        sendContentUpdates();
        return true;
    }
}
