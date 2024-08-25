package in.gracel.deathban;

import in.gracel.deathban.config.Config;
import in.gracel.deathban.helpers.MessageParser;
import in.gracel.deathban.core.DeathBanList;
import in.gracel.deathban.helpers.DateTimeCalculator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

import java.util.Date;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DeathBan.MOD_ID)
public class DeathBan {
    private static final Logger LOGGER = LogManager.getLogger();
    MinecraftServer server;
    DeathBanList banList;
    public static final String MOD_ID = "deathban";
    public static final String MOD_NAME = "DeathBan";
    public static boolean deathBanOn;

    public DeathBan() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "deathban.toml");
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        deathBanOn = Config.weekTime.get() != 0 || Config.dayTime.get() != 0 ||
                Config.hourTime.get() != 0 || Config.minuteTime.get() != 0;
    }

    @SubscribeEvent
    public void onStart(FMLServerStartedEvent serverStartedEvent) {
        server = serverStartedEvent.getServer();
        banList = new DeathBanList(this.server.getPlayerList().getBans());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        deathBanOn = Config.weekTime.get() != 0 || 
                        Config.dayTime.get() != 0 ||
                        Config.hourTime.get() != 0 || 
                        Config.minuteTime.get() != 0;

        if(!event.getPlayer().getPersistentData().getBoolean(MOD_ID + "joinedBefore") && 
            !this.server.isSingleplayer() && 
            deathBanOn) 
        {
            event.getPlayer().getPersistentData().putBoolean(MOD_ID + "joinedBefore", true);
            event.getPlayer().sendMessage(
                    MessageParser.firstTimeMessage((ServerPlayerEntity) event.getPlayer()),
                    event.getPlayer().getUUID()
            );
            LOGGER.info("Sent welcome message to " + event.getPlayer().getName().getString());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (this.server != null) {
            this.banList.removeBanIfTimeExpire();
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        deathBanOn = Config.weekTime.get() != 0 || 
                        Config.dayTime.get() != 0 ||
                        Config.hourTime.get() != 0 || 
                        Config.minuteTime.get() != 0;

        if (!event.getEntityLiving().getCommandSenderWorld().isClientSide() &&
                event.getEntityLiving() instanceof ServerPlayerEntity &&
                !this.server.isSingleplayer() && 
                deathBanOn) 
        {
            ServerPlayerEntity deadPlayer = (ServerPlayerEntity) event.getEntityLiving();
            String reason = MessageParser.deathReasonMessage(deadPlayer, event.getSource());
            Date expire = DateTimeCalculator.getExpiryDate(
                    Config.weekTime.get(),
                    Config.dayTime.get(),
                    Config.hourTime.get(),
                    Config.minuteTime.get()
            );
            banList.addToBanList(server, deadPlayer.getGameProfile(), expire, reason);
        }
    }
}