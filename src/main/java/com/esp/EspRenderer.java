package com.esp;

  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.VertexConsumer;
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.Font;
  import net.minecraft.client.renderer.LightTexture;
  import net.minecraft.client.renderer.MultiBufferSource;
  import net.minecraft.util.Mth;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.client.event.RenderLevelStageEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;

  import java.util.ArrayList;
  import java.util.List;

  public class EspRenderer {

      /** Cached per-frame snapshot — avoids iterating the player list twice. */
      private record PlayerSnapshot(
          double px, double py, double pz,
          double hw, double bh,
          float hp, int hpInt, int hpMax, int armor,
          int hpColor, String name
      ) {}

      @SubscribeEvent
      public static void onRenderLevel(RenderLevelStageEvent event) {
          if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
          if (!EspConfig.espEnabled) return;

          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          Vec3  camPos = event.getCamera().getPosition();
          float pt     = event.getPartialTick();

          // ── Single pass: collect visible player snapshots ─────────────────────
          List<PlayerSnapshot> visible = new ArrayList<>();
          for (Player player : mc.level.players()) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > EspConfig.espRange) continue;

              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());
              double hw = player.getBbWidth()  / 2.0 + 0.05;
              double bh = player.getBbHeight() + 0.05;

              float hp    = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);
              int   hpInt = (int) player.getHealth();
              int   hpMax = (int) player.getMaxHealth();
              int   armor = player.getArmorValue();
              int   hpCol = 0xFF000000 | ((int)((1f - hp) * 255) << 16) | ((int)(hp * 255) << 8);

              visible.add(new PlayerSnapshot(px, py, pz, hw, bh, hp, hpInt, hpMax, armor, hpCol,
                  player.getGameProfile().getName()));
          }

          if (visible.isEmpty()) return;

          PoseStack ps = new PoseStack();
          ps.translate(-camPos.x, -camPos.y, -camPos.z);

          MultiBufferSource.BufferSource bufSrc = mc.renderBuffers().bufferSource();

          // ── Pass 1: bounding boxes + HP bars (lines) ──────────────────────────
          VertexConsumer lines = bufSrc.getBuffer(EspRenderType.espLines());
          for (PlayerSnapshot s : visible) {
              float r = EspConfig.espR, g = EspConfig.espG, b = EspConfig.espB;
              float rd = r * 0.5f, gd = g * 0.5f, bd = b * 0.5f;

              renderCornerBox(ps, lines,
                  s.px() - s.hw(), s.py() - 0.05, s.pz() - s.hw(),
                  s.px() + s.hw(), s.py() + s.bh(), s.pz() + s.hw(),
                  r, g, b, rd, gd, bd);

              double bx  = s.px() - s.hw() - 0.10;
              double yBt = s.py() - 0.05, yTp = s.py() + s.bh();
              double yFl = yBt + (yTp - yBt) * s.hp();
              EspLineUtil.addLine(ps, lines, bx, yBt, s.pz(), bx, yTp, s.pz(), 0.25f, 0.25f, 0.25f, 0.9f);
              EspLineUtil.addLine(ps, lines, bx, yBt, s.pz(), bx, yFl, s.pz(), 1f - s.hp(), s.hp(), 0f, 1f);
          }
          bufSrc.endBatch(EspRenderType.espLines());

          // ── Pass 2: text labels (nick / HP / armor) ───────────────────────────
          for (PlayerSnapshot s : visible) {
              ps.pushPose();
              ps.translate(s.px(), s.py() + s.bh() + 0.28, s.pz());
              ps.mulPose(event.getCamera().rotation());
              ps.scale(-0.025f, -0.025f, 0.025f);

              float lineY = 0f;
              if (EspConfig.showNick) {
                  mc.font.drawInBatch(s.name(), -mc.font.width(s.name()) / 2f, lineY, 0xFFFFFFFF,
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                  lineY += 10f;
              }
              if (EspConfig.showHp) {
                  String hp = s.hpInt() + " / " + s.hpMax() + " HP";
                  mc.font.drawInBatch(hp, -mc.font.width(hp) / 2f, lineY, s.hpColor(),
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                  lineY += 10f;
              }
              if (EspConfig.showArmor && s.armor() > 0) {
                  String ar = s.armor() + " / 20 armor";
                  mc.font.drawInBatch(ar, -mc.font.width(ar) / 2f, lineY, 0xFFBBBBBB,
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
              }
              ps.popPose();
          }
          bufSrc.endBatch();
      }

      private static void renderCornerBox(PoseStack ps, VertexConsumer buf,
              double x1, double y1, double z1, double x2, double y2, double z2,
              float r, float g, float b, float rd, float gd, float bd) {
          float lx = (float)((x2 - x1) * 0.25);
          float ly = (float)((y2 - y1) * 0.25);
          float lz = (float)((z2 - z1) * 0.25);
          float mx1 = (float)x1, my1 = (float)y1, mz1 = (float)z1;
          float mx2 = (float)x2, my2 = (float)y2, mz2 = (float)z2;

          // Bottom corners
          seg(ps,buf,mx1,my1,mz1, mx1+lx,my1,mz1,  rd,gd,bd); seg(ps,buf,mx1,my1,mz1, mx1,my1,mz1+lz, rd,gd,bd); seg(ps,buf,mx1,my1,mz1, mx1,my1+ly,mz1, rd,gd,bd);
          seg(ps,buf,mx2,my1,mz1, mx2-lx,my1,mz1,  rd,gd,bd); seg(ps,buf,mx2,my1,mz1, mx2,my1,mz1+lz, rd,gd,bd); seg(ps,buf,mx2,my1,mz1, mx2,my1+ly,mz1, rd,gd,bd);
          seg(ps,buf,mx1,my1,mz2, mx1+lx,my1,mz2,  rd,gd,bd); seg(ps,buf,mx1,my1,mz2, mx1,my1,mz2-lz, rd,gd,bd); seg(ps,buf,mx1,my1,mz2, mx1,my1+ly,mz2, rd,gd,bd);
          seg(ps,buf,mx2,my1,mz2, mx2-lx,my1,mz2,  rd,gd,bd); seg(ps,buf,mx2,my1,mz2, mx2,my1,mz2-lz, rd,gd,bd); seg(ps,buf,mx2,my1,mz2, mx2,my1+ly,mz2, rd,gd,bd);
          // Top corners
          seg(ps,buf,mx1,my2,mz1, mx1+lx,my2,mz1,  r,g,b); seg(ps,buf,mx1,my2,mz1, mx1,my2,mz1+lz, r,g,b); seg(ps,buf,mx1,my2,mz1, mx1,my2-ly,mz1, r,g,b);
          seg(ps,buf,mx2,my2,mz1, mx2-lx,my2,mz1,  r,g,b); seg(ps,buf,mx2,my2,mz1, mx2,my2,mz1+lz, r,g,b); seg(ps,buf,mx2,my2,mz1, mx2,my2-ly,mz1, r,g,b);
          seg(ps,buf,mx1,my2,mz2, mx1+lx,my2,mz2,  r,g,b); seg(ps,buf,mx1,my2,mz2, mx1,my2,mz2-lz, r,g,b); seg(ps,buf,mx1,my2,mz2, mx1,my2-ly,mz2, r,g,b);
          seg(ps,buf,mx2,my2,mz2, mx2-lx,my2,mz2,  r,g,b); seg(ps,buf,mx2,my2,mz2, mx2,my2,mz2-lz, r,g,b); seg(ps,buf,mx2,my2,mz2, mx2,my2-ly,mz2, r,g,b);
      }

      private static void seg(PoseStack ps, VertexConsumer buf,
              float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
          EspLineUtil.addLine(ps, buf, x1, y1, z1, x2, y2, z2, r, g, b, 1f);
      }
  }
  