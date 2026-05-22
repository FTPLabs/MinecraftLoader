package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.world.entity.player.Player;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;

  /**
   * Клиентский тик: горячие клавиши, АнтиУрон (NoFall), КиллАура.
   * Авто-регистрация через @Mod.EventBusSubscriber — НЕ регистрировать вручную в PlayersESP.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {

      private static int killAuraTick = 0;

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          // Горячие клавиши
          while (EspKeyHandler.KEY_TOGGLE.consumeClick())   { EspConfig.espEnabled = !EspConfig.espEnabled; EspConfig.save(); }
          while (EspKeyHandler.KEY_GUI.consumeClick())       { if (mc.screen == null) mc.setScreen(new EspScreen()); }
          while (EspKeyHandler.KEY_ORE.consumeClick())       { EspConfig.oreEsp    = !EspConfig.oreEsp;    EspConfig.save(); }
          while (EspKeyHandler.KEY_NOFALL.consumeClick())   { EspConfig.noFall    = !EspConfig.noFall;    EspConfig.save(); }
          while (EspKeyHandler.KEY_KILLAURA.consumeClick()) { EspConfig.killAura  = !EspConfig.killAura;  EspConfig.save(); }

          // АнтиУрон — обнуляет накопленную дистанцию падения каждый тик
          if (EspConfig.noFall && mc.player.isAlive()) {
              mc.player.fallDistance = 0f;
          }

          // КиллАура — атакует ближайшего игрока в радиусе каждые 4 тика (~5 атак/сек)
          if (EspConfig.killAura && mc.player.isAlive()) {
              if (++killAuraTick >= 4) {
                  killAuraTick = 0;
                  Player target = null;
                  double minDist = EspConfig.killAuraRange;
                  for (Player p : mc.level.players()) {
                      if (p == mc.player || !p.isAlive()) continue;
                      double d = mc.player.distanceTo(p);
                      if (d < minDist) { minDist = d; target = p; }
                  }
                  if (target != null) {
                      mc.gameMode.attack(mc.player, target);
                  }
              }
          }
      }
  }
  