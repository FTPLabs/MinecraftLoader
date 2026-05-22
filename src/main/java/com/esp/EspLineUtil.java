package com.esp;

  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.VertexConsumer;
  import org.joml.Matrix4f;

  /**
   * Shared line-drawing helper used by all ESP renderers.
   */
  public final class EspLineUtil {
      private EspLineUtil() {}

      public static void addLine(PoseStack ps, VertexConsumer buf,
              double x1, double y1, double z1,
              double x2, double y2, double z2,
              float r, float g, float b, float a) {
          Matrix4f mat = ps.last().pose();
          float dx = (float)(x2 - x1), dy = (float)(y2 - y1), dz = (float)(z2 - z1);
          float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
          if (len > 1e-6f) { dx /= len; dy /= len; dz /= len; } else { dy = 1f; }
          buf.addVertex(mat, (float)x1, (float)y1, (float)z1).setColor(r, g, b, a).setNormal(ps.last(), dx, dy, dz);
          buf.addVertex(mat, (float)x2, (float)y2, (float)z2).setColor(r, g, b, a).setNormal(ps.last(), dx, dy, dz);
      }
  }
  