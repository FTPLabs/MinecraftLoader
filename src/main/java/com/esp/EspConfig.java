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

      // Имена типов руд для UI (индекс = miningOreType)
      public static final String[] ORE_TYPE_NAMES = {
          "Все руды", "Алмаз", "Золото", "Железо",
          "Изумруд", "Лазурит", "Редстоун", "Медь", "Уголь", "Др.Дебрис"
      };

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

      // Майнинг-бот
      public static boolean miningBot      = false;
      public static int     miningOreType  = 0; // 0=все, 1=алмаз, 2=золото, 3=железо, 4=изумруд, 5=лазурит, 6=редстоун, 7=медь, 8=уголь, 9=дрДебрис

      // Боевой
      public static boolean noFall          = false;
      public static boolean killAura        = false;
      public static float   killAuraRange   = 4.0f;
      public static boolean hitDelay        = true;
      public static boolean antiKnockback   = false;
      public static float   antiKbStrength  = 0.7f;
      public static boolean alwaysSprint    = false;
      public static boolean nightVision     = false;
      public static boolean noSlowdown      = false;
      public static boolean autoArmor       = false;

      // HUD
      public static boolean armorHud        = false;
      public static boolean potionHud       = false;
      public static boolean reachDisplay    = false;
      public static boolean arrowPredict    = false;

      // Авто
      public static boolean autoAuth        = false;
      public static boolean autoReg         = false;
      public static boolean autoLogin       = true;
      public static String  authPassword    = "";
      public static boolean smartAuth       = false;
      public static boolean antiAfk         = false;
      public static int     antiAfkDelay    = 200;
      public static boolean autoReconnect   = false;
      public static int     reconnectDelay  = 5;

      // Интерфейс (0=Компакт, 1=Нормальный, 2=Большой)
      public static int     guiScale   = 1;
      public static boolean statusHud  = true;  // список активных модулей на экране

      public static void load() {
          if (!Files.exists(PATH)) { save(); return; }
          try (Reader r = new InputStreamReader(new FileInputStream(PATH.toFile()), StandardCharsets.UTF_8)) {
              JsonObject o = GSON.fromJson(r, JsonObject.class);
              if (o == null) return;
              espEnabled    = bv(o,"espEnabled",   espEnabled);
              espR          = cf(fv(o,"espR",       espR),   0f,1f);
              espG          = cf(fv(o,"espG",       espG),   0f,1f);
              espB          = cf(fv(o,"espB",       espB),   0f,1f);
              espRange      = ci(iv(o,"espRange",   espRange),  16,1000);
              showNick      = bv(o,"showNick",      showNick);
              showHp        = bv(o,"showHp",        showHp);
              showArmor     = bv(o,"showArmor",     showArmor);
              tracer        = bv(o,"tracer",        tracer);
              oreEsp        = bv(o,"oreEsp",        oreEsp);
              oreRange      = ci(iv(o,"oreRange",   oreRange),  8,32);
              miningBot     = bv(o,"miningBot",     miningBot);
              miningOreType = ci(iv(o,"miningOreType", miningOreType), 0, ORE_TYPE_NAMES.length - 1);
              statusHud     = bv(o,"statusHud",   statusHud);
              noFall        = bv(o,"noFall",        noFall);
              killAura      = bv(o,"killAura",      killAura);
              killAuraRange = cf(fv(o,"killAuraRange", killAuraRange), 2f,6f);
              hitDelay      = bv(o,"hitDelay",      hitDelay);
              antiKnockback = bv(o,"antiKnockback", antiKnockback);
              antiKbStrength= cf(fv(o,"antiKbStrength", antiKbStrength), 0f,1f);
              alwaysSprint  = bv(o,"alwaysSprint",  alwaysSprint);
              nightVision   = bv(o,"nightVision",   nightVision);
              noSlowdown    = bv(o,"noSlowdown",    noSlowdown);
              autoArmor     = bv(o,"autoArmor",     autoArmor);
              armorHud      = bv(o,"armorHud",      armorHud);
              potionHud     = bv(o,"potionHud",     potionHud);
              reachDisplay  = bv(o,"reachDisplay",  reachDisplay);
              arrowPredict  = bv(o,"arrowPredict",  arrowPredict);
              autoAuth      = bv(o,"autoAuth",      autoAuth);
              autoReg       = bv(o,"autoReg",       autoReg);
              autoLogin     = bv(o,"autoLogin",     autoLogin);
              smartAuth     = bv(o,"smartAuth",     smartAuth);
              antiAfk       = bv(o,"antiAfk",       antiAfk);
              antiAfkDelay  = ci(iv(o,"antiAfkDelay", antiAfkDelay), 100,6000);
              autoReconnect = bv(o,"autoReconnect", autoReconnect);
              reconnectDelay= ci(iv(o,"reconnectDelay", reconnectDelay), 3,60);
              guiScale      = ci(iv(o,"guiScale",   guiScale), 0,2);
              if (o.has("authPasswordB64")) {
                  try { authPassword = new String(Base64.getDecoder().decode(o.get("authPasswordB64").getAsString()), StandardCharsets.UTF_8); }
                  catch (Exception e) { authPassword = sv(o,"authPassword", authPassword); }
              } else {
                  authPassword = sv(o,"authPassword", authPassword);
              }
          } catch (Exception e) { LOG.warn("[PlayerESP] Конфиг: {}", e.getMessage()); save(); }
      }

      public static void save() {
          try {
              Files.createDirectories(PATH.getParent());
              JsonObject o = new JsonObject();
              o.addProperty("espEnabled",    espEnabled);   o.addProperty("espR",espR); o.addProperty("espG",espG); o.addProperty("espB",espB);
              o.addProperty("espRange",      espRange);     o.addProperty("showNick",showNick); o.addProperty("showHp",showHp); o.addProperty("showArmor",showArmor);
              o.addProperty("tracer",        tracer);       o.addProperty("oreEsp",oreEsp); o.addProperty("oreRange",oreRange);
              o.addProperty("miningBot",     miningBot);    o.addProperty("miningOreType",miningOreType);
              o.addProperty("noFall",        noFall);       o.addProperty("killAura",killAura); o.addProperty("killAuraRange",killAuraRange);
              o.addProperty("hitDelay",      hitDelay);     o.addProperty("antiKnockback",antiKnockback); o.addProperty("antiKbStrength",antiKbStrength);
              o.addProperty("alwaysSprint",  alwaysSprint); o.addProperty("nightVision",nightVision); o.addProperty("noSlowdown",noSlowdown);
              o.addProperty("autoArmor",     autoArmor);    o.addProperty("armorHud",armorHud); o.addProperty("potionHud",potionHud);
              o.addProperty("reachDisplay",  reachDisplay); o.addProperty("arrowPredict",arrowPredict);
              o.addProperty("autoAuth",      autoAuth);     o.addProperty("autoReg",autoReg); o.addProperty("autoLogin",autoLogin);
              o.addProperty("authPasswordB64", Base64.getEncoder().encodeToString(authPassword.getBytes(StandardCharsets.UTF_8)));
              o.addProperty("smartAuth",     smartAuth);    o.addProperty("antiAfk",antiAfk); o.addProperty("antiAfkDelay",antiAfkDelay);
              o.addProperty("autoReconnect", autoReconnect); o.addProperty("reconnectDelay",reconnectDelay);
              o.addProperty("guiScale",      guiScale);
              o.addProperty("statusHud",     statusHud);
              try (Writer w = new OutputStreamWriter(new FileOutputStream(PATH.toFile()), StandardCharsets.UTF_8)) { GSON.toJson(o, w); }
          } catch (Exception e) { LOG.error("[PlayerESP] Сохранение: {}", e.getMessage()); }
      }

      private static boolean bv(JsonObject o,String k,boolean d){return o.has(k)?o.get(k).getAsBoolean():d;}
      private static float   fv(JsonObject o,String k,float   d){return o.has(k)?o.get(k).getAsFloat():d;}
      private static int     iv(JsonObject o,String k,int     d){return o.has(k)?o.get(k).getAsInt():d;}
      private static String  sv(JsonObject o,String k,String  d){return o.has(k)?o.get(k).getAsString():d;}
      private static float   cf(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
      private static int     ci(int   v,int   mn,int   mx){return Math.max(mn,Math.min(mx,v));}
  }
  