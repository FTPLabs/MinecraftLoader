package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.client.event.ClientChatReceivedEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;

    /**
     * Умная авто-аутентификация: слушает чат и реагирует на запросы /reg и /login
     * вместо слепой отправки по таймеру.
     */
    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class SmartAutoAuth {
        private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

        private static final String[] REG_PATTERNS = {
            "/register", "зарегистрируй", "register", "reg ", "введите пароль дважды",
            "придумайте пароль", "пройди регистрацию", "используйте /reg"
        };
        private static final String[] LOGIN_PATTERNS = {
            "/login", "войдите", "авторизуйтесь", "введите пароль",
            "вы уже зарегистрированы", "используйте /login", "login "
        };

        private static int pendingRegDelay   = -1;
        private static int pendingLoginDelay = -1;

        @SubscribeEvent
        public static void onChatReceived(ClientChatReceivedEvent event) {
            if (!EspConfig.smartAuth || EspConfig.authPassword.isEmpty()) return;

            String msg = event.getMessage().getString().toLowerCase();

            if (EspConfig.autoReg && pendingRegDelay < 0) {
                for (String pat : REG_PATTERNS) {
                    if (msg.contains(pat)) {
                        pendingRegDelay = 25;
                        LOG.info("[PlayerESP] SmartAuth: обнаружен запрос регистрации → /reg через 1.25 сек");
                        break;
                    }
                }
            }

            if (EspConfig.autoLogin && pendingLoginDelay < 0) {
                for (String pat : LOGIN_PATTERNS) {
                    if (msg.contains(pat)) {
                        pendingLoginDelay = 25;
                        LOG.info("[PlayerESP] SmartAuth: обнаружен запрос входа → /login через 1.25 сек");
                        break;
                    }
                }
            }
        }

        /** Вызывается каждый тик из EspKeyTickHandler */
        public static void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (pendingRegDelay > 0 && --pendingRegDelay == 0) {
                mc.player.connection.sendCommand("reg " + EspConfig.authPassword + " " + EspConfig.authPassword);
                LOG.info("[PlayerESP] SmartAuth: /reg отправлен");
            }
            if (pendingLoginDelay > 0 && --pendingLoginDelay == 0) {
                mc.player.connection.sendCommand("login " + EspConfig.authPassword);
                LOG.info("[PlayerESP] SmartAuth: /login отправлен");
            }
        }

        public static void reset() {
            pendingRegDelay   = -1;
            pendingLoginDelay = -1;
        }
    }
  