package com.witchersbestiary.fist;
import net.fabricmc.fabric.api.event.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
/** Server protection integration point. A veto at either start or contact always fails closed. */
public final class DuelPermissions {
 @FunctionalInterface public interface Allow {boolean allow(ServerPlayerEntity attacker,ServerPlayerEntity defender);}
 public static final Event<Allow> ALLOW=EventFactory.createArrayBacked(Allow.class,callbacks->(a,b)->{for(var callback:callbacks)if(!callback.allow(a,b))return false;return true;});
 public static boolean allowed(ServerPlayerEntity a,ServerPlayerEntity b){if(a.getServerWorld()!=b.getServerWorld()||!a.getServer().isPvpEnabled())return false;try{return ALLOW.invoker().allow(a,b)&&ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(b,a.getServerWorld().getDamageSources().playerAttack(a),0);}catch(RuntimeException failure){org.slf4j.LoggerFactory.getLogger("witchersbestiary").warn("Protection callback failed; refusing fist duel",failure);return false;}}
}
