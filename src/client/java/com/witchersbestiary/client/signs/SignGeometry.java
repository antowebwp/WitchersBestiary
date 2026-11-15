package com.witchersbestiary.client.signs;

import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.signs.SignPackets;
import com.witchersbestiary.signs.SignType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class SignGeometry {
    static void initialize() {
        VfxManager.initialize();
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.Visual.ID, (packet, context) -> accept(packet));
    }

    public static void accept(SignPackets.Visual packet) {
        var world = MinecraftClient.getInstance().world;
        var sign = SignType.byIndex(packet.sign());
        if (world == null || sign == null) return;
        long now = world.getTime();
        switch (sign) {
            case IGNI -> VfxManager.igni(packet, now);
            case AARD -> VfxManager.aard(packet, now);
            case YRDEN, QUEN, AXII -> VfxManager.ritual(packet, now);
        }
    }
}
