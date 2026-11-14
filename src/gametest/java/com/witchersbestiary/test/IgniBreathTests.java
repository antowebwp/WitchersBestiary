package com.witchersbestiary.test;

import com.witchersbestiary.signs.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;

public final class IgniBreathTests implements FabricGameTest {
    private static final String ARENA="witchersbestiary:village_board_arena";
    private final SignTests fixtures=new SignTests();
    @GameTest(templateName=ARENA)
    public void breathChannelsUntilReleasedOrEmpty(TestContext c) throws Exception {
        var world=c.getWorld();
        var player=new ServerPlayerEntity(world.getServer(),world,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BreathFixture"),
                net.minecraft.network.packet.c2s.common.SyncedClientOptions.createDefault());
        player.networkHandler=new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
                new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),player,
                net.minecraft.server.network.ConnectedClientData.createDefault(player.getGameProfile(),false));
        var pos=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(6,2,6));
        player.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var trained=SignManager.state(player);
        trained.learn(SignType.IGNI);trained.award(100);
        for(var node:SignPlayerState.Upgrade.values())trained.upgrade(SignType.IGNI,node);
        c.assertTrue(trained.unlock(SignType.IGNI,2)&&trained.equip(SignType.IGNI,1,2),"Breath equipped");
        for(int x=-1;x<=3;x++)for(int y=0;y<3;y++)for(int z=0;z<=7;z++)
            world.setBlockState(pos.add(x,y,z),net.minecraft.block.Blocks.AIR.getDefaultState());
        var near=fixtures.target(c,player);
        var side=fixtures.target(c,player);
        var blocked=fixtures.target(c,player);
        side.setPosition(player.getPos().add(2,0,3));
        blocked.setPosition(player.getPos().add(0,0,6));
        for(int y=0;y<3;y++) c.getWorld().setBlockState(player.getBlockPos().add(0,y,5),
                net.minecraft.block.Blocks.STONE.getDefaultState());
        var runtimeMethod=SignManager.class.getDeclaredMethod("runtime",ServerPlayerEntity.class);
        runtimeMethod.setAccessible(true);
        var runtime=runtimeMethod.invoke(null,player);
        var key=runtime.getClass().getDeclaredField("session");
        key.setAccessible(true);
        var session=(java.util.UUID)key.get(runtime);
        var tick=SignManager.class.getDeclaredMethod("tickBreath",ServerPlayerEntity.class,runtime.getClass());
        tick.setAccessible(true);
        String dimension=player.getWorld().getRegistryKey().getValue().toString();
        SignManager.intent(player,new SignPackets.Intent(session,1,10,0,dimension));
        tick.invoke(null,player,runtime);
        c.assertTrue(near.isOnFire()&&!side.isOnFire()&&!blocked.isOnFire(),"Only visible targets on the narrow line burn");
        var state=SignManager.state(player);
        c.assertTrue(state.concentration()==99.75,"Server charges per tick");
        SignManager.intent(player,new SignPackets.Intent(session,2,7,0,dimension));
        tick.invoke(null,player,runtime);
        c.assertTrue(state.concentration()==99.75,"Release stops payment");
        near.discard();side.discard();blocked.discard();
        SignManager.intent(player,new SignPackets.Intent(session,3,10,0,dimension));
        for(int elapsed=0;elapsed<410;elapsed++)tick.invoke(null,player,runtime);
        c.assertTrue(state.concentration()==0,"No fixed duration and no negative resource");
        var active=runtime.getClass().getDeclaredField("breathTicks");
        active.setAccessible(true);
        c.assertTrue(active.getInt(runtime)==0,"Exhaustion stops channel");
        c.complete();
    }

}
