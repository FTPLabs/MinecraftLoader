package com.esp;

  import net.minecraft.client.gui.GuiGraphics;
  import net.minecraft.client.gui.components.AbstractSliderButton;
  import net.minecraft.client.gui.components.Button;
  import net.minecraft.client.gui.components.CycleButton;
  import net.minecraft.client.gui.screens.Screen;
  import net.minecraft.network.chat.Component;

  /**
   * Главное меню ESP — Premium Web3 / Glassmorphism стиль.
   * Открыть: [H]. Разработчик: FTPDev | github.com/FTPLabs
   */
  public class EspScreen extends Screen {

      // Палитра Web3
      private static final int C_BG        = 0xF2050C1E;
      private static final int C_HDR1      = 0xFF08111E;
      private static final int C_HDR2      = 0xFF120828;
      private static final int C_SECTION   = 0xFF0D1525;
      private static final int C_BORDER    = 0xFF1E293B;
      private static final int C_ACCENT_P  = 0xFF7C5CFC;
      private static final int C_ACCENT_C  = 0xFF22D3EE;
      private static final int C_TEXT      = 0xFFE2E8F0;
      private static final int C_TAB_BG   = 0x1A7C5CFC;

      private static final int PW = 430, PH = 280;
      private static int TAB = 0;
      private int px, py;

      public EspScreen() { super(Component.literal("ESP")); }
      @Override public boolean isPauseScreen() { return false; }

      @Override
      protected void init() {
          px = (width  - PW) / 2;
          py = (height - PH) / 2;

          // Вкладки
          int tabW = (PW - 8) / 3;
          addRenderableWidget(mkTab("\u25B8 Игроки",    0, px + 2,             py + 22, tabW));
          addRenderableWidget(mkTab("\u25B8 Руды",       1, px + 3 + tabW,     py + 22, tabW));
          addRenderableWidget(mkTab("\u25B8 Боевой",     2, px + 4 + tabW * 2, py + 22, tabW - 2));

          int x = px + 14, y = py + 52, w = PW - 28;
          switch (TAB) {
              case 0 -> buildPlayers(x, y, w);
              case 1 -> buildOre(x, y, w);
              case 2 -> buildCombat(x, y, w);
          }

          addRenderableWidget(Button.builder(
              Component.literal("\u2716  Закрыть"),
              b -> onClose()
          ).pos(px + PW / 2 - 52, py + PH - 26).size(104, 18).build());
      }

      private Button mkTab(String label, int id, int x, int y, int w) {
          String txt = TAB == id
              ? "\u00A7b\u00A7l" + label.replace("\u25B8 ", "")
              : "\u00A78" + label.replace("\u25B8 ", "");
          return Button.builder(Component.literal(txt), b -> {
              TAB = id;
              if (minecraft != null) minecraft.setScreen(new EspScreen());
          }).pos(x, y).size(w, 18).build();
      }

      // Вкладка: Игроки
      private void buildPlayers(int x, int y, int w) {
          int half = (w - 4) / 2;

          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.espEnabled)
              .create(x, y, half, 18, Component.literal("Игрок ESP"),
                  (b, v) -> { EspConfig.espEnabled = v; EspConfig.save(); }));
          y += 24;

          addRenderableWidget(new AbstractSliderButton(x, y, w, 18,
                  Component.literal("Дальность: " + EspConfig.espRange + " бл."),
                  (EspConfig.espRange - 16.0) / (1000.0 - 16.0)) {
              @Override protected void updateMessage() {
                  setMessage(Component.literal("Дальность: " + (int)(16 + value * 984) + " бл."));
              }
              @Override protected void applyValue() {
                  EspConfig.espRange = (int)(16 + value * 984); EspConfig.save();
              }
          });
          y += 24;

          int sw = (w - 4) / 3;
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showNick)
              .create(x,            y, sw, 18, Component.literal("Ник"),
                  (b, v) -> { EspConfig.showNick  = v; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showHp)
              .create(x + sw + 2,   y, sw, 18, Component.literal("HP"),
                  (b, v) -> { EspConfig.showHp    = v; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showArmor)
              .create(x+(sw+2)*2,   y, sw, 18, Component.literal("Броня"),
                  (b, v) -> { EspConfig.showArmor = v; EspConfig.save(); }));
          y += 24;

          int slW = (w - 6 - 28) / 3;
          addRenderableWidget(new AbstractSliderButton(x, y, slW, 18,
                  Component.literal("R:" + pct(EspConfig.espR)), EspConfig.espR) {
              @Override protected void updateMessage() { setMessage(Component.literal("R:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espR = (float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x + slW + 2, y, slW, 18,
                  Component.literal("G:" + pct(EspConfig.espG)), EspConfig.espG) {
              @Override protected void updateMessage() { setMessage(Component.literal("G:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espG = (float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x + (slW+2)*2, y, slW, 18,
                  Component.literal("B:" + pct(EspConfig.espB)), EspConfig.espB) {
              @Override protected void updateMessage() { setMessage(Component.literal("B:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espB = (float)value; EspConfig.save(); }
          });
      }

      // Вкладка: Руды
      private void buildOre(int x, int y, int w) {
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.oreEsp)
              .create(x, y, (w - 4) / 2, 18, Component.literal("Сканер Руд"),
                  (b, v) -> { EspConfig.oreEsp = v; EspConfig.save(); }));
          y += 24;

          addRenderableWidget(new AbstractSliderButton(x, y, w, 18,
                  Component.literal("Радиус: " + EspConfig.oreRange + " бл."),
                  (EspConfig.oreRange - 8.0) / (32.0 - 8.0)) {
              @Override protected void updateMessage() {
                  setMessage(Component.literal("Радиус: " + (int)(8 + value * 24) + " бл."));
              }
              @Override protected void applyValue() {
                  EspConfig.oreRange = (int)(8 + value * 24); EspConfig.save();
              }
          });
      }

      // Вкладка: Боевой
      private void buildCombat(int x, int y, int w) {
          int half = (w - 4) / 2;

          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.noFall)
              .create(x, y, half, 18, Component.literal("АнтиУрон [N]"),
                  (b, v) -> { EspConfig.noFall = v; EspConfig.save(); }));
          y += 24;

          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.killAura)
              .create(x, y, half, 18, Component.literal("КиллАура [K]"),
                  (b, v) -> { EspConfig.killAura = v; EspConfig.save(); }));
          y += 24;

          double initRange = (EspConfig.killAuraRange - 2.0) / 4.0;
          addRenderableWidget(new AbstractSliderButton(x, y, w, 18,
                  Component.literal("Радиус атаки: " + fmt(EspConfig.killAuraRange) + " бл."),
                  initRange) {
              @Override protected void updateMessage() {
                  float v = 2f + (float)(value * 4.0);
                  setMessage(Component.literal("Радиус атаки: " + fmt(v) + " бл."));
              }
              @Override protected void applyValue() {
                  EspConfig.killAuraRange = 2f + (float)(value * 4.0); EspConfig.save();
              }
          });
      }

      // Рендер панели (Web3 стиль)
      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
          renderBackground(g, mx, my, pt);

          // Основная панель
          g.fill(px, py, px + PW, py + PH, C_BG);

          // Внешняя рамка (градиент фиолет→голубой)
          g.fillGradient(px - 1, py - 1, px + PW + 1, py,     C_ACCENT_P, C_ACCENT_C);
          g.fillGradient(px - 1, py + PH, px + PW + 1, py + PH + 1, C_ACCENT_C, C_ACCENT_P);
          g.fill(px - 1, py, px,      py + PH, C_ACCENT_P);
          g.fill(px + PW, py, px + PW + 1, py + PH, C_ACCENT_C);

          // Шапка
          g.fillGradient(px, py, px + PW, py + 21, C_HDR1, C_HDR2);
          g.fillGradient(px, py, px + 3, py + 21, C_ACCENT_P, C_ACCENT_C);

          // Заголовок
          g.drawString(font, "\u00A7b\u25C6 \u00A7f\u00A7lPlayers ESP \u00A78v1.1", px + 8, py + 7, C_TEXT);
          g.drawString(font, "\u00A73FTPDev", px + PW - 56, py + 7, C_TEXT);

          // Таб-бар
          g.fill(px, py + 21, px + PW, py + 42, C_SECTION);
          g.fill(px, py + 41, px + PW, py + 42, C_BORDER);

          // Подсветка активного таба
          int tabW = (PW - 8) / 3;
          int tabX = px + 2 + TAB * (tabW + 1);
          g.fill(tabX, py + 22, tabX + tabW, py + 41, C_TAB_BG);
          g.fillGradient(tabX, py + 39, tabX + tabW, py + 42, C_ACCENT_P, C_ACCENT_C);

          // Разделитель контент/футер
          g.fill(px, py + PH - 28, px + PW, py + PH - 27, C_BORDER);

          // Боковые акцентные линии (полупрозрачные)
          g.fillGradient(px + 2, py + 42, px + 3, py + PH - 28, 0x2A7C5CFC, 0x2A22D3EE);
          g.fillGradient(px + PW - 3, py + 42, px + PW - 2, py + PH - 28, 0x2A22D3EE, 0x2A7C5CFC);

          // Цветовой превью (вкладка Игроки)
          if (TAB == 0) {
              int col = 0xFF000000
                  | ((int)(EspConfig.espR * 255) << 16)
                  | ((int)(EspConfig.espG * 255) <<  8)
                  |  (int)(EspConfig.espB * 255);
              int bpx = px + PW - 38, bpy = py + 52 + 24 * 3;
              g.fill(bpx,      bpy,      bpx + 13, bpy + 11, 0xFF888888);
              g.fill(bpx + 13, bpy,      bpx + 26, bpy + 11, 0xFF444444);
              g.fill(bpx,      bpy + 11, bpx + 13, bpy + 22, 0xFF444444);
              g.fill(bpx + 13, bpy + 11, bpx + 26, bpy + 22, 0xFF888888);
              g.fill(bpx, bpy, bpx + 26, bpy + 22, col);
              g.fillGradient(bpx - 1, bpy - 1, bpx + 27, bpy,      C_ACCENT_P, C_ACCENT_C);
              g.fillGradient(bpx - 1, bpy + 22, bpx + 27, bpy + 23, C_ACCENT_C, C_ACCENT_P);
              g.fill(bpx - 1, bpy - 1, bpx,      bpy + 23, C_ACCENT_P);
              g.fill(bpx + 26, bpy - 1, bpx + 27, bpy + 23, C_ACCENT_C);
          }

          // Легенда руд (вкладка Руды)
          if (TAB == 1) {
              int[] oc = {0xFF00FFFF,0xFFFFEE00,0xFFCC8033,0xFF00FF4D,
                          0xFF1A55FF,0xFFFF1A1A,0xFFFF6620,0xFF595959,0xFF9933CC,0xFFFFCC00};
              String[] on = {"Алмаз","Золото","Железо","Изумруд",
                             "Лазурит","Редстоун","Медь","Уголь","Др.обломки","Нет.золото"};
              int lx = px + 14, ly = py + 52 + 24 * 2 + 8;
              int cols = 2, colW = (PW - 28) / cols;
              for (int i = 0; i < on.length; i++) {
                  int cx2 = lx + (i % cols) * colW;
                  int cy2 = ly + (i / cols) * 13;
                  g.fill(cx2, cy2, cx2 + 9, cy2 + 9, oc[i]);
                  g.fill(cx2 - 1, cy2 - 1, cx2 + 10, cy2, 0x55FFFFFF);
                  g.fill(cx2 - 1, cy2 - 1, cx2, cy2 + 10, 0x55FFFFFF);
                  g.drawString(font, "\u00A77" + on[i], cx2 + 13, cy2, 0xAAAABB);
              }
          }

          // Статус (вкладка Боевой)
          if (TAB == 2) {
              int sy = py + 52 + 24 * 3 + 4;
              String nfS = EspConfig.noFall   ? "\u00A7a\u25CF ВКЛ" : "\u00A7c\u25CF ВЫКЛ";
              String kaS = EspConfig.killAura  ? "\u00A7a\u25CF ВКЛ" : "\u00A7c\u25CF ВЫКЛ";
              g.drawString(font, "\u00A78Статус: АнтиУрон " + nfS, px + 14, sy,      0xFFFFFF);
              g.drawString(font, "\u00A78Статус: КиллАура  " + kaS, px + 14, sy + 12, 0xFFFFFF);
              g.drawString(font, "\u00A78\u26A0 Работает только без античита", px + 14, sy + 26, 0x445566);
          }

          // Футер: клавиши + кредит
          g.drawCenteredString(font,
              "\u00A78[G] ESP  [H] Меню  [J] Руды  [N] Антиурон  [K] КиллАура",
              px + PW / 2, py + PH - 20, 0x3A4A5A);
          g.drawString(font, "\u00A73FTPDev \u00A78| github.com/FTPLabs", px + 6, py + PH - 9, 0x2A3A4A);

          super.render(g, mx, my, pt);
      }

      private static String pct(float v) { return Math.round(v * 100) + "%"; }
      private static String fmt(float v)  { return String.format("%.1f", v); }
  }
  