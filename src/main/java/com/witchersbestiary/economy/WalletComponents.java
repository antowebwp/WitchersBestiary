package com.witchersbestiary.economy;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Item-carried balance, so transfers/drop/reload preserve ownership with the physical wallet. */
public final class WalletComponents {
    private WalletComponents() {}
    public static final ComponentType<Long> BALANCE = Registry.register(
        Registries.DATA_COMPONENT_TYPE, Identifier.of("witchersbestiary:wallet_balance"),
        ComponentType.<Long>builder().codec(Codec.LONG.validate(value -> value >= 0 ? com.mojang.serialization.DataResult.success(value) : com.mojang.serialization.DataResult.error(() -> "Negative wallet balance")))
            .packetCodec(PacketCodecs.VAR_LONG).build());
    public static void initialize() { }
}
