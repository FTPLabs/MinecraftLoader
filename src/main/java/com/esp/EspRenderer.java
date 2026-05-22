package com.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
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

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // Forge 1.21.1: create PoseStack, apply camera matrix, translate to world space
        @SuppressWarnings("deprecation")
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(event.getPoseStack());
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        List<? extends Player> players = mc.level.players();
        for (Player player : players) {
            if (player == mc.player) continue;
            if (player.distanceTo(mc.player) > RANGE) continue;

            AABB box = player.getBoundingBox().inflate(0.05);
            LevelRenderer.renderLineBox(poseStack, consumer, box, RED, GREEN, BLUE, ALPHA);

            float hp = player.getHealth() / player.getMaxHealth();
            AABB hpBar = new AABB(
                box.minX, box.maxY + 0.2, box.minZ,
                box.minX + (box.maxX - box.minX) * hp, box.maxY + 0.25, box.maxZ
            );
            LevelRenderer.renderLineBox(poseStack, consumer, hpBar, 1.0f - hp, hp, 0.0f, 1.0f);
        }

        bufferSource.endBatch(RenderType.lines());
    }
}
