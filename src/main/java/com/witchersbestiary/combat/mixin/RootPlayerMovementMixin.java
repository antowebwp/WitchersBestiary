package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.combat.RootBinding;
import com.witchersbestiary.unicorn.UnicornEntity;
import net.minecraft.server.network.*;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
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

    @Shadow private double updatedRiddenX;
    @Shadow private double updatedRiddenY;
    @Shadow private double updatedRiddenZ;
    @Unique private UnicornEntity wb$inputMount;
    @Unique private int wb$inputTick;
    @Unique private Vec3d wb$rideOrigin;

    @Inject(method = "onPlayerInput", at = @At("TAIL"))
    private void wb$trackRidingInput(PlayerInputC2SPacket packet, CallbackInfo ci) {
        wb$inputMount = player.getVehicle() instanceof UnicornEntity unicorn ? unicorn : null;
        wb$inputTick = player.getServer().getTicks();
    }

    @Inject(method = "onVehicleMove", at = @At("HEAD"))
    private void wb$captureRideOrigin(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        if (!player.getServer().isOnThread()) return;
        wb$rideOrigin = null;
        if (wb$inputMount == null || player.getVehicle() != wb$inputMount
            || player.getServer().getTicks() - wb$inputTick > 2) return;
        Vec3d origin = wb$inputMount.getPos();
        if (origin.squaredDistanceTo(updatedRiddenX, updatedRiddenY, updatedRiddenZ) < 1e-8) {
            wb$rideOrigin = origin;
        }
    }

    @Inject(method = "onVehicleMove", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;updatedRiddenX:D"))
    private void wb$creditAcceptedRide(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        if (wb$rideOrigin != null) {
            wb$inputMount.recordRiddenMove(player, wb$inputMount.getPos().subtract(wb$rideOrigin));
        }
    }
}
