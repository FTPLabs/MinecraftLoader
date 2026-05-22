package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.world.entity.player.Player;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;

  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {

      private static int flyPacketTick = 0;

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          // ── Клавиши ──────────────────────────────────────────────────────────
          while (EspKeyHandler.KEY_TOGGLE.consumeClick()) { EspConfig.espEnabled = !EspConfig.espEnabled; EspConfig.save(); }
          while (EspKeyHandler.KEY_GUI.consumeClick())    { if (mc.screen == null) mc.setScreen(new EspScreen()); }
          while (EspKeyHandler.KEY_ORE.consumeClick())    { EspConfig.oreEsp = !EspConfig.oreEsp; EspConfig.save(); }
          while (EspKeyHandler.KEY_FLY.consumeClick())    { EspConfig.flyEnabled = !EspConfig.flyEnabled; EspConfig.save(); }

          // ── Fly hack ─────────────────────────────────────────────────────────
          Player player = mc.player;
          boolean inGame = !player.isCreative() && !player.isSpectator();

          if (EspConfig.flyEnabled && inGame) {
              player.getAbilities().mayfly = true;
              player.getAbilities().flying = true;
              // flyingSpeed приватный — используем сеттер (MC 1.21.1)
              player.getAbilities().setFlyingSpeed(0.05f * Math.max(0.1f, EspConfig.flySpeed));
              // Отправляем пакет серверу раз в секунду
              if (++flyPacketTick >= 20) {
                  flyPacketTick = 0;
                  player.onUpdateAbilities();
              }
          } else if (!EspConfig.flyEnabled && inGame && player.getAbilities().mayfly) {
              player.getAbilities().mayfly = false;
              player.getAbilities().flying = false;
              player.getAbilities().setFlyingSpeed(0.05f);
              player.onUpdateAbilities();
          }
      }
  }