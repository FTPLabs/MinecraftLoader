package com.esp;

  import com.mojang.blaze3d.systems.RenderSystem;
  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.VertexConsumer;
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.renderer.LevelRenderer;
  import net.minecraft.client.renderer.RenderType;
  import net.minecraft.util.Mth;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.AABB;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.client.event.RenderLevelStageEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import org.joml.Matrix4f;

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
          float pt = event.getPartialTick();

          // Forge 52.x: getPoseStack() возвращает org.joml.Matrix4f (матрица вида камеры)
          Matrix4f cameraMatrix = event.getPoseStack();
          PoseStack poseStack = new PoseStack();
          poseStack.last().pose().set(cameraMatrix);
          poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

          var bufferSource = mc.renderBuffers().bufferSource();

          // Отключаем тест глубины — боксы видны сквозь любые блоки
          RenderSystem.disableDepthTest();

          VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

          List<? extends Player> players = mc.level.players();
          for (Player player : players) {
              if (player == mc.player) continue;
              if (player.distanceTo(mc.player) > RANGE) continue;

              // Интерполяция позиции по partial tick — бокс точно следует за моделью
              double px = Mth.lerp(pt, player.xo, player.getX());
              double py = Mth.lerp(pt, player.yo, player.getY());
              double pz = Mth.lerp(pt, player.zo, player.getZ());

              double hw = player.getBbWidth() / 2.0 + 0.05;
              double h  = player.getBbHeight() + 0.05;
              AABB box  = new AABB(px - hw, py - 0.05, pz - hw, px + hw, py + h, pz + hw);

              LevelRenderer.renderLineBox(poseStack, consumer, box, RED, GREEN, BLUE, ALPHA);

              float hp = player.getHealth() / player.getMaxHealth();
              AABB hpBar = new AABB(
                  box.minX, box.maxY + 0.15, box.minZ,
                  box.minX + (box.maxX - box.minX) * hp, box.maxY + 0.20, box.maxZ
              );
              LevelRenderer.renderLineBox(poseStack, consumer, hpBar, 1.0f - hp, hp, 0.0f, 1.0f);
          }

          bufferSource.endBatch(RenderType.lines());

          // Восстанавливаем тест глубины
          RenderSystem.enableDepthTest();
      }
  }
  