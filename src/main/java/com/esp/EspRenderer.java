package com.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class EspRenderer {

    // Default: red boxes
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

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        List<? extends Player> players = mc.level.players();
        for (Player player : players) {
            if (player == mc.player) continue;
            if (player.distanceTo(mc.player) > RANGE) continue;

            float r = RED, g = GREEN, b = BLUE;

            // Health bar above bounding box
            AABB box = player.getBoundingBox().inflate(0.05);
            LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, ALPHA);

            float hp = player.getHealth() / player.getMaxHealth();
            AABB hpBar = new AABB(
                box.minX, box.maxY + 0.2, box.minZ,
                box.minX + (box.maxX - box.minX) * hp, box.maxY + 0.25, box.maxZ
            );
            LevelRenderer.renderLineBox(poseStack, consumer, hpBar,
                1.0f - hp, hp, 0.0f, 1.0f);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}
