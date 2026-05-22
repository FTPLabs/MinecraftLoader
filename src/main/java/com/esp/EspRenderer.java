package com.esp;

  import com.mojang.blaze3d.systems.RenderSystem;
  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.VertexConsumer;
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.Font;
  import net.minecraft.client.renderer.LevelRenderer;
  import net.minecraft.client.renderer.LightTexture;
  import net.minecraft.client.renderer.MultiBufferSource;
  import net.minecraft.client.renderer.RenderType;
  import net.minecraft.util.Mth;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.AABB;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.client.event.RenderLevelStageEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import org.joml.Quaternionf;

  import java.util.List;

  public class EspRenderer {

      public static float RED   = 1.000f;
      public static float GREEN = 0.000f;
      public static float BLUE  = 0.000f;
      public static float ALPHA = 1.000f;
      public static int   RANGE = 64;

      @SubscribeEvent
      public static void onRenderLevel(RenderLevelStageEvent event) {
          if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
          if (!EspKeyHandler.isEnabled()) return;

          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          Vec3 camPos = event.getCamera().getPosition();
          float pt    = event.getPartialTick();

          // camera.rotation() = R_cam: ориентация камеры (camera→world).
          // Матрица вида (view matrix) = R_cam.conjugate() (world→camera).
          // Строим: viewRotation * T(-camPos) — стандартная view-матрица.
          Quaternionf viewRot = new Quaternionf(event.getCamera().rotation()).conjugate();

          PoseStack poseStack = new PoseStack();
          poseStack.mulPose(viewRot);
          poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

          MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

          RenderSystem.disableDepthTest();

          List<? extends Player> players = mc.level.players();

          // ── Проход 1: боксы и HP-бар ──────────────────────────────────────────
          VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > RANGE) continue;

              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());
              double hw = player.getBbWidth() / 2.0 + 0.05;
              double h  = player.getBbHeight() + 0.05;

              AABB box = new AABB(px - hw, py - 0.05, pz - hw, px + hw, py + h, pz + hw);
              LevelRenderer.renderLineBox(poseStack, consumer, box, RED, GREEN, BLUE, ALPHA);

              float hp = player.getHealth() / player.getMaxHealth();
              AABB hpBar = new AABB(
                  box.minX, box.maxY + 0.15, box.minZ,
                  box.minX + (box.maxX - box.minX) * hp, box.maxY + 0.20, box.maxZ
              );
              LevelRenderer.renderLineBox(poseStack, consumer, hpBar, 1.0f - hp, hp, 0.0f, 1.0f);
          }
          bufferSource.endBatch(RenderType.lines());

          // ── Проход 2: ники (billboard) ────────────────────────────────────────
          // Billboard = R_cam (не conjugate!), т.к. base уже имеет R_cam.conj:
          // R_cam.conj * T(pos) * R_cam * S * v → rotation part = Identity → текст фронтально
          Quaternionf camRot = event.getCamera().rotation();

          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > RANGE) continue;

              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());
              double h  = player.getBbHeight() + 0.05;

              String name = player.getGameProfile().getName();

              poseStack.pushPose();
              poseStack.translate(px, py + h + 0.30, pz);
              poseStack.mulPose(camRot);
              poseStack.scale(-0.025f, -0.025f, 0.025f);

              float nameX = -mc.font.width(name) / 2.0f;
              mc.font.drawInBatch(
                  name, nameX, 0, 0xFFFFFF,
                  false, poseStack.last().pose(),
                  bufferSource, Font.DisplayMode.SEE_THROUGH,
                  0, LightTexture.FULL_BRIGHT
              );
              poseStack.popPose();
          }
          bufferSource.endBatch();

          RenderSystem.enableDepthTest();
      }
  }
  