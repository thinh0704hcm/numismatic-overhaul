package com.glisco.numismaticoverhaul;

import com.glisco.numismaticoverhaul.currency.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
// TODO: Offline player support – OfflineDataLookup removed in owo 0.13

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class NumismaticCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("numismatic")
                .then(literal("balance").requires(s -> Commands.LEVEL_MODERATORS.check(s.permissions()))
                        .then(argument("player", EntityArgument.players())
                                .then(literal("get").executes(NumismaticCommand::get))
                                .then(longSubcommand("set", "value", NumismaticCommand::set))
                                .then(longSubcommand("add", "amount", NumismaticCommand.modify(1)))
                                .then(longSubcommand("subtract", "amount", NumismaticCommand.modify(-1)))))
                .then(literal("serverworth").executes(NumismaticCommand::serverWorth)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> longSubcommand(String name, String argName, Command<CommandSourceStack> command) {
        return literal(name).then(argument(argName, LongArgumentType.longArg(0)).executes(command));
    }

    @SuppressWarnings("ConstantConditions")
    private static int serverWorth(CommandContext<CommandSourceStack> context) {
        final var playerManager = context.getSource().getServer().getPlayerList();

        var onlineUUIDs = playerManager.getPlayers().stream().map(Entity::getUUID).toList();
        // TODO: Offline player scanning removed – OfflineDataLookup no longer available

        long serverWorth = 0;
        for (var onlineId : onlineUUIDs) {
            serverWorth += ModComponents.get(playerManager.getPlayer(onlineId)).getValue();
        }

        // for (var offlineId : offlineUUIDs) { // TODO offline support
            // serverWorth += OfflineDataLookup.get(offlineId).getCompound("cardinal_components").getCompound("numismatic-overhaul:currency").get(CurrencyHelper.VALUE); // TODO offline support

        long finalServerWorth = serverWorth;
        context.getSource().sendSuccess(() -> Component.literal("numismatic §> server net worth: " + finalServerWorth).withStyle(ChatFormatting.DARK_GREEN), false);

        return (int) serverWorth;
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var value = LongArgumentType.getLong(context, "value");
        final var players = EntityArgument.getPlayers(context, "player");

        for (var player : players) {
            //noinspection deprecation
            ModComponents.get(player).setValue(value);
context.getSource().sendSuccess(() -> Component.literal("numismatic §> balance of " + player.getName().getString() + " set to: " + value).withStyle(ChatFormatting.DARK_GREEN), false);
        }

        return CurrencyConverter.asInt(value);
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var players = EntityArgument.getPlayers(context, "player");
        long totalBalance = 0;

        for (var player : players) {
            final long balance = ModComponents.get(player).getValue();
            totalBalance += balance;

context.getSource().sendSuccess(() -> Component.literal("numismatic §> balance of " + player.getName().getString() + ": " + balance).withStyle(ChatFormatting.DARK_GREEN), false);
        }

        return CurrencyConverter.asInt(totalBalance);
    }

    private static Command<CommandSourceStack> modify(long multiplier) {
        return context -> {
            final var amount = LongArgumentType.getLong(context, "amount");
            final var players = EntityArgument.getPlayers(context, "player");

            long lastValue = 0;

            for (var player : players) {
                final var currencyComponent = ModComponents.get(player);

                currencyComponent.silentModify(amount * multiplier);
                lastValue = currencyComponent.getValue();

                context.getSource().sendSuccess(() -> Component.literal("numismatic §> balance of " + player.getName().getString() + " set to: " + currencyComponent.getValue()).withStyle(ChatFormatting.DARK_GREEN), false);
}

            return CurrencyConverter.asInt(lastValue);
        };
    }
}
