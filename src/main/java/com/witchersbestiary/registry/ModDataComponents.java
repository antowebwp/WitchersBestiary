package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.combat.MonsterCategory;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Данные масла хранятся на конкретном предмете и переживают сохранение мира. */
public final class ModDataComponents {
    public static final ComponentType<MonsterCategory> OIL_CATEGORY = Registry.register(
            Registries.DATA_COMPONENT_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "oil_category"),
            ComponentType.<MonsterCategory>builder()
                    .codec(com.mojang.serialization.Codec.STRING.comapFlatMap(s -> {
                        try { return com.mojang.serialization.DataResult.success(MonsterCategory.valueOf(s)); }
                        catch (IllegalArgumentException e) { return com.mojang.serialization.DataResult.error(() -> "Unknown monster category: "+s); }
                    },Enum::name))
                    .packetCodec(PacketCodecs.STRING.xmap(MonsterCategory::valueOf,Enum::name))
                    .build());
    public static final ComponentType<Integer> OIL_HITS = Registry.register(
            Registries.DATA_COMPONENT_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "oil_hits"),
            ComponentType.<Integer>builder().codec(net.minecraft.util.dynamic.Codecs.rangedInt(0, 40))
                    .packetCodec(PacketCodecs.VAR_INT).build());
    public static final ComponentType<Long> OIL_EXPIRY=Registry.register(Registries.DATA_COMPONENT_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"oil_expiry"),ComponentType.<Long>builder().codec(com.mojang.serialization.Codec.LONG).packetCodec(PacketCodecs.VAR_LONG).build());
    public static final ComponentType<Float> OIL_BONUS=Registry.register(Registries.DATA_COMPONENT_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"oil_bonus"),ComponentType.<Float>builder().codec(com.mojang.serialization.Codec.floatRange(0,1)).packetCodec(PacketCodecs.FLOAT).build());
    private ModDataComponents() { }
    public static final ComponentType<Long> HAMMER_READY_AT=Registry.register(Registries.DATA_COMPONENT_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"hammer_ready_at"),ComponentType.<Long>builder().codec(com.mojang.serialization.Codec.LONG).packetCodec(PacketCodecs.VAR_LONG).build());
    public static void initialize() { }
}
