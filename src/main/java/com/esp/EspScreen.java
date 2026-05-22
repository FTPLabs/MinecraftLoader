package com.esp;

  import net.minecraft.client.gui.GuiGraphics;
  import net.minecraft.client.gui.components.AbstractSliderButton;
  import net.minecraft.client.gui.components.Button;
  import net.minecraft.client.gui.components.CycleButton;
  import net.minecraft.client.gui.screens.Screen;
  import net.minecraft.network.chat.Component;

  /**
   * ESP configuration screen.
   * Open with [H]. Tabs: Player ESP | Ore X-Ray.
   *
   * Developer: FTPDev | github.com/FTPLabs
   */
  public class EspScreen extends Screen {

      private static int TAB = 0;

      private static final int PW = 380, PH = 220;
      private int px, py;

      public EspScreen() { super(Component.literal("ESP")); }

      @Override public boolean isPauseScreen() { return false; }

      @Override
      protected void init() {
          px = (width  - PW) / 2;
          py = (height - PH) / 2;

          int tabW = (PW - 4) / 2;
          addRenderableWidget(tabBtn("\u25B6 Player ESP", 0, px + 2,        py + 22, tabW));
          addRenderableWidget(tabBtn("\u25B6 Ore X-Ray",  1, px + 3 + tabW, py + 22, tabW - 1));

          int lx = px + 10;
          int y0 = py + 46;
          int cw = PW - 20;

          switch (TAB) {
              case 0 -> buildEsp(lx, y0, cw);
              case 1 -> buildOre(lx, y0, cw);
          }

          int cx = px + PW / 2;
          addRenderableWidget(Button.builder(Component.literal("\u2716 Close"), b -> onClose())
              .pos(cx - 44, py + PH - 24).size(88, 18).build());
      }

      private Button tabBtn(String label, int id, int x, int y, int w) {
          String txt = TAB == id
              ? "\u00A7e\u00A7l" + label.substring(2)
              : "\u00A77"         + label.substring(2);
          return Button.builder(Component.literal(txt), b -> {
              TAB = id;
              if (minecraft != null) minecraft.setScreen(new EspScreen());
          }).pos(x, y).size(w, 18).build();
      }

      // ── Player ESP tab ───────────────────────────────────────────────────────
      private void buildEsp(int x, int y, int w) {
          int half = (w - 4) / 2;
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.espEnabled)
              .create(x, y, half, 18, Component.literal("Player ESP"),
                  (b, v) -> { EspConfig.espEnabled = v; EspConfig.save(); }));
          y += 22;

          // Range 16–1000
          addRenderableWidget(new AbstractSliderButton(x, y, w, 18,
                  Component.literal("Range: " + EspConfig.espRange + " blocks"),
                  (EspConfig.espRange - 16.0) / (1000.0 - 16.0)) {
              @Override protected void updateMessage() { setMessage(Component.literal("Range: " + (int)(16 + value * (1000 - 16)) + " blocks")); }
              @Override protected void applyValue()    { EspConfig.espRange = (int)(16 + value * (1000 - 16)); EspConfig.save(); }
          });
          y += 22;

          // Nick / HP / Armor
          int sw = (w - 4) / 3;
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showNick)
              .create(x, y, sw, 18, Component.literal("Nick"),
                  (b, v) -> { EspConfig.showNick  = v; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showHp)
              .create(x + sw + 2, y, sw, 18, Component.literal("HP"),
                  (b, v) -> { EspConfig.showHp    = v; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showArmor)
              .create(x + (sw + 2) * 2, y, sw, 18, Component.literal("Armor"),
                  (b, v) -> { EspConfig.showArmor = v; EspConfig.save(); }));
          y += 22;

          // Color sliders R/G/B + preview
          int slW = (w - 6 - 26) / 3;
          addRenderableWidget(new AbstractSliderButton(x, y, slW, 18, Component.literal("R:" + pct(EspConfig.espR)), EspConfig.espR) {
              @Override protected void updateMessage() { setMessage(Component.literal("R:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espR = (float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x + slW + 2, y, slW, 18, Component.literal("G:" + pct(EspConfig.espG)), EspConfig.espG) {
              @Override protected void updateMessage() { setMessage(Component.literal("G:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espG = (float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(x + (slW + 2) * 2, y, slW, 18, Component.literal("B:" + pct(EspConfig.espB)), EspConfig.espB) {
              @Override protected void updateMessage() { setMessage(Component.literal("B:" + pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espB = (float)value; EspConfig.save(); }
          });
      }

      // ── Ore X-Ray tab ────────────────────────────────────────────────────────
      private void buildOre(int x, int y, int w) {
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.oreEsp)
              .create(x, y, (w - 4) / 2, 18, Component.literal("Ore X-Ray"),
                  (b, v) -> { EspConfig.oreEsp = v; EspConfig.save(); }));
          y += 22;

          // Scan range 8–32 (safe performance limit)
          addRenderableWidget(new AbstractSliderButton(x, y, w, 18,
                  Component.literal("Scan: " + EspConfig.oreRange + " blocks"),
                  (EspConfig.oreRange - 8.0) / (32.0 - 8.0)) {
              @Override protected void updateMessage() { setMessage(Component.literal("Scan: " + (int)(8 + value * (32 - 8)) + " blocks")); }
              @Override protected void applyValue()    { EspConfig.oreRange = (int)(8 + value * (32 - 8)); EspConfig.save(); }
          });
          y += 22;
          // Ore legend
      }

      // ── Render ───────────────────────────────────────────────────────────────
      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
          renderBackground(g, mx, my, pt);

          g.fill(px, py, px + PW, py + PH, 0xEE0A0A16);
          g.fillGradient(px, py, px + PW, py + 20, 0xFF1020A0, 0xFF501060);
          g.drawCenteredString(font, "\u00A7b\u00A7l\u25C6 ESP Menu \u25C6", px + PW / 2, py + 6, 0xFFFFFF);

          g.fill(px, py + 20, px + PW, py + 42, 0xFF0D0D20);
          int tabW = (PW - 4) / 2;
          int tabX = px + 2 + TAB * (tabW + 1);
          g.fillGradient(tabX, py + 39, tabX + tabW, py + 42, 0xFF4488FF, 0xFF8844FF);

          g.fill(px, py + 42, px + PW, py + 43, 0xFF1A1A3A);
          g.fill(px, py + PH - 26, px + PW, py + PH - 25, 0xFF1A1A3A);
          g.fillGradient(px,          py, px + 2,      py + PH, 0xFF4488FF, 0xFF8844FF);
          g.fillGradient(px + PW - 2, py, px + PW,     py + PH, 0xFF8844FF, 0xFF4488FF);

          // Color preview (Player ESP tab)
          if (TAB == 0) {
              int col = 0xFF000000
                  | ((int)(EspConfig.espR * 255) << 16)
                  | ((int)(EspConfig.espG * 255) << 8)
                  |  (int)(EspConfig.espB * 255);
              int bpx = px + PW - 36, bpy = py + 46 + 22 * 3;
              g.fill(bpx,      bpy,      bpx + 12, bpy + 9,  0xFF888888);
              g.fill(bpx + 12, bpy,      bpx + 24, bpy + 9,  0xFF444444);
              g.fill(bpx,      bpy + 9,  bpx + 12, bpy + 18, 0xFF444444);
              g.fill(bpx + 12, bpy + 9,  bpx + 24, bpy + 18, 0xFF888888);
              g.fill(bpx, bpy, bpx + 24, bpy + 18, col);
              g.fill(bpx - 1, bpy - 1, bpx + 25, bpy,      0xFF4488FF);
              g.fill(bpx - 1, bpy + 18, bpx + 25, bpy + 19, 0xFF4488FF);
              g.fill(bpx - 1, bpy - 1, bpx,       bpy + 19, 0xFF4488FF);
              g.fill(bpx + 24, bpy - 1, bpx + 25, bpy + 19, 0xFF4488FF);
          }

          // Ore legend
          if (TAB == 1) {
              int[] oc = {0xFF00FFFF,0xFFFFEE00,0xFFCC8033,0xFF00FF4D,0xFF1A55FF,0xFFFF1A1A,0xFFFF6620,0xFF595959,0xFF9933CC,0xFFFFCC00};
              String[] on = {"Diamond","Gold","Iron","Emerald","Lapis","Redstone","Copper","Coal","An.Debris","Nether Gold"};
              int lx = px + 10, ly = py + 46 + 22 * 2 + 4;
              int cols = 2, colW = (PW - 20) / cols;
              for (int i = 0; i < on.length; i++) {
                  int cx2 = lx + (i % cols) * colW;
                  int cy2 = ly + (i / cols) * 13;
                  g.fill(cx2, cy2, cx2 + 8, cy2 + 8, oc[i]);
                  g.fill(cx2 - 1, cy2 - 1, cx2 + 9, cy2, 0x88FFFFFF);
                  g.fill(cx2 - 1, cy2 - 1, cx2, cy2 + 9, 0x88FFFFFF);
                  g.drawString(font, "\u00A77" + on[i], cx2 + 11, cy2, 0xAAAAAA);
              }
          }

          // Developer credit + keys hint
          g.drawCenteredString(font, "\u00A78[G] Toggle  [H] Menu  [J] Ore", px + PW / 2, py + PH - 20, 0x555555);
          g.drawCenteredString(font, "\u00A78FTPDev | github.com/FTPLabs", px + PW / 2, py + PH - 10, 0x444444);

          super.render(g, mx, my, pt);
      }

      private static String pct(float v) { return Math.round(v * 100) + "%"; }
  }
  