package com.witchersbestiary.combat.mixin;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Invalidate after RecipeManager is updated, including server datapack reloads.
 * Kept in the client-only config/refmap: Loom compiles split source sets separately. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class RecipeCatalogReloadMixin {
 @Inject(method="onSynchronizeRecipes(Lnet/minecraft/network/packet/s2c/play/SynchronizeRecipesS2CPacket;)V",at=@At("TAIL"))
 private void bestiary$recipes(SynchronizeRecipesS2CPacket packet,CallbackInfo ci){com.witchersbestiary.client.book.AlchemyCatalogComponent.invalidate();}
}
