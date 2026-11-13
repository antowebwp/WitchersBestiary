package com.witchersbestiary.entity.monster;

import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

final class HigherVampireCombat {
    private enum Move {
        LEFT(16, 22, "claw_attack_left", "claw_left"),
        RIGHT(16, 22, "claw_attack_right", "claw_right"),
        COMBO(24, 52, "claw_combo", "double_claw"),
        TRIPLE(37, 85, "claw_combo", "double_claw"),
        DASH(26, 75, "claw_attack_right", "claw_right"),
        DODGE(12, 95, "dodge", null),
        GRAB(30, 180, "grab", null),
        WING(23, 75, null, "wing_attack"),
        LEAP(50, 100, null, "jump"),
        DIVE(57, 155, null, "wing_spread"),
        VOLLEY(36, 105, null, "blood_volley"),
        BURST(38, 180, null, "blood_burst"),
        SWARM(33, 230, null, "blood_swarm");

        final int duration, cooldown;
        final String human, hybrid;
        Move(int duration, int cooldown, String human, String hybrid) {
            this.duration = duration;
            this.cooldown = cooldown;
            this.human = human;
            this.hybrid = hybrid;
        }
    }

    private record Shot(Vec3d position, Vec3d velocity, int ticks, boolean swarm) {}
    private final HigherVampireEntity boss;
    private final CombatMotion motion = new CombatMotion();
    private final EnumMap<Move, Integer> ready = new EnumMap<>(Move.class);
    private final List<Shot> shots = new ArrayList<>();
    private Move action, last, follow;
    private LivingEntity victim, grabbed;
    private Vec3d landingAim = Vec3d.ZERO;
    private Vec3d facing = Vec3d.ZERO, direction = Vec3d.ZERO;
    private int elapsed, recovery, nextScan, nextPath, nextSwitch, nextControl, controlledUntil, lastHit, hiddenTicks, landTick;
    private boolean airborne, chained;
    private Vec3d dodgeStart = Vec3d.ZERO;
    private float dodgeHealth;

    HigherVampireCombat(HigherVampireEntity boss) { this.boss = boss; }

    void interrupt(int ticks) {
        if (boss.age < nextControl) return;
        nextControl = boss.age + 55;
        cancel();
        controlledUntil = boss.age + ticks;
    }

    void cancel() {
        endAction();
        follow = null;
        shots.clear();
        boss.getNavigation().stop();
    }

    private void endAction() {
        if (action != null) {
            boss.setNoGravity(false);
            boss.setVelocity(0, Math.min(0, boss.getVelocity().y), 0);
        }
        action = null;
        grabbed = null;
        airborne = false;
        landTick = 0;
        boss.combatClip("");
    }

    void tick(ServerWorld world, boolean needsClearance) {
        if (boss.isAiDisabled()) {
            if (action != null || !shots.isEmpty()) cancel();
            return;
        }
        if (boss.age < controlledUntil) {
            boss.getNavigation().stop();
            boss.setVelocity(0, boss.getVelocity().y, 0);
            return;
        }
        tickShots(world);
        LivingEntity target = boss.getTarget();
        if (!valid(target, 56)) {
            boss.setTarget(null);
            if (action != null) cancel();
            target = null;
        }
        if (boss.age >= nextScan && action == null) {
            nextScan = boss.age + 12;
            var players = players(40);
            if (target == null) {
                target = players.stream().filter(boss::canSee)
                        .min(Comparator.comparingDouble(boss::squaredDistanceTo)).orElse(null);
                boss.setTarget(target);
                nextSwitch = boss.age + 85;
            } else if (boss.age >= nextSwitch) {
                var previous = target;
                var alternate = players.stream().filter(p -> p != previous && boss.canSee(p))
                        .min(Comparator.comparingDouble(boss::squaredDistanceTo)).orElse(null);
                if (alternate != null && (boss.squaredDistanceTo(alternate) < 100 || !boss.canSee(previous))) {
                    target = alternate;
                    boss.setTarget(target);
                }
                nextSwitch = boss.age + 85;
            }
        }
        if (target == null) return;
        if (boss.canSee(target)) hiddenTicks = 0;
        else if (++hiddenTicks > 120) {
            boss.setTarget(null);
            hiddenTicks = 0;
            cancel();
            return;
        }

        motion.sample(boss, target);
        if (action != null) {
            if (!valid(victim, 56)) {
                cancel();
                return;
            }
            tickAction(world);
            return;
        }
        double distance = CombatMotion.distance(boss, target);
        if (needsClearance && boss.age >= nextPath) {
            nextPath = boss.age + 18;
            if (boss.seekHybridClearance()) return;
        }
        if (needsClearance && !boss.getNavigation().isIdle()) return;
        if (recovery > 0) {
            --recovery;
        } else if (boss.canSee(target)) {
            Move chosen = choose(target, distance);
            if (chosen != null) {
                start(chosen, target);
                return;
            }
        }
        if (boss.age >= nextPath) {
            nextPath = boss.age + (boss.isFinalPhase() ? 6 : 9);
            if (distance < 1.4 && boss.canSee(target)) {
                boss.getNavigation().stop();
            } else {
                boss.getNavigation().startMovingTo(target, boss.isFinalPhase() ? 1.28 : 1.08);
            }
        }
    }

    private Move choose(LivingEntity target, double distance) {
        boolean close = CombatMotion.meleeHeight(boss, target);
        boolean finale = boss.isFinalPhase();
        boolean stalled = boss.age - lastHit > 75;
        if (follow != null) {
            Move next = follow;
            follow = null;
            if (eligible(next, target, distance, close)) {
                chained = true;
                return next;
            }
        }
        chained = false;
        Move selected = null;
        double best = 0;
        for (Move move : Move.values()) {
            if (move == last || !eligible(move, target, distance, close)) continue;
            double weight = switch (move) {
                case LEFT, RIGHT -> 5;
                case COMBO -> finale ? 8 : 5;
                case TRIPLE -> finale ? 5 : 2;
                case DASH -> stalled || motion.retreatingFrom(boss) ? 10 : 5;
                case DODGE -> 4;
                case GRAB -> 2;
                case WING -> players(4).size() > 1 ? 12 : 5;
                case LEAP, DIVE -> !close || stalled ? 9 : 4;
                case VOLLEY -> distance > 7 ? 9 : 3;
                case BURST, SWARM -> 4;
            };
            double score = Math.pow(boss.getRandom().nextDouble(), 1 / weight);
            if (score > best) {
                best = score;
                selected = move;
            }
        }
        return selected;
    }

    private boolean eligible(Move move, LivingEntity target, double distance, boolean close) {
        boolean hybrid = boss.isHybrid();
        if (boss.age < ready.getOrDefault(move, 0) || (hybrid ? move.hybrid : move.human) == null) return false;
        return switch (move) {
            case LEFT, RIGHT, COMBO, TRIPLE -> close && distance <= (hybrid ? 3.2 : 2.35);
            case DASH -> close && boss.isOnGround() && distance >= 3.4 && distance <= 9.5;
            case DODGE -> close && boss.isOnGround() && distance <= 3.1 && target instanceof ServerPlayerEntity
                    && (target.handSwinging);
            case GRAB -> close && distance < 1.65;
            case WING -> close && distance <= 4.2;
            case LEAP -> boss.isOnGround() && distance >= 3.2 && distance <= 11 && Math.abs(target.getY()-boss.getY()) < 5;
            case DIVE -> boss.isOnGround() && distance <= 19 && (distance > 6 || !close)
                    && boss.getWorld().isSpaceEmpty(boss, boss.getBoundingBox().stretch(0, 2.3, 0));
            case VOLLEY -> distance >= 4.5 && distance <= 25;
            case BURST -> close && distance < 5;
            case SWARM -> boss.isFinalPhase() && distance <= 18;
        };
    }

    private void start(Move move, LivingEntity target) {
        action = move;
        victim = target;
        elapsed = 0;
        facing = CombatMotion.face(boss, motion.lead(target, 3, 1.1), 360);
        direction = facing;
        if (move == Move.DODGE) {
            dodgeStart = boss.getPos();
            dodgeHealth = boss.getHealth();
            int choice = boss.getRandom().nextInt(3);
            direction = choice == 0 ? facing.negate() : new Vec3d(-facing.z, 0, facing.x).multiply(choice == 1 ? 1 : -1);
        }
        boss.getNavigation().stop();
        ready.put(move, boss.age + move.cooldown);
        boss.combatClip(move == Move.DASH ? "dash_ready" : boss.isHybrid() ? move.hybrid : move.human);
    }

    private void tickAction(ServerWorld world) {
        ++elapsed;
        boss.getNavigation().stop();
        CombatMotion.holdFacing(boss, facing);
        if (action != Move.LEAP && action != Move.DIVE) boss.setVelocity(0, boss.getVelocity().y, 0);
        double reach = boss.isHybrid() ? 3.45 : 2.65;
        float claw = boss.isHybrid() ? 14 : 10;
        switch (action) {
            case LEFT, RIGHT -> {
                if (elapsed == 6) sector(reach, .42, claw, 0);
            }
            case COMBO, TRIPLE -> {
                if (elapsed == 6 || elapsed == 17) sector(reach, .32, claw, .15);
                if (action == Move.TRIPLE && elapsed == 23) {
                    facing = CombatMotion.face(boss, victim.getPos(), 28);
                    boss.combatClip(boss.isHybrid() ? "claw_right" : "claw_attack_right");
                }
                if (action == Move.TRIPLE && elapsed == 29) sector(reach, .12, claw + 4, .55);
            }
            case DASH -> {
                if (elapsed >= 7 && elapsed <= 13) {
                    if (CombatMotion.distance(boss, victim) > 1.25) groundStep(.96);
                    if (elapsed == 13) boss.combatClip(boss.isHybrid() ? "claw_right" : "claw_attack_right");
                }
                if (elapsed == 19) sector(reach, .3, claw + 3, .45);
            }
            case DODGE -> {
                if (elapsed >= 3 && elapsed <= 7) groundStep(.62);
            }
            case GRAB -> {
                if (elapsed == 14 && CombatMotion.contact(boss, victim, facing, 1.9, .65)) {
                    grabbed = victim;
                    grabbed.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS, 9, 1));
                }
                if (elapsed == 21 && grabbed != null && CombatMotion.contact(boss, grabbed, facing, 2.1, .3)
                        && hit(grabbed, 16, .15)) boss.heal(5);
            }
            case WING -> {
                if (elapsed == 11) sector(3.85, -.06, 15, 1.1);
            }
            case LEAP, DIVE -> flight();
            case VOLLEY -> {
                if (elapsed == 14 || elapsed == 19 || elapsed == 24 || (boss.isFinalPhase() && elapsed == 29)) shoot(false);
            }
            case BURST -> {
                if (elapsed < 24 && elapsed % 4 == 0) ring(world, 4.9);
                if (elapsed == 24) {
                    ring(world, 4.9);
                    boss.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), .85f, 1.5f);
                    area(5, 23, .8);
                }
            }
            case SWARM -> {
                if (elapsed == 17 || elapsed == 23 || elapsed == 29) shoot(true);
            }
        }
        if (elapsed >= (landTick == 0 ? action.duration : landTick + 13)) finish();
    }

    private void groundStep(double speed) {
        if (boss.hasStatusEffect(StatusEffects.SLOWNESS)) speed *= .58;
        CombatMotion.safeStep(boss, direction.multiply(speed), true);
    }

    private void flight() {
        boolean dive = action == Move.DIVE;
        if (elapsed == 8) {
            double leapSpeed = Math.clamp(CombatMotion.distance(boss, victim) / 11, .35, .92);
            Vec3d rise = direction.multiply(dive ? .24 : leapSpeed).add(0, dive ? .63 : .72, 0);
            if (!CombatMotion.safeStep(boss, rise, false)) {
                elapsed = action.duration;
                return;
            }
            boss.setVelocity(rise);
            airborne = true;
            if (dive) boss.setNoGravity(true);
        }
        if (dive && airborne && landTick == 0) {
            if (elapsed == 17) {
                boss.combatClip("flight_idle");
                boss.setVelocity(0, .03, 0);
            }
            if (elapsed == 23) {
                landingAim = motion.lead(victim, 4, 1.5);
                direction = CombatMotion.face(boss, landingAim, 360);
                facing = direction;
                boss.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 1, .8f);
            }
            if (elapsed == 28) {
                boss.combatClip("flight_move");
                boss.setNoGravity(false);
            }
            if (elapsed >= 28 && !boss.isOnGround()) {
                Vec3d delta = landingAim.subtract(boss.getPos()).multiply(1, 0, 1);
                double speed = Math.min(1.38, delta.length());
                Vec3d plunge = delta.normalize().multiply(speed).add(0, -Math.clamp(
                        (boss.getY()-landingAim.y)*.22, .34, .85), 0);
                if (CombatMotion.safeStep(boss, plunge, false)) boss.setVelocity(plunge);
                else boss.setVelocity(0, Math.min(0, boss.getVelocity().y), 0);
            }
            if (elapsed > 8 && elapsed < 17 && boss.verticalCollision) {
                boss.setNoGravity(false);
                elapsed = action.duration;
                return;
            }
        }
        if (airborne && elapsed >= (dive ? 29 : 11) && landTick == 0 && boss.isOnGround()) {
            landTick = elapsed;
            boss.combatClip("land");
        }
        if (landTick != 0 && elapsed == landTick + 3) area(dive ? 3.6 : 3.35, dive ? 22 : 18, .65);
    }

    private void finish() {
        Move finished = action;
        last = finished;
        boolean landed = landTick != 0;
        endAction();
        recovery = boss.isFinalPhase() ? (boss.getHealth() < boss.getMaxHealth()*.10f ? 4 : 6) : 10;
        if (finished == Move.DODGE && boss.getPos().squaredDistanceTo(dodgeStart) > .36
                && boss.getHealth() >= dodgeHealth && boss.getRandom().nextBoolean()) follow = Move.LEFT;
        if (boss.isFinalPhase() && !chained && boss.getRandom().nextFloat() < .68f) {
            follow = switch (finished) {
                case LEFT, RIGHT, COMBO -> Move.WING;
                case LEAP -> landed ? Move.BURST : null;
                case VOLLEY -> Move.DASH;
                case DASH -> Move.COMBO;
                default -> null;
            };
        }
        if (chained || finished == Move.BURST || finished == Move.DIVE) recovery += 7;
    }

    private void sector(double radius, double dot, float damage, double push) {
        if (!boss.isHybrid()) {
            if (CombatMotion.contact(boss, victim, facing, radius, dot)) hit(victim, damage, push);
            return;
        }
        for (var player : players(radius + 1)) {
            if (CombatMotion.contact(boss, player, facing, radius, dot)) hit(player, damage, push);
        }
    }

    private void area(double radius, float damage, double push) {
        for (var player : players(radius + 1)) {
            double distance = boss.getPos().distanceTo(player.getPos());
            if (distance <= radius && Math.abs(player.getY()-boss.getY()) <= 2 && boss.canSee(player)) {
                hit(player, damage * (float)(1 - .45 * distance / radius), push);
            }
        }
    }

    private boolean hit(LivingEntity target, float damage, double push) {
        if (!target.damage(boss.getDamageSources().mobAttack(boss), damage)) return false;
        lastHit = boss.age;
        if (push > 0) target.takeKnockback(push, boss.getX()-target.getX(), boss.getZ()-target.getZ());
        return true;
    }

    private void shoot(boolean swarm) {
        if (!valid(victim, 30) || !boss.canSee(victim) || shots.size() >= 12) return;
        Vec3d origin = boss.getPos().add(0, 1.65, 0);
        Vec3d velocity = victim.getBoundingBox().getCenter().subtract(origin).normalize().multiply(swarm ? .49 : .79);
        shots.add(new Shot(origin, velocity, swarm ? 38 : 34, swarm));
        boss.playSound(swarm ? SoundEvents.ENTITY_BAT_TAKEOFF : SoundEvents.ENTITY_BLAZE_SHOOT, .7f, swarm ? .8f : 1.4f);
    }

    private void tickShots(ServerWorld world) {
        for (int i = shots.size()-1; i >= 0; --i) {
            Shot shot = shots.get(i);
            Vec3d start = shot.position, end = start.add(shot.velocity);
            if (!world.isChunkLoaded(net.minecraft.util.math.BlockPos.ofFloored(end))) {
                shots.remove(i);
                continue;
            }
            var block = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, boss));
            if (block.getType() != HitResult.Type.MISS) end = block.getPos();
            double width = shot.swarm ? .48 : .21;
            var entity = ProjectileUtil.getEntityCollision(world, boss, start, end,
                    new Box(start, end).expand(width + .3), e -> e instanceof ServerPlayerEntity p && valid(p, 56), (float)width);
            if (entity != null && world.raycast(new RaycastContext(start, entity.getEntity().getBoundingBox().getCenter(),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss)).getType() == HitResult.Type.MISS) {
                hit((LivingEntity)entity.getEntity(), shot.swarm ? 9 : 12, 0);
            }
            if (shot.swarm) {
                Vec3d side = new Vec3d(-shot.velocity.z, 0, shot.velocity.x).normalize().multiply(.38);
                for (int wing : new int[]{-1, 1}) {
                    world.spawnParticles(ParticleTypes.SMOKE, end.x+wing*side.x,
                            end.y + .14*Math.sin(shot.ticks*1.3), end.z+wing*side.z, 2, .10, .06, .10, .012);
                }
            } else {
                world.spawnParticles(ParticleTypes.CRIMSON_SPORE, end.x, end.y, end.z, 3, .12, .12, .12, .02);
            }
            if (entity != null || block.getType() != HitResult.Type.MISS || shot.ticks <= 1) shots.remove(i);
            else shots.set(i, new Shot(end, shot.velocity, shot.ticks-1, shot.swarm));
        }
    }

    private void ring(ServerWorld world, double radius) {
        for (int i = 0; i < 16; ++i) {
            double angle = i * Math.PI / 8;
            world.spawnParticles(ParticleTypes.CRIMSON_SPORE, boss.getX()+Math.cos(angle)*radius,
                    boss.getY()+.28, boss.getZ()+Math.sin(angle)*radius, 1, 0, 0, 0, 0);
        }
    }

    List<ServerPlayerEntity> players(double distance) {
        return boss.getWorld().getEntitiesByClass(ServerPlayerEntity.class, boss.getBoundingBox().expand(distance),
                p -> valid(p, distance));
    }

    private boolean valid(LivingEntity entity, double distance) {
        return entity != null && entity.getWorld() == boss.getWorld() && entity.isAlive() && !entity.isSpectator()
                && (!(entity instanceof ServerPlayerEntity p) || !p.isCreative())
                && boss.squaredDistanceTo(entity) <= distance * distance;
    }
}
