package com.esp;

  import com.mojang.blaze3d.vertex.DefaultVertexFormat;
  import com.mojang.blaze3d.vertex.VertexFormat;
  import net.minecraft.client.renderer.RenderType;
  import java.util.OptionalDouble;

  public abstract class EspRenderType extends RenderType {
      protected EspRenderType(String n,VertexFormat f,VertexFormat.Mode m,int b,boolean ac,boolean s,Runnable su,Runnable cl){
          super(n,f,m,b,ac,s,su,cl);
      }
      private static final RenderType ESP_LINES = create(
          "esp_lines",
          DefaultVertexFormat.POSITION_COLOR_NORMAL,
          VertexFormat.Mode.LINES,
          256,
          false, false,
          CompositeState.builder()
              .setShaderState(RENDERTYPE_LINES_SHADER)
              .setLineState(new LineStateShard(OptionalDouble.of(1.5)))
              .setLayeringState(VIEW_OFFSET_Z_LAYERING)
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
              .setWriteMaskState(COLOR_WRITE)
              .setCullState(NO_CULL)
              .setDepthTestState(NO_DEPTH_TEST)
              .createCompositeState(false)
      );
      public static RenderType espLines() { return ESP_LINES; }
  }