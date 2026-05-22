package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.effect.MobEffects;
    import net.minecraft.world.entity.EquipmentSlot;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.item.ArmorItem;
    import net.minecraft.world.item.ItemStack;
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

        // Состояние
        private static int     killAuraTick  = 0;
        private static int     regDelay      = -1;
        private static int     loginDelay    = -1;
        private static boolean wasInWorld    = false;
        private static boolean nvActive      = false;
        private static int     autoArmorTick = 0;
        private static int     afkIdleTick   = 0;
        private static int     afkMoveTick   = 0;

        // Отложенное сохранение (200 тиков = 10 сек)
        private static boolean configDirty  = false;
        private static int     saveCooldown = 0;

        public static void markDirty() { configDirty = true; }

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();

            // ── Клавиши ──────────────────────────────────────────────────────────
            while (EspKeyHandler.KEY_TOGGLE.consumeClick())    { EspConfig.espEnabled    = !EspConfig.espEnabled;    markDirty(); }
            while (EspKeyHandler.KEY_GUI.consumeClick())       { if (mc.screen == null && mc.level != null) mc.setScreen(new EspScreen()); }
            while (EspKeyHandler.KEY_ORE.consumeClick())       { EspConfig.oreEsp        = !EspConfig.oreEsp;        markDirty(); }
            while (EspKeyHandler.KEY_NOFALL.consumeClick())    { EspConfig.noFall        = !EspConfig.noFall;        markDirty(); LOG.info("[ESP] АнтиУрон: {}", EspConfig.noFall?"ВКЛ":"ВЫКЛ"); }
            while (EspKeyHandler.KEY_KILLAURA.consumeClick())  { EspConfig.killAura      = !EspConfig.killAura;      markDirty(); }
            while (EspKeyHandler.KEY_SPRINT.consumeClick())    { EspConfig.alwaysSprint  = !EspConfig.alwaysSprint;  markDirty(); }
            while (EspKeyHandler.KEY_NIGHTVISION.consumeClick()){ EspConfig.nightVision  = !EspConfig.nightVision;   markDirty(); }
            while (EspKeyHandler.KEY_MININGBOT.consumeClick()) { EspConfig.miningBot     = !EspConfig.miningBot;     markDirty(); LOG.info("[ESP] МайнингБот: {}", EspConfig.miningBot?"ВКЛ":"ВЫКЛ"); }

            // ── Сохранение ────────────────────────────────────────────────────────
            if (saveCooldown > 0) saveCooldown--;
            if (configDirty && saveCooldown == 0) { EspConfig.save(); configDirty = false; saveCooldown = 200; }

            if (mc.level == null || mc.player == null) {
                wasInWorld = false;
                if (nvActive) { nvActive = false; }
                afkIdleTick = 0; afkMoveTick = 0;
                return;
            }

            // ── Вход на сервер ────────────────────────────────────────────────────
            if (!wasInWorld) {
                wasInWorld = true;
                SmartAutoAuth.reset();
                if (!EspConfig.smartAuth && EspConfig.autoAuth && !EspConfig.authPassword.isEmpty()) {
                    regDelay   = EspConfig.autoReg   ? 80  : -1;
                    loginDelay = EspConfig.autoLogin  ? 100 : -1;
                }
            }

            // ── Умная авто-аутентификация ─────────────────────────────────────────
            if (EspConfig.smartAuth) SmartAutoAuth.tick();

            // ── Обычная авто-аутентификация ───────────────────────────────────────
            if (!EspConfig.smartAuth) {
                if (regDelay   > 0 && --regDelay   == 0) mc.player.connection.sendCommand("reg " + EspConfig.authPassword + " " + EspConfig.authPassword);
                if (loginDelay > 0 && --loginDelay  == 0) mc.player.connection.sendCommand("login " + EspConfig.authPassword);
            }

            // ── АнтиУрон от падения ───────────────────────────────────────────────
            if (EspConfig.noFall && mc.player.isAlive()) {
                mc.player.fallDistance = 0f;
                if (!mc.player.onGround() && mc.player.getDeltaMovement().y < 0)
                    mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true));
            }

            // ── Всегда спринт ─────────────────────────────────────────────────────
            if (EspConfig.alwaysSprint && mc.player.isAlive() && !mc.player.isSprinting()
                    && mc.player.getFoodData().getFoodLevel() > 6
                    && mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.001)
                mc.player.setSprinting(true);

            // ── Ночное зрение ─────────────────────────────────────────────────────
            if (EspConfig.nightVision) {
                MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
                if (cur == null || cur.getDuration() < 300) {
                    mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
                    nvActive = true;
                }
            } else if (nvActive) { mc.player.removeEffect(MobEffects.NIGHT_VISION); nvActive = false; }

            // ── Без замедления при еде/луке ───────────────────────────────────────
            if (EspConfig.noSlowdown && mc.player.isAlive() && mc.player.isUsingItem()) {
                Vec3 vel = mc.player.getDeltaMovement();
                if (vel.horizontalDistanceSqr() > 0.00001)
                    mc.player.setDeltaMovement(vel.x * 1.5, vel.y, vel.z * 1.5);
            }

            // ── Анти-отброс ───────────────────────────────────────────────────────
            if (EspConfig.antiKnockback && mc.player.isAlive() && mc.player.hurtTime > 0) {
                Vec3 vel = mc.player.getDeltaMovement();
                float s = EspConfig.antiKbStrength;
                mc.player.setDeltaMovement(vel.x * (1.0 - s), vel.y, vel.z * (1.0 - s));
            }

            // ── Анти-AFK ──────────────────────────────────────────────────────────
            boolean isMoving = mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0002
                || mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                || mc.options.sneakKey.isDown();
            if (isMoving) { afkIdleTick = 0; afkMoveTick = 0; } else afkIdleTick++;
            if (EspConfig.antiAfk && afkIdleTick >= EspConfig.antiAfkDelay) {
                afkMoveTick++;
                double micro = (afkMoveTick % 20 < 10) ? 0.013 : -0.013;
                mc.player.setDeltaMovement(micro, mc.player.getDeltaMovement().y, 0);
            }

            // ── Авто-броня ────────────────────────────────────────────────────────
            if (EspConfig.autoArmor && ++autoArmorTick >= 20) {
                autoArmorTick = 0;
                autoEquipBestArmor(mc);
            }

            // ── КиллАура (с таймингом удара) ──────────────────────────────────────
            if (EspConfig.killAura && mc.player.isAlive()) {
                if (++killAuraTick >= 4) {
                    killAuraTick = 0;
                    float cooldown = mc.player.getAttackStrengthScale(0.5f);
                    if (!EspConfig.hitDelay || cooldown >= 0.9f) {
                        Player target = null;
                        double minDist = EspConfig.killAuraRange;
                        try {
                            for (Player p : java.util.List.copyOf(mc.level.players())) {
                                if (p == mc.player || !p.isAlive()) continue;
                                double d = mc.player.distanceTo(p);
                                if (d > EspConfig.killAuraRange || d >= minDist) continue;
                                minDist = d; target = p;
                            }
                        } catch (Exception ignored) {}
                        if (target != null) mc.gameMode.attack(mc.player, target);
                    }
                }
            }
        }

        // ── Авто-броня ────────────────────────────────────────────────────────────
        private static void autoEquipBestArmor(Minecraft mc) {
            EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
            Inventory inv = mc.player.getInventory();
            for (EquipmentSlot slot : slots) {
                ItemStack current    = mc.player.getItemBySlot(slot);
                int       currentProt = armorProt(current, slot);
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!(stack.getItem() instanceof ArmorItem ai) || ai.getEquipmentSlot() != slot) continue;
                    int stackProt = armorProt(stack, slot);
                    if (stackProt > currentProt) {
                        inv.setItem(i, current);
                        mc.player.setItemSlot(slot, stack);
                        current = stack; currentProt = stackProt;
                    }
                }
            }
        }

        private static int armorProt(ItemStack s, EquipmentSlot slot) {
            if (s.isEmpty() || !(s.getItem() instanceof ArmorItem ai)) return -1;
            if (ai.getEquipmentSlot() != slot) return -1;
            return ai.getDefense();
        }
    }
  