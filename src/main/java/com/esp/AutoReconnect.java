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

  /**
   * FIX v1.6:
   *  - attempts сбрасывается при успешном логине (была регрессия: после 10 дисконнектов
   *    авто-реконнект навсегда переставал работать до перезапуска игры)
   *  - FIX: attempts теперь также сбрасывается при включении/выключении autoReconnect
   *    через метод reset()
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class AutoReconnect {
      private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

      private static final int MAX_ATTEMPTS = 10;

      private static ServerData lastServer  = null;
      private static int        reconnTimer = -1;
      private static int        attempts    = 0;

      /** Сбросить счётчик попыток (например, при включении фичи) */
      public static void reset() {
          attempts    = 0;
          reconnTimer = -1;
          lastServer  = null;
      }

      @SubscribeEvent
      public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
          Minecraft mc = Minecraft.getInstance();
          if (!EspConfig.autoReconnect || mc.getCurrentServer() == null) return;
          lastServer = mc.getCurrentServer();
          attempts++;
          if (attempts <= MAX_ATTEMPTS) {
              reconnTimer = EspConfig.reconnectDelay * 20;
              LOG.info("[PlayerESP] AutoReconnect: попытка {}/{} — жду {} сек...",
                  attempts, MAX_ATTEMPTS, EspConfig.reconnectDelay);
          } else {
              LOG.warn("[PlayerESP] AutoReconnect: лимит {} попыток исчерпан. Выключите и включите авто-реконнект для сброса.", MAX_ATTEMPTS);
              lastServer = null;
          }
      }

      @SubscribeEvent
      public static void onLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
          // FIX: сбрасываем счётчик при успешном подключении
          attempts    = 0;
          reconnTimer = -1;
          LOG.info("[PlayerESP] AutoReconnect: успешно подключились, счётчик сброшен.");
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
              mc.execute(() -> {
                  ServerAddress addr = ServerAddress.parseString(srv.ip);
                  ConnectScreen.startConnecting(mc.screen, mc, addr, srv, false, null);
              });
          }
      }
  }
  