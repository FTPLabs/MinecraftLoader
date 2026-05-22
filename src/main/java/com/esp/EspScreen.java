package com.esp;

  import net.minecraft.client.gui.GuiGraphics;
  import net.minecraft.client.gui.components.AbstractSliderButton;
  import net.minecraft.client.gui.components.Button;
  import net.minecraft.client.gui.components.CycleButton;
  import net.minecraft.client.gui.screens.Screen;
  import net.minecraft.network.chat.Component;

  /**
   * Главное меню ESP — Web3 / Glassmorphism.
   * [End] открыть | перетаскивать за шапку | кнопка размера в шапке справа.
   * FTPDev | github.com/FTPLabs
   */
  public class EspScreen extends Screen {

      // ── Палитра ─────────────────────────────────────────────────────────────
      private static final int C_BG       = 0xF2050C1E;
      private static final int C_HDR1     = 0xFF08111E;
      private static final int C_HDR2     = 0xFF120828;
      private static final int C_SECTION  = 0xFF0D1525;
      private static final int C_BORDER   = 0xFF1E293B;
      private static final int C_ACCENT_P = 0xFF7C5CFC;
      private static final int C_ACCENT_C = 0xFF22D3EE;
      private static final int C_TEXT     = 0xFFE2E8F0;
      private static final int C_TAB_BG   = 0x1A7C5CFC;

      // ── Размеры по шкале (0=Компакт, 1=Норм, 2=Большой) ────────────────────
      private static final int[]    WIDTHS  = {340, 420, 510};
      private static final int[]    HEIGHTS = {240, 264, 300};
      private static final String[] SCALE_LABELS = {"Компакт","Норм","Большой"};

      // Шаг по вертикали и высота кнопок — уменьшены для компактности
      private static final int BTN_H = 16; // высота кнопок
      private static final int STEP  = 20; // шаг между строками

      // ── Состояние ────────────────────────────────────────────────────────────
      private static int savedPx = Integer.MIN_VALUE;
      private static int savedPy = Integer.MIN_VALUE;
      private static int TAB = 0;

      private int PW, PH, px, py;
      private boolean dragging;
      private int dragOffX, dragOffY;

      public EspScreen() { super(Component.literal("ESP")); }
      @Override public boolean isPauseScreen() { return false; }

      // ── Init ─────────────────────────────────────────────────────────────────
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

          // Вкладки (высота 16)
          int tabW = (PW - 8) / 3;
          addRenderableWidget(mkTab("Игроки",  0, px + 2,             py + 22, tabW));
          addRenderableWidget(mkTab("Руды",     1, px + 3 + tabW,     py + 22, tabW));
          addRenderableWidget(mkTab("Боевой",   2, px + 4 + tabW * 2, py + 22, tabW - 2));

          // Кнопка масштаба (правый край шапки, компактная)
          String scLbl = "\u00A78" + SCALE_LABELS[sc];
          addRenderableWidget(Button.builder(Component.literal(scLbl),
              b -> { EspConfig.guiScale = (EspConfig.guiScale + 1) % 3; EspConfig.save();
                     if (minecraft != null) minecraft.setScreen(new EspScreen()); }
          ).pos(px + PW - 72, py + 3).size(68, 14).build());

          // Контент
          int x = px + 12, y = py + 44, w = PW - 24;
          switch (TAB) {
              case 0 -> buildPlayers(x, y, w);
              case 1 -> buildOre(x, y, w);
              case 2 -> buildCombat(x, y, w);
          }

          // Закрыть (снизу по центру)
          addRenderableWidget(Button.builder(
              Component.literal("\u2716 Закрыть"),
              b -> onClose()
          ).pos(px + PW / 2 - 44, py + PH - 22).size(88, BTN_H).build());
      }

      private Button mkTab(String label, int id, int x, int y, int w) {
          String txt = TAB == id ? "\u00A7b\u00A7l" + label : "\u00A78" + label;
          return Button.builder(Component.literal(txt), b -> {
              TAB = id;
              if (minecraft != null) minecraft.setScreen(new EspScreen());
          }).pos(x, y).size(w, BTN_H).build();
      }

      // ── Вкладка: Игроки ───────────────────────────────────────────────────────
      private void buildPlayers(int x, int y, int w) {
          int half = (w - 4) / 2;

          // [Игрок ESP вкл/выкл]
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.espEnabled)
              .create(x, y, half, BTN_H, Component.literal("Игрок ESP"),
                  (b,v)->{ EspConfig.espEnabled=v; EspConfig.save(); }));
          y += STEP;

          // Слайдер дальности
          addRenderableWidget(new AbstractSliderButton(x, y, w, BTN_H,
                  Component.literal("Дальность: " + EspConfig.espRange + " бл."),
                  (EspConfig.espRange - 16.0) / 984.0) {
              @Override protected void updateMessage(){ setMessage(Component.literal("Дальность: "+(int)(16+value*984)+" бл.")); }
              @Override protected void applyValue()  { EspConfig.espRange=(int)(16+value*984); EspConfig.save(); }
          });
          y += STEP;

          // [Ник] [HP] [Броня] — три в ряд
          int sw = (w - 4) / 3;
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showNick)
              .create(x,           y, sw, BTN_H, Component.literal("Ник"),
                  (b,v)->{ EspConfig.showNick=v;  EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showHp)
              .create(x+sw+2,      y, sw, BTN_H, Component.literal("HP"),
                  (b,v)->{ EspConfig.showHp=v;    EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showArmor)
              .create(x+(sw+2)*2,  y, sw, BTN_H, Component.literal("Броня"),
                  (b,v)->{ EspConfig.showArmor=v; EspConfig.save(); }));
          y += STEP;

          // Цвет R/G/B — три слайдера + превью в render
          int slW = (w - 6 - 26) / 3;
          addRenderableWidget(new AbstractSliderButton(x, y, slW, BTN_H,
                  Component.literal("R:"+pct(EspConfig.espR)), EspConfig.espR){
              @Override protected void updateMessage(){ setMessage(Component.literal("R:"+pct((float)value))); }
              @Override protected void applyValue()  { EspConfig.espR=(float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x+slW+2, y, slW, BTN_H,
                  Component.literal("G:"+pct(EspConfig.espG)), EspConfig.espG){
              @Override protected void updateMessage(){ setMessage(Component.literal("G:"+pct((float)value))); }
              @Override protected void applyValue()  { EspConfig.espG=(float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x+(slW+2)*2, y, slW, BTN_H,
                  Component.literal("B:"+pct(EspConfig.espB)), EspConfig.espB){
              @Override protected void updateMessage(){ setMessage(Component.literal("B:"+pct((float)value))); }
              @Override protected void applyValue()  { EspConfig.espB=(float)value; EspConfig.save(); }
          });
      }

      // ── Вкладка: Руды ─────────────────────────────────────────────────────────
      private void buildOre(int x, int y, int w) {
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.oreEsp)
              .create(x, y, (w-4)/2, BTN_H, Component.literal("Сканер Руд"),
                  (b,v)->{ EspConfig.oreEsp=v; EspConfig.save(); }));
          y += STEP;

          addRenderableWidget(new AbstractSliderButton(x, y, w, BTN_H,
                  Component.literal("Радиус: "+EspConfig.oreRange+" бл."),
                  (EspConfig.oreRange-8.0)/24.0){
              @Override protected void updateMessage(){ setMessage(Component.literal("Радиус: "+(int)(8+value*24)+" бл.")); }
              @Override protected void applyValue()  { EspConfig.oreRange=(int)(8+value*24); EspConfig.save(); }
          });
      }

      // ── Вкладка: Боевой ───────────────────────────────────────────────────────
      private void buildCombat(int x, int y, int w) {
          int half = (w - 4) / 2;

          // [АнтиУрон] [КиллАура] — два в ряд, экономия строки
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.noFall)
              .create(x,        y, half, BTN_H, Component.literal("АнтиУрон [N]"),
                  (b,v)->{ EspConfig.noFall=v; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.killAura)
              .create(x+half+4, y, half, BTN_H, Component.literal("КиллАура [K]"),
                  (b,v)->{ EspConfig.killAura=v; EspConfig.save(); }));
          y += STEP;

          // Слайдер радиуса атаки
          addRenderableWidget(new AbstractSliderButton(x, y, w, BTN_H,
                  Component.literal("Радиус атаки: "+fmt(EspConfig.killAuraRange)+" бл."),
                  (EspConfig.killAuraRange-2.0)/4.0){
              @Override protected void updateMessage(){
                  setMessage(Component.literal("Радиус атаки: "+fmt(2f+(float)(value*4.0))+" бл."));
              }
              @Override protected void applyValue(){
                  EspConfig.killAuraRange=2f+(float)(value*4.0); EspConfig.save();
              }
          });
          // Статусный блок рисуется в render(), координаты фиксированные от y+STEP
      }

      // ── Перетаскивание за шапку ───────────────────────────────────────────────
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
              px=Math.max(0,Math.min(width -PW,px));
              py=Math.max(0,Math.min(height-PH,py));
              savedPx=px; savedPy=py; return true;
          }
          return super.mouseDragged(mx, my, button, dx, dy);
      }
      @Override
      public boolean mouseReleased(double mx, double my, int button) {
          if (button==0) dragging=false;
          return super.mouseReleased(mx, my, button);
      }

      // ── Рендер ───────────────────────────────────────────────────────────────
      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
          renderBackground(g, mx, my, pt);

          // Основная панель
          g.fill(px, py, px+PW, py+PH, C_BG);

          // Рамка-градиент фиолет→голубой
          g.fillGradient(px-1,py-1,  px+PW+1,py,       C_ACCENT_P,C_ACCENT_C);
          g.fillGradient(px-1,py+PH, px+PW+1,py+PH+1,  C_ACCENT_C,C_ACCENT_P);
          g.fill(px-1,py, px,    py+PH, C_ACCENT_P);
          g.fill(px+PW,py,px+PW+1,py+PH, C_ACCENT_C);

          // Шапка
          g.fillGradient(px,py, px+PW,py+21, C_HDR1,C_HDR2);
          g.fillGradient(px,py, px+3, py+21, C_ACCENT_P,C_ACCENT_C);
          g.drawString(font,"\u00A7b\u25C6 \u00A7f\u00A7lPlayers ESP \u00A78v1.2",px+8,py+7,C_TEXT);

          // Таб-бар
          g.fill(px,py+21,px+PW,py+40, C_SECTION);
          g.fill(px,py+39,px+PW,py+40, C_BORDER);

          // Активная вкладка
          int tabW=(PW-8)/3;
          int tabX=px+2+TAB*(tabW+1);
          g.fill(tabX,py+22,tabX+tabW,py+39, C_TAB_BG);
          g.fillGradient(tabX,py+37,tabX+tabW,py+40, C_ACCENT_P,C_ACCENT_C);

          // Разделитель футера
          g.fill(px,py+PH-26,px+PW,py+PH-25, C_BORDER);

          // Боковые полосы
          g.fillGradient(px+2,py+40,px+3,py+PH-26, 0x2A7C5CFC,0x2A22D3EE);
          g.fillGradient(px+PW-3,py+40,px+PW-2,py+PH-26, 0x2A22D3EE,0x2A7C5CFC);

          // ── Превью цвета (вкладка Игроки) ─────────────────────────────────
          if (TAB==0) {
              int col=0xFF000000|((int)(EspConfig.espR*255)<<16)|((int)(EspConfig.espG*255)<<8)|(int)(EspConfig.espB*255);
              int bpx=px+PW-36, bpy=py+44+STEP*3;
              g.fill(bpx,    bpy,    bpx+12, bpy+10, 0xFF888888);
              g.fill(bpx+12, bpy,    bpx+24, bpy+10, 0xFF444444);
              g.fill(bpx,    bpy+10, bpx+12, bpy+20, 0xFF444444);
              g.fill(bpx+12, bpy+10, bpx+24, bpy+20, 0xFF888888);
              g.fill(bpx, bpy, bpx+24, bpy+20, col);
              g.fillGradient(bpx-1,bpy-1,bpx+25,bpy,    C_ACCENT_P,C_ACCENT_C);
              g.fillGradient(bpx-1,bpy+20,bpx+25,bpy+21,C_ACCENT_C,C_ACCENT_P);
              g.fill(bpx-1,bpy-1,bpx,   bpy+21, C_ACCENT_P);
              g.fill(bpx+24,bpy-1,bpx+25,bpy+21,C_ACCENT_C);
          }

          // ── Легенда руд (вкладка Руды) ─────────────────────────────────────
          if (TAB==1) {
              int[] oc={0xFF00FFFF,0xFFFFEE00,0xFFCC8033,0xFF00FF4D,
                        0xFF1A55FF,0xFFFF1A1A,0xFFFF6620,0xFF595959,0xFF9933CC,0xFFFFCC00};
              String[] on={"Алмаз","Золото","Железо","Изумруд",
                           "Лазурит","Редстоун","Медь","Уголь","Др.обломки","Нет.золото"};
              int lx=px+12, ly=py+44+STEP*2+6, cols=2, colW=(PW-24)/cols;
              for(int i=0;i<on.length;i++){
                  int cx2=lx+(i%cols)*colW, cy2=ly+(i/cols)*13;
                  g.fill(cx2,cy2,cx2+8,cy2+8,oc[i]);
                  g.fill(cx2-1,cy2-1,cx2+9,cy2,0x55FFFFFF);
                  g.fill(cx2-1,cy2-1,cx2,cy2+9,0x55FFFFFF);
                  g.drawString(font,"\u00A77"+on[i],cx2+11,cy2,0xCCCCDD);
              }
          }

          // ── Статус боевых функций (вкладка Боевой) ─────────────────────────
          if (TAB==2) {
              // Разделительная линия перед статусом
              int sy = py + 44 + STEP * 2 + 6;
              g.fill(px+12, sy-3, px+PW-12, sy-2, C_BORDER);

              // Статус АнтиУрон
              String nfCol = EspConfig.noFall   ? "\u00A7a" : "\u00A7c";
              String nfTxt = EspConfig.noFall   ? "ВКЛ"       : "ВЫКЛ";
              String kaCol = EspConfig.killAura  ? "\u00A7a" : "\u00A7c";
              String kaTxt = EspConfig.killAura  ? "ВКЛ"       : "ВЫКЛ";

              g.drawString(font, "\u00A7fАнтиУрон: "+nfCol+"\u25CF "+nfTxt, px+12, sy,    0xFFFFFF);
              g.drawString(font, "\u00A7fКиллАура: "+kaCol+"\u25CF "+kaTxt, px+12, sy+12, 0xFFFFFF);

              // Пояснительные строки — светло-серый §7
              g.drawString(font, "\u00A77\u25AA Атака только при наведении прицела",   px+12, sy+26, 0xFFFFFF);
              g.drawString(font, "\u00A77\u25AA АнтиУрон: onGround=true → сброс урона",px+12, sy+36, 0xFFFFFF);
          }

          // ── Футер ──────────────────────────────────────────────────────────
          g.drawCenteredString(font,
              "\u00A78[G]ESP  [End]Меню  [J]Руды  [N]Антиурон  [K]КиллАура",
              px+PW/2, py+PH-18, 0x4A5A6A);
          g.drawString(font,"\u00A73FTPDev \u00A78| ftplabs.github.io",px+6,py+PH-8,0x2A3A4A);

          super.render(g, mx, my, pt);
      }

      private static String pct(float v){ return Math.round(v*100)+"%"; }
      private static String fmt(float v){ return String.format("%.1f",v); }
  }
  