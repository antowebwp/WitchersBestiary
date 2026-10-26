package com.witchersbestiary.client;

import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.client.render.entity.DrownedEntityRenderer;

public final class WitchersBestiaryClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        com.witchersbestiary.client.config.ProgressiveConfigClient.initialize();
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(com.witchersbestiary.workshop.ModWorkshop.ALCHEMY_SCREEN,com.witchersbestiary.client.workshop.AlchemyScreen::new);
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(context->context.addModels(com.witchersbestiary.workshop.ModWorkshop.id("block/witcher_chest_lid")));
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(com.witchersbestiary.workshop.ModWorkshop.URN_SCREEN,com.witchersbestiary.client.workshop.WorkshopContainerScreen::new);
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(com.witchersbestiary.workshop.ModWorkshop.EQUIPMENT_SCREEN,com.witchersbestiary.client.workshop.WorkshopContainerScreen::new);
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(com.witchersbestiary.workshop.ModWorkshop.INVENTORY,com.witchersbestiary.client.workshop.WorkshopRenderer::new);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(com.witchersbestiary.decor.ModRemains.MONSTER_ORGAN_JAR,net.minecraft.client.render.RenderLayer.getTranslucent());
        EntityRendererRegistry.register(ModEntities.GRAVE_HAG,GraveHagRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATER_HAG,WaterHagRenderer::new);
        EntityRendererRegistry.register(ModEntities.MUD_CLOD,context->new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(context,.8f,false));
        EntityRendererRegistry.register(ModEntities.INFERNAL_BASILISK,InfernalBasiliskRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((c,w)->{if(w!=null)com.witchersbestiary.alchemy.AlchemyTooltips.rebuild(w);});
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type,renderer,helper,context)->{if(renderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer player)helper.register(new RootFeatureRenderer(player));});
        EntityRendererRegistry.register(ModEntities.LESHEN, LeshenRenderer::new);
        EntityRendererRegistry.register(ModEntities.LESHEN_WOLF, WolfEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DROWNER,DrownerRenderer::new);
        EntityRendererRegistry.register(ModEntities.GHOUL, GhoulRenderer::new);
        EntityRendererRegistry.register(ModEntities.FOGLET, FogletRenderer::new);
        EntityRendererRegistry.register(ModEntities.NOONWRAITH, NoonwraithRenderer::new);
        EntityRendererRegistry.register(ModEntities.WRAITH,WraithRenderer::new);
        EntityRendererRegistry.register(ModEntities.ENDREGA_WARRIOR,EndregaWarriorRenderer::new);
        EntityRendererRegistry.register(ModEntities.FIEND,FiendRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRIFFIN,GriffinRenderer::new);
        EntityRendererRegistry.register(ModEntities.NEKKER,NekkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.BONECRUSHER,BonecrusherRenderer::new);
        EntityRendererRegistry.register(ModEntities.KATAKAN,KatakanRenderer::new);
        EntityRendererRegistry.register(ModEntities.WEREWOLF, WerewolfRenderer::new);
    }
}
