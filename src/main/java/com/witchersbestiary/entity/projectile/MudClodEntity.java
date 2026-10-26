package com.witchersbestiary.entity.projectile;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.*;
import net.minecraft.world.World;

/** Vanilla swept projectile/block collision, one impact, bounded lifetime. */
public final class MudClodEntity extends ThrownItemEntity {
    private int life=80;
    public MudClodEntity(EntityType<? extends ThrownItemEntity> type,World w){super(type,w);}
    public MudClodEntity(World w,LivingEntity owner){super(ModEntities.MUD_CLOD,owner,w);}
    @Override protected Item getDefaultItem(){return ModItems.MUD_CLOD;}
    @Override protected void onEntityHit(EntityHitResult hit){
        super.onEntityHit(hit);
        if(!getWorld().isClient&&hit.getEntity() instanceof LivingEntity victim&&getOwner() instanceof LivingEntity owner&&victim!=owner){
            if(victim.damage(getDamageSources().thrown(this,owner),4)){
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,80,0),owner);
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,30,0),owner);
            }
        }
    }
    @Override protected void onCollision(HitResult hit){
        super.onCollision(hit);if(getWorld() instanceof ServerWorld w){
            w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,net.minecraft.block.Blocks.MUD.getDefaultState()),getX(),getY(),getZ(),10,.18,.18,.18,.05);
            playSound(SoundEvents.BLOCK_MUD_BREAK,.55f,.75f);discard();
        }
    }
    @Override public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound n){super.writeCustomDataToNbt(n);n.putInt("MudLife",life);}
    @Override public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound n){super.readCustomDataFromNbt(n);life=n.contains("MudLife")?Math.clamp(n.getInt("MudLife"),1,80):80;}
    @Override public void tick(){super.tick();if(!getWorld().isClient&&--life<=0)discard();}
}
