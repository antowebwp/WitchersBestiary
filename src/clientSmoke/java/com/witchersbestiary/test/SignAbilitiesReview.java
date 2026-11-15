package com.witchersbestiary.test;

import com.witchersbestiary.client.signs.SignsClient;
import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.signs.SignManager;
import com.witchersbestiary.signs.SignPlayerState;
import com.witchersbestiary.signs.SignType;
import com.witchersbestiary.sorcery.Sorcery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

final class SignAbilitiesReview {
    private static final String[] ANGLES = {"front", "side", "45"};
    private static final Vec3d ORIGIN = new Vec3d(.5, -60, .5);
    private final ArrayList<ZombieEntity> targets = new ArrayList<>();
    private final ArrayList<CompletableFuture<Void>> writes = new ArrayList<>();
    private final java.util.concurrent.ExecutorService writer = Executors.newSingleThreadExecutor();
    private boolean requested, finished;
    private int index = Integer.getInteger("witchersbestiary.signAbilitiesStart", 0);
    private int elapsed, total, captured = -1;
    private boolean preparedWorld;
    private CompletableFuture<Void> prep;
    private long startTime;
    private long lastFrameTime = -1;
    private int frameCount;
    private int peakSigns;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.END.register(ctx -> capture());
    }

    private SignType sign() {
        return SignType.byIndex(index / 12);
    }

    private int form() { return index / 3 % 4; }
    private int angle() { return index % 3; }
    private boolean breath() { return sign() == SignType.IGNI && form() == 2; }

    private void tick(MinecraftClient client) {
        if (finished) return;
        try {
            if (++total > 4900) throw new IllegalStateException("Timeout at scene " + index);
            if (!requested && client.getOverlay() == null) {
                requested = true;
                client.options.hudHidden = true;
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(4);
                client.options.getMaxFps().setValue(100);
                client.createIntegratedServerLoader().createAndStart("SignAbilities-" + index,
                        new LevelInfo("Sign Abilities", GameMode.CREATIVE, false, Difficulty.NORMAL, true,
                                new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(57, false, false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.world == null || client.currentScreen != null) return;
            if (index == 63) {
                if (VfxManager.counts().signs() != 0 || !writes.stream().allMatch(CompletableFuture::isDone)) return;
                writes.forEach(CompletableFuture::join);
                Files.writeString(Path.of("../capture-result.txt"), "63 casts, 3 camera angles, peak sign instances "
                        + peakSigns + ", no sign instances remaining.");
                stop(client);
                return;
            }
            if (prep == null) {
                prep = client.getServer().submit(() -> prepare(client));
                elapsed = 0;
                captured = -1;
                frameCount = 0;
                lastFrameTime = -1;
                return;
            }
            if (!prep.isDone()) return;
            prep.join();
            if (++elapsed == 10) {
                setCamera(client);
                startTime = client.world.getTime();
                if (breath()) SignsClient.send(10, SignType.IGNI.ordinal());
                else client.getServer().submit(() -> cast(client)).join();
            }
            if (elapsed >= 16 && elapsed < 31 && breath() && elapsed % 4 == 0)
                SignsClient.send(8, SignType.IGNI.ordinal());
            if (elapsed == 32 && breath()) SignsClient.send(7, SignType.IGNI.ordinal());
            if (elapsed == 18 && sign() == SignType.QUEN) client.getServer().submit(() -> strike(client, false)).join();
            if (elapsed == 34 && sign() == SignType.QUEN) client.getServer().submit(() -> strike(client, true)).join();
            peakSigns = Math.max(peakSigns, VfxManager.counts().signs());
            int duration = sign() == null ? 75 : sign() == SignType.QUEN || breath() || sign() == SignType.YRDEN ? 42 : 26;
            if (elapsed >= duration) {
                if (captured < 0) throw new IllegalStateException("Missing framebuffer " + index);
                if (breath() && client.world.getTime() - startTime > 26
                        && VfxManager.counts().signs() != 0)
                    throw new IllegalStateException("Breath survived release after the visual lease");
                index++;
                prep = null;
            }
        } catch (Exception error) {
            error.printStackTrace();
            stop(client);
        }
    }

    private void prepare(MinecraftClient client) {
        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
        var world = player.getServerWorld();
        if (!preparedWorld) {
            preparedWorld = true;
            world.setTimeOfDay(13700);
            world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, client.getServer());
            world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, client.getServer());
            for (int x = -12; x <= 12; x++) for (int z = -10; z <= 14; z++) {
                world.setBlockState(new BlockPos(x, -61, z), ((x + z) % 7 == 0 ?
                        Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState());
            }
            var state = SignManager.state(player);
            state.award(1000);
            for (var sign : SignType.values()) {
                state.learn(sign);
                for (var node : SignPlayerState.Upgrade.values()) state.upgrade(sign, node);
                for (int form = 1; form < 4; form++) {
                    if (!state.unlock(sign, form)) throw new IllegalStateException("Test loadout " + sign);
                }
            }
        }
        player.teleport(world, ORIGIN.x, ORIGIN.y, ORIGIN.z, 0, 0);
        player.setHealth(player.getMaxHealth());
        targets.forEach(ZombieEntity::discard);
        targets.clear();
        SignType type = sign();
        double z = type == SignType.YRDEN ? 1.75 : type == SignType.QUEN ? 2.1 : 3.35;
        for (int i = 0; i < (type == SignType.AXII ? 3 : 1); i++) {
            var mob = EntityType.ZOMBIE.create(world);
            mob.setAiDisabled(type != SignType.AXII);
            mob.setBaby(false);
            if (type == SignType.AXII)
                mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
            mob.refreshPositionAndAngles(.5 + (i == 0 ? 0 : i == 1 ? 1.05 : -1.05), -60, z + i * .28, 180, 0);
            world.spawnEntity(mob);
            targets.add(mob);
        }
        var state = SignManager.state(player);
        for (int i = 0; i < 250; i++) state.tick();
        if (type != null) {
            state.select(type);
            state.equip(type, 0, form());
            state.equip(type, 1, form());
        } else {
            player.getInventory().setStack(35, new ItemStack(ModItems.GRIMOIRE));
            Sorcery.learn(player);
            Sorcery.state(player).cooldown = 0;
        }
    }

    private void cast(MinecraftClient client) {
        var server = client.getServer();
        var player = server.getPlayerManager().getPlayerList().getFirst();
        player.setYaw(0);
        player.setPitch(0);
        if (sign() == null || sign() == SignType.AXII) {
            var eyes = targets.getFirst().getEyePos();
            player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, eyes);
        }
        boolean accepted = sign() == null ? Sorcery.cast(player) : SignManager.cast(player, server.getTicks());
        if (!accepted) {
            var target = targets.getFirst();
            throw new IllegalStateException("Cast rejected: " + sign() + "/" + form() + "/" + angle()
                    + "; player " + player.getPos() + "; target " + target.getPos()
                    + "; visible=" + player.canSee(target) + "; enemy=" + SignManager.enemy(player, target)
                    + "; control=" + SignManager.controlSupported(target) + "; alive=" + target.isAlive());
        }
    }

    private void strike(MinecraftClient client, boolean clear) {
        var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
        player.changeGameMode(GameMode.SURVIVAL);
        SignManager.protect(player, player.getWorld().getDamageSources().mobAttack(targets.getFirst()),
                clear || form() == 1 ? 30 : 3);
        player.changeGameMode(GameMode.CREATIVE);
    }

    private void setCamera(MinecraftClient client) {
        var sign = sign();
        var focus = ORIGIN.add(0, sign == SignType.YRDEN ? .28 : 1.1,
                sign == SignType.QUEN || sign == SignType.YRDEN || sign == SignType.IGNI && form() == 1 ? .1
                        : sign == null || sign == SignType.AXII ? 2.7 : 2.35);
        var offset = switch (angle()) {
            case 0 -> new Vec3d(0, 2.0, 7);
            case 1 -> new Vec3d(7, 2.1, 0);
            default -> new Vec3d(5.1, 2.5, -5.1);
        };
        var eye = focus.add(offset);
        var dir = focus.subtract(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(dir.y, dir.horizontalLength()));
        var camera = new ArmorStandEntity(client.world, eye.x, eye.y, eye.z);
        var focusEntityPosition = eye.add(dir.normalize().multiply(4));
        client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
        camera.refreshPositionAndAngles(focusEntityPosition.x, focusEntityPosition.y - camera.getStandingEyeHeight(),
                focusEntityPosition.z, yaw, pitch);
        camera.setHeadYaw(yaw);
        camera.prevHeadYaw = yaw;
        camera.prevPitch = pitch;
        client.setCameraEntity(camera);
    }

    private void capture() {
        if (finished || index >= 63 || prep == null || elapsed < 10) return;
        var client = MinecraftClient.getInstance();
        var type = sign();
        long time = client.world.getTime();
        if (time == lastFrameTime) return;
        lastFrameTime = time;
        int age = (int)(time - startTime);
        int still = type == SignType.QUEN ? (form() == 1 || form() == 2 ? 10 : 6)
                : type == SignType.YRDEN ? 12 : breath() || type == null || type == SignType.AXII ? 7 : 4;
        boolean animated = angle() == 2 && (type == null || type == SignType.IGNI && form() == 1
                || type == SignType.YRDEN && form() == 3 || type == SignType.QUEN && form() == 2
                || type == SignType.AXII && form() == 3);
        String name = (type == null ? "icy-grasp" : type.name().toLowerCase(Locale.ROOT)) + "-" + form();
        if (captured < 0 && age >= still) {
            captured = age;
            save(client, name + "-" + ANGLES[angle()] + ".png");
        }
        if (animated && age > 0 && age <= (type == SignType.IGNI ? 9 : 26) && frameCount++ < 26)
            save(client, name + "-frame-" + String.format(Locale.ROOT, "%02d", age) + ".png");
    }

    private void save(MinecraftClient client, String filename) {
        var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
        writes.add(CompletableFuture.runAsync(() -> {
            try (image) { image.writeTo(Path.of("..", filename)); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }, writer));
    }

    private void stop(MinecraftClient client) {
        finished = true;
        writer.shutdown();
        client.scheduleStop();
    }
}
