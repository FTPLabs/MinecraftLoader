package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraft.client.gui.screens.ConnectScreen;
    import net.minecraft.client.multiplayer.ServerData;
    import net.minecraft.client.multiplayer.resolver.ServerAddress;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
    import net.minecraftforge.event.TickEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;

    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class AutoReconnect {
        private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

        private static ServerData lastServer  = null;
        private static int        reconnTimer = -1;
        private static int        attempts    = 0;

        @SubscribeEvent
        public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft mc = Minecraft.getInstance();
            if (!EspConfig.autoReconnect || mc.getCurrentServer() == null) return;
            lastServer = mc.getCurrentServer();
            attempts++;
            if (attempts <= 10) {
                reconnTimer = EspConfig.reconnectDelay * 20;
                LOG.info("[PlayerESP] AutoReconnect: попытка {} — жду {} сек...", attempts, EspConfig.reconnectDelay);
            } else {
                LOG.warn("[PlayerESP] AutoReconnect: лимит 10 попыток исчерпан");
                lastServer = null;
            }
        }

        @SubscribeEvent
        public static void onLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
            attempts    = 0;
            reconnTimer = -1;
        }

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!EspConfig.autoReconnect || reconnTimer < 0 || lastServer == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) { reconnTimer = -1; return; }
            if (--reconnTimer == 0) {
                ServerData srv = lastServer;
                LOG.info("[PlayerESP] AutoReconnect: подключаемся к {}", srv.ip);
                mc.execute(() -> ConnectScreen.startConnecting(
                    mc.screen, mc, ServerAddress.parseString(srv.ip), srv, false));
            }
        }
    }
  