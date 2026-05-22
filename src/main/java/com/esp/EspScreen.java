package com.esp;

    import net.minecraft.client.gui.GuiGraphics;
    import net.minecraft.client.gui.components.AbstractSliderButton;
    import net.minecraft.client.gui.components.Button;
    import net.minecraft.client.gui.components.CycleButton;
    import net.minecraft.client.gui.components.EditBox;
    import net.minecraft.client.gui.screens.Screen;
    import net.minecraft.network.chat.Component;

    /**
     * Главное меню ESP — Web3 / Glassmorphism.
     * [Delete] открыть | перетаскивать за шапку | кнопка размера справа в шапке.
     * FTPDev | github.com/FTPLabs
     */
    public class EspScreen extends Screen {

        // Палитра
        private static final int C_BG       = 0xF2050C1E;
        private static final int C_HDR1     = 0xFF08111E;
        private static final int C_HDR2     = 0xFF120828;
        private static final int C_SECTION  = 0xFF0D1525;
        private static final int C_BORDER   = 0xFF1E293B;
        private static final int C_ACCENT_P = 0xFF7C5CFC;
        private static final int C_ACCENT_C = 0xFF22D3EE;
        private static final int C_TEXT     = 0xFFE2E8F0;
        private static final int C_TAB_BG   = 0x1A7C5CFC;

        // Размеры: 0=Компакт, 1=Нормальный, 2=Большой
        private static final int[]    WIDTHS  = {340, 420, 510};
        private static final int[]    HEIGHTS = {280, 310, 340};
        private static final String[] SCALE_LABELS = {"Компакт","Норм","Большой"};

        private static final int BTN_H = 16;
        private static final int STEP  = 20;

        private static int savedPx = Integer.MIN_VALUE;
        private static int savedPy = Integer.MIN_VALUE;
        private static int TAB = 0;

        private int PW, PH, px, py;
        private boolean dragging;
        private int dragOffX, dragOffY;

        private static final String[] TABS = {"Игроки","Руды","Боевой","Авто"};

        public EspScreen() { super(Component.literal("ESP")); }
        @Override public boolean isPauseScreen() { return false; }

        @Override
        protected void init() {
            int sc = Math.max(0, Math.min(2, EspConfig.guiScale));
            PW = WIDTHS[sc]; PH = HEIGHTS[sc];

            if (savedPx == Integer.MIN_VALUE) {
                px = (width - PW) / 2;
                py = (height - PH) / 2;
            } else {
                px = Math.max(0, Math.min(width  - PW, savedPx));
                py = Math.max(0, Math.min(height - PH, savedPy));
            }

            // 4 вкладки
            int tabW = (PW - 10) / 4;
            for (int i = 0; i < TABS.length; i++) {
                final int id = i;
                String lbl = TAB == id ? "\u00A7b\u00A7l" + TABS[i] : "\u00A78" + TABS[i];
                addRenderableWidget(Button.builder(Component.literal(lbl),
                    b -> { TAB = id; if (minecraft != null) minecraft.setScreen(new EspScreen()); }
                ).pos(px + 2 + i*(tabW+2), py + 22).size(tabW, BTN_H).build());
            }

            // Кнопка масштаба
            addRenderableWidget(Button.builder(
                Component.literal("\u00A78" + SCALE_LABELS[sc]),
                b -> { EspConfig.guiScale = (EspConfig.guiScale + 1) % 3;
                       EspKeyTickHandler.markDirty();
                       if (minecraft != null) minecraft.setScreen(new EspScreen()); }
            ).pos(px + PW - 68, py + 3).size(64, 14).build());

            int x = px + 12, y = py + 44, w = PW - 24;
            switch (TAB) {
                case 0 -> buildPlayers(x, y, w);
                case 1 -> buildOre(x, y, w);
                case 2 -> buildCombat(x, y, w);
                case 3 -> buildAuto(x, y, w);
            }

            addRenderableWidget(Button.builder(
                Component.literal("\u2716 Закрыть"), b -> onClose()
            ).pos(px + PW/2 - 40, py + PH - 22).size(80, BTN_H).build());
        }

        // ── Вкладка: Игроки ──────────────────────────────────────────────────
        private void buildPlayers(int x, int y, int w) {
            int half = (w-4)/2;
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.espEnabled)
                .create(x,y,half,BTN_H,Component.literal("Игрок ESP"),
                    (b,v)->{ EspConfig.espEnabled=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.tracer)
                .create(x+half+4,y,half,BTN_H,Component.literal("Трейсеры"),
                    (b,v)->{ EspConfig.tracer=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;
            // ФИКС: applyValue() → markDirty() вместо save()
            addRenderableWidget(new AbstractSliderButton(x,y,w,BTN_H,
                    Component.literal("Дальность: "+EspConfig.espRange+" бл."),(EspConfig.espRange-16.0)/984.0){
                @Override protected void updateMessage(){ setMessage(Component.literal("Дальность: "+(int)(16+value*984)+" бл.")); }
                @Override protected void applyValue()  { EspConfig.espRange=(int)(16+value*984); EspKeyTickHandler.markDirty(); }
            });
            y+=STEP;
            int sw=(w-4)/3;
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showNick)
                .create(x,y,sw,BTN_H,Component.literal("Ник"),(b,v)->{ EspConfig.showNick=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showHp)
                .create(x+sw+2,y,sw,BTN_H,Component.literal("HP"),(b,v)->{ EspConfig.showHp=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showArmor)
                .create(x+(sw+2)*2,y,sw,BTN_H,Component.literal("Броня"),(b,v)->{ EspConfig.showArmor=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;
            int slW=(w-6-26)/3;
            addRenderableWidget(new AbstractSliderButton(x,y,slW,BTN_H,Component.literal("R:"+pct(EspConfig.espR)),EspConfig.espR){
                @Override protected void updateMessage(){ setMessage(Component.literal("R:"+pct((float)value))); }
                @Override protected void applyValue()  { EspConfig.espR=(float)value; EspKeyTickHandler.markDirty(); }
            });
            addRenderableWidget(new AbstractSliderButton(x+slW+2,y,slW,BTN_H,Component.literal("G:"+pct(EspConfig.espG)),EspConfig.espG){
                @Override protected void updateMessage(){ setMessage(Component.literal("G:"+pct((float)value))); }
                @Override protected void applyValue()  { EspConfig.espG=(float)value; EspKeyTickHandler.markDirty(); }
            });
            addRenderableWidget(new AbstractSliderButton(x+(slW+2)*2,y,slW,BTN_H,Component.literal("B:"+pct(EspConfig.espB)),EspConfig.espB){
                @Override protected void updateMessage(){ setMessage(Component.literal("B:"+pct((float)value))); }
                @Override protected void applyValue()  { EspConfig.espB=(float)value; EspKeyTickHandler.markDirty(); }
            });
        }

        // ── Вкладка: Руды ────────────────────────────────────────────────────
        private void buildOre(int x, int y, int w) {
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.oreEsp)
                .create(x,y,(w-4)/2,BTN_H,Component.literal("Сканер Руд"),
                    (b,v)->{ EspConfig.oreEsp=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;
            addRenderableWidget(new AbstractSliderButton(x,y,w,BTN_H,
                    Component.literal("Радиус: "+EspConfig.oreRange+" бл."),(EspConfig.oreRange-8.0)/24.0){
                @Override protected void updateMessage(){ setMessage(Component.literal("Радиус: "+(int)(8+value*24)+" бл.")); }
                @Override protected void applyValue()  { EspConfig.oreRange=(int)(8+value*24); EspKeyTickHandler.markDirty(); }
            });
        }

        // ── Вкладка: Боевой ──────────────────────────────────────────────────
        private void buildCombat(int x, int y, int w) {
            int half=(w-4)/2;
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.noFall)
                .create(x,y,half,BTN_H,Component.literal("АнтиУрон [N]"),
                    (b,v)->{ EspConfig.noFall=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.killAura)
                .create(x+half+4,y,half,BTN_H,Component.literal("КиллАура [K]"),
                    (b,v)->{ EspConfig.killAura=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;
            addRenderableWidget(new AbstractSliderButton(x,y,w,BTN_H,
                    Component.literal("Радиус атаки: "+fmt(EspConfig.killAuraRange)+" бл."),
                    (EspConfig.killAuraRange-2.0)/4.0){
                @Override protected void updateMessage(){ setMessage(Component.literal("Радиус атаки: "+fmt(2f+(float)(value*4.0))+" бл.")); }
                @Override protected void applyValue()  { EspConfig.killAuraRange=2f+(float)(value*4.0); EspKeyTickHandler.markDirty(); }
            });
            y+=STEP;
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.alwaysSprint)
                .create(x,y,half,BTN_H,Component.literal("Всегда спринт [M]"),
                    (b,v)->{ EspConfig.alwaysSprint=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.nightVision)
                .create(x+half+4,y,half,BTN_H,Component.literal("Ночное зрение [V]"),
                    (b,v)->{ EspConfig.nightVision=v; EspKeyTickHandler.markDirty(); }));
        }

        // ── Вкладка: Авто ────────────────────────────────────────────────────
        private void buildAuto(int x, int y, int w) {
            int half=(w-4)/2;

            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.autoAuth)
                .create(x,y,w,BTN_H,Component.literal("Авто-аутентификация"),
                    (b,v)->{ EspConfig.autoAuth=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;

            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.autoReg)
                .create(x,y,half,BTN_H,Component.literal("Авто-рег /reg"),
                    (b,v)->{ EspConfig.autoReg=v; EspKeyTickHandler.markDirty(); }));
            addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.autoLogin)
                .create(x+half+4,y,half,BTN_H,Component.literal("Авто-вход /login"),
                    (b,v)->{ EspConfig.autoLogin=v; EspKeyTickHandler.markDirty(); }));
            y+=STEP;

            // ФИКС: setResponder → markDirty() вместо save() при каждом символе
            EditBox passBox = new EditBox(font, x, y, w, BTN_H, Component.literal("Пароль"));
            passBox.setMaxLength(64);
            passBox.setValue(EspConfig.authPassword);
            passBox.setHint(Component.literal("\u00A78Введите пароль..."));
            passBox.setResponder(val -> { EspConfig.authPassword = val; EspKeyTickHandler.markDirty(); });
            addRenderableWidget(passBox);
            y+=STEP;
        }

        // ── Перетаскивание за шапку ───────────────────────────────────────────
        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button==0 && mx>=px && mx<=px+PW && my>=py && my<=py+21) {
                dragging=true; dragOffX=(int)(mx-px); dragOffY=(int)(my-py); return true;
            }
            return super.mouseClicked(mx, my, button);
        }
        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (dragging && button==0) {
                px=(int)(mx-dragOffX); py=(int)(my-dragOffY);
                px=Math.max(0,Math.min(width-PW,px)); py=Math.max(0,Math.min(height-PH,py));
                savedPx=px; savedPy=py; return true;
            }
            return super.mouseDragged(mx, my, button, dx, dy);
        }
        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (button==0) dragging=false;
            return super.mouseReleased(mx, my, button);
        }

        // ── Рендер ───────────────────────────────────────────────────────────
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            renderBackground(g, mx, my, pt);

            g.fill(px,py,px+PW,py+PH, C_BG);
            g.fillGradient(px-1,py-1,  px+PW+1,py,       C_ACCENT_P,C_ACCENT_C);
            g.fillGradient(px-1,py+PH, px+PW+1,py+PH+1,  C_ACCENT_C,C_ACCENT_P);
            g.fill(px-1,py,px,py+PH, C_ACCENT_P);
            g.fill(px+PW,py,px+PW+1,py+PH, C_ACCENT_C);

            // Шапка
            g.fillGradient(px,py,px+PW,py+21, C_HDR1,C_HDR2);
            g.fillGradient(px,py,px+3,py+21, C_ACCENT_P,C_ACCENT_C);
            g.drawString(font,"\u00A7b\u25C6 \u00A7f\u00A7lPlayers ESP \u00A78v1.4",px+8,py+7,C_TEXT);

            // Таб-бар
            g.fill(px,py+21,px+PW,py+40, C_SECTION);
            g.fill(px,py+39,px+PW,py+40, C_BORDER);
            int tabW=(PW-10)/4;
            int tabX=px+2+TAB*(tabW+2);
            g.fill(tabX,py+22,tabX+tabW,py+39, C_TAB_BG);
            g.fillGradient(tabX,py+37,tabX+tabW,py+40, C_ACCENT_P,C_ACCENT_C);

            g.fill(px,py+PH-26,px+PW,py+PH-25, C_BORDER);
            g.fillGradient(px+2,py+40,px+3,py+PH-26, 0x2A7C5CFC,0x2A22D3EE);
            g.fillGradient(px+PW-3,py+40,px+PW-2,py+PH-26, 0x2A22D3EE,0x2A7C5CFC);

            // Превью цвета (Игроки)
            if (TAB==0){
                int col=0xFF000000|((int)(EspConfig.espR*255)<<16)|((int)(EspConfig.espG*255)<<8)|(int)(EspConfig.espB*255);
                int bpx=px+PW-36,bpy=py+44+STEP*4;
                g.fill(bpx,bpy,bpx+12,bpy+10,0xFF888888); g.fill(bpx+12,bpy,bpx+24,bpy+10,0xFF444444);
                g.fill(bpx,bpy+10,bpx+12,bpy+20,0xFF444444); g.fill(bpx+12,bpy+10,bpx+24,bpy+20,0xFF888888);
                g.fill(bpx,bpy,bpx+24,bpy+20,col);
                g.fillGradient(bpx-1,bpy-1,bpx+25,bpy,C_ACCENT_P,C_ACCENT_C);
                g.fillGradient(bpx-1,bpy+20,bpx+25,bpy+21,C_ACCENT_C,C_ACCENT_P);
                g.fill(bpx-1,bpy-1,bpx,bpy+21,C_ACCENT_P); g.fill(bpx+24,bpy-1,bpx+25,bpy+21,C_ACCENT_C);
            }

            super.render(g, mx, my, pt);
        }

        @Override
        public void onClose() {
            // При закрытии экрана — сохраняем если есть несохранённые изменения
            EspConfig.save();
            super.onClose();
        }

        private static String pct(float v){ return (int)(v*100)+"%"; }
        private static String fmt(float v){ return String.format("%.1f",v); }
    }
  