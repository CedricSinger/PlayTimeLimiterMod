package net.c3dd1.playtimelimiter.init;

import java.util.ArrayList;
import com.mojang.brigadier.CommandDispatcher;
import net.c3dd1.playtimelimiter.command.*;
import net.c3dd1.playtimelimiter.command.impl.*;
import net.minecraft.commands.*;
import net.minecraftforge.event.*;

public class CommandInit {
    private static final ArrayList<BaseCommand> commands = new ArrayList<>();

    public static void registerCommands(final RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        commands.add(new ShowTimeCommand("showtime", 0, true));
        commands.add(new SetPlaytimeCommand("setplaytime", 3, true));

        commands.forEach(command -> {
            if(command.isEnabled() && command.setExecution() != null) {
                dispatcher.register(command.getBuilder());
            }
        });
    }

}
