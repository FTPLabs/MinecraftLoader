package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.core.BlockPos;
  import net.minecraft.core.Direction;
  import net.minecraft.tags.BlockTags;
  import net.minecraft.world.entity.player.Inventory;
  import net.minecraft.world.item.PickaxeItem;
  import net.minecraft.world.level.Level;
  import net.minecraft.world.level.block.state.BlockState;
  import net.minecraft.world.phys.BlockHitResult;
  import net.minecraft.world.phys.HitResult;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;
  import org.apache.logging.log4j.LogManager;
  import org.apache.logging.log4j.Logger;

  /**
   * Майнинг-бот v1.7.
   *
   * Изменения:
   *  - Фильтрация по типу руды (EspConfig.miningOreType).
   *  - Машина состояний: IDLE → MOVING → BREAKING, чёткие переходы.
   *  - Сканирование ТОЛЬКО при отсутствии цели — не прерывает активный взлом.
   *  - Определение face через mc.hitResult (если смотрит на блок) или по позиции.
   *  - Таймаут на взлом и на движение к цели (пропускаем недостижимую руду).
   *  - Прыжок при застревании.
   *  - Правильный порядок: lookAt → startDestroyBlock → continueDestroyBlock.
   */
  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class MiningBot {
      private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

      private enum State { IDLE, MOVING, BREAKING }

      private static State    state        = State.IDLE;
      private static BlockPos targetBlock  = null;
      private static int      breakTick    = 0;      // тиков ломаем текущий блок
      private static int      moveTick     = 0;      // тиков идём к цели
      private static int      stuckTick    = 0;      // тиков стоим на месте при движении
      private static Vec3     lastPos      = null;

      private static final int MAX_BREAK_TICKS = 200; // максимум 10 сек на один блок
      private static final int MAX_MOVE_TICKS  = 160; // максимум 8 сек на подход к блоку
      private static final int BREAK_RANGE_SQ  = 20;  // расстояние начала взлома (кв) ~4.5 бл
      private static final int STUCK_THRESHOLD = 15;  // тиков без движения → прыжок

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          if (!EspConfig.miningBot) { resetState(); return; }

          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
              resetState(); return;
          }

          // Экипировать кирку
          if (!hasPickaxe(mc)) { equipPickaxe(mc); return; }

          switch (state) {
              case IDLE    -> tickIdle(mc);
              case MOVING  -> tickMoving(mc);
              case BREAKING -> tickBreaking(mc);
          }
      }

      // ── IDLE: сканируем ближайшую подходящую руду ─────────────────────────────

      private static void tickIdle(Minecraft mc) {
          targetBlock = findTarget(mc);
          if (targetBlock == null) return; // нет руды нужного типа в радиусе
          LOG.info("[MiningBot] Найдена цель: {} ({})", targetBlock, EspConfig.ORE_TYPE_NAMES[EspConfig.miningOreType]);
          state    = State.MOVING;
          moveTick = 0;
          stuckTick = 0;
          lastPos  = mc.player.position();
      }

      // ── MOVING: двигаемся к цели ──────────────────────────────────────────────

      private static void tickMoving(Minecraft mc) {
          // Цель исчезла (добыта кем-то другим или выгружена)
          if (!isTargetOre(mc.level, targetBlock)) {
              LOG.info("[MiningBot] Цель исчезла, ищем новую.");
              state = State.IDLE; return;
          }

          double distSq = mc.player.blockPosition().distSqr(targetBlock);

          // Достаточно близко → переходим к взлому
          if (distSq <= BREAK_RANGE_SQ) {
              state     = State.BREAKING;
              breakTick = 0;
              lookAt(mc, targetBlock);
              mc.gameMode.startDestroyBlock(targetBlock, getFace(mc, targetBlock));
              return;
          }

          // Таймаут движения → пропускаем эту руду
          if (++moveTick > MAX_MOVE_TICKS) {
              LOG.warn("[MiningBot] Не удалось достичь {}, пропускаем.", targetBlock);
              state = State.IDLE;
              targetBlock = null;
              return;
          }

          // Проверка застревания
          Vec3 curPos = mc.player.position();
          if (lastPos != null && curPos.distanceToSqr(lastPos) < 0.0004) {
              stuckTick++;
              if (stuckTick >= STUCK_THRESHOLD) {
                  // Прыгаем чтобы преодолеть блок
                  mc.player.jumpFromGround();
                  stuckTick = 0;
              }
          } else {
              stuckTick = 0;
          }
          lastPos = curPos;

          lookAt(mc, targetBlock);
          moveToward(mc, targetBlock);
      }

      // ── BREAKING: ломаем блок ─────────────────────────────────────────────────

      private static void tickBreaking(Minecraft mc) {
          // Блок сломан
          if (mc.level.getBlockState(targetBlock).isAir()) {
              LOG.info("[MiningBot] Блок {} сломан, ищем следующий.", targetBlock);
              state = State.IDLE;
              targetBlock = null;
              breakTick   = 0;
              return;
          }

          // Таймаут ломания (слишком крепкий блок или недоступен)
          if (++breakTick > MAX_BREAK_TICKS) {
              LOG.warn("[MiningBot] Не удалось сломать {}, пропускаем.", targetBlock);
              state = State.IDLE;
              targetBlock = null;
              breakTick   = 0;
              return;
          }

          // Если отошли слишком далеко (например, сбило) — возвращаемся
          double distSq = mc.player.blockPosition().distSqr(targetBlock);
          if (distSq > BREAK_RANGE_SQ * 2) {
              state    = State.MOVING;
              moveTick = 0;
              mc.gameMode.stopDestroyBlock();
              return;
          }

          lookAt(mc, targetBlock);
          Direction face = getFace(mc, targetBlock);
          mc.gameMode.continueDestroyBlock(targetBlock, face);
      }

      // ── Вспомогательные ───────────────────────────────────────────────────────

      private static void resetState() {
          if (state == State.BREAKING && targetBlock != null) {
              Minecraft mc = Minecraft.getInstance();
              if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
          }
          state = State.IDLE;
          targetBlock = null;
          breakTick   = 0;
          moveTick    = 0;
          stuckTick   = 0;
          lastPos     = null;
      }

      /** Получить подходящую руду в радиусе oreRange. */
      private static BlockPos findTarget(Minecraft mc) {
          BlockPos center  = mc.player.blockPosition();
          int      r       = Math.min(EspConfig.oreRange, 24);
          BlockPos nearest = null;
          double   minDist = Double.MAX_VALUE;
          for (int dx = -r; dx <= r; dx++) {
              for (int dy = -r; dy <= r; dy++) {
                  for (int dz = -r; dz <= r; dz++) {
                      BlockPos pos = center.offset(dx, dy, dz);
                      if (!isTargetOre(mc.level, pos)) continue;
                      double d = center.distSqr(pos);
                      if (d < minDist) { minDist = d; nearest = pos; }
                  }
              }
          }
          return nearest;
      }

      /** Проверить, является ли блок целевой рудой с учётом фильтра. */
      private static boolean isTargetOre(Level level, BlockPos pos) {
          if (pos == null || !level.isLoaded(pos)) return false;
          BlockState bs = level.getBlockState(pos);
          return switch (EspConfig.miningOreType) {
              case 0 -> isAnyOre(bs);
              case 1 -> bs.is(BlockTags.DIAMOND_ORES);
              case 2 -> bs.is(BlockTags.GOLD_ORES);
              case 3 -> bs.is(BlockTags.IRON_ORES);
              case 4 -> bs.is(BlockTags.EMERALD_ORES);
              case 5 -> bs.is(BlockTags.LAPIS_ORES);
              case 6 -> bs.is(BlockTags.REDSTONE_ORES);
              case 7 -> bs.is(BlockTags.COPPER_ORES);
              case 8 -> bs.is(BlockTags.COAL_ORES);
              case 9 -> bs.getBlock().getDescriptionId().contains("ancient_debris");
              default -> false;
          };
      }

      private static boolean isAnyOre(BlockState bs) {
          return bs.is(BlockTags.DIAMOND_ORES)  || bs.is(BlockTags.GOLD_ORES)
              || bs.is(BlockTags.IRON_ORES)     || bs.is(BlockTags.EMERALD_ORES)
              || bs.is(BlockTags.LAPIS_ORES)    || bs.is(BlockTags.REDSTONE_ORES)
              || bs.is(BlockTags.COPPER_ORES)   || bs.is(BlockTags.COAL_ORES)
              || bs.getBlock().getDescriptionId().contains("ancient_debris");
      }

      /**
       * Получить грань для взлома.
       * Приоритет: если mc.hitResult указывает на наш блок — используем его грань.
       * Иначе — вычисляем по относительной позиции игрока.
       */
      private static Direction getFace(Minecraft mc, BlockPos target) {
          if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS
                  && bhr.getBlockPos().equals(target)) {
              return bhr.getDirection();
          }
          // Вычисляем: относительный вектор от центра блока к позиции глаз игрока
          Vec3 eye    = mc.player.getEyePosition(1f);
          Vec3 center = Vec3.atCenterOf(target);
          Vec3 diff   = eye.subtract(center);
          double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
          if (ax >= ay && ax >= az) return diff.x > 0 ? Direction.EAST  : Direction.WEST;
          if (ay >= az)             return diff.y > 0 ? Direction.UP     : Direction.DOWN;
          return                           diff.z > 0 ? Direction.SOUTH  : Direction.NORTH;
      }

      private static void lookAt(Minecraft mc, BlockPos target) {
          Vec3 diff  = Vec3.atCenterOf(target).subtract(mc.player.getEyePosition(1f));
          double h   = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
          float yaw  = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
          float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, h));
          mc.player.setYRot(yaw);
          mc.player.setXRot(Math.max(-90f, Math.min(90f, pitch)));
          mc.player.yHeadRot = yaw;
      }

      private static void moveToward(Minecraft mc, BlockPos target) {
          Vec3 diff = Vec3.atCenterOf(target).subtract(mc.player.position());
          Vec3 norm = new Vec3(diff.x, 0, diff.z).normalize();
          double spd = 0.18;
          mc.player.setDeltaMovement(
              norm.x * spd,
              mc.player.getDeltaMovement().y,
              norm.z * spd
          );
          if (!mc.player.isSprinting() && mc.player.getFoodData().getFoodLevel() > 6)
              mc.player.setSprinting(true);
      }

      private static boolean hasPickaxe(Minecraft mc) {
          return mc.player.getMainHandItem().getItem() instanceof PickaxeItem;
      }

      private static void equipPickaxe(Minecraft mc) {
          Inventory inv = mc.player.getInventory();
          for (int i = 0; i < 9; i++) {
              if (inv.getItem(i).getItem() instanceof PickaxeItem) {
                  inv.selected = i; return;
              }
          }
      }
  }
  