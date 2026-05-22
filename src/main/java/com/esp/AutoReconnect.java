package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.screens.ConnectScreen;
  import net.minecraft.client.gui.screens.DisconnectedScreen;
  import net.minecraft.client.multiplayer.ServerData;
  import net.minecraft.client.multiplayer.resolver.ServerAddress;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
  import net.minecraftforge.client.event.ScreenEvent;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.apache.logging.log4j.LogManager;
  import org.apache.logging.log4j.Logger;

  /**
   * Авто-реконнект.
   *
   * FIX v1.7:
   *  - Реконнект происходит ТОЛЬКО при серверном кике (DisconnectedScreen).
   *    При ручном отключении (Disconnect кнопка) — реконнект НЕ запускается.
   *  - Реализация: флаг kickedByServer устанавливается при открытии DisconnectedScreen.
   *    В onLogOut проверяем флаг и только тогда ставим таймер.
   *  - Счётчик попыток сбрасывается при успешном входе.
   *  - Метод reset() для сброса при включении/выключении фичи.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class AutoReconnect {
      private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

      private static final int MAX_ATTEMPTS = 10;

      private static ServerData lastServer    = null;
      private static int        reconnTimer   = -1;
      private static int        attempts      = 0;
      // FIX: флаг — нас кикнул сервер (не ручной дисконнект)
      private static boolean    kickedByServer = false;

      /** Сбросить счётчик попыток (например, при ручном включении фичи) */
      public static void reset() {
          attempts      = 0;
          reconnTimer   = -1;
          lastServer    = null;
          kickedByServer = false;
      }

      /**
       * FIX: Слушаем открытие экранов.
       * DisconnectedScreen открывается ТОЛЬКО при серверном кике/ошибке подключения.
       * При ручном нажатии "Disconnect" открывается главное меню/мультиплеер — не DisconnectedScreen.
       */
      @SubscribeEvent
      public static void onScreenOpen(ScreenEvent.Opening event) {
          if (event.getScreen() instanceof DisconnectedScreen) {
              kickedByServer = true;
              LOG.info("[PlayerESP] AutoReconnect: обнаружен серверный кик (DisconnectedScreen)");
          }
      }

      @SubscribeEvent
      public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
          if (!EspConfig.autoReconnect) return;

          Minecraft mc = Minecraft.getInstance();
          if (mc.getCurrentServer() == null) return;

          // FIX: не реконнектимся при ручном отключении
          if (!kickedByServer) {
              LOG.info("[PlayerESP] AutoReconnect: ручной дисконнект — пропускаем.");
              return;
          }
          kickedByServer = false; // сбрасываем флаг

          lastServer = mc.getCurrentServer();
          attempts++;
          if (attempts <= MAX_ATTEMPTS) {
              reconnTimer = EspConfig.reconnectDelay * 20;
              LOG.info("[PlayerESP] AutoReconnect: попытка {}/{} через {} сек.",
                  attempts, MAX_ATTEMPTS, EspConfig.reconnectDelay);
          } else {
              LOG.warn("[PlayerESP] AutoReconnect: лимит {} попыток исчерпан.", MAX_ATTEMPTS);
              lastServer = null;
          }
      }

      @SubscribeEvent
      public static void onLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
          attempts    = 0;
          reconnTimer = -1;
          LOG.info("[PlayerESP] AutoReconnect: подключились, счётчик сброшен.");
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
  