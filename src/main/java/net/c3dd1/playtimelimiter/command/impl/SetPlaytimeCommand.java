package net.c3dd1.playtimelimiter.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.c3dd1.playtimelimiter.command.BaseCommand;
import net.c3dd1.playtimelimiter.timer.PlayerTimerProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SetPlaytimeCommand extends BaseCommand {

    public SetPlaytimeCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution() {
        builder.then(Commands.argument("player", EntityArgument.player()));
        builder.then(Commands.argument("time", IntegerArgumentType.integer())
                .executes(source -> execute(source.getSource(), EntityArgument.getPlayer(source, "player"), IntegerArgumentType.getInteger(source, "time"))));
        return builder;
    }

    private int execute(CommandSourceStack source, Player player, int time) {
        player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
            timer.setLeftPlaytime((double) time);
        });
        return 1;
    }
}