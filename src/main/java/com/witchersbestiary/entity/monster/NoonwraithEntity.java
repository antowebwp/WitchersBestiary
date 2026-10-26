package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.CombatState;
import com.witchersbestiary.combat.Stun;
import com.witchersbestiary.combat.MonsterCategory;
import com.witchersbestiary.combat.CombatIllusions;
import net.minecraft.block.Blocks;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A hovering daylight specter. All authoritative abilities run on the server. */
public class NoonwraithEntity extends HostileEntity implements GeoEntity, CategorizedMonster, com.witchersbestiary.combat.Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(NoonwraithEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);builder.add(PHASE,0);builder.add(PHASE_START,0L);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    public enum Phase { MATERIALIZED, DISSOLVING, ETHEREAL, REAPPEARING }
    private static final net.minecraft.entity.data.TrackedData<Integer> PHASE=net.minecraft.entity.data.DataTracker.registerData(NoonwraithEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.INTEGER);
    private static final net.minecraft.entity.data.TrackedData<Long> PHASE_START=net.minecraft.entity.data.DataTracker.registerData(NoonwraithEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.LONG);
    private Vec3d phaseDestination;
    private int phaseTicks,phaseCooldown=120;
    private boolean phaseHeal,suppressPhaseLoot,normalizePhase;
    public Phase getPhase(){return Phase.values()[Math.clamp(dataTracker.get(PHASE),0,3)];}
    private void phase(Phase value,int ticks){dataTracker.set(PHASE,value.ordinal());dataTracker.set(PHASE_START,getWorld().getTime());phaseTicks=ticks;}
    public float phaseAlpha(float partial){float elapsed=getWorld().getTime()-dataTracker.get(PHASE_START)+partial;return switch(getPhase()){case DISSOLVING->MathHelper.clamp(1-elapsed/12,0,1);case ETHEREAL->0;case REAPPEARING->MathHelper.clamp(elapsed/10,0,1);default->1;};}
    @Override public boolean isPushable(){return getPhase()==Phase.MATERIALIZED&&super.isPushable();}
    @Override public void pushAwayFrom(Entity other){if(getPhase()==Phase.MATERIALIZED)super.pushAwayFrom(other);}
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean illusion;
    private final CombatIllusions copies=new CombatIllusions(illusionLimit());
    private long illusionExpiry;
    private int healingRetreats, hopCooldown=120;
    private java.util.UUID illusionOwner;
    private BlockPos ownerAnchor;
    private int retreatCooldown,retreatTicks,fadeTicks,successfulPlayerHits,retreatThreshold=2;
    private Vec3d retreatAnchor;
    private boolean priorAiDisabled,priorInvulnerable;
    public boolean isRetreating(){return getPhase()!=Phase.MATERIALIZED;}

    private int attackWindup, followup;
    private Vec3d meleeFacing=Vec3d.ZERO;
    private java.util.UUID meleeTarget;
    private boolean meleeContact(LivingEntity target){return target.getUuid().equals(meleeTarget)&&squaredDistanceTo(target)<5.76&&com.witchersbestiary.entity.ai.CombatMotion.contact(this,target,meleeFacing,2.4,.25);}
    private int illusionAge, screamCooldown = 40, illusionCooldown = 60, dodgeCooldown, attackCooldown;

    public NoonwraithEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        moveControl=new net.minecraft.entity.ai.control.FlightMoveControl(this,20,true); experiencePoints = 12; setNoGravity(true);retreatThreshold=2+random.nextInt(2);
    }

    @Override protected net.minecraft.entity.ai.pathing.EntityNavigation createNavigation(World world){
        return new com.witchersbestiary.entity.ai.SpectralNavigation(this,world);
    }
    @Override public void travel(Vec3d input){
        if(isLogicalSideForUpdatingMovement()){
            if(isRetreating()){setVelocity(Vec3d.ZERO);return;}
            updateVelocity(.25f,input);move(MovementType.SELF,getVelocity());setVelocity(getVelocity().multiply(.7));
        }else super.travel(input);
    }
    private BlockPos flightDetour;
    private Vec3d detourTarget;
    private int flightRetry;
    /** Vanilla routes remain primary; a bounded lateral waypoint escapes a finished partial route. */
    private void pursueFlight(LivingEntity target){
        if(age<flightRetry)return;
        if(flightDetour!=null&&!getNavigation().isIdle()&&detourTarget.squaredDistanceTo(target.getPos())<16)return;
        flightDetour=null;
        var path=getNavigation().findPathTo(target.getX(),target.getY()+.75,target.getZ(),0);
        if(path!=null&&(path.reachesTarget()||path.getLength()>2)){getNavigation().startMovingAlong(path,1);return;}
        getNavigation().stop();
        var forward=target.getPos().subtract(getPos()).multiply(1,0,1).normalize();
        var side=new Vec3d(-forward.z,0,forward.x).multiply((getId()%2==0?1:-1)*6);
        for(int sign:new int[]{1,-1}){
            var at=getPos().add(side.multiply(sign));var block=BlockPos.ofFloored(at);
            if(!getWorld().isChunkLoaded(block)||!getWorld().getWorldBorder().contains(block)||!getWorld().isSpaceEmpty(this,getBoundingBox().offset(at.subtract(getPos()))))continue;
            var route=getNavigation().findPathTo(at.x,at.y,at.z,0);
            if(route!=null&&route.reachesTarget()&&route.getLength()>2){flightDetour=block;detourTarget=target.getPos();getNavigation().startMovingAlong(route,1);return;}
        }
        flightRetry=age+40;
    }
    private void hover(){
        if(age%10!=0||!getNavigation().isIdle())return;
        for(int dy=0;dy<=4;dy++){
            var support=BlockPos.ofFloored(getX(),getY()-dy-.01,getZ());
            if(!getWorld().isChunkLoaded(support))return;
            if(getWorld().getBlockState(support).isSolidBlock(getWorld(),support)){
                var at=new Vec3d(getX(),support.getY()+1.75,getZ());
                if(getWorld().isSpaceEmpty(this,getBoundingBox().offset(at.subtract(getPos()))))getMoveControl().moveTo(at.x,at.y,at.z,.8);
                return;
            }
        }
    }
    // PathAwareEntity otherwise rejects bright positions through HostileEntity's
    // negative phototaxis even after our daylight SpawnRestriction accepted them.
    @Override public boolean canSpawn(net.minecraft.world.WorldAccess world, net.minecraft.entity.SpawnReason reason){
        return world.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL;
    }
    @Override protected void initGoals() {
        targetSelector.add(1, new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }

    protected boolean healingRetreatEnabled(){return true;}
    protected boolean ambientSparkles(){return false;}
    protected boolean combatHopEnabled(){return true;}
    protected boolean combatPaused(){return false;}
    protected int illusionLimit(){return 4;}
    protected float illusionDamage(){return 6;}
    protected double chaseSpeed(){return .35;}
    protected boolean doubleStrike(){return true;}
    protected void suspendAttack(int ticks){attackWindup=followup=0;attackCooldown=Math.max(attackCooldown,ticks);getNavigation().stop();setVelocity(Vec3d.ZERO);}
    public int getHealingRetreats(){return healingRetreats;}
    @Override public void onDeath(DamageSource source){if(getWorld() instanceof ServerWorld w)copies.clear(w);suppressPhaseLoot=isRetreating();if(suppressPhaseLoot)experiencePoints=0;endPhase(false);super.onDeath(source);}
    @Override public CombatState getCombatState(){if(Stun.active(this))return CombatState.STUNNED;if(isRetreating())return CombatState.SPECIAL_ABILITY;if(attackWindup>0)return CombatState.ATTACK_WINDUP;if(attackCooldown>0)return CombatState.ATTACK_RECOVERY;return getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){attackWindup=followup=0;attackCooldown=8;if(isRetreating())endPhase(false);}
    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        setNoGravity(true);
        if(normalizePhase){normalizePhase=false;endPhase(false);}
        if(phaseCooldown>0)phaseCooldown--;
        if (retreatCooldown>0)retreatCooldown--;
        if(isRetreating()){tickPhase();return;}
        if (screamCooldown > 0) screamCooldown--;
        if (illusionCooldown > 0) illusionCooldown--;
        if (dodgeCooldown > 0) dodgeCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (illusion && (++illusionAge >= CombatIllusions.LIFETIME || illusionExpiry>0 && world.getTime()>=illusionExpiry)) {discard();return;}
        if (!isAlive() || isAiDisabled()) return;
        if(com.witchersbestiary.combat.Stun.active(this)){cancelCombatForStun();com.witchersbestiary.combat.Stun.stop(this);return;}
        if(illusion&&illusionOwner!=null){var owner=world.getEntity(illusionOwner);if(owner instanceof GraveHagEntity hag && !hag.hasActiveCombat()){discard();return;}if(owner!=null&&!owner.isAlive()||owner==null&&ownerAnchor!=null&&world.isChunkLoaded(ownerAnchor)&&illusionAge>20){discard();return;}}
        if(combatPaused()){suspendAttack(8);return;}
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            Vec3d delta = target.getPos().subtract(getPos());
            double distance = delta.horizontalLength();
            if((attackWindup>0||followup>0)&&!target.getUuid().equals(meleeTarget)){attackWindup=followup=0;attackCooldown=Math.max(attackCooldown,10);}
            if(distance>1.45||Math.abs(delta.y)>.9){
                if(age%10==0)pursueFlight(target);
            }else getNavigation().stop();
            boolean committed=attackWindup>0&&attackWindup<=3||followup>0&&followup<=3;
            if(committed){com.witchersbestiary.entity.ai.CombatMotion.holdFacing(this,meleeFacing);getNavigation().stop();setVelocity(Vec3d.ZERO);}
            else if(distance<2.5&&Math.abs(delta.y)<2)meleeFacing=com.witchersbestiary.entity.ai.CombatMotion.face(this,target.getPos(),18);
            else {meleeFacing=Vec3d.fromPolar(0,getYaw());getLookControl().lookAt(target,18,18);}
            if (canSee(target) && distance < 1.9 && Math.abs(delta.y) < 2 && attackCooldown == 0 && attackWindup==0 && followup==0) {
                attackCooldown = com.witchersbestiary.config.ModConfig.attackTicks(doubleStrike()?30:16); attackWindup=7;meleeTarget=target.getUuid();triggerAnim("action","attack");
            }
            if(followup>0){if(--followup==6)triggerAnim("action","attack");if(followup==0&&meleeContact(target))super.tryAttack(target);}
            if(attackWindup>0 && --attackWindup==0){if(meleeContact(target))super.tryAttack(target);if(doubleStrike())followup=11;}
            if (!illusion && followup==0 && attackWindup==0 && distance < 7) scream();
            if (!illusion && followup==0 && !combatPaused() && attackWindup==0 && distance < 12) summonIllusions();
            if(!illusion&&combatHopEnabled()&&--hopCooldown<=0){hopCooldown=100+random.nextInt(61);tryGhostDodge();}
        }else{attackWindup=followup=0;getNavigation().stop();}
        if(target==null||!target.isAlive()||target.squaredDistanceTo(this)<2.2)hover();
        if (!illusion && ambientSparkles() && age % 8 == 0) world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + .4, getZ(), 1, .3, .2, .3, .005);
    }

    public boolean scream() {
        if (!(getWorld() instanceof ServerWorld world) || com.witchersbestiary.combat.Stun.active(this) || isRetreating() || illusion || screamCooldown > 0) return false;
        screamCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(120);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(7),
                p -> canSee(p) && p.isAlive() && !p.isSpectator() && !p.isCreative() && squaredDistanceTo(p) <= 49)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 70, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 0));
        }
        playSound(SoundEvents.ENTITY_GHAST_SCREAM, 1.6f, .75f);
        triggerAnim("action", "scream"); particles(24); return true;
    }

    /** A supported, dry destination with room for the complete entity hitbox. */
    public boolean safeDestination(Vec3d destination) {
        if (!(getWorld() instanceof ServerWorld world)) return false;
        BlockPos feet = BlockPos.ofFloored(destination);
        if (!world.isChunkLoaded(feet) || !world.isInBuildLimit(feet) || !world.getWorldBorder().contains(feet)
                || !world.getBlockState(feet.down()).isSolidBlock(world, feet.down())
                || !world.getFluidState(feet).isEmpty()) return false;
        Box box = getBoundingBox().offset(destination.subtract(getPos()));
        if(!world.getWorldBorder().contains(box) || box.maxY>=world.getTopY())return false;
        for(BlockPos p:BlockPos.iterate(BlockPos.ofFloored(box.minX,box.minY-1,box.minZ),BlockPos.ofFloored(box.maxX,box.maxY,box.maxZ))){
            if(!world.isChunkLoaded(p))return false;
            var state=world.getBlockState(p);
            if(state.isOf(Blocks.MAGMA_BLOCK)||state.isOf(Blocks.CACTUS)||state.isOf(Blocks.FIRE)||state.isOf(Blocks.SOUL_FIRE)
                    ||state.isOf(Blocks.CAMPFIRE)||state.isOf(Blocks.SOUL_CAMPFIRE)||state.isOf(Blocks.SWEET_BERRY_BUSH)
                    ||state.isOf(Blocks.WITHER_ROSE)||state.isOf(Blocks.POWDER_SNOW))return false;
        }
        return world.isSpaceEmpty(this, box) && !world.containsFluid(box);
    }

    public boolean tryGhostDodge(){return beginPhase(false);}
    /** One bounded destination search at start; movement itself never loads chunks. */
    protected boolean beginPhase(boolean healing){
        if(!(getWorld() instanceof ServerWorld)||illusion||!isAlive()||Stun.active(this)||isRetreating()||phaseCooldown>0||getTarget()==null||!getTarget().isAlive()||squaredDistanceTo(getTarget())>256)return false;
        if(!safeDestination(getPos()))return false;
        Vec3d destination=behindTarget();
        if(destination==null)return false;
        retreatAnchor=getPos();phaseDestination=destination;priorAiDisabled=isAiDisabled();priorInvulnerable=isInvulnerable();phaseHeal=healing;
        setAiDisabled(true);setInvulnerable(true);suspendAttack(8);phase(Phase.DISSOLVING,12);
        phaseCooldown=healingRetreatEnabled()?180+random.nextInt(61):280+random.nextInt(121);
        triggerAnim("action","vanish");playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT,.8f,.6f);phaseMist(12);return true;
    }
    private Vec3d behindTarget(){
        var target=getTarget();if(target==null||!target.isAlive()||target.getWorld()!=getWorld())return null;
        double yaw=Math.toRadians(target.getYaw());
        for(double offset:new double[]{0,.4,-.4,.8,-.8,Math.PI/2,-Math.PI/2})for(double radius:new double[]{2.8,3.3})for(int dy:new int[]{0,1,-1}){
            double angle=yaw+offset;
            var at=new Vec3d(target.getX()+Math.sin(angle)*radius,Math.floor(target.getY())+dy,target.getZ()-Math.cos(angle)*radius);
            if(safeDestination(at)&&!getBoundingBox().offset(at.subtract(getPos())).intersects(target.getBoundingBox()))return at;
        }
        return null;
    }
    private void phaseMist(int count){if(getWorld() instanceof ServerWorld w)w.spawnParticles(ParticleTypes.CLOUD,getX(),getY()+1,getZ(),count,.4,.7,.4,.015);}
    private boolean phaseSpace(Vec3d at){
        if(!(getWorld() instanceof ServerWorld w))return false;
        Box box=getBoundingBox().offset(at.subtract(getPos()));
        if(box.minY<w.getBottomY()+1||box.maxY>=w.getTopY()||!w.getWorldBorder().contains(box))return false;
        for(BlockPos pos:BlockPos.iterate(BlockPos.ofFloored(box.minX,box.minY,box.minZ),BlockPos.ofFloored(box.maxX,box.maxY,box.maxZ)))if(!w.isChunkLoaded(pos)||w.getBlockState(pos).isOf(Blocks.BEDROCK))return false;
        return true;
    }
    private void tickPhase(){
        setVelocity(Vec3d.ZERO);
        if(!isAlive()||Stun.active(this)||getTarget()==null||!getTarget().isAlive()||getTarget().getWorld()!=getWorld()){endPhase(false);return;}
        switch(getPhase()){
            case DISSOLVING->{if(phaseTicks%3==0)phaseMist(3);if(--phaseTicks<=0){setInvisible(true);noClip=true;phase(Phase.ETHEREAL,healingRetreatEnabled()?48:36);}}
            case ETHEREAL->{
                Vec3d delta=phaseDestination.subtract(getPos());Vec3d next=getPos().add(delta.length()>.4?delta.normalize().multiply(.4):delta);
                if(phaseSpace(next))setPosition(next);else phaseTicks=1;
                if(phaseTicks<=8&&phaseTicks%2==0)phaseMist(4);
                if(--phaseTicks<=0){
                    var destination=behindTarget();
                    if(destination!=null)requestTeleport(destination.x,destination.y,destination.z);
                    else {endPhase(false);return;}
                    noClip=false;setInvisible(false);phase(Phase.REAPPEARING,10);phaseMist(12);playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT,.7f,1.2f);triggerAnim("action","appear");
                }
            }
            case REAPPEARING->{if(--phaseTicks<=0)endPhase(true);}
            default->endPhase(false);
        }
    }
    private void endPhase(boolean completed){
        if(!isRetreating()&&!normalizePhase)return;
        boolean recoverable=true;
        if(!getWorld().isClient&&!safeDestination(getPos())){
            Vec3d safe=retreatAnchor!=null&&safeDestination(retreatAnchor)?retreatAnchor:null;
            Vec3d center=retreatAnchor==null?getPos():retreatAnchor;
            for(int radius=1;radius<=3&&safe==null;radius++)for(int direction=0;direction<8&&safe==null;direction++)for(int dy=-2;dy<=3;dy++){
                double angle=direction*Math.PI/4;Vec3d candidate=center.add(Math.cos(angle)*radius,dy,Math.sin(angle)*radius);
                if(safeDestination(candidate)){safe=candidate;break;}
            }
            if(safe!=null)setPosition(safe);else recoverable=false;
        }
        noClip=false;setInvisible(false);setInvulnerable(priorInvulnerable);setAiDisabled(priorAiDisabled);phase(Phase.MATERIALIZED,0);
        if(completed&&phaseHeal&&isAlive())heal(getMaxHealth()*.05f);
        phaseHeal=false;phaseDestination=null;suspendAttack(2);afterPhase(completed);
        if(!recoverable&&isAlive()){suppressPhaseLoot=true;experiencePoints=0;discard();}
    }
    protected void afterPhase(boolean completed){}
    @Override public void remove(Entity.RemovalReason reason){if(isRetreating())endPhase(false);super.remove(reason);}

    public int summonIllusions() {
        if (!(getWorld() instanceof ServerWorld world) || com.witchersbestiary.combat.Stun.active(this) || isRetreating() || illusion || illusionCooldown > 0) return 0;
        illusionCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(180);
        int count = 0;
        int slots=Math.min(copies.available(world),illusionLimit());
        for (int i = 0; i < slots; i++) {
            double angle=i*Math.PI/2;Vec3d position = getPos().add(Math.cos(angle)*2.8,0,Math.sin(angle)*2.8);
            if (!safeDestination(position)) continue;
            NoonwraithEntity copy = (NoonwraithEntity)getType().create(world);
            if (copy == null) continue;
            copy.refreshPositionAndAngles(position.x, position.y, position.z, getYaw(), 0);
            copy.setIllusion(true);copy.illusionOwner=getUuid();copy.ownerAnchor=getBlockPos(); copy.setAiDisabled(false); copy.setGlowing(false); copy.setHealth(6);copy.setTarget(getTarget());
            if (world.spawnEntity(copy)) {count++;copies.add(copy,copy.illusionExpiry);}
        }
        if (count > 0) { triggerAnim("action", "summon_illusions"); particles(20); }
        return count;
    }

    public void bindGraveHag(GraveHagEntity owner){setIllusion(true);illusionOwner=owner.getUuid();ownerAnchor=owner.getBlockPos();}
    public boolean isIllusion() { return illusion; }
    public void setIllusion(boolean value) { illusion = value; if (value) { getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(6);setHealth(Math.min(getHealth(),6));experiencePoints = 0; setPersistent();setGlowing(false);illusionExpiry=getWorld().getTime()+CombatIllusions.LIFETIME;getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(illusionDamage()); } }

    @Override public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.IS_FALL)) return false;
        if(isRetreating() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        float before=getHealth();
        boolean physical = source.getAttacker() instanceof LivingEntity;
        var weapon = source.getWeaponStack();
        boolean oiled = com.witchersbestiary.alchemy.InventoryOils.prepare(this,source)!=null;

        boolean hit = super.damage(source, physical && !oiled ? amount * .55f : amount);
        if (hit && isAlive()) {
            if(attackWindup==0)triggerAnim("action", "hurt");
            if(!getWorld().isClient && !illusion && !Stun.active(this) && healingRetreatEnabled() && healingRetreats<3 && retreatCooldown==0 && source.getAttacker() instanceof PlayerEntity && getHealth()<before){
                if(getTarget()==null&&source.getAttacker() instanceof LivingEntity attacker)setTarget(attacker);
                if(++successfulPlayerHits>=retreatThreshold)beginRetreat();
            }
        }
        return hit;
    }

    @Override public boolean tryAttack(Entity target) {
        if(com.witchersbestiary.combat.Stun.active(this)||isRetreating()||combatPaused())return false; return super.tryAttack(target);
    }
    @Override protected void dropLoot(DamageSource source, boolean player) { if (!illusion && !suppressPhaseLoot && !isRetreating()) super.dropLoot(source, player); }
    private void particles(int count) {
        if (!illusion && getWorld() instanceof ServerWorld world) world.spawnParticles(ParticleTypes.END_ROD,
                getX(), getY() + 1, getZ(), count, .6, .9, .6, .025);
    }
    @Override protected void updatePostDeath() { if (deathTime % 3 == 0) particles(10); super.updatePostDeath(); }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt); nbt.putInt("SpecterProfileVersion",10);nbt.putBoolean("BestiaryIllusion", illusion);
        nbt.putInt("HealingRetreats",healingRetreats);nbt.putInt("HopCooldown",hopCooldown);if(illusionOwner!=null)nbt.putUuid("IllusionOwner",illusionOwner);if(ownerAnchor!=null)nbt.putLong("OwnerAnchor",ownerAnchor.asLong());
        nbt.putInt("IllusionAge", illusionAge); nbt.putInt("ScreamCooldown", screamCooldown);
        nbt.putInt("IllusionCooldown", illusionCooldown); nbt.putInt("DodgeCooldown", dodgeCooldown);
        nbt.putLong("IllusionExpiry",illusionExpiry);copies.save(nbt);
        nbt.putInt("SpectralPhase",getPhase().ordinal());nbt.putInt("PhaseCooldown",phaseCooldown);
        nbt.putInt("RetreatCooldown",retreatCooldown);nbt.putInt("RetreatTicks",retreatTicks);nbt.putInt("FadeTicks",fadeTicks);
        nbt.putInt("RetreatHits",successfulPlayerHits);nbt.putInt("RetreatThreshold",retreatThreshold);
        nbt.putBoolean("RetreatPriorAi",priorAiDisabled);nbt.putBoolean("RetreatPriorInvulnerable",priorInvulnerable);
        if(retreatAnchor!=null){nbt.putDouble("RetreatX",retreatAnchor.x);nbt.putDouble("RetreatY",retreatAnchor.y);nbt.putDouble("RetreatZ",retreatAnchor.z);}
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);healingRetreats=Math.clamp(nbt.getInt("HealingRetreats"),0,3);hopCooldown=Math.clamp(nbt.getInt("HopCooldown"),0,160);if(nbt.containsUuid("IllusionOwner"))illusionOwner=nbt.getUuid("IllusionOwner");if(nbt.contains("OwnerAnchor"))ownerAnchor=BlockPos.fromLong(nbt.getLong("OwnerAnchor"));if(healingRetreatEnabled()&&!nbt.getBoolean("BestiaryIllusion")&&nbt.getInt("SpecterProfileVersion")<10&&getMaxHealth()==60&&getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)==8){
            float fraction=getHealth()/getMaxHealth();getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(140);getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(10);getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(.35);setHealth(140*fraction);
        }
        setIllusion(nbt.getBoolean("BestiaryIllusion"));
        illusionAge = Math.max(0, nbt.getInt("IllusionAge")); screamCooldown = Math.max(0, nbt.getInt("ScreamCooldown"));
        illusionCooldown = Math.max(0, nbt.getInt("IllusionCooldown")); dodgeCooldown = Math.max(0, nbt.getInt("DodgeCooldown"));
        if(nbt.contains("IllusionExpiry"))illusionExpiry=nbt.getLong("IllusionExpiry");if(illusion)setAiDisabled(false);copies.load(nbt);
        phaseCooldown=Math.clamp(nbt.getInt("PhaseCooldown"),0,400);
        retreatCooldown=Math.max(0,nbt.getInt("RetreatCooldown"));retreatTicks=Math.max(0,nbt.getInt("RetreatTicks"));fadeTicks=Math.max(0,nbt.getInt("FadeTicks"));
        successfulPlayerHits=nbt.getInt("RetreatHits");retreatThreshold=MathHelper.clamp(nbt.getInt("RetreatThreshold"),2,3);
        priorAiDisabled=nbt.getBoolean("RetreatPriorAi");priorInvulnerable=nbt.getBoolean("RetreatPriorInvulnerable");
        if(nbt.contains("RetreatX"))retreatAnchor=new Vec3d(nbt.getDouble("RetreatX"),nbt.getDouble("RetreatY"),nbt.getDouble("RetreatZ"));
        if(nbt.getInt("SpectralPhase")>0||retreatTicks>0||fadeTicks>0){phase(Phase.REAPPEARING,1);normalizePhase=true;setInvisible(false);noClip=false;setAiDisabled(priorAiDisabled);setInvulnerable(priorInvulnerable);}
        retreatTicks=fadeTicks=0;
    }
    private void beginRetreat(){
        if(beginPhase(true)){healingRetreats++;retreatCooldown=400;successfulPlayerHits=0;retreatThreshold=2+random.nextInt(2);}
    }
    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.SPECTER; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "movement", 5, state -> state.setAndContinue(
                RawAnimation.begin().thenLoop("animation.noonwraith." + (!isAlive() ? "death" :
                        getVelocity().horizontalLengthSquared() > .002 ? "float_move" : "idle")))));
        AnimationController<NoonwraithEntity> action = new AnimationController<>(this, "action", 0, state -> PlayState.STOP);
        for (String animation : new String[]{"attack", "scream", "summon_illusions", "hurt", "vanish", "appear"})
            action.triggerableAnim(animation, RawAnimation.begin().thenPlay("animation.noonwraith." + animation));
        registrar.add(action);
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 140)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, .35)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36).add(EntityAttributes.GENERIC_FLYING_SPEED,com.witchersbestiary.config.ModConfig.speed(.22));
    }
}
