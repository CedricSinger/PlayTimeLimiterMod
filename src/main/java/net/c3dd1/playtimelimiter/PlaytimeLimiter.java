package net.c3dd1.playtimelimiter;

import com.mojang.logging.LogUtils;
import net.c3dd1.playtimelimiter.config.PlaytimeLimiterServerConfigs;
import net.c3dd1.playtimelimiter.init.CommandInit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
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
import org.slf4j.Logger;
import net.minecraftforge.event.*;

import java.util.LinkedList;
import net.c3dd1.playtimelimiter.timer.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PlaytimeLimiter.MODID)
public class PlaytimeLimiter
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "playtimelimiter";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private LinkedList<Integer> blockList = new LinkedList<>();
    private LinkedList<Integer> refreshList = new LinkedList<>();

    private LinkedList<String> blacklist = new LinkedList<>();
    private Double allowedPlaytime;
    private Double bonusTime2P;
    private Double bonusTime3P;
    private Double bonusTime4P;
    private Double bonusTime5P;
    private Integer resetTime;

    private long oldTime;



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
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        String blacklistReadout = PlaytimeLimiterServerConfigs.BLACKLIST.get();
        allowedPlaytime = PlaytimeLimiterServerConfigs.ALLOWED_PLAYTIME.get();
        bonusTime2P = PlaytimeLimiterServerConfigs.BONUS_TIME_2_PLAYERS.get();
        bonusTime3P = PlaytimeLimiterServerConfigs.BONUS_TIME_3_PLAYERS.get();
        bonusTime4P = PlaytimeLimiterServerConfigs.BONUS_TIME_4_PLAYERS.get();
        bonusTime5P = PlaytimeLimiterServerConfigs.BONUS_TIME_5_OR_MORE_PLAYERS.get();
        resetTime = PlaytimeLimiterServerConfigs.RESET_TIME.get();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandInit.registerCommands(event);
    }




    /*
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }*/



    @Mod.EventBusSubscriber(modid = MODID, value = Dist.DEDICATED_SERVER)
    public class ServerModEvents {

        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            event.getEntity().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                event.getEntity().sendSystemMessage(Component.literal("Remaining Playtime: " + timer.getLeftPlaytime() + "minutes"));
            });
        }

        @SubscribeEvent
        public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {

        }

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
                event.getOriginal().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(oldStore -> {
                    event.getOriginal().getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(newStore -> {
                        newStore.copyFrom(oldStore);
                    });
                });
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerTimer.class);
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            /*if(java.time.ZonedDateTime.now().getHour() == resetTime && java.time.ZonedDateTime.now().getMinute() == 0 && java.time.ZonedDateTime.now().getSecond() == 0) {
                for(Player player : event.getServer().getPlayerList().getPlayers()) {
                    player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                        timer.setLeftPlaytime(allowedPlaytime);
                    });
                }
                for(Integer playerID : blockList) {
                    refreshList.add(playerID);
                    blockList.remove(playerID);
                }
            }


            int playersOnline = event.getServer().getPlayerCount();
            double amountToBeDecreased;
            long timeSinceLastTick = (System.currentTimeMillis() - oldTime);
            oldTime = System.currentTimeMillis();
            switch(playersOnline) {
                case 1:
                    amountToBeDecreased = timeSinceLastTick * 0.00001666667;
                    break;
                case 2:
                    amountToBeDecreased = timeSinceLastTick * 0.00001666667 * bonusTime2P;
                    break;
                case 3:
                    amountToBeDecreased = timeSinceLastTick * 0.00001666667 * bonusTime3P;
                    break;
                case 4:
                    amountToBeDecreased = timeSinceLastTick * 0.00001666667 * bonusTime4P;
                    break;
                default:
                    amountToBeDecreased = timeSinceLastTick * 0.00001666667 * bonusTime5P;
                    break;
            }

            for(Player player : event.getServer().getPlayerList().getPlayers()) {
                player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                    boolean playtimeLeft = timer.decreaseLeftPlaytime(amountToBeDecreased);
                    if(timer.getLeftPlaytime() == 10.0 || timer.getLeftPlaytime() == 5.0) {
                        player.sendSystemMessage(Component.literal("Remaining Playtime: " + timer.getLeftPlaytime() + "minutes"));
                    }
                    else if(timer.getLeftPlaytime() == 1.0) {
                        player.sendSystemMessage(Component.literal("Remaining Playtime: 1 minute"));
                    }
                    if(!playtimeLeft) {
                        Integer playerID = player.getId();
                        if(refreshList.contains(playerID)) {
                            timer.setLeftPlaytime(allowedPlaytime);
                            refreshList.remove(playerID);
                        }
                        else {
                            blockList.add(playerID);
                            for(ServerPlayer serverPlayer : event.getServer().getPlayerList().getPlayers()) {
                                if(serverPlayer.getId() == player.getId()) {
                                    serverPlayer.connection.disconnect(Component.literal("Your Playtime for today has run out."));
                                }
                            }
                        }
                    }
                });
            }*/
            for(ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                player.getCapability(PlayerTimerProvider.PLAYER_TIMER).ifPresent(timer -> {
                    boolean playtimeLeft = timer.decreaseLeftPlaytime(0.0001);
                });
                player.sendSystemMessage(Component.literal("ja moin"));
            }
        }
    }
}
