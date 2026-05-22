package com.esp;

  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.blaze3d.vertex.VertexConsumer;
  import net.minecraft.client.Minecraft;
  import net.minecraft.core.BlockPos;
  import net.minecraft.tags.BlockTags;
  import net.minecraft.world.level.Level;
  import net.minecraft.world.level.block.state.BlockState;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.client.event.RenderLevelStageEvent;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;

  import java.util.ArrayList;
  import java.util.List;
  import java.util.concurrent.CopyOnWriteArrayList;

  /**
   * FIX v1.6:
   *  - ORES заменён на CopyOnWriteArrayList — потокобезопасность между tick и render
   *  - Убрана лишняя аннотация @Mod.EventBusSubscriber (регистрируется через PlayersESP)
   */
  public class OreEspRenderer {

      private record OreEntry(BlockPos pos, float r, float g, float b) {}

      // FIX: CopyOnWriteArrayList — безопасно читать из рендер-треда пока тик-тред пишет
      private static final List<OreEntry> ORES = new CopyOnWriteArrayList<>();
      private static int tick = 0;

      /**
       * Сканирует руды каждые 60 тиков (3 сек). Радиус ограничен 32 блоками.
       */
      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null || !EspConfig.oreEsp) return;
          if (++tick < 60) return;
          tick = 0;

          List<OreEntry> fresh = new ArrayList<>();
          Level level  = mc.level;
          BlockPos center = mc.player.blockPosition();
          int r = Math.min(EspConfig.oreRange, 32);

          for (int dx = -r; dx <= r; dx++) {
              for (int dy = -r; dy <= r; dy++) {
                  for (int dz = -r; dz <= r; dz++) {
                      BlockPos pos = center.offset(dx, dy, dz);
                      if (!level.isLoaded(pos)) continue;
                      BlockState bs = level.getBlockState(pos);
                      float[] col = oreColor(bs);
                      if (col != null) fresh.add(new OreEntry(pos, col[0], col[1], col[2]));
                  }
              }
          }

          // Атомарная замена: clear + addAll на CopyOnWriteArrayList безопасна
          ORES.clear();
          ORES.addAll(fresh);
      }

      @SubscribeEvent
      public static void onRender(RenderLevelStageEvent event) {
          if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
          if (!EspConfig.oreEsp || ORES.isEmpty()) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null) return;

          Vec3 cam = event.getCamera().getPosition();
          PoseStack ps = new PoseStack();
          ps.translate(-cam.x, -cam.y, -cam.z);

          var bufSrc = mc.renderBuffers().bufferSource();
          VertexConsumer lines = bufSrc.getBuffer(EspRenderType.espLines());

          for (OreEntry ore : ORES) {
              double x = ore.pos().getX(), y = ore.pos().getY(), z = ore.pos().getZ();
              box(ps, lines, x - 0.01, y - 0.01, z - 0.01, x + 1.01, y + 1.01, z + 1.01, ore.r(), ore.g(), ore.b());
          }
          bufSrc.endBatch(EspRenderType.espLines());
      }

      private static float[] oreColor(BlockState bs) {
          if (bs.is(BlockTags.DIAMOND_ORES))  return new float[]{0.0f, 1.0f, 1.0f};
          if (bs.is(BlockTags.GOLD_ORES))     return new float[]{1.0f, 0.9f, 0.0f};
          if (bs.is(BlockTags.IRON_ORES))     return new float[]{0.8f, 0.5f, 0.2f};
          if (bs.is(BlockTags.EMERALD_ORES))  return new float[]{0.0f, 1.0f, 0.3f};
          if (bs.is(BlockTags.LAPIS_ORES))    return new float[]{0.1f, 0.3f, 1.0f};
          if (bs.is(BlockTags.REDSTONE_ORES)) return new float[]{1.0f, 0.1f, 0.1f};
          if (bs.is(BlockTags.COPPER_ORES))   return new float[]{0.9f, 0.4f, 0.1f};
          if (bs.is(BlockTags.COAL_ORES))     return new float[]{0.35f, 0.35f, 0.35f};
          String id = bs.getBlock().getDescriptionId();
          if (id.contains("ancient_debris")) return new float[]{0.6f, 0.2f, 0.8f};
          if (id.contains("nether_quartz"))  return new float[]{0.9f, 0.9f, 0.9f};
          if (id.contains("nether_gold"))    return new float[]{1.0f, 0.8f, 0.0f};
          return null;
      }

      private static void box(PoseStack ps, VertexConsumer buf,
              double x1, double y1, double z1, double x2, double y2, double z2,
              float r, float g, float b) {
          EspLineUtil.addLine(ps,buf, x1,y1,z1, x2,y1,z1, r,g,b,1f); EspLineUtil.addLine(ps,buf, x2,y1,z1, x2,y2,z1, r,g,b,1f);
          EspLineUtil.addLine(ps,buf, x2,y2,z1, x1,y2,z1, r,g,b,1f); EspLineUtil.addLine(ps,buf, x1,y2,z1, x1,y1,z1, r,g,b,1f);
          EspLineUtil.addLine(ps,buf, x1,y1,z2, x2,y1,z2, r,g,b,1f); EspLineUtil.addLine(ps,buf, x2,y1,z2, x2,y2,z2, r,g,b,1f);
          EspLineUtil.addLine(ps,buf, x2,y2,z2, x1,y2,z2, r,g,b,1f); EspLineUtil.addLine(ps,buf, x1,y2,z2, x1,y1,z2, r,g,b,1f);
          EspLineUtil.addLine(ps,buf, x1,y1,z1, x1,y1,z2, r,g,b,1f); EspLineUtil.addLine(ps,buf, x2,y1,z1, x2,y1,z2, r,g,b,1f);
          EspLineUtil.addLine(ps,buf, x2,y2,z1, x2,y2,z2, r,g,b,1f); EspLineUtil.addLine(ps,buf, x1,y2,z1, x1,y2,z2, r,g,b,1f);
      }
  }
  