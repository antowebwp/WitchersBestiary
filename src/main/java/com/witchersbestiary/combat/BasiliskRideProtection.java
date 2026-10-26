package com.witchersbestiary.combat;
import com.witchersbestiary.entity.monster.InfernalBasiliskEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
public final class BasiliskRideProtection {
    private BasiliskRideProtection(){}
    public static boolean active(Entity entity){return entity instanceof PlayerEntity&&entity.getVehicle() instanceof InfernalBasiliskEntity mount&&mount.isAlive()&&mount.getControllingPassenger()==entity&&mount.onLava();}
}
