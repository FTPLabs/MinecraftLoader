package com.esp;

    import com.mojang.blaze3d.vertex.PoseStack;
    import com.mojang.blaze3d.vertex.VertexConsumer;
    import net.minecraft.client.Minecraft;
    import net.minecraft.world.item.BowItem;
    import net.minecraft.world.level.ClipContext;
    import net.minecraft.world.phys.HitResult;
    import net.minecraft.world.phys.Vec3;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.client.event.RenderLevelStageEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;

    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class ArrowPredictor {

        private static final int    STEPS   = 90;
        private static final double GRAVITY = 0.05;
        private static final double DRAG    = 0.99;

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
            if (!EspConfig.arrowPredict) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            boolean holdingBow = mc.player.getMainHandItem().getItem() instanceof BowItem
                              || mc.player.getOffhandItem().getItem() instanceof BowItem;
            if (!holdingBow || !mc.player.isUsingItem()) return;

            int   use   = mc.player.getTicksUsingItem();
            float power = BowItem.getPowerForTime(use);
            if (power < 0.1f) return;

            Vec3 cam  = event.getCamera().getPosition();
            Vec3 eye  = mc.player.getEyePosition(event.getPartialTick());
            Vec3 look = mc.player.getLookAngle();

            double speed = power * 3.0;
            double vx = look.x * speed, vy = look.y * speed + 0.1, vz = look.z * speed;
            double x = eye.x, y = eye.y, z = eye.z;

            PoseStack ps = new PoseStack();
            ps.translate(-cam.x, -cam.y, -cam.z);
            var bufSrc = mc.renderBuffers().bufferSource();
            VertexConsumer lines = bufSrc.getBuffer(EspRenderType.espLines());

            double px = x, py = y, pz = z;
            for (int i = 0; i < STEPS; i++) {
                double nx = x + vx, ny = y + vy, nz = z + vz;
                float  alpha = Math.max(0.05f, 1f - (float) i / STEPS);
                float  gr    = Math.max(0.3f, 0.9f - (float) i / STEPS * 0.6f);

                HitResult hit = mc.level.clip(new ClipContext(
                    new Vec3(px, py, pz), new Vec3(nx, ny, nz),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

                EspLineUtil.addLine(ps, lines, px, py, pz, nx, ny, nz, 1f, gr, 0f, alpha);

                if (hit.getType() != HitResult.Type.MISS) {
                    double hx = hit.getLocation().x, hhy = hit.getLocation().y, hz = hit.getLocation().z;
                    float  d  = 0.12f;
                    EspLineUtil.addLine(ps, lines, hx-d, hhy, hz,  hx+d, hhy, hz,  1f, 0.3f, 0f, 1f);
                    EspLineUtil.addLine(ps, lines, hx,  hhy-d, hz,  hx,  hhy+d, hz,  1f, 0.3f, 0f, 1f);
                    EspLineUtil.addLine(ps, lines, hx,  hhy,  hz-d, hx,  hhy,  hz+d, 1f, 0.3f, 0f, 1f);
                    break;
                }
                vx *= DRAG; vy = vy * DRAG - GRAVITY; vz *= DRAG;
                px = nx; py = ny; pz = nz;
                x = nx; y = ny; z = nz;
            }
            bufSrc.endBatch(EspRenderType.espLines());
        }
    }
  