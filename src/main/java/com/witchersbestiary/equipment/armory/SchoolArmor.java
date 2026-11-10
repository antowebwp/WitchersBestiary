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
    public enum School { GRIFFIN, BEAR, CAT }
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
        int count=0;
        for(var slot:SLOTS)if(player.getEquippedStack(slot).getItem() instanceof SchoolArmor armor&&armor.school==school)count++;
        return count;
    }

    public static float fraction(ServerPlayerEntity player,School school){
        return switch(pieces(player,school)){case 2->.4f;case 3->.7f;case 4->1;default->0;};
    }

    public static float signRange(ServerPlayerEntity p){return 1+.15f*fraction(p,School.GRIFFIN);}
    public static float signDamage(ServerPlayerEntity p){return 1+.25f*fraction(p,School.GRIFFIN);}
    public static float signCombo(ServerPlayerEntity p){
        var state=STATES.computeIfAbsent(p,key->new State());
        if(pieces(p,School.GRIFFIN)!=4){state.casts=0;return 1;}
        if(++state.casts<4)return 1;
        state.casts=0;
        return 1.2f;
    }

    public static float damage(LivingEntity victim, DamageSource source,float amount){
        if(amount<=0||!Float.isFinite(amount)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(victim instanceof ServerPlayerEntity p&&source.getAttacker() instanceof CategorizedMonster monster
            && (!(monster instanceof Tameable pet)||pet.getOwnerUuid()==null)){
            var state=STATES.computeIfAbsent(p,key->new State());
            long now=p.getWorld().getTime();
            if(pieces(p,School.CAT)==4&&now>=state.catReady&&!p.blockedByShield(source)
                &&p.timeUntilRegen<=10&&p.getRandom().nextFloat()<.12f){
                state.catReady=now+100;state.catUntil=now+120;return 0;
            }
            boolean bear=pieces(p,School.BEAR)==4;
            if(bear&&state.bearUntil>now&&!p.blockedByShield(source)&&p.timeUntilRegen<=10){state.bearUntil=0;amount*=.65f;}
            else if(bear&&amount>=8&&now>=state.bearReady&&!p.blockedByShield(source)&&p.timeUntilRegen<=10){
                state.bearReady=now+160;state.bearUntil=now+80;
            }
            return amount*(1-.12f*fraction(p,School.GRIFFIN)-.30f*fraction(p,School.BEAR)-.08f*fraction(p,School.CAT));
        }
        if(victim instanceof CategorizedMonster&&(!(victim instanceof Tameable pet)||pet.getOwnerUuid()==null)
            &&source.getAttacker() instanceof ServerPlayerEntity p
            &&source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK)&&p.getMainHandStack().getItem() instanceof SwordItem){
            amount*=1+.05f*fraction(p,School.BEAR)+.20f*fraction(p,School.CAT);
            var state=STATES.get(p);
            if(state!=null&&pieces(p,School.CAT)==4&&state.catUntil>p.getWorld().getTime()&&p.canSee(victim)
                &&!victim.blockedByShield(source)&&victim.timeUntilRegen<=10){
                state.catUntil=0;amount*=1.25f;
            }
        }
        return amount;
    }
}
