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

  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspHudRenderer {

      @SubscribeEvent
      public static void onRenderTick(TickEvent.RenderTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null || mc.screen != null) return;

          boolean anyHud = EspConfig.armorHud || EspConfig.potionHud
                        || EspConfig.reachDisplay || EspConfig.statusHud;
          if (!anyHud) return;

          int sw = mc.getWindow().getGuiScaledWidth();
          int sh = mc.getWindow().getGuiScaledHeight();

          Matrix4f ortho = new Matrix4f().setOrtho(0, sw, sh, 0, -1000f, 1000f);
          RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
          RenderSystem.enableBlend();
          RenderSystem.defaultBlendFunc();

          PoseStack ps = new PoseStack();
          MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

          if (EspConfig.armorHud)     renderArmorHud(mc, ps, buf, 4, 4, sw);
          if (EspConfig.potionHud)    renderPotionHud(mc, ps, buf, sw - 4, 4);
          if (EspConfig.reachDisplay) renderReachDisplay(mc, ps, buf, sw, sh);
          if (EspConfig.statusHud)    renderStatusHud(mc, ps, buf, sw, sh);

          buf.endBatch();
      }

      // ── Helpers ──────────────────────────────────────────────────────────────

      /**
       * FIX: alpha=0 -> skip (invisible), not force-opaque.
       */
      private static void fill(Matrix4f m, int x1, int y1, int x2, int y2, int color) {
          float a = ((color >> 24) & 0xFF) / 255f;
          if (a <= 0f) return;
          float r = ((color >> 16) & 0xFF) / 255f;
          float g = ((color >> 8)  & 0xFF) / 255f;
          float b = (color         & 0xFF) / 255f;
          RenderSystem.setShader(GameRenderer::getPositionColorShader);
          var vb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
          vb.addVertex(m, x1, y1, 0).setColor(r, g, b, a);
          vb.addVertex(m, x1, y2, 0).setColor(r, g, b, a);
          vb.addVertex(m, x2, y2, 0).setColor(r, g, b, a);
          vb.addVertex(m, x2, y1, 0).setColor(r, g, b, a);
          BufferUploader.drawWithShader(vb.buildOrThrow());
      }

      private static void text(Minecraft mc, PoseStack ps, MultiBufferSource buf,
                                String txt, int x, int y, int color) {
          mc.font.drawInBatch(txt, x, y, color, false, ps.last().pose(),
              buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
      }

      // ── Armor HUD ────────────────────────────────────────────────────────────

      private static void renderArmorHud(Minecraft mc, PoseStack ps,
                                          MultiBufferSource.BufferSource buf,
                                          int x, int y, int sw) {
          EquipmentSlot[] slots  = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
          String[]        labels = { "Шлем ", "Нагр ", "Порт ", "Бот  " };
          int bgW = 112, rowH = 10, totalH = slots.length * (rowH + 4) + 4;
          var m = ps.last().pose();
          fill(m, x - 2, y - 2, x + bgW, y + totalH, 0xAA050C1E);
          fill(m, x - 2, y - 2, x + bgW, y - 1,      0xFF7C5CFC);
          buf.endBatch();

          for (int i = 0; i < slots.length; i++) {
              ItemStack stack = mc.player.getItemBySlot(slots[i]);
              int ry = y + 2 + i * (rowH + 4);
              if (stack.isEmpty()) {
                  text(mc, ps, buf, labels[i] + "-", x + 2, ry, 0xFF555555);
                  continue;
              }
              int   maxDur = stack.getMaxDamage();
              int   curDur = maxDur > 0 ? maxDur - stack.getDamageValue() : -1;
              float pct    = maxDur > 0 ? (float) curDur / maxDur : 1f;
              int   col    = durColor(pct);
              String label = labels[i] + (maxDur > 0 ? curDur + "/" + maxDur : "inf");
              text(mc, ps, buf, label, x + 2, ry, col);
              if (maxDur > 0) {
                  int bx = x + 2, by = ry + rowH;
                  buf.endBatch();
                  fill(m, bx, by, bx + bgW - 6, by + 2, 0xFF1A1A2E);
                  fill(m, bx, by, bx + (int)(pct * (bgW - 6)), by + 2, col);
              }
          }
      }

      // ── Potion HUD ───────────────────────────────────────────────────────────

      private static void renderPotionHud(Minecraft mc, PoseStack ps,
                                           MultiBufferSource.BufferSource buf,
                                           int rightX, int startY) {
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
              String time = dur > 72000 ? "inf" : String.format("%d:%02d", dur / 1200, (dur % 1200) / 20);
              String txt  = (amp > 1 ? amp + "x " : "") + name + " " + time;
              boolean good = eff.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
              text(mc, ps, buf, txt, rightX - bgW + 2, y + 2, good ? 0xFF4ADE80 : 0xFFF87171);
              y += 13;
          }
      }

      // ── Reach Display ─────────────────────────────────────────────────────────

      private static void renderReachDisplay(Minecraft mc, PoseStack ps,
                                              MultiBufferSource.BufferSource buf,
                                              int sw, int sh) {
          Player nearest = null;
          double minDist = Double.MAX_VALUE;
          try {
              for (Player p : List.copyOf(mc.level.players())) {
                  if (p == mc.player) continue;
                  double d = mc.player.distanceTo(p);
                  if (d < minDist) { minDist = d; nearest = p; }
              }
          } catch (Exception ignored) {}
          if (nearest == null) return;

          int    col = minDist <= 3.5 ? 0xFF4ADE80 : minDist <= 6 ? 0xFFFACC15 : 0xFFF87171;
          String txt = nearest.getGameProfile().getName() + " - " + String.format("%.1f", minDist) + " bl.";
          int    tw  = mc.font.width(txt);
          text(mc, ps, buf, txt, sw / 2 - tw / 2, sh / 2 + 22, col);
      }

      // ── Status HUD — список активных модулей ──────────────────────────────────

      private static void renderStatusHud(Minecraft mc, PoseStack ps,
                                           MultiBufferSource.BufferSource buf,
                                           int sw, int sh) {
          List<String> active = new ArrayList<>();
          if (EspConfig.espEnabled)    active.add("[ESP]");
          if (EspConfig.tracer)        active.add("[Tracers]");
          if (EspConfig.oreEsp)        active.add("[OreESP]");
          if (EspConfig.miningBot)     active.add("[Bot: " + EspConfig.ORE_TYPE_NAMES[EspConfig.miningOreType] + "]");
          if (EspConfig.killAura)      active.add("[KillAura]");
          if (EspConfig.noFall)        active.add("[NoFall]");
          if (EspConfig.alwaysSprint)  active.add("[Sprint]");
          if (EspConfig.nightVision)   active.add("[NightVision]");
          if (EspConfig.noSlowdown)    active.add("[NoSlow]");
          if (EspConfig.antiKnockback) active.add("[AntiKB]");
          if (EspConfig.autoArmor)     active.add("[AutoArmor]");
          if (EspConfig.autoReconnect) active.add("[Reconnect]");
          if (EspConfig.antiAfk)       active.add("[AntiAFK]");
          if (EspConfig.arrowPredict)  active.add("[AimLine]");

          if (active.isEmpty()) return;

          int rowH = 10, pad = 3;
          int bgW  = 82;
          int bgH  = active.size() * rowH + pad * 2;
          int bx   = sw - bgW - 2;
          int by   = sh - bgH - 2;
          var m    = ps.last().pose();

          fill(m, bx - 1, by - 1, bx + bgW + 1, by + bgH + 1, 0xAA050C1E);
          fill(m, bx - 1, by - 1, bx + bgW + 1, by,           0xFF7C5CFC);
          buf.endBatch();

          for (int i = 0; i < active.size(); i++) {
              text(mc, ps, buf, active.get(i), bx + pad, by + pad + i * rowH, 0xFF4ADE80);
          }
      }

      private static int durColor(float pct) {
          return 0xFF000000 | ((int)((1f - pct) * 255) << 16) | ((int)(pct * 255) << 8);
      }
  }
  