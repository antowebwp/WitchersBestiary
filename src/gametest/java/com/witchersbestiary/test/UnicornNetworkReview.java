package com.witchersbestiary.test;

import com.google.gson.*;
import com.witchersbestiary.unicorn.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public final class UnicornNetworkReview implements ModInitializer {
    private final Path output=Path.of(System.getProperty("witchersbestiary.stage20eOutput"));
    private final long[] nonce=new long[2];
    private int phase, ticks;
    private UnicornEntity unicorn;
    private Vec3d beforeRide;
    private static String name(int index){ return index==0?"WBOne":"WBTwo"; }
    private void command(int index,String action,String shot) throws Exception {
        nonce[index]=System.currentTimeMillis()+index;
        Files.writeString(output.resolve(name(index)+".command.json"),new Gson().toJson(Map.of(
            "nonce",nonce[index],"action",action,"shot",shot,"target",unicorn==null?"":unicorn.getUuid().toString())));
    }
    private boolean ack(int index)throws Exception {
        var path=output.resolve(name(index)+".reply.json");
        return Files.exists(path)&&JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("nonce").getAsLong()==nonce[index];
    }
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private void camera(ServerPlayerEntity player,ServerWorld world,double x,double z){
        player.teleport(world,x,120,z,0,0);
        player.fallDistance=0;
        player.setVelocity(Vec3d.ZERO);
        player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,unicorn.getPos().add(0,1.05,0));
    }
    @Override public void onInitialize(){
        if(Files.exists(output.resolve("unicorn-resume.json")))phase=100;
        if(Boolean.getBoolean("witchersbestiary.stage22aVisual"))phase=900;
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try {
                if(phase==999)return;
                if(++ticks>4800)throw new IllegalStateException("Unicorn review timeout at phase "+phase);
                var a=server.getPlayerManager().getPlayer(name(0));var b=server.getPlayerManager().getPlayer(name(1));
                if(a==null||b==null)return;
                var world=server.getOverworld();
                if(phase==900){
                    world.setTimeOfDay(6000);
                    a.changeGameMode(GameMode.CREATIVE);b.changeGameMode(GameMode.CREATIVE);
                    for(int x=-22;x<=22;x++)for(int z=-22;z<=22;z++)world.setBlockState(new BlockPos(x,119,z),Blocks.GRASS_BLOCK.getDefaultState());
                    for(var prior:world.getEntitiesByClass(UnicornEntity.class,new Box(-20,110,-20,20,135,20),entity->true))prior.discard();
                    unicorn=Unicorns.TYPE.create(world);unicorn.refreshPositionAndAngles(.5,120,.5,0,0);unicorn.setAiDisabled(true);world.spawnEntity(unicorn);
                    camera(a,world,2.7,3.8);camera(b,world,-3.65,.5);
                    if(Boolean.getBoolean("witchersbestiary.stage22aMotion")){
                        unicorn.bondWithPlayer(a);unicorn.saddle(new ItemStack(Items.SADDLE),null);unicorn.setAiDisabled(false);
                        camera(a,world,2.1,2.0);camera(b,world,-4.1,3.4);
                        command(0,"unicornInteract","unicorn-final-mounted");command(1,"unicornInspect","unicorn-final-saddle");phase=903;return;
                    }
                    command(0,"unicornInspect","unicorn-final-three-quarter");command(1,"unicornInspect","unicorn-final-profile");phase=901;return;
                }
                if(phase==100){
                    var id=UUID.fromString(Files.readString(output.resolve("unicorn-resume.json")));
                    if(!(world.getEntity(id) instanceof UnicornEntity restored))return;
                    check(a.getUuid().equals(restored.getOwnerUuid())&&restored.isSaddled()&&restored.recovering()&&restored.waiting(),"Owner, saddle, recovery and wait must survive dedicated restart");
                    unicorn=restored;
                    camera(a,world,unicorn.getX()+3,unicorn.getZ()+3);
                    camera(b,world,unicorn.getX()-3,unicorn.getZ()+2);
                    command(0,"unicornInspect","unicorn-after-restart");command(1,"unicornInspect","unicorn-recovery-profile");phase=101;return;
                }
                if(phase==0){
                    for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)world.setBlockState(new BlockPos(x,119,z),Blocks.GRASS_BLOCK.getDefaultState());
                    a.changeGameMode(GameMode.SURVIVAL);b.changeGameMode(GameMode.SURVIVAL);a.getInventory().clear();b.getInventory().clear();
                    unicorn=Unicorns.TYPE.create(world);unicorn.refreshPositionAndAngles(.5,120,.5,0,0);unicorn.setAiDisabled(true);world.spawnEntity(unicorn);
                    camera(a,world,3.6,4.5);camera(b,world,-4.2,.5);
                    command(0,"unicornInspect","unicorn-three-quarter");command(1,"unicornInspect","unicorn-profile");phase=1;return;
                }
                if(!ack(0)||!ack(1))return;
                switch(phase){
                    case 1->{camera(a,world,1.9,2.3);camera(b,world,-2.1,1.5);a.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE));command(0,"unicornInteract","unicorn-tamed");command(1,"unicornInspect","unicorn-front");phase=2;}
                    case 2->{check(unicorn.isTame()&&a.getUuid().equals(unicorn.getOwnerUuid())&&a.getMainHandStack().isEmpty(),"Native taming and ownership");unicorn.setAiDisabled(false);unicorn.saddle(new ItemStack(Items.SADDLE),null);command(0,"wait","");command(1,"unicornInteract","unicorn-non-owner");phase=3;}
                    case 3->{check(!b.hasVehicle(),"Foreign rider denied");command(0,"unicornInteract","unicorn-owner-mounted");command(1,"unicornInspect","unicorn-saddle");phase=4;}
                    case 4->{check(a.getVehicle()==unicorn&&unicorn.getControllingPassenger()==a&&unicorn.canJump(),"Native rider and saddle controls");beforeRide=unicorn.getPos();command(0,"unicornRide","unicorn-riding");command(1,"unicornInspect","unicorn-mounted-motion");phase=5;}
                    case 5->{check(unicorn.getPos().squaredDistanceTo(beforeRide)>.1,"Mounted inputs moved unicorn");a.stopRiding();a.setSneaking(true);unicorn.interactMob(a,Hand.MAIN_HAND);a.setSneaking(false);unicorn.damage(world.getDamageSources().genericKill(),1000);check(unicorn.recovering()&&unicorn.isAlive(),"Lethal damage recovery");Files.writeString(output.resolve("unicorn-resume.json"),unicorn.getUuid().toString());Files.writeString(output.resolve("resume.json"),"{}");command(0,"wait","");command(1,"wait","");server.save(false,true,true);server.stop(false);phase=999;}
                    case 901->{camera(a,world,.5,4.2);camera(b,world,.5,-3.6);command(0,"unicornInspect","unicorn-final-front");command(1,"unicornInspect","unicorn-final-back");phase=902;}
                    case 902->{unicorn.bondWithPlayer(a);unicorn.saddle(new ItemStack(Items.SADDLE),null);unicorn.setAiDisabled(false);camera(a,world,2.1,2.0);camera(b,world,-4.1,3.4);command(0,"unicornInteract","unicorn-final-mounted");command(1,"unicornInspect","unicorn-final-saddle");phase=903;}
                    case 903->{check(a.getVehicle()==unicorn,"Final visual saddle rider");beforeRide=unicorn.getPos();command(0,"unicornRide","unicorn-final-riding");command(1,"unicornInspect","unicorn-final-motion");phase=904;}
                    case 904->{check(unicorn.getPos().squaredDistanceTo(beforeRide)>.1,"Final gait moves under riding input");a.stopRiding();unicorn.refreshPositionAndAngles(.5,120,.5,0,0);camera(a,world,2.9,3.6);camera(b,world,-3,3.7);command(0,"wait","");command(1,"wait","");phase=905;}
                    case 905->{var threat=net.minecraft.entity.EntityType.ZOMBIE.create(world);a.setHealth(10);a.setAttacker(threat);check(unicorn.healOwner(world)&&a.getHealth()==12.5f,"Native healing fixture");threat.discard();command(0,"unicornGlow","unicorn-healing");command(1,"unicornGlow","unicorn-healing-profile");phase=906;}
                    case 906->{Files.writeString(output.resolve("complete.json"),"{\"unicorn_native_gait_bones_and_healing_horn\":true}");command(0,"stop","");command(1,"stop","");phase=999;}
                    case 101->{Files.writeString(output.resolve("complete.json"),"{\"unicorn_two_clients_owner_mount_motion_and_recovery_restart\":true}");command(0,"stop","");command(1,"stop","");phase=999;}
                }
            }catch(Throwable failure){try{Files.writeString(output.resolve("server-failure.json"),failure.toString());}catch(Exception ignored){}phase=999;server.stop(false);}
        });
    }
}
