package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.apache.logging.log4j.LogManager;
  import org.apache.logging.log4j.Logger;

  /**
   * Клиентский тик: горячие клавиши, АнтиУрон, КиллАура, Авто-аутентификация.
   * Регистрируется автоматически через @Mod.EventBusSubscriber.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {
      private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

      // ── КиллАура ──────────────────────────────────────────────────────────────
      private static int killAuraTick = 0;

      // ── Авто-аутентификация ───────────────────────────────────────────────────
      private static int     regDelay    = -1;
      private static int     loginDelay  = -1;
      private static boolean wasInWorld  = false;

      // ── Отложенное сохранение конфига ─────────────────────────────────────────
      // Запись на диск выполняется НЕ при каждом нажатии кнопки, а максимум
      // раз в 10 секунд (200 тиков) — убирает микрофризы от файлового I/O.
      private static boolean configDirty    = false;
      private static int     saveCooldown   = 0;

      /** Пометить конфиг как изменённый — сохранение произойдёт вскоре. */
      public static void markDirty() {
          configDirty = true;
      }

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();

          // ── Горячие клавиши ───────────────────────────────────────────────────
          while (EspKeyHandler.KEY_TOGGLE.consumeClick()) {
              EspConfig.espEnabled = !EspConfig.espEnabled; markDirty();
          }
          while (EspKeyHandler.KEY_GUI.consumeClick()) {
              if (mc.screen == null && mc.level != null) mc.setScreen(new EspScreen());
          }
          while (EspKeyHandler.KEY_ORE.consumeClick()) {
              EspConfig.oreEsp = !EspConfig.oreEsp; markDirty();
          }
          while (EspKeyHandler.KEY_NOFALL.consumeClick()) {
              EspConfig.noFall = !EspConfig.noFall; markDirty();
              LOG.info("[PlayerESP] АнтиУрон: {}", EspConfig.noFall ? "ВКЛ" : "ВЫКЛ");
          }
          while (EspKeyHandler.KEY_KILLAURA.consumeClick()) {
              EspConfig.killAura = !EspConfig.killAura; markDirty();
          }

          // ── Отложенное сохранение ─────────────────────────────────────────────
          if (saveCooldown > 0) saveCooldown--;
          if (configDirty && saveCooldown == 0) {
              EspConfig.save();
              configDirty = false;
              saveCooldown = 200; // следующее сохранение не раньше чем через 10 сек
          }

          if (mc.level == null || mc.player == null) {
              wasInWorld = false;
              return;
          }

          // ── Момент входа на сервер ────────────────────────────────────────────
          if (!wasInWorld) {
              wasInWorld = true;
              if (EspConfig.autoAuth && !EspConfig.authPassword.isEmpty()) {
                  regDelay   = EspConfig.autoReg   ? 80  : -1;
                  loginDelay = EspConfig.autoLogin  ? 100 : -1;
                  LOG.info("[PlayerESP] Авто-аутентификация запланирована (reg={}, login={})",
                      EspConfig.autoReg, EspConfig.autoLogin);
              }
          }

          // ── Авто-регистрация ──────────────────────────────────────────────────
          if (regDelay > 0 && --regDelay == 0) {
              mc.player.connection.sendCommand("reg " + EspConfig.authPassword + " " + EspConfig.authPassword);
              LOG.info("[PlayerESP] Отправлена команда /reg ****");
          }

          // ── Авто-логин ────────────────────────────────────────────────────────
          if (loginDelay > 0 && --loginDelay == 0) {
              mc.player.connection.sendCommand("login " + EspConfig.authPassword);
              LOG.info("[PlayerESP] Отправлена команда /login ****");
          }

          // ── АнтиУрон от падения ───────────────────────────────────────────────
          // Проблема старой версии: условие y < -0.1 пропускало первые тики падения.
          // Решение: сбрасывать fallDistance КАЖДЫЙ тик и отправлять серверу
          // "я на земле" (StatusOnly=true) каждый тик — сервер не накапливает урон.
          if (EspConfig.noFall && mc.player.isAlive()) {
              mc.player.fallDistance = 0f;
              // StatusOnly(onGround=true) сбрасывает server-side fallDistance.
              // Отправляем без условий — так сервер всегда считает игрока стоящим.
              mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true));
          }

          // ── КиллАура ──────────────────────────────────────────────────────────
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
  