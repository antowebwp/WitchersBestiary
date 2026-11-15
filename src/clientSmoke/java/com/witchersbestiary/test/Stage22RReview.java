package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.HigherVampireEntity;
import com.witchersbestiary.equipment.armory.SchoolArmor;
import com.witchersbestiary.unicorn.UnicornEntity;
import com.witchersbestiary.unicorn.Unicorns;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

/** Real framebuffer evidence from a single isolated flat world. */
final class Stage22RReview {
    private static final int ARMOR_SHOTS = 11;
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] PIECES = {"helmet","chestplate","leggings","boots"};
    private static final String[] TIERS = {"","enhanced_","mastercrafted_"};
    private static final String[] SCENES = {"horn-idle", "horn-healing", "horn-after-healing", "whistle",
            "vampire-transform", "cocoon", "cocoon-cracked", "cocoon-broken"};
    private static final int LIMIT = Boolean.getBoolean("witchersbestiary.stage22rArmorOnly") ? ARMOR_SHOTS : ARMOR_SHOTS + SCENES.length;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<CompletableFuture<Void>> frames = new ArrayList<>();

    private int ticks, requestAt = -1, capturedAge = -1;
    private int index = Boolean.getBoolean("witchersbestiary.stage22rVampireOnly") ? ARMOR_SHOTS + 4 : 0;
    private volatile int ready = -1;
    private volatile String failure;
    private volatile Vec3d origin, whistleFocus;
    private boolean worldRequested, finished;
    private UnicornEntity unicorn;
    private HigherVampireEntity vampire;
    private ArmorStandEntity stand;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.END.register(context -> capture());
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        try {
            if (++ticks > 1700) throw new IllegalStateException("Stage 22R capture timeout at " + index);
            if (failure != null) throw new IllegalStateException(failure);
            if (!worldRequested && client.getOverlay() == null) {
                worldRequested = true;
                client.options.pauseOnLostFocus = false;
                client.options.hudHidden = true;
                client.options.getViewDistance().setValue(4);
                client.options.getMaxFps().setValue(120);
                client.createIntegratedServerLoader().createAndStart("Stage22R",
                        new LevelInfo("Stage 22R Render Review", GameMode.CREATIVE, false, Difficulty.NORMAL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22,false,false),
                        r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.currentScreen != null) return;
            if (index == LIMIT) {
                if (!frames.stream().allMatch(CompletableFuture::isDone)) return;
                frames.forEach(CompletableFuture::join);
                Files.writeString(Path.of(LIMIT == ARMOR_SHOTS ? "../armor-reshoot.txt" : "../result.txt"), "Captured 9 armor variants plus side and posed comparison, unicorn and vampire scenes.");
                stop(client);
                return;
            }
            if (requestAt < 0) {
                requestAt = ticks;
                capturedAge = -1;
                int scene = index;
                client.getServer().execute(() -> {
                    try {
                        prepare(client.getServer().getPlayerManager().getPlayerList().getFirst(), scene);
                        ready = scene;
                    } catch (Exception error) {
                        failure = error.toString();
                    }
                });
                return;
            }
            if (ready != index) return;
            int age = ticks - requestAt;
            if (age == 3 || age == 4) camera(client);
            int cutoff = switch (index - ARMOR_SHOTS) {
                case 4 -> 115;
                case 5, 6 -> 18;
                default -> 18;
            };
            if (age >= cutoff) {
                index++;
                requestAt = -1;
            }
        } catch (Exception error) {
            error.printStackTrace();
            stop(client);
        }
    }

    private void prepare(ServerPlayerEntity player, int scene) {
        var world = player.getServerWorld();
        if (origin == null) {
            origin = player.getPos();
            world.getServer().getTickManager().setTickRate(10);
            world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
            world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, world.getServer());
            world.setTimeOfDay(4500);
            var foot = player.getBlockPos();
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
                world.setBlockState(foot.add(x,-1,z), Blocks.STONE_BRICKS.getDefaultState());
            if (scene < ARMOR_SHOTS) {
                stand = new ArmorStandEntity(world, origin.x + 1.2, origin.y, origin.z + 1.65);
                stand.setYaw(180);
                world.spawnEntity(stand);
            }
            player.teleport(world,origin.x - 1.2, origin.y, origin.z + 1.65,180,0);
            player.setHeadYaw(180);
        }
        if (scene < ARMOR_SHOTS) {
            int id = scene < 9 ? scene : 8;
            String school = SchoolArmor.School.values()[id / 3].name().toLowerCase(Locale.ROOT);
            for (int i = 0; i < SLOTS.length; i++) {
                var item = Registries.ITEM.get(Identifier.of("witchersbestiary",school + "_" + TIERS[id%3] + PIECES[i]));
                if (!(item instanceof SchoolArmor)) throw new IllegalStateException("No matching school armor");
                player.equipStack(SLOTS[i], new ItemStack(item));
                stand.equipStack(SLOTS[i], new ItemStack(item));
            }
            if (scene == 10) {
                stand.setHeadRotation(new EulerAngle(17,25,-7));
                stand.setBodyRotation(new EulerAngle(0,-11,0));
                stand.setLeftArmRotation(new EulerAngle(-34,0,-25));
                stand.setRightArmRotation(new EulerAngle(32,0,16));
                stand.setLeftLegRotation(new EulerAngle(-11,0,-5));
                stand.setRightLegRotation(new EulerAngle(17,0,5));
            }
            return;
        }
        int subject = scene - ARMOR_SHOTS;
        if (subject == 0) {
            stand.discard();
            for (var slot : SLOTS) player.equipStack(slot, ItemStack.EMPTY);
            unicorn = Unicorns.TYPE.create(world);
            if (unicorn == null) throw new IllegalStateException("Unicorn creation failed");
            unicorn.refreshPositionAndAngles(origin.x,origin.y,origin.z + 2,160,0);
            unicorn.setAiDisabled(true);
            unicorn.bondWithPlayer(player);
            world.spawnEntity(unicorn);
            unicorn.saddle(new ItemStack(Items.SADDLE), null);
            unicorn.equipBodyArmor(new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        } else if (subject == 1) {
            player.setHealth(8);
            if (!unicorn.healOwner(world)) throw new IllegalStateException("Unicorn healing fixture rejected");
        } else if (subject == 2) {
            player.setHealth(player.getMaxHealth());
        } else if (subject == 3) {
            var whistle = new ItemStack(Unicorns.WHISTLE);
            player.setStackInHand(Hand.MAIN_HAND, whistle);
            player.setSneaking(true);
            unicorn.interactMob(player,Hand.MAIN_HAND);
            player.setSneaking(false);
            var uuid = unicorn.getUuid();
            unicorn.setPosition(unicorn.getPos().add(15,0,0));
            if (!whistle.getItem().use(world, player, Hand.MAIN_HAND).getResult().isAccepted()
                    || world.getEntity(uuid) != unicorn || unicorn.squaredDistanceTo(player) > 45)
                throw new IllegalStateException("Original unicorn not summoned");
            whistleFocus = unicorn.getPos();
        } else if (subject == 4) {
            if (unicorn != null) unicorn.discard();
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            vampire = ModEntities.HIGHER_VAMPIRE.create(world);
            if (vampire == null) throw new IllegalStateException("Vampire creation failed");
            vampire.refreshPositionAndAngles(origin.x,origin.y,origin.z + 2,180,0);
            vampire.setBodyYaw(180);
            vampire.setHeadYaw(180);
            world.spawnEntity(vampire);
            vampire.damage(world.getDamageSources().genericKill(), 10000);
            if (Math.abs(vampire.getHealth()/vampire.getMaxHealth() - .65) > .001)
                throw new IllegalStateException("First transition health limit missing");
        } else if (subject == 5) {
            if (!vampire.isHybrid() || vampire.isTransforming()) throw new IllegalStateException("Hybrid did not finish");
            var save = vampire.writeNbt(new net.minecraft.nbt.NbtCompound());
            save.putBoolean("FinalPhase",true);
            vampire.readNbt(save);
            vampire.timeUntilRegen = 0;
            vampire.damage(world.getDamageSources().playerAttack(player), 2000);
            if (!vampire.isCocooned()) throw new IllegalStateException("Cocoon did not activate");
        } else if (subject == 6) {
            float hp = vampire.getHealth();
            vampire.damage(world.getDamageSources().playerAttack(player), 49);
            if (!vampire.isCocooned() || vampire.getHealth() != hp) throw new IllegalStateException("Cocoon did not absorb melee");
        } else {
            vampire.damage(world.getDamageSources().playerAttack(player), 200);
            if (vampire.isCocooned()) throw new IllegalStateException("Cocoon did not break");
        }
    }

    private void camera(MinecraftClient client) {
        if (index < 9) {
            client.setCameraEntity(client.player);
            client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
            client.player.setYaw(180);
            client.player.setHeadYaw(180);
            client.player.setPitch(0);
            return;
        }
        client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
        var focus = index == ARMOR_SHOTS+3 && whistleFocus != null ? whistleFocus.add(0,1.1,0) : origin.add(0, index >= ARMOR_SHOTS+4 ? 1.6 : 1.1, 1.7);
        var eye = focus.add(index == 9 ? 5.4 : .0, .25, index == 9 ? -1.2 : -5.3);
        var aim = focus.subtract(eye);
        var marker = new ArmorStandEntity(client.world,eye.x,eye.y,eye.z);
        float yaw = (float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalLength()));
        marker.refreshPositionAndAngles(eye.x,eye.y-marker.getStandingEyeHeight(),eye.z,yaw,pitch);
        marker.setHeadYaw(yaw);
        marker.prevHeadYaw = yaw;
        marker.prevPitch = pitch;
        client.setCameraEntity(marker);
    }

    private void capture() {
        if (finished || index >= LIMIT || requestAt < 0 || ready != index) return;
        int age = ticks-requestAt;
        boolean animated = index >= ARMOR_SHOTS+4;
        if (age < (index == ARMOR_SHOTS+2 ? 12 : 7) || (animated ? age <= capturedAge || age > 25 : capturedAge >= 0)) return;
        capturedAge = age;
        String name = index < ARMOR_SHOTS ? "armor-" + index
                : SCENES[index-ARMOR_SHOTS] + (animated ? "-" + age : "");
        var image = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
        frames.add(CompletableFuture.runAsync(() -> {
            try (image) {
                image.writeTo(Path.of("../" + name + ".png"));
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        },io));
    }

    private void stop(MinecraftClient client) {
        finished = true;
        io.shutdown();
        client.scheduleStop();
    }
}
