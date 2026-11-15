package com.witchersbestiary.test;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.witchersbestiary.unicorn.UnicornEntity;
import com.witchersbestiary.unicorn.Unicorns;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

final class Stage22RNetworkReview implements ModInitializer {
    private final Path output = Path.of(System.getProperty("witchersbestiary.stage20eOutput"));
    private final long[] nonce = new long[2];
    private long next = System.currentTimeMillis();
    private UnicornEntity unicorn;
    private ArmorStandEntity stand;
    private int phase, ticks, movingTicks;
    private double meters, first, second;
    private Vec3d lastPos;

    @Override public void onInitialize() {
        if (Files.exists(output.resolve("stage22r-resume"))) phase = 20;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        if (phase == 99) return;
        try {
            if (++ticks > 4200) throw new IllegalStateException("Stage 22R network timeout at " + phase);
            var a = server.getPlayerManager().getPlayer("WBOne");
            var b = server.getPlayerManager().getPlayer("WBTwo");
            if (a == null || b == null) return;
            var world = server.getOverworld();
            if (phase == 0) {
                world.setTimeOfDay(4200);
                world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,server);
                for (int x=-16;x<=16;x++) for (int z=-32;z<=16;z++)
                    world.setBlockState(new BlockPos(x,119,z), Blocks.STONE_BRICKS.getDefaultState());
                a.changeGameMode(GameMode.CREATIVE);
                b.changeGameMode(GameMode.CREATIVE);
                unicorn = Unicorns.TYPE.create(world);
                unicorn.refreshPositionAndAngles(.5,120,.5,180,0);
                unicorn.bondWithPlayer(a);
                unicorn.saddle(new ItemStack(Items.SADDLE), null);
                unicorn.equipBodyArmor(new ItemStack(Items.IRON_HORSE_ARMOR));
                unicorn.setAiDisabled(true);
                world.spawnEntity(unicorn);
                observe(a,b,world,"horn-idle");
                phase = 1;
                return;
            }
            if (phase == 20) {
                var uuid = UUID.fromString(Files.readString(output.resolve("stage22r-resume")));
                if (!(world.getEntity(uuid) instanceof UnicornEntity horse)) return;
                unicorn = horse;
                observe(a,b,world,"horn-rejoin");
                phase = 21;
                return;
            }

            if (phase == 4 || phase == 6) sample();
            if (!ack(0) || !ack(1)) return;
            switch (phase) {
                case 1 -> {
                    a.setHealth(8);
                    require(unicorn.healOwner(world),"Native owner heal");
                    command(0,"unicornGlow","horn-healing",unicorn.getUuid());
                    command(1,"unicornGlow","horn-healing",unicorn.getUuid());
                    phase=2;
                }
                case 2 -> {
                    if (unicorn.healing()) return;
                    observe(a,b,world,"horn-after-healing");
                    phase=3;
                }
                case 3 -> {
                    unicorn.setAiDisabled(false);
                    camera(a,world,.5,120,2.3,unicorn.getPos().add(0,1,0));
                    a.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
                    command(0,"unicornInteract","horn-mount",unicorn.getUuid());
                    command(1,"unicornInspect","horn-mount",unicorn.getUuid());
                    phase=5;
                }
                case 5 -> {
                    require(a.getVehicle()==unicorn,"Plain use mounted the owner");
                    orientRider(a);
                    setXp(1, 0);
                    beginMeter();
                    command(0,"unicornMeasure","speed-level-1",unicorn.getUuid());
                    command(1,"unicornInspect","speed-level-1",unicorn.getUuid());
                    phase=4;
                }
                case 4 -> {
                    first = measured();
                    a.stopRiding();
                    unicorn.refreshPositionAndAngles(.5,120,.5,180,0);
                    camera(a,world,.5,120,2.3,unicorn.getPos().add(0,1,0));
                    command(0,"unicornInteract","horn-remount",unicorn.getUuid());
                    command(1,"unicornInspect","horn-remount",unicorn.getUuid());
                    phase=7;
                }
                case 7 -> {
                    require(a.getVehicle()==unicorn,"Plain use remounted the owner");
                    setXp(2, 120);
                    orientRider(a);
                    beginMeter();
                    command(0,"unicornMeasure","speed-level-2",unicorn.getUuid());
                    command(1,"unicornInspect","speed-level-2",unicorn.getUuid());
                    phase=6;
                }
                case 6 -> {
                    second = measured();
                    var nbt = new net.minecraft.nbt.NbtCompound();
                    unicorn.writeCustomDataToNbt(nbt);
                    Files.writeString(output.resolve("ride-speed.txt"),"level1=" + first + "\nlevel2=" + second
                            + "\nprogress=" + nbt.getDouble("UnicornRidingDistance") + "\n");
                    a.stopRiding();
                    stand = new ArmorStandEntity(world, .5,120,0);
                    world.spawnEntity(stand);
                    equipStand("cat_mastercrafted_");
                    standView(a,b,world,"stand-equipped");
                    phase=8;
                }
                case 8 -> {
                    equipStand("bear_enhanced_");
                    stand.setHeadRotation(new EulerAngle(17,32,0));
                    stand.setLeftArmRotation(new EulerAngle(-43,0,-16));
                    standView(a,b,world,"stand-replaced-posed");
                    phase=9;
                }
                case 9 -> {
                    unicorn.refreshPositionAndAngles(.5,120,.5,180,0);
                    Files.writeString(output.resolve("stage22r-resume"),unicorn.getUuid().toString());
                    Files.writeString(output.resolve("resume.json"),"{}");
                    command(0,"wait","",unicorn.getUuid());
                    command(1,"wait","",unicorn.getUuid());
                    phase=99;
                    server.save(false,true,true);
                    server.stop(false);
                }
                case 21 -> {
                    Files.writeString(output.resolve("complete.json"),"{\"two_clients_horn_idle_heal_after_mount_rejoin\":true,\"armor_stand_equipment_and_pose\":true}");
                    command(0,"stop","",unicorn.getUuid());
                    command(1,"stop","",unicorn.getUuid());
                    phase=99;
                    server.stop(false);
                }
                default -> throw new IllegalStateException("Unexpected phase " + phase);
            }
        } catch (Exception error) {
            try { Files.writeString(output.resolve("server-failure.json"),error.toString()); }
            catch (Exception ignored) { }
            phase=99;
            server.stop(false);
        }
    }

    private void equipStand(String prefix) {
        String[] pieces = {"helmet","chestplate","leggings","boots"};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        for (int i=0;i<4;i++) stand.equipStack(slots[i],new ItemStack(
                Registries.ITEM.get(Identifier.of("witchersbestiary",prefix+pieces[i]))));
    }

    private void standView(ServerPlayerEntity a, ServerPlayerEntity b, ServerWorld world, String shot) throws Exception {
        camera(a,world,2.8,121,3,stand.getPos().add(0,1,0));
        camera(b,world,-3.2,121,1,stand.getPos().add(0,1,0));
        command(0,"armorInspect",shot,stand.getUuid());
        command(1,"armorInspect",shot,stand.getUuid());
    }

    private void observe(ServerPlayerEntity a, ServerPlayerEntity b, ServerWorld world, String shot) throws Exception {
        var at=unicorn.getPos();
        camera(a,world,at.x+2.65,at.y+1,at.z-3.6,at.add(0,1.05,0));
        camera(b,world,at.x-3.8,at.y+1,at.z-1.7,at.add(0,1.05,0));
        command(0,"unicornInspect",shot,unicorn.getUuid());
        command(1,"unicornInspect",shot,unicorn.getUuid());
    }

    private static void camera(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, Vec3d aim) {
        player.setNoGravity(true);
        player.teleport(world,x,y,z,0,0);
        player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,aim);
    }

    private void setXp(int level,double progress) {
        var data = new net.minecraft.nbt.NbtCompound();
        unicorn.writeCustomDataToNbt(data);
        data.putInt("UnicornSpeedLevel",level);
        data.putDouble("UnicornRidingDistance",progress);
        unicorn.readCustomDataFromNbt(data);
    }

    private static void orientRider(ServerPlayerEntity player) { player.setYaw(180); player.setPitch(0); }

    private void beginMeter() { lastPos=unicorn.getPos(); meters=0; movingTicks=0; }

    private void sample() {
        var at=unicorn.getPos();
        double movement=at.subtract(lastPos).horizontalLength();
        lastPos=at;
        if (movement < .05 || movement > .9) return;
        if (++movingTicks >= 6 && movingTicks <= 24) meters+=movement;
    }

    private double measured() {
        require(movingTicks>=12,"Not enough ridden samples");
        return meters * 20 / (Math.min(movingTicks,24)-5);
    }

    private void command(int recipient,String action,String shot,UUID target) throws Exception {
        String name=recipient==0?"WBOne":"WBTwo";
        nonce[recipient]=++next;
        var path=output.resolve(name+".command.json");
        var temporary=output.resolve(name+".command.tmp");
        Files.writeString(temporary,new Gson().toJson(Map.of("nonce",nonce[recipient],"action",action,
                "shot",shot,"target",target.toString())));
        Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }

    private boolean ack(int recipient) throws Exception {
        var path=output.resolve((recipient==0?"WBOne":"WBTwo")+".reply.json");
        return Files.exists(path) && JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("nonce").getAsLong()==nonce[recipient];
    }

    private static void require(boolean valid,String reason) {
        if(!valid)throw new IllegalStateException(reason);
    }
}
