package com.esp;

    import net.minecraftforge.common.MinecraftForge;
    import net.minecraftforge.fml.common.Mod;
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

        public PlayersESP() {
            LOG.info("[PlayerESP] Loading v1.5 | Developer: FTPDev | github.com/FTPLabs");
            EspConfig.load();

            // EspKeyHandler     — auto via @Mod.EventBusSubscriber(bus=MOD)
            // EspKeyTickHandler — auto via @Mod.EventBusSubscriber (FORGE bus)
            // EspHudRenderer    — auto via @Mod.EventBusSubscriber (FORGE bus)
            // ArrowPredictor    — auto via @Mod.EventBusSubscriber (FORGE bus)
            // SmartAutoAuth     — auto via @Mod.EventBusSubscriber (FORGE bus)
            // AutoReconnect     — auto via @Mod.EventBusSubscriber (FORGE bus)
            // MiningBot         — auto via @Mod.EventBusSubscriber (FORGE bus)

            MinecraftForge.EVENT_BUS.register(EspRenderer.class);
            MinecraftForge.EVENT_BUS.register(OreEspRenderer.class);
        }
    }
  