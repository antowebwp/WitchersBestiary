package com.witchersbestiary.accessory;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.monster.*;
import com.witchersbestiary.registry.ModItems;

import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import java.util.*;

public final class HunterAccessories {
    private static final Map<UUID,Long> REACTION=new HashMap<>(), LIFESTEAL=new HashMap<>();
    private static java.util.function.BiPredicate<LivingEntity,Item> EQUIPPED=(e,i)->false;
    private HunterAccessories(){}
    public static boolean equipped(LivingEntity entity,Item item){return EQUIPPED.test(entity,item);}
    public static boolean bestiaryMonster(Entity e){return e instanceof CategorizedMonster||e instanceof LeshenWolfEntity;}
    public static float protect(LivingEntity victim,DamageSource source,float amount){
        return !victim.getWorld().isClient&&victim instanceof PlayerEntity&&bestiaryMonster(source.getAttacker())&&equipped(victim,ModItems.HUNTER_MEDALLION)?amount*.95f:amount;
    }
    public static void afterDamage(LivingEntity victim,DamageSource source,float actualLoss){
        com.witchersbestiary.artifact.Artifacts.afterDamage(victim,source,actualLoss);
        if(victim.getWorld().isClient||!Float.isFinite(actualLoss)||actualLoss<=0||!(victim instanceof HostileEntity||victim instanceof InfernalBasiliskEntity basilisk&&!basilisk.isTamed())||victim instanceof PlayerEntity
                ||source.getSource()!=source.getAttacker()||!(source.getAttacker() instanceof PlayerEntity p)||!source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK)
                ||!equipped(p,ModItems.VAMPIRE_FANG)||victim instanceof FogletEntity f&&f.isIllusion()||victim instanceof NoonwraithEntity n&&n.isIllusion()||victim instanceof KatakanEntity k&&k.isBatForm()||victim instanceof WerewolfEntity w&&w.isHumanForm())return;
        if(p.getHealth()>=p.getMaxHealth())return;
        var reserve=(VampireBloodReserve)victim;
        float healed=Math.min(Math.min(2,actualLoss*.1f),Math.min(4-reserve.bestiary$bloodDrawn(),p.getMaxHealth()-p.getHealth()));
        long time=p.getWorld().getTime();if(healed<=0||time<LIFESTEAL.getOrDefault(p.getUuid(),0L))return;
        LIFESTEAL.put(p.getUuid(),time+20);reserve.bestiary$bloodDrawn(reserve.bestiary$bloodDrawn()+healed);p.heal(healed);
    }
    /** This nested class is resolved only after the loader confirms the optional mod. */
    private static final class OptionalTrinkets {
      static void initialize(){
        EQUIPPED=(e,i)->dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(e).map(c->c.isEquipped(i)).orElse(false);
        dev.emi.trinkets.api.TrinketsApi.registerTrinket(ModItems.HUNTER_MEDALLION,new dev.emi.trinkets.api.Trinket(){
            @Override public void onUnequip(net.minecraft.item.ItemStack stack,dev.emi.trinkets.api.SlotReference slot,LivingEntity entity){stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);}
            @Override public void tick(net.minecraft.item.ItemStack stack,dev.emi.trinkets.api.SlotReference slot,LivingEntity entity){
                if(!(entity instanceof PlayerEntity p)||!(p.getWorld() instanceof ServerWorld world))return;
                if(world.getTime()>=REACTION.getOrDefault(p.getUuid(),0L)-80 && stack.getOrDefault(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,false)){stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);slot.inventory().markDirty();}
                if(p.age%20!=0)return;
                long time=world.getTime();if(time<REACTION.getOrDefault(p.getUuid(),0L))return;
                if(world.getOtherEntities(p,p.getBoundingBox().expand(16),e->e.isAlive()&&bestiaryMonster(e)&&!(e instanceof InfernalBasiliskEntity basilisk&&basilisk.isTamed()&&basilisk.isOwner(p))&&e.squaredDistanceTo(p)<=256).isEmpty())return;
                REACTION.put(p.getUuid(),time+100);stack.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,true);slot.inventory().markDirty();
                world.playSound(null,p.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,.3f,1.6f);
                world.spawnParticles(ParticleTypes.END_ROD,p.getX(),p.getY()+1.35,p.getZ(),4,.2,.1,.2,.01);
            }
        });
        dev.emi.trinkets.api.TrinketsApi.registerTrinket(ModItems.VAMPIRE_FANG,new dev.emi.trinkets.api.Trinket(){});

        for(Item item:com.witchersbestiary.artifact.ModArtifacts.ALL)dev.emi.trinkets.api.TrinketsApi.registerTrinket(item,new dev.emi.trinkets.api.Trinket(){});
      }
    }
    public static void initialize(){
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets"))OptionalTrinkets.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{REACTION.clear();LIFESTEAL.clear();});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->{REACTION.remove(handler.player.getUuid());LIFESTEAL.remove(handler.player.getUuid());});
    }
}
