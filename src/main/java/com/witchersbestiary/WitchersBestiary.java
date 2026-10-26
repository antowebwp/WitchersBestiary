package com.witchersbestiary;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.registry.ModItems;
import com.witchersbestiary.registry.ModBlocks;
import com.witchersbestiary.registry.ModEffects;
import com.witchersbestiary.registry.ModLoot;
import com.witchersbestiary.registry.ModDataComponents;
import com.witchersbestiary.world.DrownerSpawns;
import com.witchersbestiary.world.GhoulSpawns;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WitchersBestiary implements ModInitializer {
    public static final String MOD_ID = "witchersbestiary";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override public void onInitialize() {
        com.witchersbestiary.config.ModConfig.initialize();
        ModEntities.initialize();
        com.witchersbestiary.world.graveyard.GraveyardStructure.initialize();
        com.witchersbestiary.world.graveyard.GraveyardHaunting.initialize();
        com.witchersbestiary.combat.ProgressiveDifficulty.initialize();
        ModBlocks.initialize();
        com.witchersbestiary.ward.SpiritWardBlock.initialize();
        ModEffects.initialize();
        ModLoot.initialize();
        ModDataComponents.initialize();
        com.witchersbestiary.decor.ModRemains.initialize();
        com.witchersbestiary.artifact.ModArtifacts.initialize();
        com.witchersbestiary.workshop.ModWorkshop.initialize();
        ModItems.initialize();
        com.witchersbestiary.config.ProgressiveConfigNetwork.initialize();
        com.witchersbestiary.lure.ModLures.initialize();
        com.witchersbestiary.compat.SimplySwordsCompat.initialize();
        DrownerSpawns.initialize();
        GhoulSpawns.initialize();
        com.witchersbestiary.accessory.HunterAccessories.initialize();
        com.witchersbestiary.artifact.Artifacts.initialize();
        com.witchersbestiary.alchemy.InventoryOils.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->com.witchersbestiary.alchemy.AlchemyTooltips.rebuild(s.getOverworld()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->com.witchersbestiary.combat.RootBinding.clear());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTicks()%100==0)com.witchersbestiary.combat.RootBinding.cleanup(s.getOverworld().getTime());});
        LOGGER.info("Witcher's Bestiary initialized: witchersbestiary:leshen");
    }
}
