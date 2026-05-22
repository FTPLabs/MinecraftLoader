package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraft.core.registries.Registries;
    import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.effect.MobEffects;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.phys.Vec3;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.event.TickEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;

    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class EspKeyTickHandler {
        private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

        private static int     killAuraTick  = 0;
        private static int     regDelay      = -1;
        private static int     loginDelay    = -1;
        private static boolean wasInWorld    = false;
        private static boolean nvActive      = false;

        // Отложенное сохранение конфига (200 тиков = 10 сек)
        private static boolean configDirty  = false;
        private static int     saveCooldown = 0;

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
            while (EspKeyHandler.KEY_SPRINT.consumeClick()) {
                EspConfig.alwaysSprint = !EspConfig.alwaysSprint; markDirty();
                LOG.info("[PlayerESP] Всегда спринт: {}", EspConfig.alwaysSprint ? "ВКЛ" : "ВЫКЛ");
            }
            while (EspKeyHandler.KEY_NIGHTVISION.consumeClick()) {
                EspConfig.nightVision = !EspConfig.nightVision; markDirty();
                LOG.info("[PlayerESP] Ночное зрение: {}", EspConfig.nightVision ? "ВКЛ" : "ВЫКЛ");
            }

            // ── Отложенное сохранение ─────────────────────────────────────────────
            if (saveCooldown > 0) saveCooldown--;
            if (configDirty && saveCooldown == 0) {
                EspConfig.save();
                configDirty = false;
                saveCooldown = 200;
            }

            if (mc.level == null || mc.player == null) {
                wasInWorld = false;
                // Сбрасываем ночное зрение при выходе
                if (nvActive) { nvActive = false; }
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
            // ФИКС: отправляем пакет только когда игрок реально в воздухе,
            // а не каждый тик. Это убирает пакетный флуд (был 20 пакетов/сек).
            if (EspConfig.noFall && mc.player.isAlive()) {
                mc.player.fallDistance = 0f;
                if (!mc.player.onGround() && mc.player.getDeltaMovement().y < 0) {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true));
                }
            }

            // ── Всегда спринт ─────────────────────────────────────────────────────
            if (EspConfig.alwaysSprint && mc.player.isAlive()
                    && !mc.player.isSprinting()
                    && mc.player.getFoodData().getFoodLevel() > 6
                    && mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                mc.player.setSprinting(true);
            }

            // ── Ночное зрение ─────────────────────────────────────────────────────
            if (EspConfig.nightVision) {
                MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
                // Обновляем эффект каждые 100 тиков или когда он почти истёк
                if (cur == null || cur.getDuration() < 300) {
                    mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
                    nvActive = true;
                }
            } else if (nvActive) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
                nvActive = false;
            }

            // ── КиллАура ──────────────────────────────────────────────────────────
            // ФИКС: убран raycast по лучу взгляда — атакует ближайшего в радиусе,
            // а не только того, кто строго в прицеле.
            if (EspConfig.killAura && mc.player.isAlive()) {
                if (++killAuraTick >= 4) {
                    killAuraTick = 0;
                    Player target  = null;
                    double minDist = EspConfig.killAuraRange;
                    for (Player p : mc.level.players()) {
                        if (p == mc.player || !p.isAlive()) continue;
                        double dist = mc.player.distanceTo(p);
                        if (dist > EspConfig.killAuraRange) continue;
                        if (dist < minDist) { minDist = dist; target = p; }
                    }
                    if (target != null) mc.gameMode.attack(mc.player, target);
                }
            }
        }
    }
  