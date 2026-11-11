package com.witchersbestiary.equipment.armory;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.fist.FistFights;
import com.witchersbestiary.registry.ModItems;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class SchoolArmor extends ArmorItem {
    public enum School {
        GRIFFIN(12,0), BEAR(30,5), CAT(8,20);

        public final int monsterReduction, swordDamage;

        School(int monsterReduction,int swordDamage){
            this.monsterReduction=monsterReduction;
            this.swordDamage=swordDamage;
        }
    }
    public static final int SIGN_RANGE=15, SIGN_DAMAGE=25, SIGN_COMBO=20, COMBO_CASTS=4;
    public static final int CAT_DODGE=12, CAT_REPRISAL=25, CAT_READY=100, CAT_WINDOW=120;
    public static final int BEAR_HIT=8, BEAR_GUARD=35, BEAR_READY=160, BEAR_WINDOW=80;
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Map<ServerPlayerEntity, State> STATES=new WeakHashMap<>();
    private static final class State {
        int casts;
        long bearReady, bearUntil, catReady, catUntil;
    }
    private final School school;
    private final int tier;
    private SchoolArmor(School school, net.minecraft.registry.entry.RegistryEntry<ArmorMaterial> material, Type type, int tier){
        super(material,type,new Item.Settings().maxDamage(type.getMaxDamage(19+tier*6)));
        this.school=school;
        this.tier=tier;
    }

    public int tier(){return tier;}

    public School school(){return school;}

    public static void initialize(){
        var armorItems=new ArrayList<Item>();
        for(var school:School.values())for(int tier=0;tier<3;tier++){
            var prefix=school.name().toLowerCase(Locale.ROOT)+"_"+new String[]{"","enhanced_","mastercrafted_"}[tier];
            var id=Identifier.of("witchersbestiary",prefix+"armor");
            int chest=switch(school){case CAT->5;case GRIFFIN->6;case BEAR->7;};
            var material=Registry.registerReference(Registries.ARMOR_MATERIAL,id,new ArmorMaterial(
                Map.of(Type.HELMET,2,Type.CHESTPLATE,chest,Type.LEGGINGS,chest-1,Type.BOOTS,2,Type.BODY,0),
                15,SoundEvents.ITEM_ARMOR_EQUIP_IRON,()->Ingredient.ofItems(ModItems.GRAVEBOUND_STEEL_FRAGMENT),
                List.of(new ArmorMaterial.Layer(id)),school==School.BEAR?2+tier*.5f:tier*.5f,
                school==School.BEAR?.025f:0));
            for(var type:List.of(Type.HELMET,Type.CHESTPLATE,Type.LEGGINGS,Type.BOOTS))
                armorItems.add(Registry.register(Registries.ITEM,Identifier.of("witchersbestiary",prefix+type.getName()),new SchoolArmor(school,material,type,tier)));
        }
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
            .register(entries->armorItems.forEach(entries::add));
    }

    public static int pieces(ServerPlayerEntity player,School school){
        if(FistFights.busy(player))return 0;
        return equippedPieces(player,school);
    }

    public static int equippedPieces(LivingEntity player,School school){
        int count=0;
        for(var slot:SLOTS)if(player.getEquippedStack(slot).getItem() instanceof SchoolArmor armor&&armor.school==school)count++;
        return count;
    }

    public static float fraction(ServerPlayerEntity player,School school){
        return fraction(pieces(player,school));
    }

    public static float fraction(int pieces){
        return switch(pieces){case 2->.4f;case 3->.7f;case 4->1;default->0;};
    }

    public static float averageTier(LivingEntity player, School school){
        int total=0, count=0;
        for(var slot:SLOTS){
            if(player.getEquippedStack(slot).getItem() instanceof SchoolArmor armor && armor.school==school){
                total+=armor.tier;
                count++;
            }
        }
        return count==0?0:(float)total/count;
    }

    public static float monsterReduction(School school,float tier){
        return school.monsterReduction+(school==School.BEAR?5*tier:0);
    }

    public static float swordDamage(School school,float tier){
        return school.swordDamage+(school==School.CAT?5*tier:0);
    }

    public static float signDamageBonus(float tier){return SIGN_DAMAGE+10*tier;}
    public static float comboBonus(float tier){return SIGN_COMBO+5*tier;}
    public static float guardBonus(float tier){return BEAR_GUARD+5*tier;}
    public static float dodgeChance(float tier){return CAT_DODGE+3*tier;}

    public static float signRange(ServerPlayerEntity p){return 1+(SIGN_RANGE/100f)*fraction(p,School.GRIFFIN);}
    public static float signDamage(ServerPlayerEntity p){return 1+(signDamageBonus(averageTier(p,School.GRIFFIN))/100f)*fraction(p,School.GRIFFIN);}
    public static float signCombo(ServerPlayerEntity p){
        var state=STATES.computeIfAbsent(p,key->new State());
        if(pieces(p,School.GRIFFIN)!=4){state.casts=0;return 1;}
        if(++state.casts<COMBO_CASTS)return 1;
        state.casts=0;
        return 1+comboBonus(averageTier(p,School.GRIFFIN))/100f;
    }

    public static float damage(LivingEntity victim, DamageSource source,float amount){
        if(amount<=0||!Float.isFinite(amount)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(victim instanceof ServerPlayerEntity p&&source.getAttacker() instanceof CategorizedMonster monster
            && (!(monster instanceof Tameable pet)||pet.getOwnerUuid()==null)){
            var state=STATES.computeIfAbsent(p,key->new State());
            long now=p.getWorld().getTime();
            if(pieces(p,School.CAT)==4&&now>=state.catReady&&!p.blockedByShield(source)
                &&p.timeUntilRegen<=10&&p.getRandom().nextFloat()<dodgeChance(averageTier(p,School.CAT))/100f){
                state.catReady=now+CAT_READY;state.catUntil=now+CAT_WINDOW;return 0;
            }
            boolean bear=pieces(p,School.BEAR)==4;
            if(bear&&state.bearUntil>now&&!p.blockedByShield(source)&&p.timeUntilRegen<=10){state.bearUntil=0;amount*=1-guardBonus(averageTier(p,School.BEAR))/100f;}
            else if(bear&&amount>=BEAR_HIT&&now>=state.bearReady&&!p.blockedByShield(source)&&p.timeUntilRegen<=10){
                state.bearReady=now+BEAR_READY;state.bearUntil=now+BEAR_WINDOW;
            }
            return amount*(1-(School.GRIFFIN.monsterReduction/100f)*fraction(p,School.GRIFFIN)-(monsterReduction(School.BEAR,averageTier(p,School.BEAR))/100f)*fraction(p,School.BEAR)-(School.CAT.monsterReduction/100f)*fraction(p,School.CAT));
        }
        if(victim instanceof CategorizedMonster&&(!(victim instanceof Tameable pet)||pet.getOwnerUuid()==null)
            &&source.getAttacker() instanceof ServerPlayerEntity p
            &&source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK)&&p.getMainHandStack().getItem() instanceof SwordItem){
            amount*=1+(School.BEAR.swordDamage/100f)*fraction(p,School.BEAR)+(swordDamage(School.CAT,averageTier(p,School.CAT))/100f)*fraction(p,School.CAT);
            var state=STATES.get(p);
            if(state!=null&&pieces(p,School.CAT)==4&&state.catUntil>p.getWorld().getTime()&&p.canSee(victim)
                &&!victim.blockedByShield(source)&&victim.timeUntilRegen<=10){
                state.catUntil=0;amount*=1+CAT_REPRISAL/100f;
            }
        }
        return amount;
    }
}
