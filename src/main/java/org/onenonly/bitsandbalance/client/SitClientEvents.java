package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-only events for Sitting visuals and dimensions.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class SitClientEvents {
    private SitClientEvents() {}

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        try { if (player.isPassenger()) return; } catch (Throwable ignored) {}
        // Do not adjust while crawling/swimming; this mod uses forced swimming for crawl
        try { if (player.isSwimming()) return; } catch (Throwable ignored) {}

        boolean sitting = false;
        try {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.player != null && mc.player.getUUID().equals(player.getUUID())) {
                sitting = ClientState.sitting;
            } else {
                sitting = ClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
            }
        } catch (Throwable ignored) {}

        if (!sitting) return;

        // Start from standing dimensions and reduce height
        EntityDimensions base = player.getDimensions(Pose.STANDING);
        float width = base.width();
        float height = Math.max(0.9F, base.height() - 0.5F); // tweak offset here
        event.setNewSize(EntityDimensions.scalable(width, height));
        // Eye height follows from dimensions in current NeoForge versions; older mappings exposed setNewEyeHeight
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        try {
            ClientState.sitting = false;
            ClientState.SYNCED_SITTING_PLAYERS.clear();
        } catch (Throwable ignored) {}
    }

    /**
     * Force visual sitting effects when client state changes
     */
    public static void updateVisualSitting(boolean sitting) {
        try {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                Player player = mc.player; // Store reference to avoid null pointer access
                SitVisuals.setLocalPlayer(sitting);

                // Align with Fabric: set the actual pose for better vanilla visuals.
                try {
                    if (sitting) {
                        player.setPose(Pose.SITTING);
                    } else if (player.getPose() == Pose.SITTING) {
                        player.setPose(Pose.STANDING);
                    }
                } catch (Throwable ignored) {
                }

                // Force dimension refresh to apply size changes
                player.refreshDimensions();
            }
        } catch (Throwable ignored) {}
    }
}
