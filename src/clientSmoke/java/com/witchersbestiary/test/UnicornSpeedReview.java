package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.UnicornEntity;
import com.witchersbestiary.unicorn.Unicorns;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class UnicornSpeedReview {
    private static final int[] LEVELS = {1, 13, 25, 24};
    private static final double[] BASES = {.19, .33};
    private final UnicornEntity[] horses = new UnicornEntity[2];
    private final StringBuilder csv = new StringBuilder("horse,level,client_blocks_per_second,server_blocks_per_second,steady_client_blocks_per_second,tooltip\n");
    private CompletableFuture<Double> measured;
    private CompletableFuture<Void> ready;
    private final ArrayList<Double> steps = new ArrayList<>();
    private Vec3d from;
    private double previousZ;
    private int ticks, trial, elapsed;
    private boolean requested, finished;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        try {
            if (++ticks > 1800) throw new IllegalStateException("Riding review timed out");
            if (!requested && client.getOverlay() == null) {
                requested = true;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(5);
                client.createIntegratedServerLoader().createAndStart("UnicornSpeed",
                        new LevelInfo("Unicorn Speed", GameMode.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        registries -> registries.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT)
                                .createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.getOverlay() != null) return;
            var server = client.getServer();
            int index = trial % BASES.length;
            int level = LEVELS[trial / BASES.length];
            if (ready == null) {
                ready = server.submit(() -> prepare(server, index, level));
                return;
            }
            if (!ready.isDone()) return;
            ready.join();
            if (!(client.player.getVehicle() instanceof UnicornEntity unicorn)) return;
            client.player.setYaw(0);
            client.player.setPitch(0);
            client.options.forwardKey.setPressed(elapsed >= 5 && elapsed < 70);
            // Read the two positions after acceleration, before releasing full forward input.
            if (elapsed == 22) {
                from = unicorn.getPos();
                previousZ = from.z;
                steps.clear();
                measured = server.submit(() -> horses[index].getZ());
            }
            if (elapsed > 22 && elapsed <= 62) {
                double z = unicorn.getZ();
                steps.add((z - previousZ) * 20);
                previousZ = z;
            }
            if (elapsed == 62) {
                double clientSpeed = (unicorn.getZ() - from.z) / 2;
                double start = measured.join();
                measured = server.submit(() -> (horses[index].getZ() - start) / 2);
                String tooltip = jadeSpeed(unicorn);
                double serverSpeed = measured.join();
                double expected = BASES[index] * 42.1579;
                expected += (22 - expected) * (level - 1) / 24;
                if (level == 24) expected = 22;
                steps.sort(Double::compare);
                double steadySpeed = steps.get(steps.size() / 2);
                var line = String.format(Locale.ROOT, "%s,%d,%.4f,%.4f,%.4f,%s%n",
                        index == 0 ? "slow" : "fast", level, clientSpeed, serverSpeed, steadySpeed, tooltip);
                csv.append(line);
                Files.writeString(Path.of("../measurements.csv"), csv);
                if (Math.abs(steadySpeed - expected) > .03 || Math.abs(serverSpeed - expected) > .6)
                    throw new IllegalStateException("Riding speed exceeded tolerance: " + line + "expected " + expected);
                if (!tooltip.contains(String.format(Locale.ROOT, "%.2f / 22.00", expected)))
                    throw new IllegalStateException("Incorrect Jade tooltip: " + line);
            }
            if (elapsed++ < 74) return;
            ready = null;
            elapsed = 0;
            if (++trial == LEVELS.length * BASES.length) {
                finished = true;
                client.scheduleStop();
            }
        } catch (Exception failure) {
            finished = true;
            client.options.forwardKey.setPressed(false);
            throw new IllegalStateException("Unicorn speed review failed", failure);
        }
    }

    private void prepare(MinecraftServer server, int index, int level) {
        var world = server.getOverworld();
        var player = server.getPlayerManager().getPlayerList().getFirst();
        player.stopRiding();
        player.teleport(world, .5, -60, .5, 0, 0);
        var horse = horses[index];
        if (horse == null) {
            horse = Unicorns.TYPE.create(world);
            horse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(BASES[index]);
            horse.bondWithPlayer(player);
            horse.saddle(new ItemStack(Items.SADDLE), null);
            horse.setAiDisabled(true);
            horses[index] = horse;
            horse.refreshPositionAndAngles(.5, -60, .5, 0, 0);
            world.spawnEntity(horse);
        }
        var nbt = new NbtCompound();
        horse.writeCustomDataToNbt(nbt);
        nbt.putInt("UnicornSpeedLevel", level);
        nbt.putDouble("UnicornRidingDistance", level == 24 ? 831 : 0);
        horse.readCustomDataFromNbt(nbt);
        var stored = horse.writeNbt(new NbtCompound());
        var reloaded = Unicorns.TYPE.create(world);
        reloaded.readNbt(stored);
        if (Math.abs(reloaded.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - BASES[index]) > .00001
                || Math.abs(reloaded.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        - horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) > .00001) {
            throw new IllegalStateException("Riding speed did not survive NBT round trip");
        }
        reloaded.discard();
        horse.refreshPositionAndAngles(.5, -60, .5, 0, 0);
        horse.setVelocity(Vec3d.ZERO);
        var otherHorse = horses[1 - index];
        if (otherHorse != null) otherHorse.refreshPositionAndAngles(5.5, -60, .5, 0, 0);
        if (!player.startRiding(horse, true)) throw new IllegalStateException("Mount failed");
    }

    private static String jadeSpeed(UnicornEntity unicorn) throws ReflectiveOperationException {
        var provider = Class.forName("snownee.jade.addon.vanilla.HorseStatsProvider");
        var tooltip = Class.forName("snownee.jade.api.ITooltip");
        var accessor = Class.forName("snownee.jade.api.EntityAccessor");
        var config = Class.forName("snownee.jade.api.config.IPluginConfig");
        var lines = new ArrayList<String>();
        Object view = Proxy.newProxyInstance(tooltip.getClassLoader(), new Class<?>[]{tooltip}, (proxy, method, args) -> {
            if (method.getName().equals("add") && args[0] instanceof Text text) lines.add(text.getString());
            return null;
        });
        Object target = Proxy.newProxyInstance(accessor.getClassLoader(), new Class<?>[]{accessor}, (proxy, method, args) ->
                switch (method.getName()) {
                    case "getEntity" -> unicorn;
                    case "showDetails" -> true;
                    default -> null;
                });
        provider.getMethod("appendTooltip", tooltip, accessor, config)
                .invoke(provider.getField("INSTANCE").get(null), view, target, null);
        return lines.stream().filter(line -> line.contains(" / 22.00")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Jade returned " + lines));
    }
}
