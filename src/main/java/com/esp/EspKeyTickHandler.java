package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.AABB;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.apache.logging.log4j.LogManager;
  import org.apache.logging.log4j.Logger;

  /**
   * Клиентский тик: горячие клавиши, АнтиУрон, КиллАура, Авто-аутентификация.
   * Авто-регистрация через @Mod.EventBusSubscriber — НЕ регистрировать вручную.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {
      private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

      private static int  killAuraTick  = 0;

      // Авто-аутентификация
      // Этапы: -1=ждём входа на сервер, >0=обратный отсчёт
      // Этап 1: regDelay > 0  → отправить /reg
      // Этап 2: loginDelay > 0 → через 1 сек после reg отправить /login
      private static int  regDelay      = -1;
      private static int  loginDelay    = -1;
      private static boolean wasInWorld = false;

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();

          // ── Горячие клавиши ────────────────────────────────────────────────
          while (EspKeyHandler.KEY_TOGGLE.consumeClick())
              { EspConfig.espEnabled = !EspConfig.espEnabled; EspConfig.save(); }
          while (EspKeyHandler.KEY_GUI.consumeClick())
              { if (mc.screen == null && mc.level != null) mc.setScreen(new EspScreen()); }
          while (EspKeyHandler.KEY_ORE.consumeClick())
              { EspConfig.oreEsp   = !EspConfig.oreEsp;   EspConfig.save(); }
          while (EspKeyHandler.KEY_NOFALL.consumeClick())
              { EspConfig.noFall   = !EspConfig.noFall;   EspConfig.save(); }
          while (EspKeyHandler.KEY_KILLAURA.consumeClick())
              { EspConfig.killAura = !EspConfig.killAura; EspConfig.save(); }

          if (mc.level == null || mc.player == null) {
              wasInWorld = false;
              return;
          }

          // ── Определяем момент входа на сервер ─────────────────────────────
          if (!wasInWorld) {
              wasInWorld = true;
              if (EspConfig.autoAuth && !EspConfig.authPassword.isEmpty()) {
                  // Регистрация: через 4 сек (80 тиков), логин: через 5 сек (100 тиков)
                  regDelay   = EspConfig.autoReg   ? 80  : -1;
                  loginDelay = EspConfig.autoLogin  ? 100 : -1;
                  LOG.info("[PlayerESP] Авто-аутентификация запланирована (reg={}, login={})",
                      EspConfig.autoReg, EspConfig.autoLogin);
              }
          }

          // ── Авто-регистрация ───────────────────────────────────────────────
          if (regDelay > 0) {
              regDelay--;
              if (regDelay == 0) {
                  String p = EspConfig.authPassword;
                  mc.player.connection.sendCommand("reg " + p + " " + p);
                  LOG.info("[PlayerESP] Отправлена команда /reg ****");
              }
          }

          // ── Авто-логин ─────────────────────────────────────────────────────
          if (loginDelay > 0) {
              loginDelay--;
              if (loginDelay == 0) {
                  mc.player.connection.sendCommand("login " + EspConfig.authPassword);
                  LOG.info("[PlayerESP] Отправлена команда /login ****");
              }
          }

          // ── АнтиУрон ──────────────────────────────────────────────────────
          if (EspConfig.noFall && mc.player.isAlive()) {
              mc.player.fallDistance = 0f;
              if (!mc.player.onGround() && mc.player.getDeltaMovement().y < -0.1) {
                  mc.player.connection.send(
                      new ServerboundMovePlayerPacket.StatusOnly(true)
                  );
              }
          }

          // ── КиллАура ──────────────────────────────────────────────────────
          if (EspConfig.killAura && mc.player.isAlive()) {
              if (++killAuraTick >= 4) {
                  killAuraTick = 0;
                  Vec3 eyes = mc.player.getEyePosition(1.0f);
                  Vec3 look = mc.player.getLookAngle();
                  Vec3 end  = eyes.add(look.scale(EspConfig.killAuraRange));
                  Player target  = null;
                  double minDist = EspConfig.killAuraRange;
                  for (Player p : mc.level.players()) {
                      if (p == mc.player || !p.isAlive()) continue;
                      double dist = mc.player.distanceTo(p);
                      if (dist > EspConfig.killAuraRange) continue;
                      if (p.getBoundingBox().inflate(0.1).clip(eyes, end).isEmpty()) continue;
                      if (dist < minDist) { minDist = dist; target = p; }
                  }
                  if (target != null) mc.gameMode.attack(mc.player, target);
              }
          }
      }
  }
  