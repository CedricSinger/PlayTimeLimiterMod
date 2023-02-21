package net.c3dd1.playtimelimiter.config;

import net.minecraftforge.common.ForgeConfigSpec;



public class PlaytimeLimiterServerConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ACTIVATED;
    public static final ForgeConfigSpec.ConfigValue<Double> ALLOWED_PLAYTIME;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_2_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_3_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_4_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Double> BONUS_TIME_5_OR_MORE_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<Integer> RESET_TIME_HOURS;
    public static final ForgeConfigSpec.ConfigValue<Integer> RESET_TIME_MINUTES;
    public static final ForgeConfigSpec.ConfigValue<String> BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<String> PLAYER_LIST;

    static {
        BUILDER.push("Configs for Playtime Limiter Mod");

        ACTIVATED = BUILDER.comment("Decides if Playtime Limitation is active, meaning players will get kicked after their playtime runs out")
                .define("Active", true);
        ALLOWED_PLAYTIME = BUILDER.comment("The daily allowed playtime for each player")
                .define("Time in minutes", 60.0);
        BONUS_TIME_2_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 2 players are online")
                .define("2-Player-Factor", 0.5);
        BONUS_TIME_3_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 3 players are online")
                .define("3-Player-Factor", 1.0/3.0);
        BONUS_TIME_4_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 4 players are online")
                .define("4-Player-Factor", 0.0);
        BONUS_TIME_5_OR_MORE_PLAYERS = BUILDER.comment("The multiplier for allowed playtime when 5 or more players are online")
                .define("5-Player-Factor", 0.0);
        RESET_TIME_HOURS = BUILDER.comment("Time at which the playtime limit resets (hours of day)")
                .define("Time in hours (24h-Format)", 12);
        RESET_TIME_MINUTES = BUILDER.comment("Time at which the playtime limit resets (minutes of hour)")
                .define("Time in minutes", 0);
        BLACKLIST = BUILDER.comment("Players who will have no playtime limit")
                        .define("List of players, separated by ','", "");
        PLAYER_LIST = BUILDER.comment("Don't change if you don't know what you're doing! List of all Players and their respective playtime, \nonly updated when players log out and when playtime gets updated")
                        .define("Players", "");


        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
