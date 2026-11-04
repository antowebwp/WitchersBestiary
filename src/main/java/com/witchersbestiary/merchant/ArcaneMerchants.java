package com.witchersbestiary.merchant;
import net.minecraft.entity.*;import net.minecraft.entity.attribute.EntityAttributes;import net.minecraft.item.*;import net.minecraft.registry.*;import net.minecraft.util.Identifier;
public final class ArcaneMerchants {
 public static final Identifier ID=Identifier.of("witchersbestiary:arcane_merchant");
 public static final EntityType<ArcaneMerchant> TYPE=Registry.register(Registries.ENTITY_TYPE,ID,EntityType.Builder.<ArcaneMerchant>create(ArcaneMerchant::new,SpawnGroup.CREATURE).dimensions(.65f,1.95f).maxTrackingRange(8).build(ID.toString()));
 public static final net.minecraft.screen.ScreenHandlerType<MerchantHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,ID,new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(MerchantHandler::new,net.minecraft.network.codec.PacketCodecs.VAR_INT));
 public static void initialize(){net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,net.minecraft.entity.mob.MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,30).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.25).add(EntityAttributes.GENERIC_FOLLOW_RANGE,16));var egg=Registry.register(Registries.ITEM,Identifier.of("witchersbestiary:arcane_merchant_spawn_egg"),new SpawnEggItem(TYPE,0x392c24,0xb19a6b,new Item.Settings()));net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e->e.add(egg));MerchantPackets.initialize();MerchantSpawns.initialize();}
}
