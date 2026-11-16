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
    private boolean requested, finished, called;

    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("witchersbestiary.naturalEncounterReview"))
            ClientTickEvents.END_CLIENT_TICK.register(new NaturalEncounters()::tick);
        else if (Boolean.getBoolean("witchersbestiary.whistleArtReview"))
            ClientTickEvents.END_CLIENT_TICK.register(new WhistleArt()::tick);
        else ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        try {
            if (total == 0) org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().getHandle());
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
                    world.getGameRules().get(GameRules.SPAWN_CHUNK_RADIUS).set(0,world.getServer());
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
            if (horse == null && phase != 13) {
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
                        try (var frame=net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                            frame.writeTo(Path.of("../whistle-in-hand.png"));
                        }
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
                            player.setSneaking(true);
                            actual.interactMob(player, Hand.MAIN_HAND);
                            player.setSneaking(false);
                            require(actual.waiting(), "Whistle fixture requires WAIT before leaving the chunk");
                            player.teleport(player.getServerWorld(), actual.getX(), actual.getY(), actual.getZ() - 384, 0, 0);
                            player.setStackInHand(Hand.MAIN_HAND, player.getInventory().getStack(1).copy());
                        });
                        advance();
                        client.world.disconnect();
                        client.disconnect(new TitleScreen());
                        client.createIntegratedServerLoader().start(worldName, () -> {
                            throw new IllegalStateException("Unable to reload with owner away from unicorn");
                        });
                    }
                }
                case 13 -> {
                    if (age < 40) return;
                    if (age < 160 && !called) {
                        boolean unloaded = client.getServer().submit(() ->
                                client.getServer().getOverworld().getEntity(unicornId) == null).join();
                        if (!unloaded) return;
                        require(client.player.getMainHandStack().isOf(Unicorns.WHISTLE), "Whistle was not restored on rejoin");
                        called = true;
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    }
                    if (age > 250) throw new IllegalStateException("Unloaded-chunk whistle did not complete; requested=" + called);
                    if (called && horse != null && horse.squaredDistanceTo(client.player) < 60) {
                        require(horse.waiting(), "Whistle changed WAIT");
                        try (var frame=net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                            frame.writeTo(Path.of("../whistle-recovered.png"));
                        }
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
                                "PASS tame, saddle, mounted movement, recovery, same UUID/owner/gear/health/XP, WAIT/GUARD/FOLLOW, mount after recovery, rejoin and whistle from an unloaded chunk.");
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
    /** Observes unmodified chunk generation and normal passive spawning on a fresh default world. */
    private static final class NaturalEncounters {
        private final String[] cases = {"vanilla-unicorn", "vanilla-troll", "bop-unicorn", "bop-troll"};
        private boolean started, done, busy;
        private int scene, region, ticks;
        private volatile java.util.UUID visible;
        private final java.util.Map<String,String> observations = new java.util.LinkedHashMap<>();

        void tick(MinecraftClient client) {
            if (done) return;
            try {
                if (!started) {
                    if (client.getOverlay()!=null) return;
                    started=true;
                    org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().getHandle());
                    client.options.pauseOnLostFocus=false;
                    client.options.getViewDistance().setValue(4);
                    client.options.getSimulationDistance().setValue(4);
                    client.createIntegratedServerLoader().createAndStart("Natural-" + System.currentTimeMillis(),
                            new LevelInfo("Natural encounter survey", GameMode.CREATIVE,false,Difficulty.NORMAL,
                                    true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(22,true,false),
                            r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.DEFAULT).createDimensionsRegistryHolder(),null);
                    return;
                }
                if (client.player==null || busy) return;
                if (scene==cases.length) {
                    Files.writeString(Path.of("../natural-results.txt"), observations.toString());
                    done=true;
                    client.scheduleStop();
                    return;
                }
                if (visible!=null) {
                    if (++ticks<20) return;
                    UUID id=visible;
                    if (java.util.stream.StreamSupport.stream(client.world.getEntities().spliterator(),false)
                            .anyMatch(entity -> entity.getUuid().equals(id))) {
                        try (var frame=net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                            frame.writeTo(Path.of("../"+cases[scene]+".png"));
                        }
                    }
                    advance();
                    return;
                }
                if (++ticks==1) searchRegion(client);
                if (ticks>240 && region<2) {
                    ticks=0;
                    region++;
                    return;
                }
                if (ticks>240) {
                    observations.put(cases[scene],"not observed during the bounded survey");
                    advance();
                    return;
                }
                if (ticks%20==0) lookForSpawn(client);
            } catch (Throwable ex) {
                ex.printStackTrace();
                done=true;
                client.scheduleStop();
            }
        }

        private void advance() {
            visible=null;
            scene++;
            ticks=0;
            region=0;
        }

        private void searchRegion(MinecraftClient client) {
            boolean unicorn=scene%2==0;
            String namespace=scene<2?"minecraft":"biomesoplenty";
            int current=scene, attempt=region;
            busy=true;
            client.getServer().submit(() -> {
                var server=client.getServer();
                var world=server.getOverworld();
                world.getGameRules().get(GameRules.SPAWN_CHUNK_RADIUS).set(0,server);
                var habitat=unicorn?Unicorns.HABITAT:com.witchersbestiary.fist.RockTrollSpawns.HABITATS;
                var anchor=world.locateBiome(biome -> biome.isIn(habitat)
                                && biome.getKey().orElseThrow().getValue().getNamespace().equals(namespace)
                                && !biome.matchesKey(net.minecraft.world.biome.BiomeKeys.DRIPSTONE_CAVES),
                        new net.minecraft.util.math.BlockPos((current*3+attempt+1)*1700,110,0),3600,48,40);
                if (anchor==null) return;
                var at=anchor.getFirst();
                world.getChunk(at);
                int y=world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,at.getX(),at.getZ());
                var player=server.getPlayerManager().getPlayerList().getFirst();
                player.teleport(world,at.getX()+.5,y+3,at.getZ()+.5,0,0);
                player.getAbilities().flying=true;
                player.sendAbilitiesUpdate();
            }).whenComplete((unused,error) -> client.execute(() -> {
                if(error!=null) error.printStackTrace();
                busy=false;
            }));
        }

        private void lookForSpawn(MinecraftClient client) {
            boolean unicorn=scene%2==0;
            String namespace=scene<2?"minecraft":"biomesoplenty";
            String label=cases[scene];
            busy=true;
            client.getServer().submit(() -> {
                var player=client.getServer().getPlayerManager().getPlayerList().getFirst();
                var world=player.getServerWorld();
                var found=world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,
                        player.getBoundingBox().expand(115), entity ->
                            entity.getType()==(unicorn?Unicorns.TYPE:com.witchersbestiary.fist.FistRegistry.ROCK_TROLL)
                            && world.getBiome(entity.getBlockPos()).getKey().orElseThrow().getValue().getNamespace().equals(namespace));
                if (found.isEmpty()) return;
                var mob=found.getFirst();
                player.teleport(world,mob.getX()-4,mob.getY()+1.2,mob.getZ(),-90,10);
                var id=mob.getUuid();
                var note=world.getBiome(mob.getBlockPos()).getKey().orElseThrow().getValue()+" at "+mob.getBlockPos()+" count="+found.size();
                client.execute(() -> {
                    observations.put(label,note);
                    visible=id;
                    ticks=0;
                });
            }).whenComplete((unused,error) -> client.execute(() -> {
                if(error!=null) error.printStackTrace();
                busy=false;
            }));
        }
    }
    private static final class WhistleArt {
        private boolean started;
        private int age;
        void tick(MinecraftClient client) {
            try {
                if (!started) {
                    if (client.getOverlay()!=null) return;
                    started=true;
                    org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().getHandle());
                    client.options.pauseOnLostFocus=false;
                    client.options.getViewDistance().setValue(3);
                    client.options.getSimulationDistance().setValue(3);
                    client.createIntegratedServerLoader().createAndStart("WhistleArt",
                            new LevelInfo("Whistle art review", GameMode.SURVIVAL,false,Difficulty.PEACEFUL,
                                    true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(22,false,false),
                            r -> r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(),null);
                    return;
                }
                if (client.player==null || client.getOverlay()!=null) return;
                switch (++age) {
                    case 15 -> edit(client,player->{
                        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Unicorns.WHISTLE));
                        player.getServerWorld().setTimeOfDay(6000);
                    });
                    case 35 -> capture(client,"first-right");
                    case 41 -> client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
                    case 56 -> filterJei();
                    case 63 -> {
                        var manager=client.getBakedModelManager();
                        var icon=manager.getModel(net.minecraft.util.Identifier.of("witchersbestiary","item/unicorn_whistle_icon"));
                        var worldModel=client.getItemRenderer().getModel(new ItemStack(Unicorns.WHISTLE),client.world,client.player,0);
                        require(icon!=manager.getMissingModel() && !icon.hasDepth(), "Independent flat inventory icon");
                        require(worldModel!=manager.getMissingModel() && worldModel.hasDepth(), "Volumetric model baked");
                        capture(client,"inventory-jei");
                    }
                    case 70 -> {
                        client.setScreen(null);
                        client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
                    }
                    case 93 -> capture(client,"third-right");
                    case 98 -> edit(client,player->{
                        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
                        player.setStackInHand(Hand.OFF_HAND,new ItemStack(Unicorns.WHISTLE));
                    });
                    case 122 -> capture(client,"third-left");
                    case 129 -> client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
                    case 149 -> capture(client,"first-left");
                    case 156 -> edit(client,player->{
                        player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);
                        var item=new net.minecraft.entity.ItemEntity(player.getWorld(),player.getX(),player.getY()+.04,
                                player.getZ()+2.2,new ItemStack(Unicorns.WHISTLE),0,0,0);
                        item.setPickupDelay(200);
                        player.getWorld().spawnEntity(item);
                        player.teleport(player.getServerWorld(),player.getX(),player.getY(),player.getZ(),0,31);
                    });
                    case 180 -> capture(client,"ground");
                    case 194 -> {
                        Files.writeString(Path.of("../result.txt"),"Inventory, right and left hands in first and third person, ground. JEI loaded="
                                +FabricLoader.getInstance().isModLoaded("jei"));
                        client.scheduleStop();
                    }
                    default -> {}
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                client.scheduleStop();
            }
        }

        private static void filterJei() throws ReflectiveOperationException {
            if (!FabricLoader.getInstance().isModLoaded("jei")) return;
            var internal=Class.forName("mezz.jei.common.Internal");
            var runtime=internal.getMethod("getJeiRuntime").invoke(null);
            var api=Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            var filter=api.getMethod("getIngredientFilter").invoke(runtime);
            var filterApi=Class.forName("mezz.jei.api.runtime.IIngredientFilter");
            filterApi.getMethod("setFilterText",String.class).invoke(filter,"unicorn whistle");
            var items=(java.util.List<?>)filterApi.getMethod("getFilteredItemStacks").invoke(filter);
            require(items.stream().anyMatch(item -> item instanceof ItemStack stack && stack.isOf(Unicorns.WHISTLE)),
                    "JEI includes the whistle");
        }

        private static void edit(MinecraftClient client,java.util.function.Consumer<ServerPlayerEntity> action) {
            var server=client.getServer();
            server.execute(()->action.accept(server.getPlayerManager().getPlayerList().getFirst()));
        }

        private static void capture(MinecraftClient client,String name) throws java.io.IOException {
            try (var frame=net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                frame.writeTo(Path.of("../"+name+".png"));
            }
        }
    }
}
