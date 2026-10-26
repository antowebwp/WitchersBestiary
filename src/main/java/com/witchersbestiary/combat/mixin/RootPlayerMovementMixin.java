package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.combat.RootBinding;
import net.minecraft.server.network.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class RootPlayerMovementMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method="onPlayerMove",at=@At("HEAD"),cancellable=true)
    private void holdPosition(PlayerMoveC2SPacket packet,CallbackInfo ci){
        if(!RootBinding.active(player))return;
        // This injection runs again on the server thread after NetworkThreadUtils dispatch;
        // never mutate entity state from the Netty receiving thread.
        if(!player.getServer().isOnThread())return;
        if(!Double.isFinite(packet.getX(player.getX()))||!Double.isFinite(packet.getY(player.getY()))||!Double.isFinite(packet.getZ(player.getZ()))||!Float.isFinite(packet.getYaw(player.getYaw()))||!Float.isFinite(packet.getPitch(player.getPitch())))return;
        player.setYaw(packet.getYaw(player.getYaw()));player.setPitch(packet.getPitch(player.getPitch()));
        var pos=RootBinding.anchor(player);
        if(new net.minecraft.util.math.Vec3d(packet.getX(pos.x),packet.getY(pos.y),packet.getZ(pos.z)).squaredDistanceTo(pos)>.0001)
            player.networkHandler.requestTeleport(pos.x,pos.y,pos.z,player.getYaw(),player.getPitch());
        ci.cancel();
    }
}
