package com.witchersbestiary.alchemy;

import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Bounded bursts using the vanilla particle protocol, with no client dependency on the server. */
final class BombVfx {
    static final DustParticleEffect GAS = dust(.53f, .62f, .09f, 2.3f);
    private static final DustParticleEffect SILVER = dust(.78f, .69f, .98f, 1.6f);
    private static final DustParticleEffect GOLD = dust(1, .77f, .22f, 1.4f);

    private BombVfx() {}

    static void impact(ServerWorld world, Vec3d center, int type) {
        switch (type) {
            case 0 -> {
                burst(world, center, ParticleTypes.FLAME, 24, .28, .12);
                burst(world, center, new BlockStateParticleEffect(ParticleTypes.BLOCK,
                        Blocks.IRON_BLOCK.getDefaultState()), 22, .24, .25);
                burst(world, center.add(0, .4, 0), ParticleTypes.LARGE_SMOKE, 15, .5, .065);
                ring(world, center, ParticleTypes.CAMPFIRE_COSY_SMOKE, 14, .45, .12);
                world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.PLAYERS, .7f, 1.3f);
            }
            case 1 -> {
                burst(world, center.add(0, .4, 0), SILVER, 44, 1.0, .03);
                burst(world, center, ParticleTypes.END_ROD, 18, .45, .1);
                ring(world, center, SILVER, 28, 1.6, 0);
            }
            case 2 -> {
                burst(world, center, ParticleTypes.FLASH, 1, 0, 0);
                for (int ray = 0; ray < 9; ray++) {
                    double angle = ray * Math.PI * 2 / 9;
                    for (int step = 1; step <= 4; step++) {
                        double length = step * .38;
                        var point = center.add(Math.cos(angle) * length, step * (ray % 3) * .12,
                                Math.sin(angle) * length);
                        burst(world, point, GOLD, 1, 0, 0);
                    }
                }
                burst(world, center, ParticleTypes.FIREWORK, 12, .18, .17);
            }
            default -> throw new IllegalArgumentException("Unsupported impact type");
        }
    }

    static void ignition(ServerWorld world, Vec3d center) {
        burst(world, center, ParticleTypes.FLAME, 32, .55, .15);
        burst(world, center.add(0, .65, 0), ParticleTypes.LAVA, 12, .75, .03);
        ring(world, center, ParticleTypes.FLAME, 24, 1.65, .14);
    }

    private static DustParticleEffect dust(float red, float green, float blue, float size) {
        return new DustParticleEffect(new Vector3f(red, green, blue), size);
    }

    private static void burst(ServerWorld world, Vec3d at, ParticleEffect particle, int count,
                              double spread, double speed) {
        world.spawnParticles(particle, at.x, at.y, at.z, count, spread, spread * .45, spread, speed);
    }

    private static void ring(ServerWorld world, Vec3d center, ParticleEffect particle,
                             int count, double radius, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            double x = Math.cos(angle), z = Math.sin(angle);
            world.spawnParticles(particle, center.x + radius * x, center.y + .06,
                    center.z + radius * z, 0, x, .15, z, speed);
        }
    }
}
