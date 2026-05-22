package com.esp;

    import com.google.gson.*;
    import net.minecraftforge.fml.loading.FMLPaths;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import java.io.*;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.*;
    import java.util.Base64;

    public class EspConfig {
        private static final Logger LOG  = LogManager.getLogger(PlayersESP.MOD_ID);
        private static final Path   PATH = FMLPaths.CONFIGDIR.get().resolve("playersesp.json");
        private static final Gson   GSON = new GsonBuilder().setPrettyPrinting().create();

        // Игрок ESP
        public static boolean espEnabled    = true;
        public static float   espR = 1f, espG = 0f, espB = 0f;
        public static int     espRange      = 128;
        public static boolean showNick      = true;
        public static boolean showHp        = true;
        public static boolean showArmor     = true;
        public static boolean tracer        = false;

        // Сканер руд
        public static boolean oreEsp        = false;
        public static int     oreRange      = 16;

        // Боевой
        public static boolean noFall        = false;
        public static boolean killAura      = false;
        public static float   killAuraRange = 4.0f;
        public static boolean alwaysSprint  = false;
        public static boolean nightVision   = false;

        // Авто-аутентификация
        public static boolean autoAuth     = false;
        public static boolean autoReg      = false;
        public static boolean autoLogin    = true;
        public static String  authPassword = "";

        // Интерфейс (0=Компакт, 1=Нормальный, 2=Большой)
        public static int     guiScale     = 1;

        public static void load() {
            if (!Files.exists(PATH)) { save(); return; }
            try (Reader rdr = new InputStreamReader(new FileInputStream(PATH.toFile()), StandardCharsets.UTF_8)) {
                JsonObject o = GSON.fromJson(rdr, JsonObject.class);
                if (o == null) return;
                espEnabled    = bv(o,"espEnabled",    espEnabled);
                espR          = clampF(fv(o,"espR",   espR),   0f, 1f);
                espG          = clampF(fv(o,"espG",   espG),   0f, 1f);
                espB          = clampF(fv(o,"espB",   espB),   0f, 1f);
                espRange      = clampI(iv(o,"espRange",  espRange),  16, 1000);
                showNick      = bv(o,"showNick",      showNick);
                showHp        = bv(o,"showHp",        showHp);
                showArmor     = bv(o,"showArmor",     showArmor);
                tracer        = bv(o,"tracer",        tracer);
                oreEsp        = bv(o,"oreEsp",        oreEsp);
                oreRange      = clampI(iv(o,"oreRange", oreRange), 8, 32);
                noFall        = bv(o,"noFall",        noFall);
                killAura      = bv(o,"killAura",      killAura);
                killAuraRange = clampF(fv(o,"killAuraRange", killAuraRange), 2f, 6f);
                alwaysSprint  = bv(o,"alwaysSprint",  alwaysSprint);
                nightVision   = bv(o,"nightVision",   nightVision);
                autoAuth      = bv(o,"autoAuth",      autoAuth);
                autoReg       = bv(o,"autoReg",       autoReg);
                autoLogin     = bv(o,"autoLogin",     autoLogin);
                guiScale      = clampI(iv(o,"guiScale", guiScale), 0, 2);
                // Пароль: поддержка legacy plain-text и нового base64
                if (o.has("authPasswordB64")) {
                    try { authPassword = new String(Base64.getDecoder().decode(o.get("authPasswordB64").getAsString()), StandardCharsets.UTF_8); }
                    catch (Exception ignored) { authPassword = sv(o,"authPassword", authPassword); }
                } else {
                    authPassword = sv(o,"authPassword", authPassword);
                }
            } catch (Exception e) {
                LOG.warn("[PlayerESP] Ошибка загрузки конфига: {}", e.getMessage()); save();
            }
        }

        public static void save() {
            try {
                Files.createDirectories(PATH.getParent());
                JsonObject o = new JsonObject();
                o.addProperty("espEnabled",    espEnabled);
                o.addProperty("espR",          espR);
                o.addProperty("espG",          espG);
                o.addProperty("espB",          espB);
                o.addProperty("espRange",      espRange);
                o.addProperty("showNick",      showNick);
                o.addProperty("showHp",        showHp);
                o.addProperty("showArmor",     showArmor);
                o.addProperty("tracer",        tracer);
                o.addProperty("oreEsp",        oreEsp);
                o.addProperty("oreRange",      oreRange);
                o.addProperty("noFall",        noFall);
                o.addProperty("killAura",      killAura);
                o.addProperty("killAuraRange", killAuraRange);
                o.addProperty("alwaysSprint",  alwaysSprint);
                o.addProperty("nightVision",   nightVision);
                o.addProperty("autoAuth",      autoAuth);
                o.addProperty("autoReg",       autoReg);
                o.addProperty("autoLogin",     autoLogin);
                o.addProperty("authPasswordB64", Base64.getEncoder().encodeToString(authPassword.getBytes(StandardCharsets.UTF_8)));
                o.addProperty("guiScale",      guiScale);
                try (Writer w = new OutputStreamWriter(new FileOutputStream(PATH.toFile()), StandardCharsets.UTF_8)) {
                    GSON.toJson(o, w);
                }
            } catch (Exception e) {
                LOG.error("[PlayerESP] Ошибка сохранения: {}", e.getMessage());
            }
        }

        private static boolean bv(JsonObject o,String k,boolean d){return o.has(k)?o.get(k).getAsBoolean():d;}
        private static float   fv(JsonObject o,String k,float   d){return o.has(k)?o.get(k).getAsFloat()  :d;}
        private static int     iv(JsonObject o,String k,int     d){return o.has(k)?o.get(k).getAsInt()    :d;}
        private static String  sv(JsonObject o,String k,String  d){return o.has(k)?o.get(k).getAsString() :d;}
        private static float   clampF(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
        private static int     clampI(int   v,int   mn,int   mx){return Math.max(mn,Math.min(mx,v));}
    }
  