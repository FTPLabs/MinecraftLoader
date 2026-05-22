package com.esp;

  import net.minecraft.client.KeyMapping;
  import net.minecraft.client.Minecraft;
  import net.minecraft.network.chat.Component;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.lwjgl.glfw.GLFW;

  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
  public class EspKeyHandler {

      private static boolean enabled   = false;
      private static boolean wasPressed = false;

      public static final KeyMapping ESP_TOGGLE = new KeyMapping(
          "key.playersesp.toggle",
          GLFW.GLFW_KEY_G,
          "key.categories.playersesp"
      );

      @SubscribeEvent
      public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
          event.register(ESP_TOGGLE);
      }

      // Аннотация убрана — регистрируется вручную в PlayersESP.clientSetup()
      // Это устраняет двойную регистрацию (двойной тик → toggle сбрасывался сам)
      public static class ClientTickHandler {
          @SubscribeEvent
          public static void onClientTick(TickEvent.ClientTickEvent event) {
              if (event.phase != TickEvent.Phase.END) return;
              Minecraft mc = Minecraft.getInstance();
              if (mc.screen != null) return;

              boolean pressed = ESP_TOGGLE.isDown();
              if (pressed && !wasPressed) {
                  enabled = !enabled;
                  if (mc.player != null) {
                      mc.player.sendSystemMessage(Component.literal(
                          "\u00a7b[ESP] \u00a7f" + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled")
                      ));
                  }
              }
              wasPressed = pressed;
          }
      }

      public static boolean isEnabled() { return enabled; }
  }
  