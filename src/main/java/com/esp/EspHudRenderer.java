package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraft.client.gui.GuiGraphics;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.entity.EquipmentSlot;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.item.ItemStack;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.client.event.RenderGuiEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;

    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.List;

    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class EspHudRenderer {

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.screen != null) return;

            GuiGraphics g = event.getGuiGraphics();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();

            int y = 4;
            if (EspConfig.armorHud)   { y = renderArmorHud(g, mc, 4, y) + 6; }
            if (EspConfig.potionHud)  { renderPotionHud(g, mc, sw - 4, 4); }
            if (EspConfig.reachDisplay){ renderReachDisplay(g, mc, sw, sh); }
        }

        // ── Armor HUD ─────────────────────────────────────────────────────────
        private static int renderArmorHud(GuiGraphics g, Minecraft mc, int x, int y) {
            EquipmentSlot[] slots  = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
            String[]        labels = { "Шлем ", "Нагр  ", "Порт  ", "Ботин " };

            // Measure max text width for background
            int bgW = 100;
            int rowH = 10;
            int totalH = slots.length * (rowH + 4) + 4;
            g.fill(x - 2, y - 2, x + bgW + 2, y + totalH, 0xAA050C1E);
            g.fillGradient(x - 2, y - 2, x + bgW + 2, y - 1, 0xFF7C5CFC, 0xFF22D3EE);

            for (int i = 0; i < slots.length; i++) {
                ItemStack stack = mc.player.getItemBySlot(slots[i]);
                int ry = y + 2 + i * (rowH + 4);
                if (stack.isEmpty()) {
                    g.drawString(mc.font, labels[i] + "—", x + 2, ry, 0xFF555555);
                    continue;
                }
                int maxDur = stack.getMaxDamage();
                int curDur = maxDur > 0 ? maxDur - stack.getDamageValue() : -1;
                float pct  = maxDur > 0 ? (float) curDur / maxDur : 1f;
                int col    = durColor(pct);
                String text = labels[i] + (maxDur > 0 ? curDur + "/" + maxDur : "∞");
                g.drawString(mc.font, text, x + 2, ry, col);
                // Durability bar
                if (maxDur > 0) {
                    int bx = x + 2, by = ry + rowH;
                    g.fill(bx, by, bx + bgW - 4, by + 2, 0xFF222222);
                    g.fill(bx, by, bx + (int)(pct * (bgW - 4)), by + 2, col);
                }
            }
            return y + totalH;
        }

        // ── Potion HUD ────────────────────────────────────────────────────────
        private static void renderPotionHud(GuiGraphics g, Minecraft mc, int rightX, int y) {
            Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
            if (effects.isEmpty()) return;

            List<MobEffectInstance> sorted = new ArrayList<>(effects);
            sorted.sort((a, b) -> Boolean.compare(!a.getEffect().isBeneficial(), !b.getEffect().isBeneficial()));

            int bgW = 126;
            int bgH = sorted.size() * 13 + 6;
            g.fill(rightX - bgW - 2, y - 2, rightX + 2, y + bgH, 0xAA050C1E);
            g.fillGradient(rightX - bgW - 2, y - 2, rightX + 2, y - 1, 0xFF22D3EE, 0xFF7C5CFC);

            for (MobEffectInstance eff : sorted) {
                String name = eff.getEffect().getDisplayName().getString();
                if (name.length() > 12) name = name.substring(0, 11) + ".";
                int amp = eff.getAmplifier() + 1;
                int dur = eff.getDuration();
                String time = dur > 72000 ? "∞" : String.format("%d:%02d", dur / 1200, (dur % 1200) / 20);
                String text = (amp > 1 ? amp + "x " : "") + name + " " + time;
                int col = eff.getEffect().isBeneficial() ? 0xFF4ADE80 : 0xFFF87171;
                g.drawString(mc.font, text, rightX - bgW + 2, y + 2, col);
                y += 13;
            }
        }

        // ── Reach Display ─────────────────────────────────────────────────────
        private static void renderReachDisplay(GuiGraphics g, Minecraft mc, int sw, int sh) {
            if (mc.level == null || mc.player == null) return;
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

            int col = minDist <= 3.5 ? 0xFF4ADE80 : minDist <= 6 ? 0xFFFACC15 : 0xFFF87171;
            String name = nearest.getGameProfile().getName();
            String text = name + " §r— " + String.format("%.1f", minDist) + " бл.";
            g.drawCenteredString(mc.font, text, sw / 2, sh / 2 + 22, col);
        }

        private static int durColor(float pct) {
            int r = (int)((1f - pct) * 255);
            int gr = (int)(pct * 255);
            return 0xFF000000 | (r << 16) | (gr << 8);
        }
    }
  