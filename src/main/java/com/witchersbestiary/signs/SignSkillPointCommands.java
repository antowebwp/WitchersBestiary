package com.witchersbestiary.signs;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

final class SignSkillPointCommands {
    private static final SimpleCommandExceptionType LIMIT = new SimpleCommandExceptionType(
            Text.literal("Skill point limit is 100000. Lower the unspent balance before adding points or refunding mastery."));

    private enum Action { ADD, SET, GET, RESET }

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(literal("wb").then(literal("skillpoints")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("add").then(target(argument("amount",
                                IntegerArgumentType.integer(0, SignPlayerState.MAX_POINTS)), Action.ADD)))
                        .then(literal("set").then(target(argument("amount",
                                IntegerArgumentType.integer(0, SignPlayerState.MAX_POINTS)), Action.SET)))
                        .then(target(literal("get"), Action.GET))
                        .then(target(literal("reset"), Action.RESET)))));
    }

    private static <T extends ArgumentBuilder<ServerCommandSource, T>> T target(T node, Action action) {
        return node.executes(context -> execute(context, action, context.getSource().getPlayerOrThrow()))
                .then(argument("player", EntityArgumentType.player())
                        .executes(context -> execute(context, action, EntityArgumentType.getPlayer(context, "player"))));
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
                               Action action, net.minecraft.server.network.ServerPlayerEntity player)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var state = SignManager.state(player);
        int refund = 0;
        switch (action) {
            case ADD -> {
                int amount = IntegerArgumentType.getInteger(context, "amount");
                if (amount > SignPlayerState.MAX_POINTS - state.points()) throw LIMIT.create();
                state.award(amount);
            }
            case SET -> state.setPoints(IntegerArgumentType.getInteger(context, "amount"));
            case RESET -> {
                refund = state.resetMastery();
                if (refund < 0) throw LIMIT.create();
            }
            case GET -> { }
        }
        if (action != Action.GET) {
            SignLedger.get(player.getServer()).markDirty();
            SignManager.syncMastery(player);
        }
        String suffix = action == Action.RESET ? " (refunded " + refund + ")" : "";
        context.getSource().sendFeedback(() -> Text.literal(
                player.getName().getString() + ": " + state.points() + " unspent sign skill points" + suffix), false);
        return Command.SINGLE_SUCCESS;
    }
}
