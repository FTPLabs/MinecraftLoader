package com.esp;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("playersesp")
public class PlayersESP {

    public static final String MOD_ID = "playersesp";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public PlayersESP() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(EspRenderer.class);
        MinecraftForge.EVENT_BUS.register(EspKeyHandler.ClientTickHandler.class);
        LOGGER.info("Players ESP loaded successfully!");
    }
}
