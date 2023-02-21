package net.c3dd1.playtimelimiter.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.c3dd1.playtimelimiter.command.BaseCommand;
import net.c3dd1.playtimelimiter.config.PlaytimeLimiterServerConfigs;
import net.c3dd1.playtimelimiter.timer.PlayerTimerProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static net.c3dd1.playtimelimiter.util.Utilities.mapToString;
import static net.c3dd1.playtimelimiter.util.Utilities.stringToMap;

public class ResetAllTimesCommand extends BaseCommand {

    public ResetAllTimesCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution() {
        return builder.then(Commands.argument("time", IntegerArgumentType.integer())
                .executes(source -> execute(source.getSource(), IntegerArgumentType.getInteger(source, "time"))));
    }

    private int execute(CommandSourceStack source, int time) {
        HashMap<String, Double> playerMap = stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
        for(Player player : source.getServer().getPlayerList().getPlayers()) {
            if(playerMap.containsKey(player.getUUID().toString())) {
                player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                    timer.setLeftPlaytime((double) time);
                });
            }
        }

        List<String> playerIDs = new LinkedList<>();
        for(ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            playerIDs.add(player.getUUID().toString());
        }
        for(String player : playerMap.keySet()) {
            if(!playerIDs.contains(player)) {
                playerMap.put(player, ((double) time));
            }
        }
        PlaytimeLimiterServerConfigs.PLAYER_LIST.set(mapToString(playerMap));
        return 1;
    }
}
