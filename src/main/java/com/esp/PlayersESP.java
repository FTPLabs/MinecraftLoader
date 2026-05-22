package com.esp;

  import net.minecraftforge.common.MinecraftForge;
  import net.minecraftforge.eventbus.api.IEventBus;
  import net.minecraftforge.fml.common.Mod;
  import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

  @Mod(PlayersESP.MOD_ID)
  public class PlayersESP {
      public static final String MOD_ID = "playersesp";

      public PlayersESP() {
          EspConfig.load();

          IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
          modBus.addListener(EspKeyHandler::onRegisterKeys);

          MinecraftForge.EVENT_BUS.register(EspRenderer.class);
          MinecraftForge.EVENT_BUS.register(OreEspRenderer.class);
          MinecraftForge.EVENT_BUS.register(EspKeyTickHandler.class);
      }
  }