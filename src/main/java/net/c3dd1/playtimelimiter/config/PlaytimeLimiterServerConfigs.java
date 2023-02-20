package net.c3dd1.playtimelimiter.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class PlaytimeLimiterServerConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Double> ALLOWED_PLAYTIME;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_2_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_3_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_4_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_5_OR_MORE_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Integer> RESET_TIME;
    public static final ForgeConfigSpec.ConfigValue<String> BLACKLIST;

    static {
        BUILDER.push("Configs for Playtime Limiter Mod");

        ALLOWED_PLAYTIME = BUILDER.comment("The daily allowed playtime for each player")
                .define("Time in minutes", 120.0);
        BONUS_TIME_2_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 2 players are online")
                .define("Factor", 2.0/3.0);
        BONUS_TIME_3_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 3 players are online")
                .define("Factor", 0.5);
        BONUS_TIME_4_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 4 players are online")
                .define("Factor", 0.0);
        BONUS_TIME_5_OR_MORE_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 5 or more players are online")
                .define("Factor by which time passes slower", 0.0);
        RESET_TIME = BUILDER.comment("Time at which the playtime limit resets")
                .define("Time in hours (24h-Format)", 12);
        BLACKLIST = BUILDER.comment("Players who will have no playtime limit")
                        .define("List of players, separated by ','", "");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
