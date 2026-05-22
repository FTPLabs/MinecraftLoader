package com.esp;

  import com.google.gson.*;
  import net.minecraftforge.fml.loading.FMLPaths;
  import java.io.*;
  import java.nio.file.*;

  public class EspConfig {
      private static final Path  PATH = FMLPaths.CONFIGDIR.get().resolve("playersesp.json");
      private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

      public static boolean espEnabled = true;
      public static float   espR = 1f, espG = 0f, espB = 0f;
      public static int     espRange   = 64;
      public static boolean showNick   = true;
      public static boolean showHp     = true;
      public static boolean showArmor  = true;
      public static boolean oreEsp     = false;
      public static int     oreRange   = 20;

      public static void load() {
          if (!Files.exists(PATH)) { save(); return; }
          try (Reader rdr = new FileReader(PATH.toFile())) {
              JsonObject o = GSON.fromJson(rdr, JsonObject.class);
              if (o == null) return;
              espEnabled=bv(o,"espEnabled",espEnabled); espR=fv(o,"espR",espR); espG=fv(o,"espG",espG); espB=fv(o,"espB",espB);
              espRange=iv(o,"espRange",espRange); showNick=bv(o,"showNick",showNick); showHp=bv(o,"showHp",showHp);
              showArmor=bv(o,"showArmor",showArmor); oreEsp=bv(o,"oreEsp",oreEsp); oreRange=iv(o,"oreRange",oreRange);
          } catch (Exception e) { save(); }
      }

      public static void save() {
          try {
              Files.createDirectories(PATH.getParent());
              JsonObject o = new JsonObject();
              o.addProperty("espEnabled",espEnabled); o.addProperty("espR",espR); o.addProperty("espG",espG); o.addProperty("espB",espB);
              o.addProperty("espRange",espRange); o.addProperty("showNick",showNick); o.addProperty("showHp",showHp);
              o.addProperty("showArmor",showArmor); o.addProperty("oreEsp",oreEsp); o.addProperty("oreRange",oreRange);
              try (Writer w = new FileWriter(PATH.toFile())) { GSON.toJson(o, w); }
          } catch (Exception ignored) {}
      }

      private static boolean bv(JsonObject o,String k,boolean d){return o.has(k)?o.get(k).getAsBoolean():d;}
      private static float   fv(JsonObject o,String k,float   d){return o.has(k)?o.get(k).getAsFloat():d;}
      private static int     iv(JsonObject o,String k,int     d){return o.has(k)?o.get(k).getAsInt():d;}
  }