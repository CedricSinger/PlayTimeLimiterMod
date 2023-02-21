package net.c3dd1.playtimelimiter.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.c3dd1.playtimelimiter.command.BaseCommand;
import net.c3dd1.playtimelimiter.timer.PlayerTimerProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import static net.c3dd1.playtimelimiter.util.Utilities.round;



public class ShowTimeCommand extends BaseCommand {

    public ShowTimeCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution() {
        return builder.then(Commands.argument("player", EntityArgument.player())
                .executes(source -> execute(source.getSource(), EntityArgument.getPlayer(source, "player"))));

    }

    private int execute(CommandSourceStack source, Player player) {
        player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
            source.sendSystemMessage(Component.literal("Remaining Playtime of Player " + player + ": " + round(timer.getLeftPlaytime(), 2) + " minutes"));

        });
        return 1;
    }
}
