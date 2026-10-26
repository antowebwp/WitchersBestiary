package com.witchersbestiary.combat;

import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.monster.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/** Per-instance persistent modifier; no entity scans, global attributes or damage recursion. */
public final class ProgressiveDifficulty {
    public static final Identifier HEALTH=Identifier.of("witchersbestiary","progressive_health");
    private ProgressiveDifficulty(){}
    public static void initialize(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e,w)->{
            if(e instanceof LivingEntity living && e instanceof CategorizedMonster)refresh(living);
        });
    }
    public static boolean eligible(LivingEntity e){
        if(!(e instanceof CategorizedMonster)||!Registries.ENTITY_TYPE.getId(e.getType()).getNamespace().equals("witchersbestiary"))return false;
        if(!(e instanceof net.minecraft.entity.mob.HostileEntity) && !(e instanceof InfernalBasiliskEntity))return false;
        if(e instanceof TameableEntity pet && pet.isTamed())return false;
        if(e instanceof Tameable pet && pet.getOwnerUuid()!=null)return false;
        if(e instanceof WerewolfEntity w && w.isHumanForm())return false;
        if(e instanceof FogletEntity f && f.isIllusion())return false;
        if(e instanceof NoonwraithEntity w && w.isIllusion())return false;
        return !ModConfig.progressive().excluded().contains(Registries.ENTITY_TYPE.getId(e.getType()).toString());
    }
    public static long steps(LivingEntity e){
        if(!(e.getWorld() instanceof ServerWorld w)||!eligible(e))return 0;
        return ModConfig.progressive().steps(w.getServer().getOverworld().getTimeOfDay()/24000L);
    }
    public static void tick(LivingEntity e){
        if(!e.getWorld().isClient && e instanceof CategorizedMonster && (e.age+e.getId())%20==0)refresh(e);
    }
    public static void refresh(LivingEntity e){refresh(e,steps(e));}
    /** Explicit phase also permits deterministic tests without changing shared world clocks. */
    public static void refresh(LivingEntity e,long phase){
        if(e.getWorld().isClient || !(e instanceof CategorizedMonster))return;
        var attribute=e.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(attribute==null)return;
        // The bat's 8HP decoy pool is intentionally fixed; its beast pool retains progression.
        boolean active=ModConfig.progressive().enabled()&&eligible(e)&&!(e instanceof KatakanEntity k&&k.isBatForm());
        double base=attribute.getBaseValue();
        boolean elite=ModConfig.progressive().elites().contains(Registries.ENTITY_TYPE.getId(e.getType()).toString());
        double bonus=active?ModConfig.progressive().health(base,phase,elite)-base:0;
        var old=attribute.getModifier(HEALTH);
        boolean needsModifier=bonus>0 || active&&base>1024;
        if(old!=null && needsModifier && Double.compare(old.value(),bonus)==0)return;
        if(old!=null)attribute.removeModifier(HEALTH);
        if(needsModifier)attribute.addPersistentModifier(new EntityAttributeModifier(HEALTH,bonus,EntityAttributeModifier.Operation.ADD_VALUE));
        // Recalculation may clamp a reduced maximum, but never heals an existing monster.
        if(e.getHealth()>e.getMaxHealth())e.setHealth(e.getMaxHealth());
    }
    /** Called only by MobEntity.initialize for fresh spawns, never by chunk/NBT loading. */
    public static void freshSpawn(LivingEntity e){
        if(!(e instanceof CategorizedMonster))return;
        boolean full=e.getHealth()>=e.getMaxHealth();refresh(e);if(full)e.setHealth(e.getMaxHealth());
    }
    public static float damage(DamageSource source,float amount){
        return source.getAttacker() instanceof LivingEntity attacker?(float)(amount*ModConfig.progressive().damage(steps(attacker))):amount;
    }
    /** Armor points only: toughness, enchantments, Resistance and downstream hooks remain intact. */
    public static float armor(DamageSource source,float armor){
        return source.getAttacker() instanceof LivingEntity attacker?(float)(armor*(1-ModConfig.progressive().bypass(steps(attacker)))):armor;
    }
}
