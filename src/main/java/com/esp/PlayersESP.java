package com.esp;

  import com.mojang.blaze3d.platform.InputConstants;
  import net.minecraft.client.KeyMapping;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.api.distmarker.OnlyIn;
  import net.minecraftforge.common.MinecraftForge;
  import net.minecraftforge.eventbus.api.IEventBus;
  import net.minecraftforge.fml.common.Mod;
  import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
  import org.apache.logging.log4j.LogManager;
  import org.apache.logging.log4j.Logger;

  /**
   * Players ESP — Minecraft Forge 1.21.1
   * Developer: FTPDev | github.com/FTPLabs
   */
  @Mod(PlayersESP.MOD_ID)
  public class PlayersESP {
      public static final String MOD_ID = "playersesp";
      private static final Logger LOG   = LogManager.getLogger(MOD_ID);

      public PlayersESP(IEventBus modBus) {
          LOG.info("[PlayerESP] Loading | Developer: FTPDev | github.com/FTPLabs");
          EspConfig.load();

          // Принудительно привязать KEY_GUI к клавише End при каждом запуске
          // (перекрывает кэш options.txt, если там осталась старая клавиша)
          modBus.addListener(PlayersESP::onClientSetup);

          // EspKeyHandler     — auto via @Mod.EventBusSubscriber(bus=MOD)
          // EspKeyTickHandler — auto via @Mod.EventBusSubscriber (FORGE bus)
          MinecraftForge.EVENT_BUS.register(EspRenderer.class);
          MinecraftForge.EVENT_BUS.register(OreEspRenderer.class);
      }

      @OnlyIn(Dist.CLIENT)
      private static void onClientSetup(FMLClientSetupEvent event) {
          event.enqueueWork(() -> {
              // Сбрасываем KEY_GUI на End, игнорируя сохранённое значение в options.txt
              EspKeyHandler.KEY_GUI.setKey(InputConstants.getKey("key.keyboard.end"));
              KeyMapping.resetMapping();
              LOG.info("[PlayerESP] Клавиша меню принудительно задана: End");
          });
      }
  }
  