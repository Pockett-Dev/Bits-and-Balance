package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Client: apply visual sitting effects to players
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class SitRenderHandler {
    private SitRenderHandler() {}

    private static final float SITTING_RENDER_Y_OFFSET = -0.65F;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        Player player = null;
        try {
            // Prefer the actual entity being rendered if the event exposes it.
            // (NeoForge API has changed across versions; use reflection to stay resilient.)
            var m = event.getClass().getMethod("getEntity");
            Object e = m.invoke(event);
            if (e instanceof Player p) {
                player = p;
            }
        } catch (Throwable ignored) {
        }

        if (player == null) {
            try {
                int id = event.getRenderState().id;
                Entity entity = mc.level.getEntity(id);
                if (entity instanceof Player p) {
                    player = p;
                }
            } catch (Throwable ignored) {
            }
        }

        if (player == null) return;
        if (player.isPassenger()) return;

        boolean isLocalPlayer = false;
        try {
            if (mc.player != null && mc.player.getUUID().equals(player.getUUID())) {
                isLocalPlayer = true;
            }
        } catch (Throwable ignored) {}

        boolean sitting = false;
        if (isLocalPlayer) {
            sitting = ClientState.sitting;
        } else {
            sitting = ClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
        }

        if (!sitting) return;

        // Apply visual sitting effect - translate down and adjust pose
        // Tuned by request.
        event.getPoseStack().translate(0.0F, SITTING_RENDER_Y_OFFSET, 0.0F);
    }
}
