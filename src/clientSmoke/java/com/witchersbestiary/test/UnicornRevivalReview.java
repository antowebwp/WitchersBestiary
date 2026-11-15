package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.UnicornEntity;
import com.witchersbestiary.unicorn.Unicorns;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
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

public final class UnicornRevivalReview implements net.fabricmc.api.ClientModInitializer {
    private UUID unicornId;
    private Vec3d routeStart;
    private NbtCompound before;
    private String worldName;
    private int phase, age, total;
    private boolean requested, finished;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        try {
            if (++total > 2800) throw new IllegalStateException("Revival review timed out at phase " + phase);
            if (!requested && client.getOverlay() == null) {
                requested = true;
                require(FabricLoader.getInstance().isModLoaded("redomesticate")
                        && FabricLoader.getInstance().isModLoaded("icys-better-horses"), "Compatibility mods missing");
                client.options.pauseOnLostFocus = false;
                client.options.getViewDistance().setValue(4);
                worldName = "Revival-" + System.currentTimeMillis();
                client.createIntegratedServerLoader().createAndStart(worldName,
                        new LevelInfo("Unicorn Revival", GameMode.CREATIVE, false, Difficulty.NORMAL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE), new GeneratorOptions(22, false, false),
                        registries -> registries.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT)
                                .createDimensionsRegistryHolder(), null);
                return;
            }
            if (client.player == null || client.getServer() == null || client.currentScreen != null) return;
            age++;
            if (phase == 0) {
                unicornId = client.getServer().submit(() -> {
                    var player = client.getServer().getPlayerManager().getPlayerList().getFirst();
                    var world = player.getServerWorld();
                    world.setTimeOfDay(6000);
                    var horse = Unicorns.TYPE.create(world);
                    horse.refreshPositionAndAngles(.5, -60, .5, 0, 0);
                    world.spawnEntity(horse);
                    player.teleport(world, .5, -60, -1.5, 0, 0);
                    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
                    return horse.getUuid();
                }).join();
                advance();
                return;
            }
            var unicorn = client.world.getEntities().iterator();
            UnicornEntity horse = null;
            while (unicorn.hasNext()) {
                if (unicorn.next() instanceof UnicornEntity candidate && candidate.getUuid().equals(unicornId)) {
                    horse = candidate;
                    break;
                }
            }
            if (horse == null) {
                age--;
                return;
            }

            switch (phase) {
                case 1 -> {
                    if (age == 12) click(client, horse);
                    if (age == 20) {
                        require(horse.isTame(), "Golden apple did not tame");
                        hand(client, new ItemStack(Items.SADDLE));
                        advance();
                    }
                }
                case 2 -> {
                    if (age == 12) click(client, horse);
                    if (age == 20) {
                        require(horse.isSaddled(), "Saddle interaction failed");
                        hand(client, new ItemStack(Unicorns.WHISTLE));
                        advance();
                    }
                }
                case 3 -> {
                    client.options.sneakKey.setPressed(age < 16);
                    if (age == 12) click(client, horse);
                    if (age == 23) {
                        require(client.player.getMainHandStack().contains(net.minecraft.component.DataComponentTypes.CUSTOM_DATA), "Whistle did not bind");
                        hand(client, ItemStack.EMPTY);
                        advance();
                    }
                }
                case 4 -> {
                    client.options.sneakKey.setPressed(age < 16);
                    if (age == 12) click(client, horse);
                    if (age == 23) {
                        require(horse.waiting(), "Redomesticate masked Witcher WAIT");
                        advance();
                    }
                }
                case 5 -> {
                    if (age == 12) click(client, horse);
                    if (age == 24) {
                        require(client.player.getVehicle() == horse, "Mount before defeat failed");
                        routeStart = horse.getPos();
                    }
                    client.options.forwardKey.setPressed(age >= 25 && age < 52);
                    if (age == 58) {
                        require(horse.getPos().distanceTo(routeStart) > 4, "Unicorn could not ride before defeat");
                        server(client, (player, actual) -> {
                            actual.equipBodyArmor(new ItemStack(Items.IRON_HORSE_ARMOR));
                            var data = actual.writeNbt(new NbtCompound());
                            data.putInt("UnicornSpeedLevel", 7);
                            actual.readNbt(data);
                            before = actual.writeNbt(new NbtCompound());
                            actual.damage(player.getWorld().getDamageSources().genericKill(), 1000);
                            require(actual.recovering() && actual.isAlive() && !player.hasVehicle(), "Lethal hit did not start recovery");
                            // Reproduce a previously saved, different third-party horse owner.
                            try {
                                actual.getClass().getMethod("bh_setOwner", UUID.class).invoke(actual, UUID.randomUUID());
                            } catch (ReflectiveOperationException error) {
                                throw new IllegalStateException(error);
                            }
                            player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 2, 0, 0);
                        });
                        advance();
                    }
                }
                case 6 -> {
                    if (age < 15 || horse.recovering()) return;
                    require(horse.waiting() && horse.isSaddled(), "WAIT or saddle lost at revival");
                    server(client, (player, actual) -> verifyState(player, actual));
                    click(client, horse);
                    advance();
                }
                case 7 -> {
                    if (age != 10) return;
                    require(client.player.getVehicle() == horse, "Ordinary right click did not mount just after recovery");
                    server(client, (player, actual) -> {
                        player.stopRiding();
                        player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 14, 0, 0);
                    });
                    routeStart = horse.getPos();
                    advance();
                }
                case 8 -> {
                    if (age != 35) return;
                    require(horse.getPos().distanceTo(routeStart) < 1 && horse.waiting(), "WAIT moved towards owner");
                    server(client, (player, actual) -> {
                        player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 2, 0, 0);
                    });
                    advance();
                }
                case 9, 10 -> {
                    client.options.sneakKey.setPressed(age < 16);
                    if (age == 12) click(client, horse);
                    if (age == 23) {
                        int modePhase = phase;
                        server(client, (player, actual) -> {
                            var mode = actual.writeNbt(new NbtCompound()).getString("UnicornBehavior");
                            require(mode.equals(modePhase == 9 ? "GUARD" : "FOLLOW"), "Wrong command: " + mode);
                            if (modePhase == 10) {
                                routeStart = actual.getPos();
                                player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 14, 0, 0);
                            }
                        });
                        advance();
                    }
                }
                case 11 -> {
                    if (age != 65) return;
                    require(horse.getPos().distanceTo(routeStart) > 3, "FOLLOW did not move after revival");
                    server(client, (player, actual) -> {
                        verifyState(player, actual);
                        player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 2, 0, 0);
                    });
                    advance();
                    Files.writeString(Path.of("../before-rejoin.txt"), "PASS normal right click after full recovery, WAIT, GUARD and FOLLOW.");
                    client.world.disconnect();
                    client.disconnect(new TitleScreen());
                    client.createIntegratedServerLoader().start(worldName, () -> {
                        throw new IllegalStateException("Unable to reopen test world");
                    });
                }
                case 12 -> {
                    if (age == 16) {
                        server(client, (player, actual) -> {
                            verifyState(player, actual);
                            var whistle = player.getInventory().getStack(1);
                            var binding = whistle.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                    net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
                            require(binding.containsUuid("Unicorn") && binding.getUuid("Unicorn").equals(unicornId),
                                    "Whistle binding changed after rejoin");
                        });
                        click(client, horse);
                    }
                    if (age == 27) {
                        require(client.player.getVehicle() == horse, "Mount after rejoin failed");
                        server(client, (player, actual) -> {
                            player.stopRiding();
                            player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 16, 0, 0);
                            player.setStackInHand(Hand.MAIN_HAND, player.getInventory().getStack(1).copy());
                        });
                        advance();
                    }
                }
                case 13 -> {
                    if (age == 16) client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    if (age == 30) {
                        require(horse.squaredDistanceTo(client.player) < 30, "Whistle did not summon recovered unicorn");
                        server(client, (player, actual) -> {
                            verifyState(player, actual);
                            player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 2, 0, 0);
                            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        });
                        advance();
                    }
                }
                case 14 -> {
                    if (age == 35) click(client, horse);
                    if (age == 48) {
                        require(client.player.getVehicle() == horse, "Mount after whistle failed");
                        Files.writeString(Path.of("../result.txt"),
                                "PASS tame, saddle, mounted movement, recovery, same UUID/owner/gear/health/XP, WAIT/GUARD/FOLLOW, mount after recovery, rejoin and whistle.");
                        finished = true;
                        client.scheduleStop();
                    }
                }
                default -> throw new IllegalStateException("Unexpected phase " + phase);
            }
        } catch (Exception failure) {
            finished = true;
            throw new IllegalStateException("Unicorn revival review failed at phase " + phase, failure);
        }
    }

    private void verifyState(ServerPlayerEntity player, UnicornEntity horse) {
        var data = horse.writeNbt(new NbtCompound());
        require(horse.getUuid().equals(unicornId) && horse.getOwnerUuid().equals(player.getUuid()) && horse.isTame(),
                "UUID, owner or taming changed");
        require(horse.isAlive() && horse.getHealth() == horse.getMaxHealth() && horse.deathTime == 0
                && horse.getMaxHealth() == horse.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH),
                "Invalid recovered health");
        require(horse.isSaddled() && horse.getBodyArmor().isOf(Items.IRON_HORSE_ARMOR), "Equipment changed");
        require(data.getInt("UnicornSpeedLevel") == before.getInt("UnicornSpeedLevel")
                && data.getDouble("UnicornRidingDistance") == before.getDouble("UnicornRidingDistance"), "Riding progress changed");
    }

    private void hand(MinecraftClient client, ItemStack stack) {
        server(client, (player, horse) -> {
            if (stack.isEmpty() && player.getMainHandStack().isOf(Unicorns.WHISTLE))
                player.getInventory().setStack(1, player.getMainHandStack().copy());
            player.setStackInHand(Hand.MAIN_HAND, stack);
        });
    }

    private void server(MinecraftClient client, java.util.function.BiConsumer<ServerPlayerEntity, UnicornEntity> action) {
        var server = client.getServer();
        server.submit(() -> {
            var player = server.getPlayerManager().getPlayerList().getFirst();
            action.accept(player, (UnicornEntity) player.getServerWorld().getEntity(unicornId));
        }).join();
    }

    private static void click(MinecraftClient client, UnicornEntity horse) {
        client.interactionManager.interactEntity(client.player, horse, Hand.MAIN_HAND);
    }

    private void advance() { phase++; age = 0; }

    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException(reason);
    }
}
