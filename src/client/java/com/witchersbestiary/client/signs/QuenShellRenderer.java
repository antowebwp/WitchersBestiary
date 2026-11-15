package com.witchersbestiary.client.signs;

import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.signs.SignPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class QuenShellRenderer {
    static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.QuenShell.ID, (packet, context) -> accept(packet));
    }

    public static void accept(SignPackets.QuenShell packet) {
        var world = MinecraftClient.getInstance().world;
        if (world != null) VfxManager.quen(packet, world.getTime());
    }
}
