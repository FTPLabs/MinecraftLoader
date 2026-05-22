package com.esp;

  import net.minecraft.client.gui.GuiGraphics;
  import net.minecraft.client.gui.components.AbstractSliderButton;
  import net.minecraft.client.gui.components.Button;
  import net.minecraft.client.gui.components.CycleButton;
  import net.minecraft.client.gui.screens.Screen;
  import net.minecraft.network.chat.Component;

  public class EspScreen extends Screen {
      public EspScreen() { super(Component.literal("ESP Settings")); }

      @Override public boolean isPauseScreen() { return false; }

      @Override
      protected void init() {
          int cx = width / 2, y = height / 2 - 95;

          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.espEnabled)
              .create(cx-155, y, 150, 20, Component.literal("Player ESP"),
                  (b, val) -> { EspConfig.espEnabled = val; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.oreEsp)
              .create(cx+5, y, 150, 20, Component.literal("Ore X-Ray"),
                  (b, val) -> { EspConfig.oreEsp = val; EspConfig.save(); }));
          y += 25;

          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showNick)
              .create(cx-155, y, 95, 20, Component.literal("Nick"),
                  (b, val) -> { EspConfig.showNick = val; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showHp)
              .create(cx-53, y, 95, 20, Component.literal("HP"),
                  (b, val) -> { EspConfig.showHp = val; EspConfig.save(); }));
          addRenderableWidget(CycleButton.<Boolean>onOffBuilder(EspConfig.showArmor)
              .create(cx+48, y, 107, 20, Component.literal("Armor"),
                  (b, val) -> { EspConfig.showArmor = val; EspConfig.save(); }));
          y += 25;

          addRenderableWidget(new AbstractSliderButton(cx-155, y, 310, 20,
                  Component.literal("Range: " + EspConfig.espRange),
                  (EspConfig.espRange - 16.0) / (128.0 - 16.0)) {
              @Override protected void updateMessage() { setMessage(Component.literal("Range: " + (int)(16+value*(128-16)))); }
              @Override protected void applyValue()    { EspConfig.espRange=(int)(16+value*(128-16)); EspConfig.save(); }
          });
          y += 25;

          addRenderableWidget(new AbstractSliderButton(cx-155, y, 97, 20,
                  Component.literal("R: " + pct(EspConfig.espR)), EspConfig.espR) {
              @Override protected void updateMessage() { setMessage(Component.literal("R: "+pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espR=(float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(cx-52, y, 97, 20,
                  Component.literal("G: " + pct(EspConfig.espG)), EspConfig.espG) {
              @Override protected void updateMessage() { setMessage(Component.literal("G: "+pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espG=(float)value; EspConfig.save(); }
          });
          addRenderableWidget(new AbstractSliderButton(cx+51, y, 104, 20,
                  Component.literal("B: " + pct(EspConfig.espB)), EspConfig.espB) {
              @Override protected void updateMessage() { setMessage(Component.literal("B: "+pct((float)value))); }
              @Override protected void applyValue()    { EspConfig.espB=(float)value; EspConfig.save(); }
          });
          y += 25;

          addRenderableWidget(new AbstractSliderButton(cx-155, y, 310, 20,
                  Component.literal("Ore Range: " + EspConfig.oreRange),
                  (EspConfig.oreRange - 8.0) / (32.0 - 8.0)) {
              @Override protected void updateMessage() { setMessage(Component.literal("Ore Range: "+(int)(8+value*(32-8)))); }
              @Override protected void applyValue()    { EspConfig.oreRange=(int)(8+value*(32-8)); EspConfig.save(); }
          });
          y += 30;

          addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
              .pos(cx-50, y).size(100, 20).build());
      }

      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
          renderBackground(g, mx, my, pt);
          int cx = width/2, py = height/2;
          g.fill(cx-165, py-108, cx+165, py+90, 0xBB000000);
          g.drawCenteredString(font, "§b§lESP Settings", cx, py-104, 0xFFFFFF);
          g.drawCenteredString(font, "§7[G] Toggle  [H] Menu  [J] Ore X-Ray", cx, py+75, 0x888888);
          super.render(g, mx, my, pt);
      }

      private static String pct(float v) { return Math.round(v*100)+"%"; }
  }