package com.witchersbestiary.entity;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.monster.LeshenEntity;
import com.witchersbestiary.entity.monster.LeshenWolfEntity;
import com.witchersbestiary.entity.monster.DrownerEntity;
import com.witchersbestiary.entity.monster.GhoulEntity;
import com.witchersbestiary.entity.monster.FogletEntity;
import com.witchersbestiary.entity.monster.NoonwraithEntity;
import com.witchersbestiary.world.NoonwraithSpawns;
import com.witchersbestiary.world.WerewolfSpawns;
import com.witchersbestiary.entity.monster.WerewolfEntity;
import com.witchersbestiary.world.FogletSpawns;
import net.minecraft.entity.passive.WolfEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final EntityType<LeshenEntity> LESHEN = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "leshen"),
            EntityType.Builder.create(LeshenEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 3.8f).maxTrackingRange(12)
                    .build(WitchersBestiary.MOD_ID + ":leshen"));

    private ModEntities() { }
    public static final EntityType<LeshenWolfEntity> LESHEN_WOLF = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "leshen_wolf"),
            EntityType.Builder.create(LeshenWolfEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 0.85f).maxTrackingRange(10)
                    .build(WitchersBestiary.MOD_ID + ":leshen_wolf"));
    public static final EntityType<DrownerEntity> DROWNER = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID,"drowner"),
            EntityType.Builder.create(DrownerEntity::new,SpawnGroup.MONSTER).dimensions(.8f,2.4f)
                    .maxTrackingRange(8).build(WitchersBestiary.MOD_ID+":drowner"));
    public static final EntityType<FogletEntity> FOGLET = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "foglet"),
            EntityType.Builder.create(FogletEntity::new, SpawnGroup.MONSTER)
                    .dimensions(.9f, 2.2f).maxTrackingRange(12)
                    .build(WitchersBestiary.MOD_ID + ":foglet"));
    public static final EntityType<GhoulEntity> GHOUL = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "ghoul"),
            EntityType.Builder.create(GhoulEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.15f, 1.4f).maxTrackingRange(10)
                    .build(WitchersBestiary.MOD_ID + ":ghoul"));

    public static final EntityType<WerewolfEntity> WEREWOLF=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"werewolf"),
            EntityType.Builder.create(WerewolfEntity::new,SpawnGroup.MONSTER).dimensions(1.15f,2.8f).maxTrackingRange(12).build(WitchersBestiary.MOD_ID+":werewolf"));

    public static final EntityType<com.witchersbestiary.entity.monster.KatakanEntity> KATAKAN=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"katakan"),EntityType.Builder.create(com.witchersbestiary.entity.monster.KatakanEntity::new,SpawnGroup.MONSTER).dimensions(2.1f,2.6f).maxTrackingRange(12).build(WitchersBestiary.MOD_ID+":katakan"));

    public static final EntityType<com.witchersbestiary.entity.monster.WraithEntity> WRAITH=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"wraith"),EntityType.Builder.create(com.witchersbestiary.entity.monster.WraithEntity::new,SpawnGroup.MONSTER).dimensions(.8f,2.7f).maxTrackingRange(12).build(WitchersBestiary.MOD_ID+":wraith"));
    public static final EntityType<com.witchersbestiary.entity.monster.BonecrusherEntity> BONECRUSHER=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"bonecrusher"),EntityType.Builder.create(com.witchersbestiary.entity.monster.BonecrusherEntity::new,SpawnGroup.MONSTER).dimensions(2.15f,3.4f).maxTrackingRange(12).build(WitchersBestiary.MOD_ID+":bonecrusher"));
    public static final EntityType<com.witchersbestiary.entity.monster.NekkerEntity> NEKKER=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"nekker"),EntityType.Builder.create(com.witchersbestiary.entity.monster.NekkerEntity::new,SpawnGroup.MONSTER).dimensions(.65f,1.4f).maxTrackingRange(10).build(WitchersBestiary.MOD_ID+":nekker"));
    public static final EntityType<com.witchersbestiary.entity.monster.FiendEntity> FIEND=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"fiend"),EntityType.Builder.create(com.witchersbestiary.entity.monster.FiendEntity::new,SpawnGroup.MONSTER).dimensions(3.1f,3.4f).maxTrackingRange(14).build(WitchersBestiary.MOD_ID+":fiend"));
    public static final EntityType<com.witchersbestiary.entity.monster.EndregaWarriorEntity> ENDREGA_WARRIOR=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"endrega_warrior"),EntityType.Builder.create(com.witchersbestiary.entity.monster.EndregaWarriorEntity::new,SpawnGroup.MONSTER).dimensions(2.3f,1.65f).maxTrackingRange(10).build(WitchersBestiary.MOD_ID+":endrega_warrior"));
    public static final EntityType<com.witchersbestiary.entity.monster.GriffinEntity> GRIFFIN=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"griffin"),EntityType.Builder.create(com.witchersbestiary.entity.monster.GriffinEntity::new,SpawnGroup.MONSTER).dimensions(2.4f,2.8f).maxTrackingRange(16).build(WitchersBestiary.MOD_ID+":griffin"));
    public static final EntityType<com.witchersbestiary.entity.monster.WaterHagEntity> WATER_HAG=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"water_hag"),EntityType.Builder.create(com.witchersbestiary.entity.monster.WaterHagEntity::new,SpawnGroup.MONSTER).dimensions(1.1f,2.25f).maxTrackingRange(10).build("witchersbestiary:water_hag"));
    public static final EntityType<com.witchersbestiary.entity.projectile.MudClodEntity> MUD_CLOD=Registry.register(Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"mud_clod"),EntityType.Builder.<com.witchersbestiary.entity.projectile.MudClodEntity>create(com.witchersbestiary.entity.projectile.MudClodEntity::new,SpawnGroup.MISC).dimensions(.3f,.3f).maxTrackingRange(6).trackingTickInterval(2).build("witchersbestiary:mud_clod"));
    public static final EntityType<com.witchersbestiary.entity.monster.GraveHagEntity> GRAVE_HAG=Registry.register(Registries.ENTITY_TYPE,Identifier.of("witchersbestiary","grave_hag"),EntityType.Builder.create(com.witchersbestiary.entity.monster.GraveHagEntity::new,SpawnGroup.MONSTER).dimensions(.95f,2.1f).maxTrackingRange(12).build("witchersbestiary:grave_hag"));
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GRAVE_HAG,com.witchersbestiary.entity.monster.GraveHagEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WATER_HAG,com.witchersbestiary.entity.monster.WaterHagEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(INFERNAL_BASILISK,com.witchersbestiary.entity.monster.InfernalBasiliskEntity.createAttributes());
        com.witchersbestiary.world.BasiliskSpawns.initialize();
        FabricDefaultAttributeRegistry.register(GRIFFIN,com.witchersbestiary.entity.monster.GriffinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ENDREGA_WARRIOR,com.witchersbestiary.entity.monster.EndregaWarriorEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FIEND,com.witchersbestiary.entity.monster.FiendEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NEKKER,com.witchersbestiary.entity.monster.NekkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BONECRUSHER,com.witchersbestiary.entity.monster.BonecrusherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WRAITH,com.witchersbestiary.entity.monster.WraithEntity.createWraithAttributes());
        FabricDefaultAttributeRegistry.register(WEREWOLF,WerewolfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NOONWRAITH, NoonwraithEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LESHEN, LeshenEntity.createLeshenAttributes());
        FabricDefaultAttributeRegistry.register(LESHEN_WOLF, WolfEntity.createWolfAttributes());
        FabricDefaultAttributeRegistry.register(DROWNER,DrownerEntity.createDrownerAttributes());
        FabricDefaultAttributeRegistry.register(GHOUL, GhoulEntity.createGhoulAttributes());
        FabricDefaultAttributeRegistry.register(FOGLET, FogletEntity.createFogletAttributes());
        com.witchersbestiary.world.NaturalSpawns.initialize();
        FogletSpawns.initialize();
        NoonwraithSpawns.initialize();
        WerewolfSpawns.initialize();
        FabricDefaultAttributeRegistry.register(KATAKAN,com.witchersbestiary.entity.monster.KatakanEntity.createAttributes());
        com.witchersbestiary.world.KatakanSpawns.initialize();
    }
    public static final EntityType<NoonwraithEntity> NOONWRAITH = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(WitchersBestiary.MOD_ID, "noonwraith"),
            EntityType.Builder.create(NoonwraithEntity::new, SpawnGroup.MONSTER).dimensions(.8f, 2.8f)
                    .maxTrackingRange(12).build(WitchersBestiary.MOD_ID + ":noonwraith"));
    public static final EntityType<com.witchersbestiary.entity.monster.InfernalBasiliskEntity> INFERNAL_BASILISK=Registry.register(
            Registries.ENTITY_TYPE,Identifier.of(WitchersBestiary.MOD_ID,"infernal_basilisk"),
            EntityType.Builder.create(com.witchersbestiary.entity.monster.InfernalBasiliskEntity::new,SpawnGroup.MONSTER)
                    .dimensions(2.4f,2.2f).makeFireImmune().maxTrackingRange(16).build(WitchersBestiary.MOD_ID+":infernal_basilisk"));
}
