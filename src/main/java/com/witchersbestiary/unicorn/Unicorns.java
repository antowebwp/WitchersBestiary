package com.witchersbestiary.unicorn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public final class Unicorns {
    public static final Identifier ID = Identifier.of("witchersbestiary", "unicorn");
    public static final EntityType<UnicornEntity> TYPE = Registry.register(Registries.ENTITY_TYPE, ID,
        EntityType.Builder.create(UnicornEntity::new, SpawnGroup.CREATURE).dimensions(1.52f, 1.79f)
            .passengerAttachments(new net.minecraft.util.math.Vec3d(0, 1.60, -.157))
            .maxTrackingRange(10).build(ID.toString()));

    public static final Item WHISTLE = Registry.register(Registries.ITEM,
            Identifier.of("witchersbestiary", "unicorn_whistle"), new UnicornWhistle());

    public static void initialize() {
        UnicornWhistle.initialize();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(WHISTLE));
        FabricDefaultAttributeRegistry.register(TYPE, AbstractHorseEntity.createBaseHorseAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 28).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, .235)
            .add(EntityAttributes.GENERIC_JUMP_STRENGTH, .65).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3));
        SpawnRestriction.register(TYPE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            (type, world, reason, pos, random) -> AnimalEntity.isValidNaturalSpawn(type, world, reason, pos, random)
                && random.nextInt(3) == 0
                && world.isSpaceEmpty(type.getDimensions().getBoxAt(pos.toBottomCenterPos()))
                && world.toServerWorld().getEntitiesByClass(UnicornEntity.class, new net.minecraft.util.math.Box(pos).expand(48),
                    existing -> !existing.isTame()).isEmpty());
        BiomeModifications.addSpawn(BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, Identifier.of("witchersbestiary", "unicorn_habitat"))),
            SpawnGroup.CREATURE, TYPE, 1, 1, 1);
        var egg = Registry.register(Registries.ITEM, Identifier.of("witchersbestiary", "unicorn_spawn_egg"),
            new SpawnEggItem(TYPE, 0xf5eef9, 0xc5a0e9, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(egg));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof UnicornEntity unicorn) || !unicorn.beginRecovery());
    }
}
