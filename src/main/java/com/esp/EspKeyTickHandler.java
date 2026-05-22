package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;

  /**
   * Handles hotkey toggles on the client tick.
   * Auto-registered via @Mod.EventBusSubscriber — NOT registered manually in PlayersESP.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          while (EspKeyHandler.KEY_TOGGLE.consumeClick()) { EspConfig.espEnabled = !EspConfig.espEnabled; EspConfig.save(); }
          while (EspKeyHandler.KEY_GUI.consumeClick())    { if (mc.screen == null) mc.setScreen(new EspScreen()); }
          while (EspKeyHandler.KEY_ORE.consumeClick())    { EspConfig.oreEsp    = !EspConfig.oreEsp;    EspConfig.save(); }
      }
  }
  