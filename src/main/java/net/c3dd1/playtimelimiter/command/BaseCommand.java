package net.c3dd1.playtimelimiter.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.*;

public class BaseCommand {
    protected LiteralArgumentBuilder<CommandSourceStack> builder;
    boolean enabled;

    public BaseCommand(String name, int permissionLevel, boolean enabled) {
        this.builder = Commands.literal(name).requires(source -> source.hasPermission(permissionLevel));
        this.enabled = enabled;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getBuilder() {
        return builder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LiteralArgumentBuilder<CommandSourceStack> setExecution() {
        return null;
    }

}
