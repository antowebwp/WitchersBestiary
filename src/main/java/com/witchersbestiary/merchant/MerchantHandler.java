package com.witchersbestiary.merchant;
import com.google.gson.*;import java.util.*;
import net.minecraft.entity.player.*;import net.minecraft.item.*;import net.minecraft.screen.ScreenHandler;import net.minecraft.server.network.ServerPlayerEntity;
public final class MerchantHandler extends ScreenHandler {
 private final ArcaneMerchant merchant;private final PlayerEntity owner;private final UUID token=UUID.randomUUID();private long sequence;private int lastAction=-100,lastSync=-100;
 public UUID session;public long nonce;public JsonObject snapshot=new JsonObject();private String message="";
 public MerchantHandler(int sync,PlayerInventory inv,int entity){super(ArcaneMerchants.SCREEN,sync);owner=inv.player;var e=owner.getWorld().getEntityById(entity);merchant=e instanceof ArcaneMerchant m?m:null;}
 public ArcaneMerchant merchant(){return merchant;}
 @Override public boolean canUse(PlayerEntity p){return p==owner&&p.isAlive()&&!p.isSpectator()&&merchant!=null&&merchant.isAlive()&&merchant.getWorld()==p.getWorld()&&p.squaredDistanceTo(merchant)<=36&&!com.witchersbestiary.fist.FistFights.busy(p);}
 @Override public ItemStack quickMove(PlayerEntity p,int index){return ItemStack.EMPTY;}
 public void buy(ServerPlayerEntity p,MerchantPackets.Buy packet){if(packet.sync()!=syncId||!token.equals(packet.session())||packet.sequence()!=sequence||p.currentScreenHandler!=this||!canUse(p))return;sequence++;if(p.getServer().getTicks()-lastAction<4){message="wait";send(p);return;}lastAction=p.getServer().getTicks();message=MerchantTransactions.purchase(p,merchant,packet.offer());send(p);p.playerScreenHandler.sendContentUpdates();}
 private void send(ServerPlayerEntity p){var j=new JsonObject();j.addProperty("balance",MerchantTransactions.balance(p));j.addProperty("mode",merchant.mode().name());j.addProperty("message",message);var rows=new JsonArray();for(var offer:merchant.offers()){var row=new JsonObject();row.addProperty("id",offer.id);row.addProperty("item",net.minecraft.registry.Registries.ITEM.getId(offer.result.getItem()).toString());row.addProperty("count",offer.result.getCount());row.addProperty("price",offer.price);row.addProperty("stock",offer.stock);rows.add(row);}j.add("offers",rows);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new MerchantPackets.State(syncId,token,sequence,j.toString()));}
 @Override public void sendContentUpdates(){super.sendContentUpdates();if(owner instanceof ServerPlayerEntity p){if(!canUse(p)){p.closeHandledScreen();return;}if(p.getServer().getTicks()-lastSync>=10){lastSync=p.getServer().getTicks();send(p);}}}
}
