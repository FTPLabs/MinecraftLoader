package com.esp;

    import net.minecraft.client.Minecraft;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.Direction;
    import net.minecraft.tags.BlockTags;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.item.PickaxeItem;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.block.state.BlockState;
    import net.minecraft.world.phys.Vec3;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.event.TickEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;

    /**
     * Бот автоматической добычи руд.
     * Сканирует ближайшую руду → поворачивает к ней → подходит → ломает.
     * Клавиша [B] для включения/выключения.
     */
    @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
    public class MiningBot {
        private static final Logger LOG = LogManager.getLogger(PlayersESP.MOD_ID);

        private static BlockPos targetBlock = null;
        private static int      breakTick   = 0;
        private static int      scanTick    = 0;

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!EspConfig.miningBot) { targetBlock = null; return; }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
                targetBlock = null; return;
            }

            // Экипировать кирку перед началом
            if (!hasPickaxe(mc)) { equipPickaxe(mc); return; }

            // Пересканируем каждые 40 тиков или когда цель исчезла
            if (++scanTick >= 40 || targetBlock == null || !isOre(mc.level, targetBlock)) {
                scanTick    = 0;
                targetBlock = findNearestOre(mc);
                breakTick   = 0;
                if (targetBlock == null) return;
                LOG.info("[MiningBot] Цель: {}", targetBlock);
            }

            double distSq = mc.player.blockPosition().distSqr(targetBlock);

            if (distSq > 16) {
                // Движение к цели
                moveToward(mc, targetBlock);
            } else {
                // Смотрим на блок и ломаем его
                lookAt(mc, targetBlock);
                Direction face = bestFace(mc.player.blockPosition(), targetBlock);
                if (breakTick == 0) {
                    mc.gameMode.startDestroyBlock(targetBlock, face);
                } else {
                    mc.gameMode.continueDestroyBlock(targetBlock, face);
                }
                breakTick++;
                if (mc.level.getBlockState(targetBlock).isAir()) {
                    targetBlock = null;
                    breakTick   = 0;
                }
            }
        }

        // ── Вспомогательные ───────────────────────────────────────────────────

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

        private static boolean isOre(Level level, BlockPos pos) {
            if (!level.isLoaded(pos)) return false;
            BlockState bs = level.getBlockState(pos);
            return bs.is(BlockTags.COAL_ORES)    || bs.is(BlockTags.IRON_ORES)
                || bs.is(BlockTags.GOLD_ORES)    || bs.is(BlockTags.DIAMOND_ORES)
                || bs.is(BlockTags.EMERALD_ORES) || bs.is(BlockTags.LAPIS_ORES)
                || bs.is(BlockTags.REDSTONE_ORES)|| bs.is(BlockTags.COPPER_ORES);
        }

        private static BlockPos findNearestOre(Minecraft mc) {
            BlockPos center = mc.player.blockPosition();
            int r = Math.min(EspConfig.oreRange, 20);
            BlockPos nearest = null;
            double minDist = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!isOre(mc.level, pos)) continue;
                        double d = center.distSqr(pos);
                        if (d < minDist) { minDist = d; nearest = pos; }
                    }
                }
            }
            return nearest;
        }

        private static void moveToward(Minecraft mc, BlockPos target) {
            Vec3 diff = Vec3.atCenterOf(target).subtract(mc.player.position()).normalize();
            double spd = mc.player.isSprinting() ? 0.22 : 0.15;
            mc.player.setDeltaMovement(diff.x * spd, mc.player.getDeltaMovement().y, diff.z * spd);
            lookAt(mc, target);
        }

        private static void lookAt(Minecraft mc, BlockPos target) {
            Vec3 diff = Vec3.atCenterOf(target).subtract(mc.player.getEyePosition(1f));
            double h   = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
            float yaw  = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, h));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yHeadRot = yaw;
        }

        private static Direction bestFace(BlockPos from, BlockPos target) {
            int dx = target.getX() - from.getX();
            int dy = target.getY() - from.getY();
            int dz = target.getZ() - from.getZ();
            int ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
            if (ax >= ay && ax >= az) return dx > 0 ? Direction.WEST  : Direction.EAST;
            if (ay >= az)             return dy > 0 ? Direction.DOWN   : Direction.UP;
            return                           dz > 0 ? Direction.NORTH  : Direction.SOUTH;
        }
    }
  