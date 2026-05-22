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

            // ФИКС CME: копируем список перед итерацией
            List<? extends Player> playerList;
            try { playerList = List.copyOf(mc.level.players()); }
            catch (Exception e) { return; }

            List<PlayerSnapshot> visible = new ArrayList<>();
            for (Player player : playerList) {
                if (player == mc.player) continue;
                if (player.distanceTo(mc.player) > EspConfig.espRange) continue;

                double px = Mth.lerp(pt, player.xo, player.getX());
                double py = Mth.lerp(pt, player.yo, player.getY());
                double pz = Mth.lerp(pt, player.zo, player.getZ());
                // ФИКС: минимальная ширина 0.4 — бокс не сужается вблизи
                double hw = Math.max(0.4, player.getBbWidth() / 2.0 + 0.05);
                double bh = Math.max(1.8, player.getBbHeight() + 0.05);

                float hp    = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);
                int   hpInt = Math.round(player.getHealth());
                int   hpMax = Math.round(player.getMaxHealth());
                int   armor = player.getArmorValue();
                int   hpCol = 0xFF000000 | ((int)((1f - hp) * 255) << 16) | ((int)(hp * 255) << 8);

                visible.add(new PlayerSnapshot(px, py, pz, hw, bh, hp, hpInt, hpMax, armor, hpCol,
                    player.getGameProfile().getName()));
            }

            if (visible.isEmpty()) return;

            PoseStack ps = new PoseStack();
            ps.translate(-camPos.x, -camPos.y, -camPos.z);

            MultiBufferSource.BufferSource bufSrc = mc.renderBuffers().bufferSource();
            VertexConsumer lines = bufSrc.getBuffer(EspRenderType.espLines());

            for (PlayerSnapshot s : visible) {
                float r = EspConfig.espR, g = EspConfig.espG, b = EspConfig.espB;
                float rd = r * 0.45f, gd = g * 0.45f, bd = b * 0.45f;

                // Glow-слой (чуть больше, полупрозрачный) — эффект свечения
                renderCornerBox(ps, lines,
                    s.px() - s.hw() - 0.04, s.py() - 0.09, s.pz() - s.hw() - 0.04,
                    s.px() + s.hw() + 0.04, s.py() + s.bh() + 0.04, s.pz() + s.hw() + 0.04,
                    r * 0.3f, g * 0.3f, b * 0.3f, rd * 0.3f, gd * 0.3f, bd * 0.3f);

                // Основной бокс
                renderCornerBox(ps, lines,
                    s.px() - s.hw(), s.py() - 0.05, s.pz() - s.hw(),
                    s.px() + s.hw(), s.py() + s.bh(), s.pz() + s.hw(),
                    r, g, b, rd, gd, bd);

                // HP-бар (слева от бокса)
                double bx  = s.px() - s.hw() - 0.12;
                double yBt = s.py() - 0.05, yTp = s.py() + s.bh();
                double yFl = yBt + (yTp - yBt) * s.hp();
                EspLineUtil.addLine(ps, lines, bx, yBt, s.pz(), bx, yTp, s.pz(), 0.2f, 0.2f, 0.2f, 0.9f);
                EspLineUtil.addLine(ps, lines, bx, yBt, s.pz(), bx, yFl, s.pz(), 1f - s.hp(), s.hp(), 0f, 1f);

                // Трейсеры
                if (EspConfig.tracer) {
                    EspLineUtil.addLine(ps, lines,
                        camPos.x, camPos.y, camPos.z,
                        s.px(), s.py() + s.bh() * 0.5, s.pz(),
                        r, g, b, 0.55f);
                }
            }
            bufSrc.endBatch(EspRenderType.espLines());

            // Pass 2: текстовые метки
            for (PlayerSnapshot s : visible) {
                ps.pushPose();
                ps.translate(s.px(), s.py() + s.bh() + 0.30, s.pz());
                ps.mulPose(event.getCamera().rotation());
                ps.scale(-0.025f, -0.025f, 0.025f);

                float lineY = 0f;
                if (EspConfig.showNick) {
                    String nm = s.name();
                    mc.font.drawInBatch(nm, -mc.font.width(nm) / 2f, lineY, 0xFFFFFFFF,
                        false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                    lineY += 10f;
                }
                if (EspConfig.showHp) {
                    String hp = s.hpInt() + "/" + s.hpMax() + " HP";
                    mc.font.drawInBatch(hp, -mc.font.width(hp) / 2f, lineY, s.hpColor(),
                        false, ps.last().pose(), bufSrc, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                    lineY += 10f;
                }
                if (EspConfig.showArmor && s.armor() > 0) {
                    String ar = s.armor() + " arm";
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
            float lx = (float)((x2-x1)*0.25f), ly = (float)((y2-y1)*0.25f), lz = (float)((z2-z1)*0.25f);
            float x1f=(float)x1,y1f=(float)y1,z1f=(float)z1,x2f=(float)x2,y2f=(float)y2,z2f=(float)z2;
            // Bottom
            seg(ps,buf,x1f,y1f,z1f, x1f+lx,y1f,z1f, rd,gd,bd); seg(ps,buf,x1f,y1f,z1f, x1f,y1f,z1f+lz, rd,gd,bd); seg(ps,buf,x1f,y1f,z1f, x1f,y1f+ly,z1f, rd,gd,bd);
            seg(ps,buf,x2f,y1f,z1f, x2f-lx,y1f,z1f, rd,gd,bd); seg(ps,buf,x2f,y1f,z1f, x2f,y1f,z1f+lz, rd,gd,bd); seg(ps,buf,x2f,y1f,z1f, x2f,y1f+ly,z1f, rd,gd,bd);
            seg(ps,buf,x1f,y1f,z2f, x1f+lx,y1f,z2f, rd,gd,bd); seg(ps,buf,x1f,y1f,z2f, x1f,y1f,z2f-lz, rd,gd,bd); seg(ps,buf,x1f,y1f,z2f, x1f,y1f+ly,z2f, rd,gd,bd);
            seg(ps,buf,x2f,y1f,z2f, x2f-lx,y1f,z2f, rd,gd,bd); seg(ps,buf,x2f,y1f,z2f, x2f,y1f,z2f-lz, rd,gd,bd); seg(ps,buf,x2f,y1f,z2f, x2f,y1f+ly,z2f, rd,gd,bd);
            // Top
            seg(ps,buf,x1f,y2f,z1f, x1f+lx,y2f,z1f, r,g,b); seg(ps,buf,x1f,y2f,z1f, x1f,y2f,z1f+lz, r,g,b); seg(ps,buf,x1f,y2f,z1f, x1f,y2f-ly,z1f, r,g,b);
            seg(ps,buf,x2f,y2f,z1f, x2f-lx,y2f,z1f, r,g,b); seg(ps,buf,x2f,y2f,z1f, x2f,y2f,z1f+lz, r,g,b); seg(ps,buf,x2f,y2f,z1f, x2f,y2f-ly,z1f, r,g,b);
            seg(ps,buf,x1f,y2f,z2f, x1f+lx,y2f,z2f, r,g,b); seg(ps,buf,x1f,y2f,z2f, x1f,y2f,z2f-lz, r,g,b); seg(ps,buf,x1f,y2f,z2f, x1f,y2f-ly,z2f, r,g,b);
            seg(ps,buf,x2f,y2f,z2f, x2f-lx,y2f,z2f, r,g,b); seg(ps,buf,x2f,y2f,z2f, x2f,y2f,z2f-lz, r,g,b); seg(ps,buf,x2f,y2f,z2f, x2f,y2f-ly,z2f, r,g,b);
        }

        private static void seg(PoseStack ps,VertexConsumer buf,float x1,float y1,float z1,float x2,float y2,float z2,float r,float g,float b) {
            EspLineUtil.addLine(ps,buf,x1,y1,z1,x2,y2,z2,r,g,b,1f);
        }
    }
  