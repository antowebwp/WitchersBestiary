package com.witchersbestiary.signs;
import com.witchersbestiary.combat.StageCombatBalance;
import com.witchersbestiary.equipment.armory.SchoolArmor;
import java.util.*;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.entity.*;import net.minecraft.entity.mob.*;import net.minecraft.entity.passive.*;import net.minecraft.entity.effect.*;import net.minecraft.entity.damage.*;import net.minecraft.registry.*;import net.minecraft.registry.tag.DamageTypeTags;import net.minecraft.util.*;import net.minecraft.util.math.*;import net.minecraft.particle.*;import net.minecraft.sound.*;import net.minecraft.text.Text;import net.fabricmc.fabric.api.networking.v1.*;import net.fabricmc.fabric.api.event.lifecycle.v1.*;
public final class SignManager {
 public static final RegistryKey<DamageType> DAMAGE=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary:sign"));
 public static final RegistryKey<DamageType> FIRE=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary:igni"));
 public static final net.minecraft.registry.tag.TagKey<EntityType<?>> CONTROL_IMMUNE=net.minecraft.registry.tag.TagKey.of(RegistryKeys.ENTITY_TYPE,Identifier.of("witchersbestiary:control_immune"));
 public static boolean signDamage(DamageSource source){return source.isOf(DAMAGE)||source.isOf(FIRE);}
 private record Burn(UUID owner,RegistryKey<net.minecraft.world.World> dimension,long until){}
 private static final Map<UUID,Burn> BURNS=new HashMap<>();
 public static DamageSource resolveBurn(LivingEntity victim,DamageSource source){if(!source.isOf(DamageTypes.ON_FIRE)||!(victim.getWorld() instanceof ServerWorld w))return source;var burn=BURNS.get(victim.getUuid());if(burn==null||burn.until<w.getTime())return source;var owner=w.getServer().getPlayerManager().getPlayer(burn.owner);if(owner==null||owner.getWorld()!=w||!allowed(owner)||!enemy(owner,victim)){victim.extinguish();BURNS.remove(victim.getUuid());return null;}return new DamageSource(w.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(FIRE),owner);}
 public static float burnDamage(LivingEntity victim, ServerPlayerEntity owner){return StageCombatBalance.burnDamage(victim,level(owner,SignType.IGNI,SignPlayerState.Upgrade.POWER));}
 private static void clearBurns(net.minecraft.server.MinecraftServer server,UUID owner){var it=BURNS.entrySet().iterator();while(it.hasNext()){var entry=it.next();var burn=entry.getValue();if(owner==null||owner.equals(burn.owner)){var w=server.getWorld(burn.dimension);if(w!=null&&w.getEntity(entry.getKey()) instanceof LivingEntity e)e.extinguish();it.remove();}}}
 private static final Map<UUID,Runtime> ACTIVE=new HashMap<>();private static final Map<UUID,Long> CALM=new HashMap<>(),EXPOSED=new HashMap<>();
 public static boolean yrdenExposed(LivingEntity e){return EXPOSED.getOrDefault(e.getUuid(),Long.MIN_VALUE)>e.getWorld().getTime();}
 private record Redirect(UUID owner,UUID target,long until){}
 private static final Map<UUID,Redirect> REDIRECTS=new HashMap<>();
 private static final class Runtime {UUID session=UUID.randomUUID();long sequence=-1;int requestWindow,requests;float shield;int shieldTicks,trapTicks;Vec3d trap;List<Vec3d> traps=List.of();int trapForm,shieldForm,breathTicks,breathSlot,breathLease;net.minecraft.item.ItemStack breathMain,breathOff;net.minecraft.item.Item breathMainType,breathOffType;int breathSelectedSlot;float trapDamageMultiplier=1,breathDamageMultiplier=1;RegistryKey<net.minecraft.world.World> dimension;}
 private static Runtime runtime(ServerPlayerEntity p){var r=ACTIVE.computeIfAbsent(p.getUuid(),k->new Runtime());var dimension=p.getWorld().getRegistryKey();if(r.dimension!=null&&r.dimension!=dimension){r.shield=0;r.shieldTicks=0;r.trapTicks=0;r.trap=null;r.breathTicks=0;clearBurns(p.getServer(),p.getUuid());}r.dimension=dimension;return r;}
 public static SignPlayerState state(ServerPlayerEntity p){return SignLedger.get(p.getServer()).player(p.getUuid());}
 public static boolean calmed(LivingEntity e){Long until=CALM.get(e.getUuid());return until!=null&&until>e.getWorld().getTime();}
 public static boolean allowed(ServerPlayerEntity p){return p.isAlive()&&!p.isSpectator()&&!com.witchersbestiary.fist.FistFights.busy(p);}
 /** Hostility is a relationship/classification, never a mod-specific target whitelist. */
 public static boolean enemy(ServerPlayerEntity p,LivingEntity e){
  if(e==p||!e.isAlive()||e.getWorld()!=p.getWorld()||e.isInvulnerable()||p.isTeammate(e)||e.isTeammate(p)||com.witchersbestiary.fist.FistFights.busy(e)||e instanceof net.minecraft.village.Merchant||e instanceof net.minecraft.entity.Ownable owned&&owned.getOwner() instanceof net.minecraft.entity.player.PlayerEntity||e instanceof Tameable pet&&pet.getOwnerUuid()!=null||e instanceof AbstractHorseEntity horse&&horse.isTame()||e instanceof net.minecraft.entity.decoration.ArmorStandEntity)return false;
  if(e instanceof net.minecraft.entity.player.PlayerEntity other)return !other.isCreative()&&!other.isSpectator()&&p.getServer().isPvpEnabled()&&p.shouldDamagePlayer(other);
  if(e instanceof PassiveEntity&&!(e instanceof Monster)&&e.getType().getSpawnGroup()!=SpawnGroup.MONSTER&&(!(e instanceof MobEntity mob)||mob.getTarget()!=p))return false;
  boolean provoked=e.getAttacker()==p||e instanceof MobEntity m&&m.getTarget()==p;
  if(e instanceof Angerable angry)return provoked||angry.shouldAngerAt(p);
  if(e instanceof PiglinEntity)return provoked;
  return provoked||e instanceof Monster||e.getType().getSpawnGroup()==SpawnGroup.MONSTER||e instanceof com.witchersbestiary.combat.CategorizedMonster;
 }
 public static boolean controlSupported(LivingEntity e){return e instanceof MobEntity m&&!m.isAiDisabled()&&!e.getType().isIn(CONTROL_IMMUNE );}
 private static boolean large(LivingEntity e){return e.getWidth()>1.5||e.getMaxHealth()>=200;}
 private record TrapHit(RegistryKey<net.minecraft.world.World> dimension,long next){}
 private static final Map<UUID,TrapHit> TRAP_HITS=new HashMap<>();

 public static void intent(ServerPlayerEntity p,SignPackets.Intent packet){var r=runtime(p);int tick=p.getServer().getTicks();if(tick-r.requestWindow>=20){r.requests=0;r.requestWindow=tick;}if(packet.action()!=7&&++r.requests>8)return;if(!p.getWorld().getRegistryKey().getValue().toString().equals(packet.dimension())||!r.session.equals(packet.session())||packet.sequence()<=r.sequence)return;r.sequence=packet.sequence();var sign=SignType.byIndex(packet.sign());if(packet.action()==0){if(sign!=null&&state(p).select(sign))SignLedger.get(p.getServer()).markDirty();}else if(packet.action()==1)cast(p,packet.sequence());else if(packet.action()==2&&sign!=null)learnFromBestiary(p,sign);
 else if(packet.action()==6)cast(p,packet.sequence(),1);
 else if(packet.action()==7)r.breathTicks=0;
 else if(packet.action()==8){if(r.breathTicks>0)r.breathLease=tick+16;return;}
 else if(packet.action()==9||packet.action()==10)startBreath(p,packet.action()-9);
 else if(packet.action()>=3&&packet.action()<=5&&p.isAlive()&&!p.isSpectator()&&hasBestiary(p)){
  int encoded=packet.sign();var branch=SignType.byIndex(encoded/16);int choice=encoded%16;var playerState=state(p);
  if(branch!=null&&encoded>=0){
   boolean changed=switch(packet.action()){
    case 3->choice<6&&playerState.upgrade(branch,SignPlayerState.Upgrade.values()[choice]);
    case 4->playerState.unlock(branch,choice);
    case 5->playerState.equip(branch,choice/4,choice%4);
    default->false;
   };
   if(changed)SignLedger.get(p.getServer()).markDirty();
  }
  syncMastery(p);
 }
 sync(p);}
 private static boolean hasBestiary(ServerPlayerEntity p){
  for(int i=0;i<p.getInventory().size();i++)if(p.getInventory().getStack(i).isOf(com.witchersbestiary.registry.ModItems.BESTIARY))return true;
  return false;
 }
 public static boolean learnFromBestiary(ServerPlayerEntity p,SignType sign){
  if(!hasBestiary(p)||!p.isAlive()||p.isSpectator())return false;
  if(state(p).learn(sign))SignLedger.get(p.getServer()).markDirty();sync(p);syncMastery(p);return true;
 }
 public static void awardContract(ServerPlayerEntity player){state(player).award(3);SignLedger.get(player.getServer()).markDirty();syncMastery(player);}
 private static void awardCombat(ServerPlayerEntity p,LivingEntity target){
  if(!(target instanceof MobEntity mob)||!enemy(p,target)||mob.isAiDisabled()||mob.getTarget()!=p&&p.getAttacker()!=target)return;
  if(!state(p).awardCombat(target.getUuid(),p.getServer().getOverworld().getTime()))return;
  SignLedger.get(p.getServer()).markDirty();syncMastery(p);
 }
 public static void syncMastery(ServerPlayerEntity p){
  if(!ServerPlayNetworking.canSend(p,SignPackets.Mastery.ID))return;
  var player=state(p);var values=new ArrayList<Integer>(45);
  for(var sign:SignType.values()){
   for(var node:SignPlayerState.Upgrade.values())values.add(player.level(sign,node));
   values.add(player.unlocked(sign));values.add(player.form(sign,0));values.add(player.form(sign,1));
  }
  ServerPlayNetworking.send(p,new SignPackets.Mastery(player.points(),values));
 }
 private static int level(ServerPlayerEntity p,SignType sign,SignPlayerState.Upgrade node){return state(p).level(sign,node);}
 private static double range(ServerPlayerEntity p,SignType sign){return (sign.range+.35*level(p,sign,SignPlayerState.Upgrade.RANGE))*SchoolArmor.signRange(p);}
 private static List<LivingEntity> targets(ServerPlayerEntity p,SignType sign,double radius,double angle,boolean ground){
  var origin=p.getEyePos();var look=p.getRotationVec(1);
  var found=p.getServerWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(radius),e->enemy(p,e)
   &&(sign!=SignType.AXII||controlSupported(e))&&(!ground||Math.abs(e.getY()-p.getY())<1.4)
   &&p.canSee(e)&&e.getEyePos().distanceTo(origin)<=radius
   &&e.getEyePos().subtract(origin).normalize().dotProduct(look)>=Math.cos(Math.toRadians(angle)));
  found.sort(Comparator.comparingDouble(p::squaredDistanceTo));return found;
 }
 public static boolean cast(ServerPlayerEntity p,long request){return cast(p,request,0);}
 public static boolean cast(ServerPlayerEntity p,long request,int slot){
  if(!allowed(p)){p.sendMessage(Text.translatable("sign.witchersbestiary.unavailable"),true);return false;}
  var player=state(p);var sign=player.selected();int form=player.form(sign,slot);var w=p.getServerWorld();var r=runtime(p);
  if(sign==SignType.IGNI&&form==2)return false;
  r.breathTicks=0;
  if(sign==SignType.QUEN&&r.shieldTicks>0&&r.shield>0)return false;
  double angle=form==1&&(sign==SignType.IGNI||sign==SignType.AARD)?180:sign==SignType.AXII&&(form==1||form==3)?65:halfAngle(sign);
  var targets=sign==SignType.QUEN||sign==SignType.YRDEN?List.<LivingEntity>of():targets(p,sign,range(p,sign),angle,sign==SignType.IGNI&&form==3);
  if(sign==SignType.AXII){targets.removeIf(e->CALM.getOrDefault(e.getUuid(),Long.MIN_VALUE)+80>w.getTime()||REDIRECTS.containsKey(e.getUuid()));if(targets.isEmpty())return false;}
  Vec3d trap=p.getPos();
  if(sign==SignType.YRDEN&&!validTrap(w,trap))return false;
  if(!player.commit(sign,request,form))return false;
  SignLedger.get(p.getServer()).markDirty();r.dimension=w.getRegistryKey();p.swingHand(Hand.OFF_HAND,true);
  float combo=targets.isEmpty()&&sign!=SignType.QUEN&&sign!=SignType.YRDEN ? 1 : SchoolArmor.signCombo(p);
  int duration=level(p,sign,SignPlayerState.Upgrade.DURATION);
  int power=level(p,sign,SignPlayerState.Upgrade.POWER),effect=level(p,sign,SignPlayerState.Upgrade.EFFECT);
  switch(sign){
   case IGNI,AARD->{
    float multiplier=SchoolArmor.signDamage(p)*combo;
    if(sign==SignType.IGNI){
     var eye=p.getEyePos();
     com.witchersbestiary.alchemy.DragonGas.ignite(p,eye,eye.add(p.getRotationVec(1).multiply(range(p,sign))),angle);
    }
    for(var target:targets.stream().limit(12).toList())hit(p,target,sign,form,StageCombatBalance.signDamage(target,sign,form,power,multiplier),duration,effect);
    var origin=sign==SignType.IGNI&&form==3?p.getPos().add(0,.2,0):p.getEyePos();
    visual(w,p.getId(),sign,form,9,origin,origin.add(p.getRotationVec(1).multiply(range(p,sign))),
        form==1?180:sign==SignType.IGNI?37.5f:45);
   }
   case QUEN->{r.shield=(12+power*2)*combo;r.shieldTicks=240+duration*20;r.shieldForm=form;syncQuen(p,form,12);}
   case YRDEN->{
    r.trap=trap;r.trapForm=form;r.trapDamageMultiplier=SchoolArmor.signDamage(p)*combo;r.trapTicks=Math.round((200+duration*20)*combo);
    var centers=new ArrayList<Vec3d>();centers.add(trap);
    if(form==2)for(int side:new int[]{-1,1}){var edge=trap.add(p.getRotationVec(1).multiply(1,0,1).normalize().rotateY((float)(side*Math.PI/2)).multiply(2.7));if(validTrap(w,edge))centers.add(edge);}
    r.traps=List.copyOf(centers);
    float radius=(float)trapRadius(p,r.trapForm);
    for(var center:r.traps)visual(w,p.getId(),sign,form,12,center,center,radius);
   }
   case AXII->{
    int count=form==0||form==2?1:Math.min(6,targets.size());
    for(int i=0;i<count;i++){
     var target=(MobEntity)targets.get(i);awardCombat(p,target);
     if(target instanceof com.witchersbestiary.entity.monster.HigherVampireEntity vampire){
      vampire.interruptCombat(9);
      visual(w,target.getId(),SignType.AXII,form,9,p.getEyePos(),target.getEyePos(),.42f);
      continue;
     }
     long until=w.getTime()+(large(target)?30:Math.round((sign.duration+duration*15+effect*6)*combo));
     MobEntity redirected=null;
     if((form==2||form==3)&&!large(target)){
      final int index=i;
      redirected=targets.stream().skip(index+1).filter(MobEntity.class::isInstance).map(MobEntity.class::cast).findFirst().orElseGet(()->
       w.getEntitiesByClass(MobEntity.class,target.getBoundingBox().expand(8),other->other!=target&&enemy(p,other)&&target.canSee(other)).stream().findFirst().orElse(null));
     }
     if(redirected!=null){REDIRECTS.put(target.getUuid(),new Redirect(p.getUuid(),redirected.getUuid(),until));target.setTarget(redirected);}
     else {CALM.put(target.getUuid(),until);target.setTarget(null);target.getNavigation().stop();}
     visual(w,target.getId(),SignType.AXII,form,(int)(until-w.getTime()),p.getEyePos(),target.getEyePos(),.42f);
    }
   }
  }
  w.playSound(null,p.getBlockPos(),switch(sign){case IGNI->SoundEvents.ITEM_FIRECHARGE_USE;case AARD->SoundEvents.ENTITY_BREEZE_SHOOT;case QUEN->SoundEvents.ITEM_SHIELD_BLOCK;case YRDEN->SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE;case AXII->SoundEvents.BLOCK_BEACON_AMBIENT;},SoundCategory.PLAYERS,.45f,1.15f);
  sync(p);return true;
 }
 private static boolean validTrap(ServerWorld world,Vec3d point){
  var pos=BlockPos.ofFloored(point);return world.isChunkLoaded(pos)&&world.getBlockState(pos).getCollisionShape(world,pos).isEmpty()
   &&world.getBlockState(pos.down()).isSideSolidFullSquare(world,pos.down(),Direction.UP);
 }
 private static DamageSource source(ServerPlayerEntity p,SignType sign){return new DamageSource(p.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(sign==SignType.IGNI?FIRE:DAMAGE),p);}
 private static void hit(ServerPlayerEntity p,LivingEntity target,SignType sign,int form,float damage,int duration,int effect){
  if(sign==SignType.IGNI&&(target.isFireImmune()||target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)))return;
  if(!target.damage(source(p,sign),damage))return;
  awardCombat(p,target);
  burst(p.getServerWorld(),target.getPos().add(0,target.getHeight()*.6,0),sign,4);
  if(sign==SignType.IGNI){int seconds=3+(duration+effect)/2;target.setOnFireFor(seconds);BURNS.put(target.getUuid(),new Burn(p.getUuid(),p.getWorld().getRegistryKey(),p.getWorld().getTime()+seconds*20));}
  else {
   if(target instanceof com.witchersbestiary.entity.monster.HigherVampireEntity vampire)vampire.interruptCombat(6);
   if(target instanceof com.witchersbestiary.entity.monster.GriffinEntity griffin)griffin.interruptDive();
   double force=(form==2?1.45:.85)*(large(target)?.3:1)*(1+.1*effect);
   if(form==3){var pull=p.getPos().subtract(target.getPos()).multiply(1,0,1).normalize().multiply(force);target.addVelocity(pull.x,.1,pull.z);}
   else target.takeKnockback(force,p.getX()-target.getX(),p.getZ()-target.getZ());
   target.velocityModified=true;
  }
 }
 private static double halfAngle(SignType sign){return switch(sign){case IGNI->37.5;case AARD->45;case AXII->12;default->30;};}
 private static void castWave(ServerWorld world,Vec3d origin,Vec3d forward,SignType sign,int form,double radius){
  boolean fire=sign==SignType.IGNI;
  var core=dust(sign,fire?.82f:.64f);
  var flow=fire?ParticleTypes.SMALL_FLAME:ParticleTypes.POOF;
  int spokes=form==1?16:9;
  if(form==1)forward=forward.multiply(1,0,1).normalize();
  for(int spoke=0;spoke<spokes;spoke++){
   double angle=form==1?360.0*spoke/spokes:halfAngle(sign)*(2.0*spoke/(spokes-1)-1);
   var ray=forward.rotateY((float)Math.toRadians(angle));
   for(int band=1;band<=3;band++){
    var at=origin.add(ray.multiply(radius*(band*.28+.16)));
    var pos=BlockPos.ofFloored(at);
    if(!world.isChunkLoaded(pos)||world.raycast(new net.minecraft.world.RaycastContext(origin,at,
        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,
        net.minecraft.block.ShapeContext.absent())).getType()!=net.minecraft.util.hit.HitResult.Type.MISS)break;
    if(band==3||spoke%2==0)world.spawnParticles(core,at.x,at.y,at.z,1,0,0,0,0);
    if(band==2&&spoke%2==0){
     var motion=ray.multiply(!fire&&form==3?-.20:!fire&&form==2?.32:.16);
     world.spawnParticles(flow,at.x,at.y,at.z,0,motion.x,motion.y+.02,motion.z,1);
    }
   }
  }
 }
 private static void axiiThreads(ServerWorld world,Vec3d origin,Vec3d eye,int form){
  var core=dust(SignType.AXII,.65f);
  var direction=eye.subtract(origin);
  var side=direction.crossProduct(new Vec3d(0,1,0)).normalize();
  int points=form==0?6:form==1?4:8;
  for(int i=1;i<=points;i++){
   double progress=i/(double)points;
   var at=origin.add(direction.multiply(progress)).add(side.multiply(Math.sin(progress*Math.PI*2)*.18));
   world.spawnParticles(core,at.x,at.y,at.z,1,0,0,0,0);
  }
  for(int i=0;i<8;i++){
   double angle=i*Math.PI/4;
   var at=eye.add(.42*Math.cos(angle),.05+i*.053,.42*Math.sin(angle));
   world.spawnParticles(core,at.x,at.y,at.z,1,0,0,0,0);
  }
 }
 private static DustParticleEffect dust(SignType sign,float size){
  int rgb=sign.color;
  return new DustParticleEffect(new org.joml.Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f),size);
 }
 private static void syncQuen(ServerPlayerEntity player,int form,int ticks){
  var packet=new SignPackets.QuenShell(player.getId(),form,ticks);
  if(ServerPlayNetworking.canSend(player,SignPackets.QuenShell.ID))ServerPlayNetworking.send(player,packet);
  for(var observer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player))
   if(observer!=player&&observer.squaredDistanceTo(player)<=40*40&&ServerPlayNetworking.canSend(observer,SignPackets.QuenShell.ID))
    ServerPlayNetworking.send(observer,packet);
 }
 private static void burst(ServerWorld w,Vec3d at,SignType sign,int count){
  w.spawnParticles(dust(sign,.9f),at.x,at.y,at.z,count,.45,.5,.45,.01);
 }
 public static boolean permitsRedirectedAttack(LivingEntity attacker,LivingEntity victim){
  var redirect=REDIRECTS.get(attacker.getUuid());
  return redirect==null||redirect.until<=attacker.getWorld().getTime()||redirect.target.equals(victim.getUuid());
 }
 public static float protect(LivingEntity victim,DamageSource damage,float amount){
  if(!(victim instanceof ServerPlayerEntity p)||!allowed(p)||!Float.isFinite(amount)||amount<=0||p.isInvulnerableTo(damage)
   ||p.blockedByShield(damage)||damage.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
  var owner=p;var shield=runtime(p);
  if(shield.shieldTicks<=0||shield.shield<=0){
   for(var ally:p.getServerWorld().getPlayers()){
    if(ally==p||!p.isTeammate(ally)||!allowed(ally)||p.squaredDistanceTo(ally)>16||!ally.canSee(p))continue;
    var candidate=runtime(ally);
    if(candidate.shieldForm==3&&candidate.shieldTicks>0&&candidate.shield>0){owner=ally;shield=candidate;break;}
   }
  }
  if(shield.shieldTicks<=0||shield.shield<=0)return amount;
  float taken=Math.min(shield.shield,amount);shield.shield-=taken;
  if(shield.shield<=0)syncQuen(owner,shield.shieldForm,0);
  var world=owner.getServerWorld();
  var contact=p.getPos().add(0,1,0);
  var direction=damage.getAttacker()==null?contact:damage.getAttacker().getPos().add(0,1,0);
  int reaction=shield.shield<=0&&shield.shieldForm==1?1:shield.shieldForm==2&&!signDamage(damage)?2:0;
  visual(world,p.getId(),SignType.QUEN,4+reaction,10,contact,direction,3);
  if(damage.getAttacker() instanceof LivingEntity attacker&&enemy(owner,attacker)&&owner.canSee(attacker)){
   awardCombat(owner,attacker);
   if(shield.shieldForm==2&&!signDamage(damage)&&owner.squaredDistanceTo(attacker)<=64)
    attacker.damage(source(owner,SignType.QUEN),StageCombatBalance.signDamage(attacker,SignType.QUEN,2,level(owner,SignType.QUEN,SignPlayerState.Upgrade.EFFECT),1));
  }
  if(shield.shieldForm==1&&shield.shield<=0&&!signDamage(damage))
   for(var target:targets(owner,SignType.QUEN,3+.35*level(owner,SignType.QUEN,SignPlayerState.Upgrade.RANGE),180,false).stream().limit(12).toList())
    target.damage(source(owner,SignType.QUEN),StageCombatBalance.signDamage(target,SignType.QUEN,1,level(owner,SignType.QUEN,SignPlayerState.Upgrade.EFFECT),1));
  sync(owner);return amount-taken;
 }
 public static void sync(ServerPlayerEntity p){if(!ServerPlayNetworking.canSend(p,SignPackets.View.ID))return;var s=state(p);var r=runtime(p);ServerPlayNetworking.send(p,new SignPackets.View(r.session,s.learned(),s.selected().ordinal(),(float)s.concentration(),s.cooldown(s.selected()),r.shield,r.shieldTicks,r.trapTicks,s.cooldown(SignType.IGNI),s.cooldown(SignType.AARD),s.cooldown(SignType.QUEN),s.cooldown(SignType.YRDEN),s.cooldown(SignType.AXII)));}
 private static double trapRadius(ServerPlayerEntity player,int form){
  return ((form==2?1.85:3)+.35*level(player,SignType.YRDEN,SignPlayerState.Upgrade.RANGE))*SchoolArmor.signRange(player);
 }
 private static void tickTrap(ServerPlayerEntity player,Runtime runtime){
  var world=player.getServerWorld();double radius=trapRadius(player,runtime.trapForm);
  int effect=level(player,SignType.YRDEN,SignPlayerState.Upgrade.EFFECT);
  for(var center:runtime.traps){
   for(var target:world.getEntitiesByClass(LivingEntity.class,new Box(center.add(-radius,-1,-radius),center.add(radius,3,radius)),
      e->enemy(player,e)&&e.squaredDistanceTo(center)<=radius*radius&&player.canSee(e)).stream().limit(16).toList()){
    if(!target.getType().isIn(CONTROL_IMMUNE))target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,15,large(target)?0:1+(runtime.trapForm==1?1:0)+(effect>=3?1:0)));
    var hit=TRAP_HITS.get(target.getUuid());
    if(hit==null||hit.dimension()!=world.getRegistryKey()||hit.next()<=world.getTime()){
     TRAP_HITS.put(target.getUuid(),new TrapHit(world.getRegistryKey(),world.getTime()+40));
     if(target.damage(source(player,SignType.YRDEN),StageCombatBalance.signDamage(target,SignType.YRDEN,runtime.trapForm,level(player,SignType.YRDEN,SignPlayerState.Upgrade.POWER),runtime.trapDamageMultiplier))){
      awardCombat(player,target);
      var impact=target.getPos().add(0,.55,0);
      visual(world,target.getId(),SignType.YRDEN,4+runtime.trapForm,10,impact,impact,1.3f);
     }
    }
    if(target instanceof com.witchersbestiary.combat.CategorizedMonster monster&&monster.getMonsterCategory()==com.witchersbestiary.combat.MonsterCategory.SPECTER)EXPOSED.put(target.getUuid(),world.getTime()+15);
   }
  }
 }
 private static void updateRedirects(net.minecraft.server.MinecraftServer server){
  REDIRECTS.entrySet().removeIf(entry->{
   var redirect=entry.getValue();var owner=server.getPlayerManager().getPlayer(redirect.owner());
   var world=owner==null?null:owner.getServerWorld();
   if(world==null||!(world.getEntity(entry.getKey()) instanceof MobEntity mob))return true;
   if(!allowed(owner)||world.getTime()>=redirect.until()||!(world.getEntity(redirect.target()) instanceof LivingEntity target)||!enemy(owner,target)){
    mob.setTarget(null);return true;
   }
   mob.setTarget(target);return false;
  });
 }
 private static void visual(ServerWorld world,int owner,SignType sign,int form,int ticks,Vec3d from,Vec3d to,float radius){
  var packet=new SignPackets.Visual(owner,sign.ordinal(),form,ticks,from,to,radius);
  for(var viewer:world.getPlayers(p->p.squaredDistanceTo(from)<48*48))
   if(ServerPlayNetworking.canSend(viewer,SignPackets.Visual.ID))ServerPlayNetworking.send(viewer,packet);
 }

 private static void startBreath(ServerPlayerEntity p,int slot){
  var r=runtime(p);var state=state(p);
  if(r.breathTicks>0||!allowed(p)||p.currentScreenHandler!=p.playerScreenHandler
     ||state.selected()!=SignType.IGNI||!state.knows(SignType.IGNI)||state.form(SignType.IGNI,slot)!=2
     ||state.cooldown(SignType.IGNI)>0||state.concentration()<.25)return;
  r.breathTicks=1;r.breathSlot=slot;r.breathLease=p.getServer().getTicks()+16;
  r.breathMain=p.getMainHandStack();r.breathOff=p.getOffHandStack();
  r.breathMainType=r.breathMain.getItem();r.breathOffType=r.breathOff.getItem();
  r.breathSelectedSlot=p.getInventory().selectedSlot;
  r.breathDamageMultiplier=SchoolArmor.signDamage(p)*SchoolArmor.signCombo(p);
 }

 private static void tickBreath(ServerPlayerEntity p,Runtime r){
  if(r.breathTicks==0)return;
  var state=state(p);
  if(!allowed(p)||p.currentScreenHandler!=p.playerScreenHandler||state.selected()!=SignType.IGNI
     ||state.form(SignType.IGNI,r.breathSlot)!=2||p.getServer().getTicks()>=r.breathLease
     ||p.getInventory().selectedSlot!=r.breathSelectedSlot||p.getMainHandStack()!=r.breathMain
     ||p.getOffHandStack()!=r.breathOff||p.getMainHandStack().getItem()!=r.breathMainType
     ||p.getOffHandStack().getItem()!=r.breathOffType||!state.spendBreathTick()){
   r.breathTicks=0;return;
  }
  int elapsed=r.breathTicks++;
  if(elapsed%2!=1)return;
  var world=p.getServerWorld();
  var look=p.getRotationVec(1);var eye=p.getEyePos().add(0,-.12,0);
  var end=clipBreath(p,eye,eye.add(look.multiply(range(p,SignType.IGNI))));
  var start=eye.add(look.multiply(Math.min(.35,eye.distanceTo(end))));
  double length=start.distanceTo(end);
  visual(world,p.getId(),SignType.IGNI,2,4,start,end,.45f);
  if(elapsed%20==1)world.playSound(null,p.getBlockPos(),SoundEvents.ITEM_FIRECHARGE_USE,SoundCategory.PLAYERS,.35f,.68f);
  com.witchersbestiary.alchemy.DragonGas.ignite(p,start,end,0);
  if(elapsed%10!=1||length<.01)return;
  int power=level(p,SignType.IGNI,SignPlayerState.Upgrade.POWER);
  for(var target:world.getEntitiesByClass(LivingEntity.class,new Box(start,end).expand(.45),
      e->enemy(p,e)&&breathContact(p,e,start,end)).stream().limit(12).toList())
   hit(p,target,SignType.IGNI,2,StageCombatBalance.signDamage(target,SignType.IGNI,2,power,r.breathDamageMultiplier),0,0);
 }

 private static Vec3d clipBreath(ServerPlayerEntity p,Vec3d start,Vec3d end){
  return p.getServerWorld().raycast(new net.minecraft.world.RaycastContext(start,end,
      net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,p)).getPos();
 }

 private static boolean breathContact(ServerPlayerEntity p,LivingEntity target,Vec3d start,Vec3d end){
  var box=target.getBoundingBox();
  var expanded=box.expand(.45);
  var entry=expanded.contains(start)?start:expanded.raycast(start,end).orElse(null);
  if(entry==null)return false;
  var contact=new Vec3d(Math.clamp(entry.x,box.minX,box.maxX),Math.clamp(entry.y,box.minY,box.maxY),
      Math.clamp(entry.z,box.minZ,box.maxZ));
  if(contact.subtract(end).dotProduct(end.subtract(start))>1e-8)return false;
  return clipBreath(p,start,contact).squaredDistanceTo(contact)<1e-8;
 }

 private static void tick(net.minecraft.server.MinecraftServer server){
  for(var player:server.getPlayerManager().getPlayerList()){
   var runtime=runtime(player);state(player).tick(1+.15*SchoolArmor.fraction(player,SchoolArmor.School.GRIFFIN));
   if(!allowed(player)){if(runtime.shield>0)syncQuen(player,runtime.shieldForm,0);runtime.shield=0;runtime.shieldTicks=0;runtime.trapTicks=0;runtime.breathTicks=0;}
   if(runtime.shieldTicks>0){
    if(--runtime.shieldTicks==0){
     if(runtime.shield>0)syncQuen(player,runtime.shieldForm,0);
     runtime.shield=0;
     var at=player.getPos().add(0,1,0);
     visual(player.getServerWorld(),player.getId(),SignType.QUEN,4,8,at,at,1.2f);
    }else if(runtime.shieldTicks%8==0&&runtime.shield>0){
     syncQuen(player,runtime.shieldForm,Math.min(runtime.shieldTicks,12));
    }
   }
   if(runtime.trapTicks>0){
    runtime.trapTicks--;
    if(server.getTicks()%10==0) {
     tickTrap(player,runtime);
     float radius=(float)(trapRadius(player,runtime.trapForm));
     for(var center:runtime.traps)visual(player.getServerWorld(),player.getId(),SignType.YRDEN,runtime.trapForm,
         Math.min(runtime.trapTicks,12),center,center,radius);
    }
   }
   tickBreath(player,runtime);
   if(server.getTicks()%10==0)sync(player);
  }
  if(!REDIRECTS.isEmpty())updateRedirects(server);
  if(server.getTicks()%20==0){
   SignLedger.get(server).markDirty();
   TRAP_HITS.entrySet().removeIf(e->{var world=server.getWorld(e.getValue().dimension());return world==null||e.getValue().next()+40<world.getTime();});
   BURNS.entrySet().removeIf(e->{var world=server.getWorld(e.getValue().dimension());return world==null||e.getValue().until<world.getTime();});
   EXPOSED.entrySet().removeIf(e->e.getValue()<server.getOverworld().getTime());
   CALM.entrySet().removeIf(e->e.getValue()+80<server.getOverworld().getTime());
  }
 }
 public static void initialize(){
  SignPackets.initialize();
  SignSkillPointCommands.initialize();
  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{ACTIVE.remove(newPlayer.getUuid());clearBurns(newPlayer.getServer(),newPlayer.getUuid());state(newPlayer).resetProtocol();sync(newPlayer);syncMastery(newPlayer);});
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof ServerPlayerEntity player){ACTIVE.remove(player.getUuid());clearBurns(player.getServer(),player.getUuid());}});
  ServerPlayConnectionEvents.JOIN.register((h,sender,server)->{state(h.player).resetProtocol();ACTIVE.remove(h.player.getUuid());sync(h.player);syncMastery(h.player);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{ACTIVE.remove(h.player.getUuid());clearBurns(s,h.player.getUuid());SignLedger.get(s).markDirty();});
  ServerLifecycleEvents.SERVER_STOPPING.register(server->clearBurns(server,null));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.clear();CALM.clear();EXPOSED.clear();BURNS.clear();TRAP_HITS.clear();REDIRECTS.clear();});
  ServerTickEvents.END_SERVER_TICK.register(SignManager::tick);
 }
}
