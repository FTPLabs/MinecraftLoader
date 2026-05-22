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
        public static final KeyMapping KEY_TOGGLE      = new KeyMapping("key.playersesp.toggle",      InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,      "key.categories.playersesp");
        public static final KeyMapping KEY_GUI         = new KeyMapping("key.playersesp.open",         InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "key.categories.playersesp");
        public static final KeyMapping KEY_ORE         = new KeyMapping("key.playersesp.ore",          InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,      "key.categories.playersesp");
        public static final KeyMapping KEY_NOFALL      = new KeyMapping("key.playersesp.nofall",       InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,      "key.categories.playersesp");
        public static final KeyMapping KEY_KILLAURA    = new KeyMapping("key.playersesp.killaura",     InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,      "key.categories.playersesp");
        public static final KeyMapping KEY_SPRINT      = new KeyMapping("key.playersesp.sprint",       InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,      "key.categories.playersesp");
        public static final KeyMapping KEY_NIGHTVISION = new KeyMapping("key.playersesp.nightvision",  InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,      "key.categories.playersesp");
        public static final KeyMapping KEY_MININGBOT   = new KeyMapping("key.playersesp.miningbot",    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,      "key.categories.playersesp");

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_TOGGLE);
            event.register(KEY_GUI);
            event.register(KEY_ORE);
            event.register(KEY_NOFALL);
            event.register(KEY_KILLAURA);
            event.register(KEY_SPRINT);
            event.register(KEY_NIGHTVISION);
            event.register(KEY_MININGBOT);
        }
    }
  