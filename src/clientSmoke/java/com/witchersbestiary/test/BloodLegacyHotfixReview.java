package com.witchersbestiary.test;

import com.witchersbestiary.entity.monster.WraithEntity;
import com.witchersbestiary.equipment.armory.BloodAuraState;
import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.registry.ModItems;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.glfw.GLFW;

final class BloodLegacyHotfixReview {
    private static final InputUtil.Key DEFAULT = InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_B);
    private static final InputUtil.Key MOUSE = InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_5);
    private static final InputUtil.Key KEYBOARD = InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_Y);
    private int ticks, ready;
    private boolean started, finished;
    private KeyBinding binding;
    private ZombieEntity near, far, chargeTarget;
    private Vec3d nearStart;
    private long activatedAt;
    private String screenshot;

    void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, counter) -> capture(MinecraftClient.getInstance()));
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        require(++ticks < 2400, "Review timed out at " + ready);
        if (!started) {
            if (client.getOverlay() != null) return;
            started = true;
            client.options.pauseOnLostFocus = false;
            client.options.getViewDistance().setValue(5);
            client.options.getSimulationDistance().setValue(5);
            client.createIntegratedServerLoader().createAndStart("BloodLegacy-" + System.currentTimeMillis(),
                    new LevelInfo("Blood Legacy Hotfix", GameMode.SURVIVAL, false, Difficulty.NORMAL,
                            true, new GameRules(), DataConfiguration.SAFE_MODE),
                    new GeneratorOptions(42L, false, false), registries -> registries.get(RegistryKeys.WORLD_PRESET)
                            .get(WorldPresets.FLAT).createDimensionsRegistryHolder(), null);
            return;
        }
        if (client.player == null || client.world == null || client.currentScreen != null || client.getOverlay() != null) return;
        switch (++ready) {
            case 45 -> {
                binding = Arrays.stream(client.options.allKeys)
                        .filter(key -> key.getTranslationKey().equals("key.witchersbestiary.legendary_ability"))
                        .findFirst().orElseThrow();
                require(binding.getCategory().equals("category.witchersbestiary") && binding.getDefaultKey().equals(DEFAULT),
                        "Standard Witcher's Bestiary binding with B as default");
                server(client, player -> {
                    var world = player.getServerWorld();
                    world.setTimeOfDay(15000);
                    world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, player.getServer());
                    player.setHealth(5);
                    player.getHungerManager().setFoodLevel(8);
                    player.setStackInHand(Hand.MAIN_HAND, ModItems.BLOOD_LEGACY.getDefaultStack());
                    near = zombie(player, 3, true);
                    far = zombie(player, 6.5, true);
                    nearStart = near.getPos();
                    chargeTarget = zombie(player, 0, false);
                    require(!LegendaryAbilities.activate(player), "Empty charge is rejected");
                    for (int i = 0; i < LegendaryAbilities.FULL_CHARGE; i++) hit(player);
                    require(player.getHealth() == 5, "Charging does not heal");
                });
                client.player.setYaw(0);
                client.player.setPitch(12);
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                bind(client, InputUtil.UNKNOWN_KEY);
                require(binding.isUnbound(), "Standard unbinding");
                press(client, DEFAULT);
            }
            case 55 -> {
                server(client, player -> require(near.getHealth() == 100 && !aura(player), "Old key no longer activates"));
                bind(client, MOUSE);
                press(client, MOUSE);
            }
            case 63 -> {
                server(client, player -> {
                    require(aura(player), "Mouse binding starts the synchronized Bloodlust buff");
                    require(near.getHealth() == 98 && far.getHealth() == 100, "One 2% wave within five blocks");
                    require(near.getZ() > nearStart.z || near.getVelocity().z > .15, "Nearby target is pushed away");
                    activatedAt = player.getServerWorld().getTime();
                });
                require(((BloodAuraState) client.player).bestiary$bloodAura(), "Client receives tracked aura");
                screenshot = "blood-wave-third-person.png";
                press(client, MOUSE);
            }
            case 70 -> {
                server(client, player -> {
                    require(near.getHealth() == 98, "Repeat key does not fire another wave");
                    for (int i = 0; i < 6; i++) hit(player);
                    require(player.getHealth() == 13, "Confirmed melee damage heals at most 2 per hit and 8 total");
                });
                client.options.setPerspective(Perspective.FIRST_PERSON);
            }
            case 74 -> screenshot = "blood-aura-first-person.png";
            case 278 -> {
                server(client, player -> {
                    require(player.getServerWorld().getTime() >= activatedAt + 190 && !aura(player), "Buff expires");
                    require(near.getHealth() == 98, "Wave does not tick");
                    player.setHealth(5);
                    hit(player);
                    require(player.getHealth() == 5, "Expired buff no longer heals");
                    near.discard();
                    far.discard();
                    chargeTarget.discard();
                    player.setStackInHand(Hand.MAIN_HAND, ModItems.GRAVE_BLADE.getDefaultStack());
                });
                require(!((BloodAuraState) client.player).bestiary$bloodAura(), "Client aura clears after expiry");
                bind(client, KEYBOARD);
            }
            case 289 -> press(client, KEYBOARD);
            case 302 -> {
                server(client, player -> require(!player.getServerWorld().getEntitiesByClass(WraithEntity.class,
                        player.getBoundingBox().expand(20), ghost -> ghost.getOwner() == player).isEmpty(),
                        "Rebound keyboard key selects Grave Blade in the main hand"));
                finished = true;
                try {
                    Files.writeString(Path.of("result.txt"), "PASS: wave, knockback, healing, expiry, mouse/keyboard/unbound and Grave Blade.");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                client.scheduleStop();
            }
            default -> {}
        }
    }

    private static ZombieEntity zombie(ServerPlayerEntity player, double offset, boolean spawn) {
        var world = player.getServerWorld();
        var zombie = EntityType.ZOMBIE.create(world);
        zombie.setBaby(false);
        zombie.setAiDisabled(true);
        zombie.setNoGravity(true);
        zombie.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100);
        zombie.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
        zombie.setHealth(100);
        zombie.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ() + offset, 180, 0);
        if (spawn) world.spawnEntity(zombie);
        else {
            zombie.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
            zombie.setHealth(1000);
        }
        return zombie;
    }

    private void hit(ServerPlayerEntity player) {
        var source = player.getDamageSources().playerAttack(player);
        chargeTarget.timeUntilRegen = 0;
        ModItems.BLOOD_LEGACY.getBonusAttackDamage(chargeTarget, 14, source);
        require(chargeTarget.damage(source, 14), "Confirmed primary sword hit");
    }

    private static boolean aura(ServerPlayerEntity player) {
        return ((BloodAuraState) player).bestiary$bloodAura();
    }

    private void bind(MinecraftClient client, InputUtil.Key key) {
        client.options.setKeyCode(binding, key);
        KeyBinding.updateKeysByCode();
    }

    private static void press(MinecraftClient client, InputUtil.Key key) {
        client.onWindowFocusChanged(true);
        KeyBinding.onKeyPressed(key);
    }

    private static void server(MinecraftClient client, java.util.function.Consumer<ServerPlayerEntity> run) {
        var server = client.getServer();
        server.submit(() -> run.accept(server.getPlayerManager().getPlayerList().getFirst())).join();
    }

    private void capture(MinecraftClient client) {
        if (screenshot == null || client.world == null) return;
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(Path.of(screenshot));
            screenshot = null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void require(boolean ok, String description) {
        if (!ok) throw new IllegalStateException(description);
    }
}
