package com.witchersbestiary.combat.mixin;
import org.spongepowered.asm.mixin.Mixin;import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.entity.Entity;
/** Exactly the real PlayerEntity.attack scope, including Better Combat's normal attack calls. */
@Mixin(PlayerEntity.class)
public abstract class SwordAttackMixin {
 @WrapMethod(method="attack")private void bestiary$swordAttack(Entity target,Operation<Void> original){var p=(PlayerEntity)(Object)this;if(p.getWorld().isClient){original.call(target);return;}var previous=com.witchersbestiary.enchantment.SwordEnchantments.begin(p);try{original.call(target);}finally{com.witchersbestiary.enchantment.SwordEnchantments.end(previous);}}
}
