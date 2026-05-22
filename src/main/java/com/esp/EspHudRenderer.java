package com.esp;

  import com.mojang.blaze3d.systems.RenderSystem;
  import com.mojang.blaze3d.vertex.BufferUploader;
  import com.mojang.blaze3d.vertex.DefaultVertexFormat;
  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.Tesselator;
  import com.mojang.blaze3d.vertex.VertexFormat;
  import com.mojang.blaze3d.vertex.VertexSorting;
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.Font;
  import net.minecraft.client.renderer.GameRenderer;
  import net.minecraft.client.renderer.LightTexture;
  import net.minecraft.client.renderer.MultiBufferSource;
  import net.minecraft.network.chat.Component;
  import net.minecraft.world.effect.MobEffectCategory;
  import net.minecraft.world.effect.MobEffectInstance;
  import net.minecraft.world.entity.EquipmentSlot;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.item.ItemStack;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.joml.Matrix4f;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * Custom HUD overlay: Armor HUD, Potion HUD, Reach Display.
   *
   * FIX v1.6:
   *  - fill(): исправлена логика alpha — 0-alpha = невидимый (не заменять на 1)
   *  - renderArmorHud(): убраны лишние endBatch внутри цикла (оптимизация)
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspHudRenderer {

      @SubscribeEvent
      public static void onRenderTick(TickEvent.RenderTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;

          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null || mc.screen != null) return;
          boolean anyHud = EspConfig.armorHud || EspConfig.potionHud || EspConfig.reachDisplay || EspConfig.statusHud;
          if (!anyHud) return;

          int sw = mc.getWindow().getGuiScaledWidth();
          int sh = mc.getWindow().getGuiScaledHeight();

          Matrix4f ortho = new Matrix4f().setOrtho(0, sw, sh, 0, -1000f, 1000f);
          RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
          RenderSystem.enableBlend();
          RenderSystem.defaultBlendFunc();

          PoseStack ps = new PoseStack();
          MultiBufferSource.BufferSource bufSource = mc.renderBuffers().bufferSource();

          if (EspConfig.armorHud)     renderArmorHud(mc, ps, bufSource, 4, 4, sw);
          if (EspConfig.potionHud)    renderPotionHud(mc, ps, bufSource, sw - 4, 4);
          if (EspConfig.reachDisplay) renderReachDisplay(mc, ps, bufSource, sw, sh);
          if (EspConfig.statusHud)    renderStatusHud(mc, ps, bufSource, sw, sh);

          bufSource.endBatch();
      }

      /**
       * FIX: оригинальный код заменял alpha=0 на 1 (непрозрачный), что некорректно.
       * Теперь: если alpha=0 — пропускаем отрисовку (невидимый цвет).
       */
      private static void fill(Matrix4f m, int x1, int y1, int x2, int y2, int color) {
          float a = ((color >> 24) & 0xFF) / 255f;
          if (a <= 0f) return; // FIX: прозрачный — не рисуем вообще
          float r = ((color >> 16) & 0xFF) / 255f;
          float g = ((color >> 8)  & 0xFF) / 255f;
          float b = (color         & 0xFF) / 255f;
          RenderSystem.setShader(GameRenderer::getPositionColorShader);
          var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
          buf.addVertex(m, x1, y1, 0).setColor(r, g, b, a);
          buf.addVertex(m, x1, y2, 0).setColor(r, g, b, a);
          buf.addVertex(m, x2, y2, 0).setColor(r, g, b, a);
          buf.addVertex(m, x2, y1, 0).setColor(r, g, b, a);
          BufferUploader.drawWithShader(buf.buildOrThrow());
      }

      private static void text(Minecraft mc, PoseStack ps, MultiBufferSource bufSource,
                                String txt, int x, int y, int color) {
          mc.font.drawInBatch(txt, x, y, color, false, ps.last().pose(),
              bufSource, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
      }

      // ── Armor HUD ──────────────────────────────────────────────────────────

      private static void renderArmorHud(Minecraft mc, PoseStack ps,
                                          MultiBufferSource.BufferSource buf, int x, int y, int sw) {
          EquipmentSlot[] slots  = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
          String[]        labels = { "Шлем ", "Нагр ", "Порт ", "Бот  " };
          int bgW = 112, rowH = 10, totalH = slots.length * (rowH + 4) + 4;
          var m = ps.last().pose();

          // Фон рендерим один раз до всего текста
          fill(m, x - 2, y - 2, x + bgW, y + totalH, 0xAA050C1E);
          fill(m, x - 2, y - 2, x + bgW, y - 1,      0xFF7C5CFC);
          buf.endBatch();

          for (int i = 0; i < slots.length; i++) {
              ItemStack stack = mc.player.getItemBySlot(slots[i]);
              int ry = y + 2 + i * (rowH + 4);
              if (stack.isEmpty()) {
                  text(mc, ps, buf, labels[i] + "—", x + 2, ry, 0xFF555555);
                  continue;
              }
              int   maxDur = stack.getMaxDamage();
              int   curDur = maxDur > 0 ? maxDur - stack.getDamageValue() : -1;
              float pct    = maxDur > 0 ? (float) curDur / maxDur : 1f;
              int   col    = durColor(pct);
              text(mc, ps, buf, labels[i] + (maxDur > 0 ? curDur + "/" + maxDur : "\u221e"), x + 2, ry, col);

              if (maxDur > 0) {
                  int bx = x + 2, by = ry + rowH;
                  // FIX: endBatch только один раз после всего текста в итерации, не внутри цикла
                  buf.endBatch();
                  fill(m, bx, by, bx + bgW - 6, by + 2, 0xFF1A1A2E);
                  fill(m, bx, by, bx + (int)(pct * (bgW - 6)), by + 2, col);
              }
          }
      }

      // ── Potion HUD ─────────────────────────────────────────────────────────

      private static void renderPotionHud(Minecraft mc, PoseStack ps,
                                           MultiBufferSource.BufferSource buf, int rightX, int startY) {
          var effects = mc.player.getActiveEffects();
          if (effects.isEmpty()) return;

          List<MobEffectInstance> sorted = new ArrayList<>(effects);
          sorted.sort((a, b) -> {
              boolean ab = a.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
              boolean bb = b.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
              return Boolean.compare(!ab, !bb);
          });

          int bgW = 130, bgH = sorted.size() * 13 + 6, y = startY;
          var m = ps.last().pose();
          fill(m, rightX - bgW - 2, y - 2, rightX + 2, y + bgH, 0xAA050C1E);
          fill(m, rightX - bgW - 2, y - 2, rightX + 2, y - 1,   0xFF22D3EE);
          buf.endBatch();

          for (MobEffectInstance eff : sorted) {
              String name = Component.translatable(eff.getEffect().value().getDescriptionId()).getString();
              if (name.length() > 13) name = name.substring(0, 12) + "..";
              int    amp  = eff.getAmplifier() + 1;
              int    dur  = eff.getDuration();
              String time = dur > 72000 ? "\u221e" : String.format("%d:%02d", dur / 1200, (dur % 1200) / 20);
              String txt  = (amp > 1 ? amp + "x " : "") + name + " " + time;
              boolean good = eff.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
              text(mc, ps, buf, txt, rightX - bgW + 2, y + 2, good ? 0xFF4ADE80 : 0xFFF87171);
              y += 13;
          }
      }

      // ── Reach Display ──────────────────────────────────────────────────────

      private static void renderReachDisplay(Minecraft mc, PoseStack ps,
                                              MultiBufferSource.BufferSource buf, int sw, int sh) {
          Player nearest  = null;
          double minDist  = Double.MAX_VALUE;
          try {
              for (Player p : List.copyOf(mc.level.players())) {
                  if (p == mc.player) continue;
                  double d = mc.player.distanceTo(p);
                  if (d < minDist) { minDist = d; nearest = p; }
              }
          } catch (Exception ignored) {}
          if (nearest == null) return;

          int    col = minDist <= 3.5 ? 0xFF4ADE80 : minDist <= 6 ? 0xFFFACC15 : 0xFFF87171;
          String txt = nearest.getGameProfile().getName() + " \u2014 " + String.format("%.1f", minDist) + " \
      // ── Status HUD — список активных модулей ──────────────────────────────────

      private static void renderStatusHud(Minecraft mc, PoseStack ps,
                                           MultiBufferSource.BufferSource buf, int sw, int sh) {
          // Собираем только включённые модули
          java.util.List<String> lines = new java.util.ArrayList<>();
          if (EspConfig.espEnabled)    lines.add("\u00A7aESP");
          if (EspConfig.tracer)        lines.add("\u00A7aТрейсеры");
          if (EspConfig.oreEsp)        lines.add("\u00A7aОреESP");
          if (EspConfig.miningBot)     lines.add("\u00A7eМайнБот [" + EspConfig.ORE_TYPE_NAMES[EspConfig.miningOreType] + "]");
          if (EspConfig.killAura)      lines.add("\u00A7cКиллАура");
          if (EspConfig.noFall)        lines.add("\u00A7aАнтиУрон");
          if (EspConfig.alwaysSprint)  lines.add("\u00A7aСпринт");
          if (EspConfig.nightVision)   lines.add("\u00A7aНочьВид");
          if (EspConfig.noSlowdown)    lines.add("\u00A7aБезЗамедл.");
          if (EspConfig.antiKnockback) lines.add("\u00A7aАнтиОтброс");
          if (EspConfig.autoArmor)     lines.add("\u00A7aАвтоБроня");
          if (EspConfig.autoReconnect) lines.add("\u00A7eАвтоРеконн.");
          if (EspConfig.antiAfk)       lines.add("\u00A7eАнтиAFK");
          if (EspConfig.arrowPredict)  lines.add("\u00A7aТраект.");

          if (lines.isEmpty()) return;

          int rowH  = 10;
          int pad   = 3;
          int bgW   = 80;
          int bgH   = lines.size() * rowH + pad * 2;
          int bx    = sw - bgW - 2;
          int by    = sh - bgH - 2;
          var m = ps.last().pose();

          fill(m, bx - 1, by - 1, bx + bgW + 1, by + bgH + 1, 0xAA050C1E);
          fill(m, bx - 1, by - 1, bx + bgW + 1, by,           0xFF7C5CFC);
          buf.endBatch();

          for (int i = 0; i < lines.size(); i++) {
              text(mc, ps, buf, lines.get(i), bx + pad, by + pad + i * rowH, 0xFFFFFFFF);
          }
      }

  u0431\u043b.";
          int    tw  = mc.font.width(txt);
          text(mc, ps, buf, txt, sw / 2 - tw / 2, sh / 2 + 22, col);
      }

      private static int durColor(float pct) {
          return 0xFF000000 | ((int)((1f - pct) * 255) << 16) | ((int)(pct * 255) << 8);
      }
  }
  