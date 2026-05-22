package com.esp;

  import com.mojang.blaze3d.platform.InputConstants;
  import net.minecraft.client.KeyMapping;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.lwjgl.glfw.GLFW;

  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
  public class EspKeyHandler {
      /** [G] — ESP вкл/выкл */
      public static final KeyMapping KEY_TOGGLE = new KeyMapping(
          "key.playersesp.toggle",   InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,   "key.categories.playersesp");

      /**
       * [End] — Открыть меню.
       * ID изменён с "key.playersesp.gui" на "key.playersesp.menu",
       * чтобы Minecraft не подхватил старую запись HOME из options.txt.
       */
      public static final KeyMapping KEY_GUI = new KeyMapping(
          "key.playersesp.menu",     InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_END, "key.categories.playersesp");

      /** [J] — Сканер руд */
      public static final KeyMapping KEY_ORE = new KeyMapping(
          "key.playersesp.ore",      InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,   "key.categories.playersesp");

      /** [N] — АнтиУрон */
      public static final KeyMapping KEY_NOFALL = new KeyMapping(
          "key.playersesp.nofall",   InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,   "key.categories.playersesp");

      /** [K] — КиллАура */
      public static final KeyMapping KEY_KILLAURA = new KeyMapping(
          "key.playersesp.killaura", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,   "key.categories.playersesp");

      @SubscribeEvent
      public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
          event.register(KEY_TOGGLE);
          event.register(KEY_GUI);
          event.register(KEY_ORE);
          event.register(KEY_NOFALL);
          event.register(KEY_KILLAURA);
      }
  }
  