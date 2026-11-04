package com.witchersbestiary.contracts;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

public final class Contracts {
    public static final ContractDefinitions DEFINITIONS = new ContractDefinitions();
    private Contracts() {}
    public static ContractInstance assign(ServerPlayerEntity player, Identifier definitionId) {return assign(player,definitionId,null);}
    public static ContractInstance assign(ServerPlayerEntity player, Identifier definitionId, Long offerSeed) {return assign(player,definitionId,offerSeed,false);}
    /** Player-facing boards only commit a hunt after both clues and its lair are validated. */
    public static ContractInstance assignPrepared(ServerPlayerEntity player, Identifier definitionId, Long offerSeed) {return assign(player,definitionId,offerSeed,true);}
    private static ContractInstance assign(ServerPlayerEntity player, Identifier definitionId, Long offerSeed, boolean requireRoute) {
        com.witchersbestiary.contracts.hunt.SwampLord.validateAccept(player,definitionId);
        var definition = DEFINITIONS.find(definitionId);
        if (definition == null) throw new IllegalArgumentException("Missing/invalid contract definition: " + definitionId);
        if (player.experienceLevel < definition.minimumLevel() || (!definition.dimension().isEmpty()
                && !player.getServerWorld().getRegistryKey().getValue().toString().equals(definition.dimension())))
            throw new IllegalStateException("Player does not meet contract eligibility");
        var store = ContractState.get(player.getServer()); store.writable();
        for (var existing : store.ownedBy(player.getUuid())) if (existing.expire(System.currentTimeMillis())) store.markDirty();
        if (store.ownedBy(player.getUuid()).stream().filter(c -> c.status() == ContractInstance.Status.ACCEPTED || c.status() == ContractInstance.Status.COMPLETED).count() >= 16)
            throw new IllegalStateException("At most 16 active/unclaimed contracts per player");
        var instanceId=UUID.randomUUID();
        var profile=com.witchersbestiary.contracts.procedural.ProceduralHunts.isDefinition(definitionId)?com.witchersbestiary.contracts.procedural.ProceduralProfile.generate(offerSeed==null?instanceId.getMostSignificantBits()^instanceId.getLeastSignificantBits():offerSeed,com.witchersbestiary.contracts.procedural.ContractSpecies.species(definition.target().getPath())):null;
        if(profile!=null)definition=com.witchersbestiary.contracts.procedural.ProceduralHunts.snapshot(definition,profile);
        definition=com.witchersbestiary.economy.CrownRewards.fresh(definition);
        var c = new ContractInstance(instanceId, player.getUuid(), definition, System.currentTimeMillis());c.procedural=profile;
        c.transition(ContractInstance.Status.ACCEPTED);
        if(requireRoute&&com.witchersbestiary.contracts.hunt.SwampLord.named(c)&&!com.witchersbestiary.contracts.hunt.SwampLord.prepareAcceptance(player,c)){
            player.sendMessage(Text.translatable("contract.witchersbestiary.no_safe_route"),false);
            throw new IllegalStateException("No safe loaded hunt area with two clues; contract not accepted");
        }
        store.add(c); com.witchersbestiary.contracts.hunt.SwampLord.accepted(player,c); return c;
    }
    /** DamageSource resolves projectile owners; no fallback to last attacker for environmental deaths. */
    public static ServerPlayerEntity killer(LivingEntity victim, DamageSource damage) {
        return damage.getAttacker() instanceof ServerPlayerEntity player && victim.getWorld() instanceof ServerWorld world
                && player.getServer() == world.getServer() ? player : null;
    }
    public static void onDeath(LivingEntity victim, DamageSource source) {
        if(com.witchersbestiary.contracts.hunt.SwampLord.death(victim)){com.witchersbestiary.contracts.tracking.Investigations.afterDeath(victim);return;}
        var player = killer(victim, source);
        if (player == null || victim.isAlive()) return;
        var store = ContractState.get(player.getServer()); if (store.readOnly()) return; long now = System.currentTimeMillis();
        var type = Registries.ENTITY_TYPE.getId(victim.getType());
        for (var c : store.ownedBy(player.getUuid())) {
            if (c.expire(now)) store.markDirty();
            if (!com.witchersbestiary.contracts.hunt.SwampLord.named(c) && DEFINITIONS.find(c.definition().id()) != null && c.credit(victim.getUuid(), player.getUuid(), type, now)) store.markDirty();
        }
    }
    /** Destruction without a credited death is a failed hunt, never completion or permission to duplicate a target. */
    public static void targetRemoved(net.minecraft.entity.Entity e,ServerWorld world){
        var reason=e.getRemovalReason();if(reason==null||!reason.shouldDestroy())return;
        var binding=com.witchersbestiary.contracts.procedural.ProceduralHunts.binding(e);if(binding==null)return;
        var store=ContractState.get(world.getServer());if(store.readOnly())return;var c=store.find(binding);
        if(c==null||c.hunt==null||!e.getUuid().equals(c.hunt.target)||c.status()!=ContractInstance.Status.ACCEPTED)return;
        c.transition(ContractInstance.Status.CANCELLED);c.hunt.status=com.witchersbestiary.contracts.hunt.SwampHunt.Status.CANCELLED;
        if(c.investigation!=null)c.investigation.stage=com.witchersbestiary.contracts.tracking.ContractInvestigation.Stage.CANCELLED;
        store.markDirty();
        if(e instanceof com.witchersbestiary.entity.monster.DrownerEntity d)com.witchersbestiary.contracts.hunt.SwampLord.cleanup(d,world);
        if(e instanceof com.witchersbestiary.entity.monster.FogletEntity f)f.clearContractCopies(world);
    }
    public static void claim(ServerPlayerEntity player, UUID instance) {
        var store = ContractState.get(player.getServer()); var c = store.find(instance);
        if (c == null || DEFINITIONS.find(c.definition().id()) == null) throw new IllegalStateException("Contract or its definition is unavailable");
        ContractRewards.claim(store, c, player);if(instance.equals(store.tracked(player.getUuid())))store.track(player.getUuid(),null);com.witchersbestiary.contracts.tracking.Investigations.refresh(player);
    }
    public static void cancel(ServerPlayerEntity player, UUID instance) {
        var store = ContractState.get(player.getServer()); store.writable(); var c = store.find(instance);
        if (c == null || !c.owner().equals(player.getUuid())) throw new IllegalArgumentException("Contract not owned by player");
        c.transition(ContractInstance.Status.CANCELLED); if(c.hunt!=null)c.hunt.status=com.witchersbestiary.contracts.hunt.SwampHunt.Status.CANCELLED;if(c.investigation!=null)c.investigation.stage=com.witchersbestiary.contracts.tracking.ContractInvestigation.Stage.CANCELLED; store.markDirty();
        if(instance.equals(store.tracked(player.getUuid())))store.track(player.getUuid(),null);
        if(c.hunt!=null)com.witchersbestiary.contracts.hunt.SwampLord.cancelSearch(player);
        com.witchersbestiary.contracts.tracking.Investigations.refresh(player);
    }
    public static UUID abandonActive(ServerPlayerEntity player) {
        var store=ContractState.get(player.getServer());store.writable();
        var selected=com.witchersbestiary.contracts.tracking.Investigations.activeContract(player);
        if(selected==null||selected.status()!=ContractInstance.Status.ACCEPTED){
            var accepted=store.ownedBy(player.getUuid()).stream().filter(c->c.status()==ContractInstance.Status.ACCEPTED).toList();
            if(accepted.size()!=1)throw new IllegalStateException(accepted.isEmpty()?"No active contract to abandon":"Multiple active contracts; no contract was changed");
            selected=accepted.getFirst();
        }
        UUID id=selected.id();cancel(player,id);return id;
    }
    private static int command(ServerCommandSource source, Runnable action) {
        try { action.run(); return 1; }
        catch (IllegalArgumentException | IllegalStateException e) { source.sendError(Text.literal(e.getMessage())); return 0; }
    }
    public static void initialize() {
        com.witchersbestiary.contracts.hunt.SwampLord.initialize();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(DEFINITIONS);
        ServerLivingEntityEvents.AFTER_DEATH.register(Contracts::onDeath);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("wbcontract")
            .then(literal("abandon").executes(context -> {var player=context.getSource().getPlayerOrThrow();return command(context.getSource(),()->{var id=abandonActive(player);context.getSource().sendFeedback(()->Text.translatable("contract.witchersbestiary.abandoned",id.toString()),false);});}))));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("wbcontracts").requires(s -> s.hasPermissionLevel(2))
            .then(literal("difficulty").then(argument("target",EntityArgumentType.entity()).executes(c->{var entity=EntityArgumentType.getEntity(c,"target");if(!(entity instanceof LivingEntity living)){c.getSource().sendError(Text.literal("Target must be living"));return 0;}c.getSource().sendFeedback(()->Text.literal(com.witchersbestiary.combat.ProgressiveDifficulty.describe(living)),false);return 1;})))
            .then(literal("list").executes(c -> { DEFINITIONS.all().values().stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(d -> c.getSource().sendFeedback(() -> Text.literal(d.id() + " target=" + d.target() + " kills=" + d.required()), false)); return DEFINITIONS.all().size(); }))
            .then(literal("assign").then(argument("player", EntityArgumentType.player()).then(argument("definition", StringArgumentType.word()).suggests((c,b) -> { DEFINITIONS.all().keySet().forEach(id -> b.suggest(id.toString())); return b.buildFuture(); }).executes(c -> {
                var player = EntityArgumentType.getPlayer(c,"player"); return command(c.getSource(), () -> { var instance = assignPrepared(player, Identifier.of(StringArgumentType.getString(c,"definition")),null); c.getSource().sendFeedback(() -> Text.literal("Assigned " + instance.id()), true); }); }))))
            .then(literal("inspect").then(argument("player", EntityArgumentType.player()).executes(c -> {
                var player = EntityArgumentType.getPlayer(c,"player"); var store = ContractState.get(player.getServer());
                for (var instance : store.ownedBy(player.getUuid())) {
                    if (instance.expire(System.currentTimeMillis())) store.markDirty();
                    c.getSource().sendFeedback(() -> Text.literal(instance.id() + " " + instance.definition().id() + " " + instance.status() + " " + instance.progress() + "/" + instance.definition().required() + (DEFINITIONS.find(instance.definition().id()) == null ? " [definition unavailable]" : "")), false);
                } return 1; })))
            .then(literal("claim").then(argument("instance", StringArgumentType.word()).executes(c -> { var player = c.getSource().getPlayerOrThrow(); return command(c.getSource(), () -> claim(player, UUID.fromString(StringArgumentType.getString(c,"instance")))); })))
            .then(literal("cancel").then(argument("instance", StringArgumentType.word()).executes(c -> { var player = c.getSource().getPlayerOrThrow(); return command(c.getSource(), () -> cancel(player, UUID.fromString(StringArgumentType.getString(c,"instance")))); })))
        ));
    }
}
