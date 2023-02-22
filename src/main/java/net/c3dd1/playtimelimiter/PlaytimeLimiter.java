package net.c3dd1.playtimelimiter;

import com.mojang.logging.LogUtils;
import net.c3dd1.playtimelimiter.config.PlaytimeLimiterServerConfigs;
import net.c3dd1.playtimelimiter.init.CommandInit;
import net.c3dd1.playtimelimiter.timer.PlayerTimer;
import net.c3dd1.playtimelimiter.timer.PlayerTimerProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import net.minecraftforge.event.*;

import java.util.*;

import static net.c3dd1.playtimelimiter.util.Utilities.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PlaytimeLimiter.MODID)
public class PlaytimeLimiter
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "playtimelimiter";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();



    private static Player playerBeingKicked;
    private static MinecraftServer serverReference;

    private static Timer timer = new Timer();
    private static Calendar date = Calendar.getInstance();

    private static List<String> blacklist = new LinkedList<>();
    private static Double allowedPlaytime;
    private static Double bonusTime2P;
    private static Double bonusTime3P;
    private static Double bonusTime4P;
    private static Double bonusTime5P;
    private static Integer resetTimeHours;
    private static Integer resetTimeMinutes;

    private static long oldTime;





    public PlaytimeLimiter()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PlaytimeLimiterServerConfigs.SPEC, "playtimelimiter-server.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        //Set date for resets
        /*date.set(Calendar.HOUR, PlaytimeLimiterServerConfigs.RESET_TIME_HOURS.get());
        date.set(Calendar.MINUTE, PlaytimeLimiterServerConfigs.RESET_TIME_MINUTES.get());
        date.set(Calendar.SECOND, 0);

        // Schedule playtime reset
        timer.schedule(
                new PlaytimeReset(),
                date.getTime()
        );*/

    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        serverReference = event.getServer();

        //Setup the Blacklist from config
        LOGGER.info("Blacklist in config: " + PlaytimeLimiterServerConfigs.BLACKLIST.get());
        String[] players = PlaytimeLimiterServerConfigs.BLACKLIST.get().split(",");
        LOGGER.info("Blacklist readout: " + players.toString());
        for (int i = 0; i < players.length; i++) {
            blacklist.add(players[i]);
        }
        LOGGER.info("Blacklist: " + blacklist.toString());

        //Read out the rest of the config file
        allowedPlaytime = PlaytimeLimiterServerConfigs.ALLOWED_PLAYTIME.get();
        bonusTime2P = PlaytimeLimiterServerConfigs.BONUS_TIME_2_PLAYERS.get();
        bonusTime3P = PlaytimeLimiterServerConfigs.BONUS_TIME_3_PLAYERS.get();
        bonusTime4P = PlaytimeLimiterServerConfigs.BONUS_TIME_4_PLAYERS.get();
        bonusTime5P = PlaytimeLimiterServerConfigs.BONUS_TIME_5_OR_MORE_PLAYERS.get();
        resetTimeHours = PlaytimeLimiterServerConfigs.RESET_TIME_HOURS.get();
        resetTimeMinutes = PlaytimeLimiterServerConfigs.RESET_TIME_MINUTES.get();
    }





    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandInit.registerCommands(event);
    }





    @Mod.EventBusSubscriber(modid = MODID)
    public class ServerModEvents {

        //The following three methods initialize and register the playtime capability for the player entity
        @SubscribeEvent
        public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
            if(event.getObject() instanceof Player) {
                if(!event.getObject().getCapability(PlayerTimerProvider.PLAYER_TIMER).isPresent()) {
                    event.addCapability(new ResourceLocation(MODID, "properties"), new PlayerTimerProvider());
                }
            }
        }
        @SubscribeEvent
        public static void onPlayerCloned(PlayerEvent.Clone event) {
            if(event.isWasDeath()) {
                event.getOriginal().reviveCaps();
                event.getOriginal().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(oldStore -> {
                    event.getEntity().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(newStore -> {
                        newStore.copyFrom(oldStore);
                    });
                });
                event.getOriginal().invalidateCaps();
            }
        }
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerTimer.class);
        }





        //Tells a player his left playtime when joining the server
        //and registers a player in the config file if he joins for the first time
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            String playerID = event.getEntity().getUUID().toString();

            //Register new players in config file
            event.getEntity().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                if(PlaytimeLimiterServerConfigs.PLAYER_LIST.get().equals("")) {
                    LOGGER.info("First player joined!");
                    PlaytimeLimiterServerConfigs.PLAYER_LIST.set(playerID + ":" + allowedPlaytime);
                    LOGGER.info("Current Player List: " + PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
                }
                else {
                    HashMap<String, Double> playerMap = stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get());

                    //Adds the player to the list in the config file
                    if(!playerMap.containsKey(playerID)) {
                        LOGGER.info("New player joined!");
                        playerMap.put(playerID, timer.getLeftPlaytime());
                        PlaytimeLimiterServerConfigs.PLAYER_LIST.set(mapToString(playerMap));
                    }

                    //Resets the playtime of the player if it was reset while the player was offline
                    else if(playerMap.containsKey(playerID)) {
                        timer.setLeftPlaytime(playerMap.get(playerID));
                    }
                }

            });

            //Deny connection on login if no playtime left
            if(stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get()).get(playerID) == 0.0) {
                Integer kickID = player.getId();
                for(ServerPlayer serverplayer : player.getServer().getPlayerList().getPlayers()) {
                    if(serverplayer.getId() == kickID) {
                        boolean playerOnBlacklist = false;
                        for(String s : blacklist) {
                            if(("literal{" + s + "}").equals(player.getName())) {
                                playerOnBlacklist = true;
                            }
                        }
                        if(PlaytimeLimiterServerConfigs.ACTIVATED.get() && !playerOnBlacklist) {
                            LOGGER.info("Player disconnected on login");
                            playerBeingKicked = player;
                            serverplayer.connection.disconnect(Component.literal("Your Playtime for today has run out."));
                        }
                    }
                }
            }

            //Tells the player how much playtime he has left
            event.getEntity().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                event.getEntity().sendSystemMessage(Component.literal("Remaining Playtime: " + round(timer.getLeftPlaytime(), 2) + " minutes"));
            });
        }





        //Saves the left playtime for the player when leaving the server
        //so his playtime still resets even if he is not online at the time
        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            event.getEntity().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                LOGGER.info("Left Playtime: " + timer.getLeftPlaytime());
                HashMap<String, Double> playerMap = stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
                playerMap.put(event.getEntity().getUUID().toString(), timer.getLeftPlaytime());
                PlaytimeLimiterServerConfigs.PLAYER_LIST.set(mapToString(playerMap));
                LOGGER.info("Player List: " + PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
            });
        }





        //Updates playtimes
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {

            //Reset playtimes at the specified time
            if(resetTimeHours.equals(java.time.LocalDateTime.now().getHour())
                    && resetTimeMinutes.equals(java.time.LocalDateTime.now().getMinute())
                    && java.time.LocalDateTime.now().getSecond() == 0) {
                LOGGER.info("Resetting playtime for all players");
                HashMap<String, Double> playerMap = stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
                for(Player player : event.getServer().getPlayerList().getPlayers()) {
                    if(playerMap.containsKey(player.getUUID().toString())) {
                        player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                            timer.setLeftPlaytime(allowedPlaytime);
                        });
                    }
                }

                List<String> playerIDs = new LinkedList<>();
                for(ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    playerIDs.add(player.getUUID().toString());
                }
                for(String player : playerMap.keySet()) {
                    if(!playerIDs.contains(player)) {
                        playerMap.put(player, allowedPlaytime);
                    }
                }
                PlaytimeLimiterServerConfigs.PLAYER_LIST.set(mapToString(playerMap));
            }



            //Calculate how much the playtime has to be decreased
            int playersOnline = event.getServer().getPlayerCount();
            double amount;
            double amountToBeDecreased;
            long timeSinceLastTick = (System.currentTimeMillis() - oldTime);
            oldTime = System.currentTimeMillis();
            switch(playersOnline) {
                case 1:
                    amount = timeSinceLastTick * 0.00001666667;
                    break;
                case 2:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime2P;
                    break;
                case 3:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime3P;
                    break;
                case 4:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime4P;
                    break;
                default:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime5P;
                    break;
            }
            if(amount > 0.1) {
                amountToBeDecreased = 0.0;
            }
            else {
                amountToBeDecreased = amount;
            }



            for(Player player : event.getServer().getPlayerList().getPlayers()) {

                //Don't handle if player is already being kicked
                if(player == playerBeingKicked) {
                    break;
                }

                //Decrease playtime of the player according to calculation
                player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                    Double timeBefore = timer.getLeftPlaytime();
                    boolean playtimeLeft = timer.decreaseLeftPlaytime(amountToBeDecreased);
                    Double timeAfter = timer.getLeftPlaytime();
                    if((timeBefore >= 10.0 && timeAfter < 10.0) || (timeBefore >= 5.0 && timeAfter < 5.0)) {
                        player.sendSystemMessage(Component.literal("Remaining Playtime: " + round(timer.getLeftPlaytime(), 2) + " minutes"));
                    }
                    else if(timeBefore >= 1.0 && timeAfter < 1.0) {
                        player.sendSystemMessage(Component.literal("Remaining Playtime: 1 minute"));
                    }

                    //If player has no playtime left, kick if needed
                    if(!playtimeLeft) {
                        Integer playerID = player.getId();
                        for(ServerPlayer serverplayer : event.getServer().getPlayerList().getPlayers()) {
                            if(serverplayer.getId() == playerID) {
                                boolean playerOnBlacklist = false;
                                for(String s : blacklist) {
                                    if(("literal{" + s + "}").equals(player.getName())) {
                                        playerOnBlacklist = true;
                                    }
                                }
                                if(PlaytimeLimiterServerConfigs.ACTIVATED.get() && !playerOnBlacklist) {
                                    serverplayer.connection.disconnect(Component.literal("Your Playtime for today has run out."));
                                }
                            }
                        }
                    }
                });
            }

            //Update playerBeingKicked if no player is being kicked
            if(event.getServer().getPlayerList().getPlayerCount() == 0) {
                playerBeingKicked = null;
            }
        }





        //Decreases playtime for each player according to how many players are online
        /*@SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            event.player.sendSystemMessage(Component.literal("Knecht"));
            //Calculate how much time a player will lose, depending on how many players are online
            int playersOnline = event.player.getServer().getPlayerCount();
            double amount;
            double amountToBeDecreased;
            long timeSinceLastTick = (System.currentTimeMillis() - oldTime);
            oldTime = System.currentTimeMillis();
            switch(playersOnline) {
                case 1:
                    amount = timeSinceLastTick * 0.00001666667;
                    break;
                case 2:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime2P;
                    break;
                case 3:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime3P;
                    break;
                case 4:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime4P;
                    break;
                default:
                    amount = timeSinceLastTick * 0.00001666667 * bonusTime5P;
                    break;
            }
            if(amount > 0.1) {
                amountToBeDecreased = 0.0;
            }
            else {
                amountToBeDecreased = amount;
            }


            //Decrease playtime of the player according to calculation
            event.player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                Double timeBefore = timer.getLeftPlaytime();
                boolean playtimeLeft = timer.decreaseLeftPlaytime(amountToBeDecreased);
                Double timeAfter = timer.getLeftPlaytime();
                if((timeBefore >= 10.0 && timeAfter < 10.0) || (timeBefore >= 5.0 && timeAfter < 5.0)) {
                    event.player.sendSystemMessage(Component.literal("Remaining Playtime: " + round(timer.getLeftPlaytime(), 2) + " minutes"));
                }
                else if(timeBefore >= 1.0 && timeAfter < 1.0) {
                    event.player.sendSystemMessage(Component.literal("Remaining Playtime: 1 minute"));
                }
                if(!playtimeLeft) {
                    Integer playerID = event.player.getId();
                    for(ServerPlayer serverplayer : event.player.getServer().getPlayerList().getPlayers()) {
                        if(serverplayer.getId() == playerID) {
                            boolean playerOnBlacklist = false;
                            for(String s : blacklist) {
                                if(("literal{" + s + "}").equals(event.player.getName())) {
                                    playerOnBlacklist = true;
                                }
                            }
                            if(PlaytimeLimiterServerConfigs.ACTIVATED.get() && !playerOnBlacklist) {
                                serverplayer.connection.disconnect(Component.literal("Your Playtime for today has run out."));
                            }
                        }
                    }
                }
            });
        }*/
    }

    /*public class PlaytimeReset extends TimerTask {
        public void run() {
            LOGGER.info("Resetting playtime for all players");
            HashMap<String, Double> playerMap = stringToMap(PlaytimeLimiterServerConfigs.PLAYER_LIST.get());
            for(Player player : serverReference.getPlayerList().getPlayers()) {
                if(playerMap.containsKey(player.getUUID().toString())) {
                    player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                        timer.setLeftPlaytime(allowedPlaytime);
                    });
                }
            }

            List<String> playerIDs = new LinkedList<>();
            for(ServerPlayer player : serverReference.getPlayerList().getPlayers()) {
                playerIDs.add(player.getUUID().toString());
            }
            for(String player : playerMap.keySet()) {
                if(!playerIDs.contains(player)) {
                    playerMap.put(player, allowedPlaytime);
                }
            }
            PlaytimeLimiterServerConfigs.PLAYER_LIST.set(mapToString(playerMap));
        }
    }*/
}
