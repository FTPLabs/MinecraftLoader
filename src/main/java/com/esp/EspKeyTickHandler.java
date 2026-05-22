package com.esp;

  import net.minecraft.client.Minecraft;
  import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
  import net.minecraft.world.entity.player.Player;
  import net.minecraft.world.phys.AABB;
  import net.minecraft.world.phys.Vec3;
  import net.minecraftforge.api.distmarker.Dist;
  import net.minecraftforge.event.TickEvent;
  import net.minecraftforge.eventbus.api.SubscribeEvent;
  import net.minecraftforge.fml.common.Mod;

  @Mod.EventBusSubscriber(modid = PlayersESP.MOD_ID, value = Dist.CLIENT)
  public class EspKeyTickHandler {

      private static int killAuraTick = 0;

      @SubscribeEvent
      public static void onTick(TickEvent.ClientTickEvent event) {
          if (event.phase != TickEvent.Phase.END) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          while (EspKeyHandler.KEY_TOGGLE.consumeClick())
              { EspConfig.espEnabled = !EspConfig.espEnabled; EspConfig.save(); }
          while (EspKeyHandler.KEY_GUI.consumeClick())
              { if (mc.screen == null) mc.setScreen(new EspScreen()); }
          while (EspKeyHandler.KEY_ORE.consumeClick())
              { EspConfig.oreEsp   = !EspConfig.oreEsp;   EspConfig.save(); }
          while (EspKeyHandler.KEY_NOFALL.consumeClick())
              { EspConfig.noFall   = !EspConfig.noFall;   EspConfig.save(); }
          while (EspKeyHandler.KEY_KILLAURA.consumeClick())
              { EspConfig.killAura = !EspConfig.killAura; EspConfig.save(); }

          // АнтиУрон: сброс клиентского fallDistance + пакет onGround=true серверу
          if (EspConfig.noFall && mc.player.isAlive()) {
              mc.player.fallDistance = 0f;
              if (!mc.player.onGround() && mc.player.getDeltaMovement().y < -0.1) {
                  mc.player.connection.send(
                      new ServerboundMovePlayerPacket.StatusOnly(true, false)
                  );
              }
          }

          // КиллАура: атака только если прицел наведён на хитбокс (AABB.clip)
          if (EspConfig.killAura && mc.player.isAlive()) {
              if (++killAuraTick >= 4) {
                  killAuraTick = 0;
                  Vec3 eyes = mc.player.getEyePosition(1.0f);
                  Vec3 look = mc.player.getLookAngle();
                  Vec3 end  = eyes.add(look.scale(EspConfig.killAuraRange));

                  Player target  = null;
                  double minDist = EspConfig.killAuraRange;

                  for (Player p : mc.level.players()) {
                      if (p == mc.player || !p.isAlive()) continue;
                      double dist = mc.player.distanceTo(p);
                      if (dist > EspConfig.killAuraRange) continue;
                      AABB hitbox = p.getBoundingBox().inflate(0.1);
                      if (hitbox.clip(eyes, end).isEmpty()) continue;
                      if (dist < minDist) { minDist = dist; target = p; }
                  }
                  if (target != null) mc.gameMode.attack(mc.player, target);
              }
          }
      }
  }
  