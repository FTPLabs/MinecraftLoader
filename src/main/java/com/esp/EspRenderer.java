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
  import org.joml.Matrix4f;

  import java.util.List;

  public class EspRenderer {

      @SubscribeEvent
      public static void onRenderLevel(RenderLevelStageEvent event) {
          if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
          if (!EspConfig.espEnabled) return;

          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          Vec3  camPos = event.getCamera().getPosition();
          float pt     = event.getPartialTick();

          PoseStack ps = new PoseStack();
          ps.translate(-camPos.x, -camPos.y, -camPos.z);

          MultiBufferSource.BufferSource bufSrc = mc.renderBuffers().bufferSource();
          VertexConsumer lines = bufSrc.getBuffer(EspRenderType.espLines());

          List<? extends Player> players = mc.level.players();

          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > EspConfig.espRange) continue;

              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());
              double hw = player.getBbWidth()  / 2.0 + 0.05;
              double bh = player.getBbHeight() + 0.05;

              float hp  = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);
              float r = EspConfig.espR, g = EspConfig.espG, b = EspConfig.espB;
              float rd = r * 0.5f, gd = g * 0.5f, bd = b * 0.5f;

              renderCornerBox(ps, lines,
                  px-hw, py-0.05, pz-hw,
                  px+hw, py+bh,   pz+hw,
                  r, g, b, rd, gd, bd);

              // Вертикальный HP-бар (левее бокса)
              double bx  = px - hw - 0.10;
              double yBt = py - 0.05, yTp = py + bh;
              double yFl = yBt + (yTp - yBt) * hp;
              addLine(ps, lines, bx, yBt, pz, bx, yTp, pz, 0.25f, 0.25f, 0.25f, 0.9f);
              addLine(ps, lines, bx, yBt, pz, bx, yFl, pz, 1f-hp, hp, 0f, 1f);
          }
          bufSrc.endBatch(EspRenderType.espLines());

          // ── Текст (ник / HP / броня) ──────────────────────────────────────
          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > EspConfig.espRange) continue;

              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());
              double bh = player.getBbHeight() + 0.05;

              float hp    = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);
              int   hpInt = (int) player.getHealth();
              int   hpMax = (int) player.getMaxHealth();
              int   armor = player.getArmorValue();
              int   hpCol = 0xFF000000 | ((int)((1f-hp)*255) << 16) | ((int)(hp*255) << 8);

              ps.pushPose();
              ps.translate(px, py + bh + 0.28, pz);
              ps.mulPose(event.getCamera().rotation());
              ps.scale(-0.025f, -0.025f, 0.025f);

              float lineY = 0f;
              if (EspConfig.showNick) {
                  String s = player.getGameProfile().getName();
                  mc.font.drawInBatch(s, -mc.font.width(s)/2f, lineY, 0xFFFFFFFF,
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                  lineY += 10f;
              }
              if (EspConfig.showHp) {
                  String s = hpInt + " / " + hpMax + " HP";
                  mc.font.drawInBatch(s, -mc.font.width(s)/2f, lineY, hpCol,
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                  lineY += 10f;
              }
              if (EspConfig.showArmor && armor > 0) {
                  String s = armor + " / 20 armor";
                  mc.font.drawInBatch(s, -mc.font.width(s)/2f, lineY, 0xFFBBBBBB,
                      false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
              }
              ps.popPose();
          }
          bufSrc.endBatch();
      }

      private static void renderCornerBox(PoseStack ps, VertexConsumer buf,
              double x1,double y1,double z1,double x2,double y2,double z2,
              float r,float g,float b,float rd,float gd,float bd) {
          float lx=(float)((x2-x1)*0.25f), ly=(float)((y2-y1)*0.25f), lz=(float)((z2-z1)*0.25f);
          float mx1=(float)x1,my1=(float)y1,mz1=(float)z1,mx2=(float)x2,my2=(float)y2,mz2=(float)z2;
          // Нижние 4 угла
          seg(ps,buf,mx1,my1,mz1,mx1+lx,my1,mz1,rd,gd,bd); seg(ps,buf,mx1,my1,mz1,mx1,my1,mz1+lz,rd,gd,bd); seg(ps,buf,mx1,my1,mz1,mx1,my1+ly,mz1,rd,gd,bd);
          seg(ps,buf,mx2,my1,mz1,mx2-lx,my1,mz1,rd,gd,bd); seg(ps,buf,mx2,my1,mz1,mx2,my1,mz1+lz,rd,gd,bd); seg(ps,buf,mx2,my1,mz1,mx2,my1+ly,mz1,rd,gd,bd);
          seg(ps,buf,mx1,my1,mz2,mx1+lx,my1,mz2,rd,gd,bd); seg(ps,buf,mx1,my1,mz2,mx1,my1,mz2-lz,rd,gd,bd); seg(ps,buf,mx1,my1,mz2,mx1,my1+ly,mz2,rd,gd,bd);
          seg(ps,buf,mx2,my1,mz2,mx2-lx,my1,mz2,rd,gd,bd); seg(ps,buf,mx2,my1,mz2,mx2,my1,mz2-lz,rd,gd,bd); seg(ps,buf,mx2,my1,mz2,mx2,my1+ly,mz2,rd,gd,bd);
          // Верхние 4 угла
          seg(ps,buf,mx1,my2,mz1,mx1+lx,my2,mz1,r,g,b); seg(ps,buf,mx1,my2,mz1,mx1,my2,mz1+lz,r,g,b); seg(ps,buf,mx1,my2,mz1,mx1,my2-ly,mz1,r,g,b);
          seg(ps,buf,mx2,my2,mz1,mx2-lx,my2,mz1,r,g,b); seg(ps,buf,mx2,my2,mz1,mx2,my2,mz1+lz,r,g,b); seg(ps,buf,mx2,my2,mz1,mx2,my2-ly,mz1,r,g,b);
          seg(ps,buf,mx1,my2,mz2,mx1+lx,my2,mz2,r,g,b); seg(ps,buf,mx1,my2,mz2,mx1,my2,mz2-lz,r,g,b); seg(ps,buf,mx1,my2,mz2,mx1,my2-ly,mz2,r,g,b);
          seg(ps,buf,mx2,my2,mz2,mx2-lx,my2,mz2,r,g,b); seg(ps,buf,mx2,my2,mz2,mx2,my2,mz2-lz,r,g,b); seg(ps,buf,mx2,my2,mz2,mx2,my2-ly,mz2,r,g,b);
      }

      private static void seg(PoseStack ps,VertexConsumer buf,
              float x1,float y1,float z1,float x2,float y2,float z2,float r,float g,float b) {
          addLine(ps,buf,x1,y1,z1,x2,y2,z2,r,g,b,1f);
      }

      private static void addLine(PoseStack ps, VertexConsumer buf,
              double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a) {
          Matrix4f mat=ps.last().pose();
          float dx=(float)(x2-x1),dy=(float)(y2-y1),dz=(float)(z2-z1);
          float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
          if(len>1e-6f){dx/=len;dy/=len;dz/=len;}else{dy=1f;}
          // MC 1.21.1: setNormal принимает PoseStack.Pose (ps.last()), не Matrix3f
          buf.addVertex(mat,(float)x1,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(ps.last(),dx,dy,dz);
          buf.addVertex(mat,(float)x2,(float)y2,(float)z2).setColor(r,g,b,a).setNormal(ps.last(),dx,dy,dz);
      }
  }