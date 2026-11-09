package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.data.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import com.witchersbestiary.combat.mixin.ItemPickupOwner;
import com.witchersbestiary.contracts.procedural.ProceduralHunts;
import com.witchersbestiary.fist.FistFights;
import com.witchersbestiary.nekker.NekkerHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Local coordination runs once per second. Each committed attack has one contact window. */
public final class NekkerEntity extends HostileEntity implements CategorizedMonster,GeoEntity,Stunnable,Tameable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN=net.minecraft.entity.data.DataTracker.registerData(NekkerEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> OWNER =
        DataTracker.registerData(NekkerEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private final SimpleInventory storage = new SimpleInventory(9);
    private boolean waiting;
    private boolean collecting;
    private static final Identifier FURY=Identifier.of("witchersbestiary","nekker_ally_fury");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public enum Action { NONE, CLAW_ATTACK, BITE, LEAP }
    private Action action=Action.NONE;
    private int actionTick,attackCooldown,leapCooldown=100,angerTicks,slot,packSize=1;
    private boolean contact,biteNext;
    private Vec3d committedDirection=Vec3d.ZERO;
    private final CombatMotion pursuit=new CombatMotion();
    public NekkerEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=5;}
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);b.add(OWNER,Optional.empty());}
    @Override public boolean trackedStun(){return dataTracker.get(STUN);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN,value);}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.OGROID;}
    @Override protected void initGoals(){
        targetSelector.add(1,new RevengeGoal(this) {
            @Override public boolean canStart() { return !isTamed() && super.canStart(); }
            @Override public boolean shouldContinue() { return !isTamed() && super.shouldContinue(); }
        });
        targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null) {
            @Override public boolean canStart() { return !isTamed() && super.canStart(); }
            @Override public boolean shouldContinue() { return !isTamed() && super.shouldContinue(); }
        });
        goalSelector.add(5,new WanderAroundFarGoal(this,.7){@Override public boolean canStart(){return !isTamed()&&getTarget()==null&&action==Action.NONE&&super.canStart();}});
        goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,10));
    }
    public Action getAction(){return action;}
    public int getAngerTicks(){return angerTicks;}
    public int getPackSlot(){return slot;}
    public int getPackSize(){return packSize;}
    public int getLeapCooldown(){return leapCooldown;}
    @Override public int getLimitPerChunk(){return 5;}
    @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action!=Action.NONE?CombatState.SPECIAL_ABILITY:attackCooldown>0?CombatState.ATTACK_RECOVERY:getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){action=Action.NONE;attackCooldown=12;getNavigation().stop();setVelocity(0,getVelocity().y,0);}
    private boolean valid(LivingEntity t){return t!=null&&t.isAlive()&&(!isTamed()||defendsAgainst(owner(),t))&&!(t instanceof PlayerEntity p&&(p.isCreative()||p.isSpectator()))&&squaredDistanceTo(t)<1600;}
    @Override public UUID getOwnerUuid() { return dataTracker.get(OWNER).orElse(null); }
    public boolean isTamed() { return getOwnerUuid() != null; }
    public SimpleInventory storage() { return storage; }
    public boolean waiting() { return waiting; }
    public boolean collecting() { return collecting; }
    public void setCollecting(boolean enabled) { collecting = enabled; }

    private boolean ownedBy(PlayerEntity player) {
        return dataTracker.get(OWNER).filter(player.getUuid()::equals).isPresent();
    }

    private PlayerEntity owner() {
        return dataTracker.get(OWNER).map(getWorld()::getPlayerByUuid).orElse(null);
    }

    public boolean canAccess(PlayerEntity player) {
        return isAlive() && player.isAlive() && !player.isSpectator() && ownedBy(player)
            && getWorld() == player.getWorld() && squaredDistanceTo(player) <= 36 && !FistFights.busy(player);
    }

    @Override protected boolean isDisallowedInPeaceful() { return !isTamed(); }

    @Override public void pushAwayFrom(Entity entity) {
        if (!(entity instanceof PlayerEntity player && ownedBy(player))) super.pushAwayFrom(entity);
    }

    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var food = player.getStackInHand(hand);
        boolean meat = food.isOf(Items.BEEF) || food.isOf(Items.PORKCHOP) || food.isOf(Items.MUTTON);
        if (!isAlive() || !player.canInteractWithEntity(this, 0) || player.isSpectator()) return ActionResult.PASS;
        if (!isTamed()) {
            if (!meat || ProceduralHunts.binding(this) != null || FistFights.busy(player)) return ActionResult.PASS;
            if (!getWorld().isClient) {
                if (!player.isCreative()) food.decrement(1);
                boolean success = random.nextInt(3) == 0;
                if (success) {
                    dataTracker.set(OWNER, Optional.of(player.getUuid()));
                    setPersistent();
                    setTarget(null);
                    setAttacking(false);
                    cancelCombatForStun();
                    stopTriggeredAnim("action", null);
                    angerTicks = 0;
                    applyFury();
                }
                getWorld().sendEntityStatus(this, (byte)(success ? 7 : 6));
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!canAccess(player)) return ActionResult.PASS;
        if (player.isSneaking()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory<Integer>() {
                    @Override public Integer getScreenOpeningData(ServerPlayerEntity p) { return getId(); }
                    @Override public Text getDisplayName() { return Text.translatable("nekker.witchersbestiary.title"); }
                    @Override public ScreenHandler createMenu(int sync, PlayerInventory inventory, PlayerEntity p) {
                        return canAccess(p) ? new NekkerHandler(sync, inventory, NekkerEntity.this) : null;
                    }
                });
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (meat) return ActionResult.PASS;
        if (!getWorld().isClient) {
            waiting = !waiting;
            setTarget(null);
            cancelCombatForStun();
            stopTriggeredAnim("action", null);
            player.sendMessage(Text.translatable(waiting ? "nekker.witchersbestiary.wait" : "nekker.witchersbestiary.follow"), true);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override public void handleStatus(byte status) {
        if (status != 6 && status != 7) { super.handleStatus(status); return; }
        var particle = status == 7 ? net.minecraft.particle.ParticleTypes.HEART : net.minecraft.particle.ParticleTypes.SMOKE;
        for (int i = 0; i < 7; i++) getWorld().addParticle(particle, getParticleX(1), getRandomBodyY() + .3,
            getParticleZ(1), random.nextGaussian() * .02, .02, random.nextGaussian() * .02);
    }

    private boolean defendsAgainst(PlayerEntity owner, LivingEntity target) {
        return !waiting && owner != null && owner.isAlive() && !owner.isSpectator()
            && squaredDistanceTo(owner) <= 144 && owner.squaredDistanceTo(target) <= 144
            && target instanceof HostileEntity
            && !(target instanceof Tameable pet && pet.getOwnerUuid() != null)
            && !isTeammate(target) && !owner.isTeammate(target) && !FistFights.busy(owner) && !FistFights.busy(target);
    }

    private void tickCompanion(ServerWorld world) {
        PlayerEntity owner = owner();
        if (!valid(getTarget())) setTarget(null);
        if (owner == null || !owner.isAlive() || owner.isSpectator() || waiting) {
            getNavigation().stop();
            return;
        }
        if (getTarget() == null && !FistFights.busy(owner)) {
            LivingEntity attacker = owner.getAttacker();
            if (attacker != null && attacker.isAlive() && owner.age - owner.getLastAttackedTime() <= 100
                    && defendsAgainst(owner, attacker)) setTarget(attacker);
            else {
                LivingEntity victim = owner.getAttacking();
                if (victim != null && victim.isAlive() && owner.age - owner.getLastAttackTime() <= 100
                        && defendsAgainst(owner, victim)) setTarget(victim);
            }
        }
        if ((age + getId()) % 10 != 0) return;
        if (collecting && squaredDistanceTo(owner) <= 144 && !FistFights.busy(owner)) collectItems(world, owner);
        if (getTarget() != null) return;
        double distance = squaredDistanceTo(owner);
        if (distance >= 144 && tryCatchUp(world, owner)) return;
        if (distance > 9) getNavigation().startMovingTo(owner, 1.1);
        else getNavigation().stop();
    }

    private boolean tryCatchUp(ServerWorld world, PlayerEntity owner) {
        for (int i = 0; i < 10; i++) {
            int x = random.nextBetween(-3, 3), z = random.nextBetween(-3, 3);
            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;
            var pos = owner.getBlockPos().add(x, random.nextBetween(-1, 1), z);
            if (!world.isChunkLoaded(pos) || !world.isChunkLoaded(pos.add(-1, 0, -1))
                    || !world.isChunkLoaded(pos.add(1, 0, 1)) || !world.getWorldBorder().contains(pos)
                    || LandPathNodeMaker.getLandNodeType(this, pos) != PathNodeType.WALKABLE) continue;
            var destination = Vec3d.ofBottomCenter(pos);
            var box = getBoundingBox().offset(destination.subtract(getPos()));
            if (!world.getWorldBorder().contains(box) || !world.isSpaceEmpty(this, box) || world.containsFluid(box)) continue;
            refreshPositionAfterTeleport(destination);
            getNavigation().stop();
            return true;
        }
        return false;
    }

    private void collectItems(ServerWorld world, PlayerEntity owner) {
        if (storage.getHeldStacks().stream().noneMatch(stack -> stack.isEmpty() || stack.getCount() < stack.getMaxCount())) return;
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, getBoundingBox().expand(5), Entity::isAlive)) {
            if (item.cannotPickup() || squaredDistanceTo(item) > 25 || !canSee(item)) continue;
            UUID pickupOwner = ((ItemPickupOwner) item).witchersbestiary$getPickupOwner();
            if (pickupOwner != null && !pickupOwner.equals(owner.getUuid())) continue;
            var stack = item.getStack();
            if (!storage.canInsert(stack)) continue;
            var remainder = storage.addStack(stack.copy());
            if (remainder.isEmpty()) item.discard();
            else item.setStack(remainder);
        }
    }

    private void coordinate(){
        var nearby=getWorld().getEntitiesByClass(NekkerEntity.class,getBoundingBox().expand(16),e->e.isAlive()&&!e.isAiDisabled()&&!e.isTamed());
        nearby.sort(java.util.Comparator.comparing(Entity::getUuid));
        // Stable local order yields different flanking destinations, even with matching spawn yaw.
        slot=Math.max(0,nearby.indexOf(this));packSize=Math.min(5,Math.max(1,nearby.size()));
        LivingEntity chosen=valid(getTarget())?getTarget():null;
        for(var ally:nearby)if(valid(ally.getTarget())){chosen=ally.getTarget();break;}
        if(chosen!=null)setTarget(chosen);
    }
    public Vec3d approachPosition(LivingEntity target){
        double angle=slot*Math.PI*2/Math.max(3,packSize),radius=squaredDistanceTo(target)>36?2.2:.85;
        return target.getPos().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    private void start(Action next){
        action=next;actionTick=0;contact=false;getNavigation().stop();
        committedDirection=Vec3d.fromPolar(0,getYaw());
        triggerAnim("action",next.name().toLowerCase(java.util.Locale.ROOT));
    }
    public boolean beginLeap(){
        var t=getTarget();double d=t==null?0:squaredDistanceTo(t);
        if(getWorld().isClient||!isAlive()||isAiDisabled()||Stun.active(this)||action!=Action.NONE||attackCooldown>0||leapCooldown>0||!valid(t)||!isOnGround()||!canSee(t)||d<9||d>49)return false;
        leapCooldown=angerTicks>0?120+random.nextInt(41):160+random.nextInt(81);start(Action.LEAP);return true;
    }
    private boolean impact(LivingEntity t,double range){
        if(!valid(t)||!canSee(t)||squaredDistanceTo(t)>range*range||Math.abs(t.getY()-getY())>1.8)return false;
        var delta=t.getPos().subtract(getPos()).multiply(1,0,1).normalize();
        float damage = (float)getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        return delta.dotProduct(committedDirection)>.1&&t.damage(getDamageSources().mobAttack(this),isTamed()?Math.min(3,damage):damage);
    }
    public void mournAlly(){if(!isAlive()||isTamed())return;angerTicks=160;leapCooldown=Math.min(leapCooldown,40);applyFury();}
    private void applyFury(){var speed=getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);speed.removeModifier(FURY);if(angerTicks>0)speed.addTemporaryModifier(new EntityAttributeModifier(FURY,.18,EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
    @Override public void tick(){
        super.tick();if(getWorld().isClient||!isAlive())return;
        if(attackCooldown>0)attackCooldown--;if(leapCooldown>0)leapCooldown--;if(angerTicks>0&&--angerTicks==0)applyFury();
        if(Stun.active(this)){cancelCombatForStun();return;}if(isAiDisabled())return;
        if (isTamed()) tickCompanion((ServerWorld)getWorld());
        else if((age+getId())%20==0)coordinate();
        var t=getTarget();setAttacking(valid(t));if(!valid(t)){setTarget(null);if(action!=Action.NONE)cancelCombatForStun();return;}
        pursuit.sample(this,t);
        if(action!=Action.NONE){
            actionTick++;getNavigation().stop();
            if(action==Action.LEAP){
                if(actionTick<=5)committedDirection=CombatMotion.face(this,pursuit.lead(t,4,.7),20);
                else CombatMotion.holdFacing(this,committedDirection);
                if(actionTick==8){setVelocity(committedDirection.x*.65,.38,committedDirection.z*.65);velocityModified=true;}
                if(actionTick>8&&actionTick<=22&&!contact)contact=impact(t,1.65);
                if(horizontalCollision&&actionTick>8)setVelocity(0,getVelocity().y,0);
            }else{setVelocity(0,getVelocity().y,0);int windup=action==Action.BITE?4:7;
                if(actionTick<windup-1)committedDirection=CombatMotion.face(this,t.getPos(),22);
                if(actionTick<windup-1&&CombatMotion.distance(this,t)>1.4)CombatMotion.safeStep(this,committedDirection.multiply(.12),true);
                if(!contact&&actionTick>=windup&&actionTick<=windup+2)contact=impact(t,action==Action.BITE?1.55:1.9);}
            int duration=action==Action.LEAP?32:action==Action.BITE?12:19;
            if(actionTick>=duration){action=Action.NONE;attackCooldown=contact?9:18;}
            return;
        }
        getLookControl().lookAt(t,35,35);CombatMotion.face(this,t.getPos(),22);double distance=squaredDistanceTo(t);
        if(beginLeap())return;
        if(distance<3.2&&canSee(t)){getNavigation().stop();if(attackCooldown==0){start(biteNext&&distance<2.3?Action.BITE:Action.CLAW_ATTACK);biteNext=!biteNext;}}
        else if((age+getId())%8==0){var p=approachPosition(t);if(!getNavigation().startMovingTo(p.x,p.y,p.z,1.05))getNavigation().startMovingTo(t,1.05);}
    }
    @Override public boolean damage(DamageSource source,float amount){boolean hit=super.damage(source,amount);if(hit&&isAlive()&&action==Action.NONE)triggerAnim("action","hurt");return hit;}
    @Override public void onDeath(DamageSource source){if(!getWorld().isClient && isTamed()) ItemScatterer.spawn(getWorld(), this, storage);
        if(!getWorld().isClient&&!isTamed())for(var ally:getWorld().getEntitiesByClass(NekkerEntity.class,getBoundingBox().expand(14),e->e!=this&&e.isAlive()))ally.mournAlly();action=Action.NONE;super.onDeath(source);}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putInt("NekkerLeapCooldown",leapCooldown);n.putInt("NekkerAnger",angerTicks);
        dataTracker.get(OWNER).ifPresent(owner -> {
            n.putUuid("NekkerOwner", owner);
            n.putBoolean("NekkerWaiting", waiting);
            n.putBoolean("NekkerPickup", collecting);
            n.put("NekkerStorage", Inventories.writeNbt(new NbtCompound(), storage.getHeldStacks(), getRegistryManager()));
        });
    }
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);leapCooldown=n.contains("NekkerLeapCooldown")?Math.clamp(n.getInt("NekkerLeapCooldown"),0,240):100;angerTicks=Math.clamp(n.getInt("NekkerAnger"),0,160);applyFury();action=Action.NONE;attackCooldown=20;
        dataTracker.set(OWNER, n.containsUuid("NekkerOwner") ? Optional.of(n.getUuid("NekkerOwner")) : Optional.empty());
        waiting = n.getBoolean("NekkerWaiting");
        collecting = n.getBoolean("NekkerPickup");
        storage.clear();
        if (isTamed()) {
            setPersistent();
            Inventories.readNbt(n.getCompound("NekkerStorage"), storage.getHeldStacks(), getRegistryManager());
        }
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<>(this,"movement",3,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.nekker."+(!isAlive()?"death":state.isMoving()?isInSneakingPose()?"crawl":isAttacking()?"run":"walk":"idle")))));
        var a=new AnimationController<>(this,"action",0,state->PlayState.STOP);for(String name:new String[]{"claw_attack","bite","leap","hurt"})a.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.nekker."+name));c.add(a);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,26).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,5).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.34).add(EntityAttributes.GENERIC_FOLLOW_RANGE,32).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
