package com.witchersbestiary.signs;
import java.util.*;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.entity.*;import net.minecraft.entity.mob.*;import net.minecraft.entity.passive.*;import net.minecraft.entity.effect.*;import net.minecraft.entity.damage.*;import net.minecraft.registry.*;import net.minecraft.registry.tag.DamageTypeTags;import net.minecraft.util.*;import net.minecraft.util.math.*;import net.minecraft.particle.*;import net.minecraft.sound.*;import net.minecraft.text.Text;import net.fabricmc.fabric.api.networking.v1.*;import net.fabricmc.fabric.api.event.lifecycle.v1.*;
public final class SignManager {
 public static final RegistryKey<DamageType> DAMAGE=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary:sign"));
 public static final RegistryKey<DamageType> FIRE=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary:igni"));
 public static final net.minecraft.registry.tag.TagKey<EntityType<?>> CONTROL_IMMUNE=net.minecraft.registry.tag.TagKey.of(RegistryKeys.ENTITY_TYPE,Identifier.of("witchersbestiary:control_immune"));
 public static boolean signDamage(DamageSource source){return source.isOf(DAMAGE)||source.isOf(FIRE);}
 private record Burn(UUID owner,RegistryKey<net.minecraft.world.World> dimension,long until){}
 private static final Map<UUID,Burn> BURNS=new HashMap<>();
 public static DamageSource resolveBurn(LivingEntity victim,DamageSource source){if(!source.isOf(DamageTypes.ON_FIRE)||!(victim.getWorld() instanceof ServerWorld w))return source;var burn=BURNS.get(victim.getUuid());if(burn==null||burn.until<w.getTime())return source;var owner=w.getServer().getPlayerManager().getPlayer(burn.owner);if(owner==null||owner.getWorld()!=w||!allowed(owner)||!enemy(owner,victim)){victim.extinguish();BURNS.remove(victim.getUuid());return null;}return new DamageSource(w.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(FIRE),owner);}
 private static void clearBurns(net.minecraft.server.MinecraftServer server,UUID owner){var it=BURNS.entrySet().iterator();while(it.hasNext()){var entry=it.next();var burn=entry.getValue();if(owner==null||owner.equals(burn.owner)){var w=server.getWorld(burn.dimension);if(w!=null&&w.getEntity(entry.getKey()) instanceof LivingEntity e)e.extinguish();it.remove();}}}
 private static final Map<UUID,Runtime> ACTIVE=new HashMap<>();private static final Map<UUID,Long> CALM=new HashMap<>(),EXPOSED=new HashMap<>();
 public static boolean yrdenExposed(LivingEntity e){return EXPOSED.getOrDefault(e.getUuid(),Long.MIN_VALUE)>e.getWorld().getTime();}
 private record Redirect(UUID owner,UUID target,long until){}
 private static final Map<UUID,Redirect> REDIRECTS=new HashMap<>();
 private static final class Runtime {UUID session=UUID.randomUUID();long sequence=-1;int requestWindow,requests;float shield;int shieldTicks,trapTicks;Vec3d trap;List<Vec3d> traps=List.of();int trapForm,shieldForm,breathTicks;RegistryKey<net.minecraft.world.World> dimension;}
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

 public static void intent(ServerPlayerEntity p,SignPackets.Intent packet){var r=runtime(p);int tick=p.getServer().getTicks();if(tick-r.requestWindow>=20){r.requests=0;r.requestWindow=tick;}if(++r.requests>8)return;if(!p.getWorld().getRegistryKey().getValue().toString().equals(packet.dimension())||!r.session.equals(packet.session())||packet.sequence()<=r.sequence)return;r.sequence=packet.sequence();var sign=SignType.byIndex(packet.sign());if(packet.action()==0){if(sign!=null&&state(p).select(sign))SignLedger.get(p.getServer()).markDirty();}else if(packet.action()==1)cast(p,packet.sequence());else if(packet.action()==2&&sign!=null)learnFromBestiary(p,sign);
 else if(packet.action()==6)cast(p,packet.sequence(),1);
 else if(packet.action()==7)r.breathTicks=0;
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
 private static double range(ServerPlayerEntity p,SignType sign){return sign.range+.35*level(p,sign,SignPlayerState.Upgrade.RANGE);}
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
  if(sign==SignType.QUEN&&r.shieldTicks>0&&r.shield>0)return false;
  double angle=form==1&&(sign==SignType.IGNI||sign==SignType.AARD)?180:sign==SignType.AXII&&(form==1||form==3)?65:halfAngle(sign);
  var targets=sign==SignType.QUEN||sign==SignType.YRDEN?List.<LivingEntity>of():targets(p,sign,range(p,sign),angle,sign==SignType.IGNI&&form==3);
  if(sign==SignType.AXII){targets.removeIf(e->CALM.getOrDefault(e.getUuid(),Long.MIN_VALUE)+80>w.getTime()||REDIRECTS.containsKey(e.getUuid()));if(targets.isEmpty())return false;}
  Vec3d trap=p.getPos();
  if(sign==SignType.YRDEN&&!validTrap(w,trap))return false;
  if(!player.commit(sign,request,form))return false;
  SignLedger.get(p.getServer()).markDirty();r.dimension=w.getRegistryKey();p.swingHand(Hand.OFF_HAND,true);
  int duration=level(p,sign,SignPlayerState.Upgrade.DURATION);
  int power=level(p,sign,SignPlayerState.Upgrade.POWER),effect=level(p,sign,SignPlayerState.Upgrade.EFFECT);
  switch(sign){
   case IGNI,AARD->{
    float damage=(sign==SignType.IGNI?6.24f:1.44f)*(1+.1f*power);
    if(sign==SignType.IGNI&&form==2){r.breathTicks=24+duration*4;damage*=.42f;}
    for(var target:targets.stream().limit(12).toList())hit(p,target,sign,form,damage,duration,effect);
    if(form==1)for(int i=0;i<16;i++){double a=i*Math.PI/8;burst(w,p.getPos().add(2*Math.cos(a),.65,2*Math.sin(a)),sign,1);}
    else coneParticles(w,sign==SignType.IGNI&&form==3?p.getPos().add(0,.2,0):p.getEyePos(),p.getRotationVec(1),sign);
   }
   case QUEN->{r.shield=12+power*2;r.shieldTicks=240+duration*20;r.shieldForm=form;burst(w,p.getPos().add(0,1,0),sign,16);}
   case YRDEN->{
    r.trap=trap;r.trapForm=form;r.trapTicks=200+duration*20;
    var centers=new ArrayList<Vec3d>();centers.add(trap);
    if(form==2)for(int side:new int[]{-1,1}){var edge=trap.add(p.getRotationVec(1).multiply(1,0,1).normalize().rotateY((float)(side*Math.PI/2)).multiply(2.7));if(validTrap(w,edge))centers.add(edge);}
    r.traps=List.copyOf(centers);
   }
   case AXII->{
    int count=form==0||form==2?1:Math.min(6,targets.size());
    for(int i=0;i<count;i++){
     var target=(MobEntity)targets.get(i);awardCombat(p,target);
     long until=w.getTime()+(large(target)?30:sign.duration+duration*15+effect*6);
     MobEntity redirected=null;
     if((form==2||form==3)&&!large(target)){
      final int index=i;
      redirected=targets.stream().skip(index+1).filter(MobEntity.class::isInstance).map(MobEntity.class::cast).findFirst().orElseGet(()->
       w.getEntitiesByClass(MobEntity.class,target.getBoundingBox().expand(8),other->other!=target&&enemy(p,other)&&target.canSee(other)).stream().findFirst().orElse(null));
     }
     if(redirected!=null){REDIRECTS.put(target.getUuid(),new Redirect(p.getUuid(),redirected.getUuid(),until));target.setTarget(redirected);}
     else {CALM.put(target.getUuid(),until);target.setTarget(null);target.getNavigation().stop();}
     burst(w,target.getEyePos(),sign,6);
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
  if(sign==SignType.IGNI){int seconds=3+(duration+effect)/2;target.setOnFireFor(seconds);BURNS.put(target.getUuid(),new Burn(p.getUuid(),p.getWorld().getRegistryKey(),p.getWorld().getTime()+seconds*20));}
  else {
   double force=(form==2?1.45:.85)*(large(target)?.3:1)*(1+.1*effect);
   if(form==3){var pull=p.getPos().subtract(target.getPos()).multiply(1,0,1).normalize().multiply(force);target.addVelocity(pull.x,.1,pull.z);}
   else target.takeKnockback(force,p.getX()-target.getX(),p.getZ()-target.getZ());
   target.velocityModified=true;
  }
 }
 private static double halfAngle(SignType sign){return switch(sign){case IGNI->37.5;case AARD->45;case AXII->12;default->30;};}
 private static void coneParticles(ServerWorld world,Vec3d origin,Vec3d forward,SignType sign){
  var particle=sign==SignType.IGNI?ParticleTypes.FLAME:ParticleTypes.CLOUD;
  for(int spoke=-3;spoke<=3;spoke++){
   var ray=forward.rotateY((float)Math.toRadians(halfAngle(sign)*spoke/3));
   for(int step=1;step<=5;step++){
    var at=origin.add(ray.multiply(sign.range*step/5));
    if(!world.getBlockState(BlockPos.ofFloored(at)).getCollisionShape(world,BlockPos.ofFloored(at)).isEmpty())break;
    world.spawnParticles(particle,at.x,at.y,at.z,1,.06,.09,.06,.006);
   }
  }
 }
 private static void yrdenCircle(ServerWorld world,Vec3d center,int ticks,double radius){
  var dust=new DustParticleEffect(new org.joml.Vector3f(.78f,.31f,1),ticks%20==0?1.35f:1.1f);
  for(int i=0;i<36;i++){
   double angle=i*Math.PI/18;
   world.spawnParticles(dust,center.x+Math.cos(angle)*radius,center.y+.075,center.z+Math.sin(angle)*radius,1,0,0,0,0);
  }
  // Four short chevrons point from the boundary into the circle.
  for(int quadrant=0;quadrant<4;quadrant++){
   double angle=quadrant*Math.PI/2;
   for(int n=-2;n<=2;n++){
    var point=new Vec3d(n*.15,.09,radius*.67+Math.abs(n)*.17).rotateY((float)angle).add(center);
    world.spawnParticles(dust,point.x,point.y,point.z,1,0,0,0,0);
   }
  }
 }
 private static void burst(ServerWorld w,Vec3d at,SignType sign,int count){int rgb=sign.color;w.spawnParticles(new DustParticleEffect(new org.joml.Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f),.9f),at.x,at.y,at.z,count,.45,.5,.45,.01);}
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
  var world=owner.getServerWorld();burst(world,p.getPos().add(0,1,0),SignType.QUEN,shield.shield<=0?12:4);
  if(damage.getAttacker() instanceof LivingEntity attacker&&enemy(owner,attacker)&&owner.canSee(attacker)){
   awardCombat(owner,attacker);
   if(shield.shieldForm==2&&!signDamage(damage)&&owner.squaredDistanceTo(attacker)<=64)
    attacker.damage(source(owner,SignType.QUEN),taken*(.35f+.05f*level(owner,SignType.QUEN,SignPlayerState.Upgrade.EFFECT)));
  }
  if(shield.shieldForm==1&&shield.shield<=0&&!signDamage(damage))
   for(var target:targets(owner,SignType.QUEN,3+.35*level(owner,SignType.QUEN,SignPlayerState.Upgrade.RANGE),180,false).stream().limit(12).toList())
    target.damage(source(owner,SignType.QUEN),4+level(owner,SignType.QUEN,SignPlayerState.Upgrade.EFFECT));
  sync(owner);return amount-taken;
 }
 public static void sync(ServerPlayerEntity p){if(!ServerPlayNetworking.canSend(p,SignPackets.View.ID))return;var s=state(p);var r=runtime(p);ServerPlayNetworking.send(p,new SignPackets.View(r.session,s.learned(),s.selected().ordinal(),(float)s.concentration(),s.cooldown(s.selected()),r.shield,r.shieldTicks,r.trapTicks,s.cooldown(SignType.IGNI),s.cooldown(SignType.AARD),s.cooldown(SignType.QUEN),s.cooldown(SignType.YRDEN),s.cooldown(SignType.AXII)));}
 private static void tickTrap(ServerPlayerEntity player,Runtime runtime){
  var world=player.getServerWorld();double radius=(runtime.trapForm==2?1.85:3)+.35*level(player,SignType.YRDEN,SignPlayerState.Upgrade.RANGE);
  int effect=level(player,SignType.YRDEN,SignPlayerState.Upgrade.EFFECT);
  for(var center:runtime.traps){
   for(var target:world.getEntitiesByClass(LivingEntity.class,new Box(center.add(-radius,-1,-radius),center.add(radius,3,radius)),
      e->enemy(player,e)&&e.squaredDistanceTo(center)<=radius*radius&&player.canSee(e)).stream().limit(16).toList()){
    if(!target.getType().isIn(CONTROL_IMMUNE))target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,15,large(target)?0:1+(runtime.trapForm==1?1:0)+(effect>=3?1:0)));
    var hit=TRAP_HITS.get(target.getUuid());
    if(hit==null||hit.dimension()!=world.getRegistryKey()||hit.next()<=world.getTime()){
     TRAP_HITS.put(target.getUuid(),new TrapHit(world.getRegistryKey(),world.getTime()+40));
     if(target.damage(source(player,SignType.YRDEN),(runtime.trapForm==3?2:1)+.25f*level(player,SignType.YRDEN,SignPlayerState.Upgrade.POWER)))awardCombat(player,target);
    }
    if(target instanceof com.witchersbestiary.combat.CategorizedMonster monster&&monster.getMonsterCategory()==com.witchersbestiary.combat.MonsterCategory.SPECTER)EXPOSED.put(target.getUuid(),world.getTime()+15);
   }
   yrdenCircle(world,center,runtime.trapTicks,radius);
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
 private static void tick(net.minecraft.server.MinecraftServer server){
  for(var player:server.getPlayerManager().getPlayerList()){
   var runtime=runtime(player);state(player).tick();
   if(!allowed(player)){runtime.shield=0;runtime.shieldTicks=0;runtime.trapTicks=0;runtime.breathTicks=0;}
   if(runtime.shieldTicks>0&&--runtime.shieldTicks==0)runtime.shield=0;
   if(runtime.trapTicks>0){runtime.trapTicks--;if(server.getTicks()%10==0)tickTrap(player,runtime);}
   if(runtime.breathTicks>0&&--runtime.breathTicks%6==0){
    int power=level(player,SignType.IGNI,SignPlayerState.Upgrade.POWER);
    for(var target:targets(player,SignType.IGNI,range(player,SignType.IGNI),37.5,false).stream().limit(12).toList())
     hit(player,target,SignType.IGNI,2,1.8f+.2f*power,0,0);
    coneParticles(player.getServerWorld(),player.getEyePos(),player.getRotationVec(1),SignType.IGNI);
   }
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
  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{ACTIVE.remove(newPlayer.getUuid());clearBurns(newPlayer.getServer(),newPlayer.getUuid());state(newPlayer).resetProtocol();sync(newPlayer);syncMastery(newPlayer);});
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof ServerPlayerEntity player){ACTIVE.remove(player.getUuid());clearBurns(player.getServer(),player.getUuid());}});
  ServerPlayConnectionEvents.JOIN.register((h,sender,server)->{state(h.player).resetProtocol();ACTIVE.remove(h.player.getUuid());sync(h.player);syncMastery(h.player);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{ACTIVE.remove(h.player.getUuid());clearBurns(s,h.player.getUuid());SignLedger.get(s).markDirty();});
  ServerLifecycleEvents.SERVER_STOPPING.register(server->clearBurns(server,null));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.clear();CALM.clear();EXPOSED.clear();BURNS.clear();TRAP_HITS.clear();REDIRECTS.clear();});
  ServerTickEvents.END_SERVER_TICK.register(SignManager::tick);
 }
}
