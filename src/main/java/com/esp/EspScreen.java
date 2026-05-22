package com.esp;

    import net.minecraft.client.gui.GuiGraphics;
    import net.minecraft.client.gui.components.AbstractSliderButton;
    import net.minecraft.client.gui.components.Button;
    import net.minecraft.client.gui.components.CycleButton;
    import net.minecraft.client.gui.components.EditBox;
    import net.minecraft.client.gui.screens.Screen;
    import net.minecraft.network.chat.Component;

    /**
     * Players ESP — главное меню. 5 вкладок.
     * [Delete] открыть | перетаскивать за шапку | кнопка размера справа.
     * FTPDev | github.com/FTPLabs  v1.5
     */
    public class EspScreen extends Screen {

        private static final int C_BG       = 0xF2050C1E;
        private static final int C_HDR1     = 0xFF08111E;
        private static final int C_HDR2     = 0xFF120828;
        private static final int C_SECTION  = 0xFF0D1525;
        private static final int C_BORDER   = 0xFF1E293B;
        private static final int C_ACCENT_P = 0xFF7C5CFC;
        private static final int C_ACCENT_C = 0xFF22D3EE;
        private static final int C_TEXT     = 0xFFE2E8F0;
        private static final int C_TAB_BG   = 0x1A7C5CFC;

        private static final int[]    WIDTHS  = {380, 460, 540};
        private static final int[]    HEIGHTS = {330, 370, 410};
        private static final String[] SCALE_LABELS = {"Компакт","Норм","Большой"};

        private static final int BTN_H = 16;
        private static final int STEP  = 20;

        private static int savedPx = Integer.MIN_VALUE;
        private static int savedPy = Integer.MIN_VALUE;
        private static int TAB = 0;

        private static final String[] TABS = {"\u00A7bИгроки","\u00A79Руды","\u00A7cБоевой","\u00A7eHUD","\u00A7aАвто"};

        private int PW, PH, px, py;
        private boolean dragging;
        private int dragOffX, dragOffY;

        public EspScreen() { super(Component.literal("ESP")); }
        @Override public boolean isPauseScreen() { return false; }

        @Override
        protected void init() {
            int sc = Math.max(0, Math.min(2, EspConfig.guiScale));
            PW = WIDTHS[sc]; PH = HEIGHTS[sc];
            if (savedPx == Integer.MIN_VALUE) { px = (width - PW) / 2; py = (height - PH) / 2; }
            else { px = Math.max(0, Math.min(width - PW, savedPx)); py = Math.max(0, Math.min(height - PH, savedPy)); }

            int tabW = (PW - 12) / TABS.length;
            for (int i = 0; i < TABS.length; i++) {
                final int id = i;
                String active  = TABS[i];
                String passive = "\u00A78" + TABS[i].replaceAll("\\u00A7.", "");
                String lbl = (TAB == id) ? "\u00A7l" + active : passive;
                addRenderableWidget(Button.builder(Component.literal(lbl),
                    b -> { TAB = id; if (minecraft != null) minecraft.setScreen(new EspScreen()); }
                ).pos(px + 2 + i * (tabW + 2), py + 22).size(tabW, BTN_H).build());
            }

            addRenderableWidget(Button.builder(
                Component.literal("\u00A78" + SCALE_LABELS[sc]),
                b -> { EspConfig.guiScale = (EspConfig.guiScale + 1) % 3;
                       EspKeyTickHandler.markDirty();
                       if (minecraft != null) minecraft.setScreen(new EspScreen()); }
            ).pos(px + PW - 64, py + 3).size(60, 14).build());

            int x = px + 12, y = py + 44, w = PW - 24;
            switch (TAB) {
                case 0 -> buildPlayers(x, y, w);
                case 1 -> buildOre(x, y, w);
                case 2 -> buildCombat(x, y, w);
                case 3 -> buildHud(x, y, w);
                case 4 -> buildAuto(x, y, w);
            }

            addRenderableWidget(Button.builder(
                Component.literal("\u2716 Закрыть"), b -> onClose()
            ).pos(px + PW / 2 - 40, py + PH - 22).size(80, BTN_H).build());
        }

        // ── Вкладка: Игроки ──────────────────────────────────────────────────
        private void buildPlayers(int x, int y, int w) {
            int half = (w - 4) / 2;
            tog(x, y, half, "Игрок ESP",  EspConfig.espEnabled,  v -> { EspConfig.espEnabled = v;  EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Трейсеры", EspConfig.tracer, v -> { EspConfig.tracer = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            sld(x, y, w, "Дальность: ", EspConfig.espRange, (EspConfig.espRange-16.0)/984.0,
                v -> { EspConfig.espRange = (int)(16 + v * 984); EspKeyTickHandler.markDirty(); },
                v -> "Дальность: " + (int)(16 + v * 984) + " бл."); y += STEP;
            int sw = (w - 4) / 3;
            tog(x, y, sw, "Ник",   EspConfig.showNick,  v -> { EspConfig.showNick  = v; EspKeyTickHandler.markDirty(); });
            tog(x+sw+2, y, sw, "HP",    EspConfig.showHp,    v -> { EspConfig.showHp    = v; EspKeyTickHandler.markDirty(); });
            tog(x+(sw+2)*2, y, sw, "Броня", EspConfig.showArmor, v -> { EspConfig.showArmor = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            int sl = (w - 8) / 3;
            sld(x, y, sl, "R:", EspConfig.espR, EspConfig.espR,
                v -> { EspConfig.espR = (float)v; EspKeyTickHandler.markDirty(); }, v -> "R:" + pct((float)v));
            sld(x+sl+2, y, sl, "G:", EspConfig.espG, EspConfig.espG,
                v -> { EspConfig.espG = (float)v; EspKeyTickHandler.markDirty(); }, v -> "G:" + pct((float)v));
            sld(x+(sl+2)*2, y, sl, "B:", EspConfig.espB, EspConfig.espB,
                v -> { EspConfig.espB = (float)v; EspKeyTickHandler.markDirty(); }, v -> "B:" + pct((float)v));
        }

        // ── Вкладка: Руды ────────────────────────────────────────────────────
        private void buildOre(int x, int y, int w) {
            int half = (w - 4) / 2;
            tog(x, y, half, "Сканер Руд", EspConfig.oreEsp, v -> { EspConfig.oreEsp = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Майнинг-бот [B]", EspConfig.miningBot, v -> { EspConfig.miningBot = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            sld(x, y, w, "Радиус: ", EspConfig.oreRange, (EspConfig.oreRange-8.0)/24.0,
                v -> { EspConfig.oreRange = (int)(8 + v * 24); EspKeyTickHandler.markDirty(); },
                v -> "Радиус: " + (int)(8 + v * 24) + " бл.");
        }

        // ── Вкладка: Боевой ──────────────────────────────────────────────────
        private void buildCombat(int x, int y, int w) {
            int half = (w - 4) / 2;
            tog(x, y, half, "АнтиУрон [N]", EspConfig.noFall,   v -> { EspConfig.noFall   = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "КиллАура [K]", EspConfig.killAura, v -> { EspConfig.killAura = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            sld(x, y, w, "Радиус атаки: ", EspConfig.killAuraRange, (EspConfig.killAuraRange-2.0)/4.0,
                v -> { EspConfig.killAuraRange = 2f+(float)(v*4); EspKeyTickHandler.markDirty(); },
                v -> "Радиус атаки: " + fmt(2f+(float)(v*4)) + " бл."); y += STEP;
            tog(x, y, half, "Тайминг удара", EspConfig.hitDelay,      v -> { EspConfig.hitDelay      = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Анти-отброс",   EspConfig.antiKnockback, v -> { EspConfig.antiKnockback = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            sld(x, y, w, "Сила антиотброса: ", EspConfig.antiKbStrength, EspConfig.antiKbStrength,
                v -> { EspConfig.antiKbStrength = (float)v; EspKeyTickHandler.markDirty(); },
                v -> "Сила антиотброса: " + pct((float)v)); y += STEP;
            tog(x, y, half, "Всегда спринт [M]", EspConfig.alwaysSprint, v -> { EspConfig.alwaysSprint = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Ночное зрение [V]", EspConfig.nightVision, v -> { EspConfig.nightVision = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            tog(x, y, half, "Без замедления", EspConfig.noSlowdown, v -> { EspConfig.noSlowdown = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Авто-броня",     EspConfig.autoArmor,  v -> { EspConfig.autoArmor  = v; EspKeyTickHandler.markDirty(); });
        }

        // ── Вкладка: HUD ─────────────────────────────────────────────────────
        private void buildHud(int x, int y, int w) {
            int half = (w - 4) / 2;
            tog(x, y, half, "Броня HUD",         EspConfig.armorHud,    v -> { EspConfig.armorHud    = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Зелья HUD",  EspConfig.potionHud,   v -> { EspConfig.potionHud   = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            tog(x, y, half, "Дистанция цели",    EspConfig.reachDisplay, v -> { EspConfig.reachDisplay = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Траект. стрелы", EspConfig.arrowPredict, v -> { EspConfig.arrowPredict = v; EspKeyTickHandler.markDirty(); });
        }

        // ── Вкладка: Авто ────────────────────────────────────────────────────
        private void buildAuto(int x, int y, int w) {
            int half = (w - 4) / 2;
            tog(x, y, w, "Авто-аутентификация (мастер)", EspConfig.autoAuth, v -> { EspConfig.autoAuth = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            tog(x, y, half, "Авто-рег /reg",   EspConfig.autoReg,   v -> { EspConfig.autoReg   = v; EspKeyTickHandler.markDirty(); });
            tog(x+half+4, y, half, "Авто-вход /login", EspConfig.autoLogin, v -> { EspConfig.autoLogin  = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            EditBox passBox = new EditBox(font, x, y, w, BTN_H, Component.literal("Пароль"));
            passBox.setMaxLength(64); passBox.setValue(EspConfig.authPassword);
            passBox.setHint(Component.literal("\u00A78Введите пароль..."));
            passBox.setResponder(val -> { EspConfig.authPassword = val; EspKeyTickHandler.markDirty(); });
            addRenderableWidget(passBox); y += STEP;
            tog(x, y, w, "Умная авт. (по чату сервера)", EspConfig.smartAuth, v -> { EspConfig.smartAuth = v; EspKeyTickHandler.markDirty(); }); y += STEP;
            tog(x, y, half, "Анти-AFK", EspConfig.antiAfk, v -> { EspConfig.antiAfk = v; EspKeyTickHandler.markDirty(); });
            sld(x+half+4, y, half, "AFK: ", EspConfig.antiAfkDelay, (EspConfig.antiAfkDelay-100.0)/1100.0,
                v -> { EspConfig.antiAfkDelay = (int)(100+v*1100); EspKeyTickHandler.markDirty(); },
                v -> "AFK: " + (int)(100+v*1100)/20 + "с"); y += STEP;
            tog(x, y, half, "Авто-реконнект", EspConfig.autoReconnect, v -> { EspConfig.autoReconnect = v; EspKeyTickHandler.markDirty(); });
            sld(x+half+4, y, half, "Задержка: ", EspConfig.reconnectDelay, (EspConfig.reconnectDelay-3.0)/57.0,
                v -> { EspConfig.reconnectDelay = (int)(3+v*57); EspKeyTickHandler.markDirty(); },
                v -> "Задержка: " + (int)(3+v*57) + "с");
        }

        // ── Хелперы создания виджетов ─────────────────────────────────────────
        @FunctionalInterface interface BoolConsumer { void accept(boolean v); }
        @FunctionalInterface interface DblConsumer  { void accept(double v);  }
        @FunctionalInterface interface DblFn        { String apply(double v); }

        private void tog(int x, int y, int w, String label, boolean cur, BoolConsumer onChange) {
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(cur)
                .create(x, y, w, BTN_H, Component.literal(label), (b, v) -> onChange.accept(v)));
        }
        private void sld(int x, int y, int w, String label, Object init, double val0, DblConsumer onChange, DblFn msg) {
            String initMsg = msg.apply(val0);
            addRenderableWidget(new AbstractSliderButton(x, y, w, BTN_H, Component.literal(initMsg), val0) {
                @Override protected void updateMessage() { setMessage(Component.literal(msg.apply(value))); }
                @Override protected void applyValue()   { onChange.accept(value); }
            });
        }

        // ── Drag ─────────────────────────────────────────────────────────────
        @Override public boolean mouseClicked(double mx, double my, int btn) {
            if (btn==0 && mx>=px && mx<=px+PW && my>=py && my<=py+21) { dragging=true; dragOffX=(int)(mx-px); dragOffY=(int)(my-py); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (dragging && btn==0) { px=(int)(mx-dragOffX); py=(int)(my-dragOffY); px=Math.max(0,Math.min(width-PW,px)); py=Math.max(0,Math.min(height-PH,py)); savedPx=px; savedPy=py; return true; }
            return super.mouseDragged(mx, my, btn, dx, dy);
        }
        @Override public boolean mouseReleased(double mx, double my, int btn) { if (btn==0) dragging=false; return super.mouseReleased(mx, my, btn); }
        @Override public void onClose() { EspConfig.save(); super.onClose(); }

        // ── Рендер ───────────────────────────────────────────────────────────
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            renderBackground(g, mx, my, pt);
            g.fill(px, py, px+PW, py+PH, C_BG);
            g.fillGradient(px-1, py-1, px+PW+1, py,      C_ACCENT_P, C_ACCENT_C);
            g.fillGradient(px-1, py+PH, px+PW+1, py+PH+1,C_ACCENT_C, C_ACCENT_P);
            g.fill(px-1, py, px, py+PH, C_ACCENT_P); g.fill(px+PW, py, px+PW+1, py+PH, C_ACCENT_C);
            g.fillGradient(px, py, px+PW, py+21, C_HDR1, C_HDR2);
            g.fillGradient(px, py, px+3, py+21, C_ACCENT_P, C_ACCENT_C);
            g.drawString(font, "\u00A7b\u25C6 \u00A7f\u00A7lPlayers ESP \u00A78v1.5", px+8, py+7, C_TEXT);
            g.fill(px, py+21, px+PW, py+40, C_SECTION);
            g.fill(px, py+39, px+PW, py+40, C_BORDER);
            int tabW = (PW - 12) / TABS.length;
            int tabX = px + 2 + TAB * (tabW + 2);
            g.fill(tabX, py+22, tabX+tabW, py+39, C_TAB_BG);
            g.fillGradient(tabX, py+37, tabX+tabW, py+40, C_ACCENT_P, C_ACCENT_C);
            g.fill(px, py+PH-26, px+PW, py+PH-25, C_BORDER);
            g.fillGradient(px+2, py+40, px+3, py+PH-26, 0x2A7C5CFC, 0x2A22D3EE);
            g.fillGradient(px+PW-3, py+40, px+PW-2, py+PH-26, 0x2A22D3EE, 0x2A7C5CFC);
            // Превью цвета ESP (вкладка Игроки)
            if (TAB == 0) {
                int col = 0xFF000000|((int)(EspConfig.espR*255)<<16)|((int)(EspConfig.espG*255)<<8)|(int)(EspConfig.espB*255);
                int bpx = px+PW-36, bpy = py+44+STEP*4;
                g.fill(bpx,bpy,bpx+12,bpy+10,0xFF888888); g.fill(bpx+12,bpy,bpx+24,bpy+10,0xFF444444);
                g.fill(bpx,bpy+10,bpx+12,bpy+20,0xFF444444); g.fill(bpx+12,bpy+10,bpx+24,bpy+20,0xFF888888);
                g.fill(bpx,bpy,bpx+24,bpy+20,col);
                g.fillGradient(bpx-1,bpy-1,bpx+25,bpy,C_ACCENT_P,C_ACCENT_C);
                g.fillGradient(bpx-1,bpy+20,bpx+25,bpy+21,C_ACCENT_C,C_ACCENT_P);
                g.fill(bpx-1,bpy-1,bpx,bpy+21,C_ACCENT_P); g.fill(bpx+24,bpy-1,bpx+25,bpy+21,C_ACCENT_C);
            }
            super.render(g, mx, my, pt);
        }

        private static String pct(float v) { return (int)(v*100)+"%"; }
        private static String fmt(float v)  { return String.format("%.1f",v); }
    }
  