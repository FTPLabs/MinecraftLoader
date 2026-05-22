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

          // Правильный подход: только translate(-camPos), никаких camera rotation в PoseStack.
          // Шейдер (ModelViewMat uniform) сам применяет view rotation.
          // Это идентично тому, как рендерятся ванильные entities.
          PoseStack poseStack = new PoseStack();
          poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

          MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

          // Отключаем тест глубины — видно сквозь стены и объекты
          RenderSystem.disableDepthTest();

          List<? extends Player> players = mc.level.players();

          // ── Проход 1: боксы и HP-бар ──────────────────────────────────────────
          VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > RANGE) continue;

              // Partial-tick интерполяция — бокс точно следует за анимацией модели
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
          // camera.rotation() — стандартный MC-billboard, такой же как в EntityRenderer.
          // ModelViewMat(shader) * camera.rotation()(наш) = R_cam.conj * R_cam = Identity
          // → текст всегда смотрит фронтально в сторону камеры.
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
              poseStack.mulPose(event.getCamera().rotation());
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
  