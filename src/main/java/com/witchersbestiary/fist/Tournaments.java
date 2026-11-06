package com.witchersbestiary.fist;
import java.util.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.text.Text;
public final class Tournaments {
 private record Bout(FistNpc npc,ServerWorld world,BlockPos arena){}
 private record Menu(UUID token,net.minecraft.entity.LivingEntity organizer){}
 private static final Map<UUID,Bout> bouts=new HashMap<>();
 private static final Map<UUID,Menu> menus=new HashMap<>();
 public static boolean opponent(FistNpc n){return bouts.values().stream().anyMatch(b->b.npc==n);}
 public static void open(ServerPlayerEntity p,net.minecraft.entity.LivingEntity n){if(FistFights.busy(p)||FistFights.busy(n)||(n instanceof FistNpc f&&opponent(f))||p.squaredDistanceTo(n)>36)return;menus.put(p.getUuid(),new Menu(UUID.randomUUID(),n));send(p,true);}
 public static String hud(ServerPlayerEntity p){var e=TournamentProgress.get(p.getServer()).entry(p.getUuid());return bouts.containsKey(p.getUuid())&&e.active!=null?Text.translatable("tournament.witchersbestiary."+e.active.name().toLowerCase(Locale.ROOT)).getString()+" "+(e.round+1)+"/"+e.active.rounds+" — ":"";}
 public static void send(ServerPlayerEntity p,boolean open){var m=menus.get(p.getUuid());if(m==null)return;var e=TournamentProgress.get(p.getServer()).entry(p.getUuid());TournamentPackets.send(p,new TournamentPackets.View(m.token,e.titles,e.active==null?-1:e.active.ordinal(),e.round,e.fighting,open,e.lastCategory,e.lastRound,e.lastWon));}
 public static void request(ServerPlayerEntity p,TournamentPackets.Control packet){var m=menus.get(p.getUuid());if(m==null||!m.token.equals(packet.token()))return;menus.put(p.getUuid(),new Menu(UUID.randomUUID(),m.organizer));
  if(packet.action()==4){if(FistFights.active(p)!=null)FistFights.cancel(p);TournamentProgress.get(p.getServer()).abandon(p.getUuid());send(p,true);return;}
  if(m.organizer.isRemoved()||m.organizer.getWorld()!=p.getWorld()||p.squaredDistanceTo(m.organizer)>64||FistFights.busy(p)||FistFights.busy(m.organizer))return;
  var ledger=TournamentProgress.get(p.getServer());if(packet.action()>=0&&packet.action()<3)ledger.enter(p.getUuid(),TournamentProgress.Category.values()[packet.action()]);
  else if(packet.action()==3)start(p,m.organizer);send(p,!ledger.entry(p.getUuid()).fighting);
 }
 public static boolean safe(ServerWorld w,BlockPos center){
  if(!w.isChunkLoaded(center)||!w.isSkyVisible(center))return false;
  for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var at=center.add(x,0,z);if(!w.isChunkLoaded(at)||!w.getWorldBorder().contains(at)||!w.getBlockState(at.down()).isFullCube(w,at.down())||!w.getBlockState(at.down()).getFluidState().isEmpty()||w.getBlockState(at.down()).isOf(net.minecraft.block.Blocks.MAGMA_BLOCK))return false;
   for(int y=0;y<3;y++){var state=w.getBlockState(at.up(y));if(!state.getCollisionShape(w,at.up(y)).isEmpty()||!state.getFluidState().isEmpty()||state.isOf(net.minecraft.block.Blocks.FIRE)||state.isOf(net.minecraft.block.Blocks.SOUL_FIRE)||state.isOf(net.minecraft.block.Blocks.WITHER_ROSE)||state.isOf(net.minecraft.block.Blocks.POWDER_SNOW))return false;}
  }
  return w.getOtherEntities(null,new Box(center).expand(3,2,3),e->e instanceof net.minecraft.entity.LivingEntity).isEmpty();
 }
 public static boolean start(ServerPlayerEntity p,net.minecraft.entity.LivingEntity organizer){var ledger=TournamentProgress.get(p.getServer());var e=ledger.entry(p.getUuid());if(e.active==null||e.fighting||bouts.containsKey(p.getUuid())||FistFights.busy(p)||!p.getMainHandStack().isEmpty()||!p.getOffHandStack().isEmpty()){p.sendMessage(Text.translatable("tournament.witchersbestiary.empty"),false);return false;}
  var w=(ServerWorld)p.getWorld();BlockPos chosen=null;for(int radius=8;radius<=32&&chosen==null;radius+=4)for(int dir=0;dir<8;dir++){double angle=dir*Math.PI/4;var at=organizer.getBlockPos().add((int)(Math.cos(angle)*radius),0,(int)(Math.sin(angle)*radius));for(int dy=-3;dy<=3;dy++){var candidate=at.up(dy);if(bouts.values().stream().anyMatch(b->b.world==w&&b.arena.getSquaredDistance(candidate)<625))continue;if(safe(w,candidate)){chosen=candidate;break;}}if(chosen!=null)break;}
  if(chosen==null){p.sendMessage(Text.translatable("tournament.witchersbestiary.no_arena"),false);return false;}
  int tier=e.active.tier(e.round);var type=tier==0?FistRegistry.NOVICE:tier==1?FistRegistry.EXPERIENCED:FistRegistry.CHAMPION;var npc=type.create(w);if(npc==null)return false;npc.refreshPositionAndAngles(chosen.getX()+.5,chosen.getY(),chosen.getZ()+.5,0,0);
  if(!w.spawnEntity(npc))return false;npc.addCommandTag("wb_tournament_opponent");if(!ledger.begin(p.getUuid())){npc.discard();return false;}bouts.put(p.getUuid(),new Bout(npc,w,chosen));p.teleport(w,chosen.getX()+.5,chosen.getY(),chosen.getZ()+2.5,180,0);FistFights.invite(p,npc);if(!FistFights.confirm(p,npc)){bouts.remove(p.getUuid());ledger.abandon(p.getUuid());npc.discard();return false;}send(p,false);return true;
 }
 public static boolean ended(FistFights.Active a,boolean normal,boolean won){var p=a.player();var b=bouts.remove(p.getUuid());if(b==null||b.npc!=a.npc())return false;var ledger=TournamentProgress.get(p.getServer());ledger.finish(p.getUuid(),normal&&won,p.getServer().getOverworld().getTime(),FistRewards.get(p.getServer()));FistRewards.get(p.getServer()).claim(p);b.npc.discard();p.sendMessage(Text.translatable(normal&&won?"tournament.witchersbestiary.round_win":"tournament.witchersbestiary.defeat"),false);send(p,true);return true;}
 public static void initialize(){TournamentPackets.initialize();net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->menus.remove(h.player.getUuid()));net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var p:s.getPlayerManager().getPlayerList())if(bouts.containsKey(p.getUuid()))FistFights.cancel(p);});net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{bouts.clear();menus.clear();});net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,w)->{if(entity.getCommandTags().contains("wb_tournament_opponent")&&entity instanceof FistNpc npc&&!opponent(npc))entity.discard();});}
}
