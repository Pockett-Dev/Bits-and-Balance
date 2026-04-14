package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;
import org.onenonly.bitsandbalance.mechanics.Validation;
import org.onenonly.bitsandbalance.network.SyncSitPayload;

/**
 * Sitting System Module
 * 
 * Manages the player sitting functionality, including:
 * - Server-side sitting state tracking
 * - Network synchronization between clients
 * - Player tracking events (login, start tracking)
 * 
 * Note: This module only handles the server-side event coordination.
 * The actual sitting mechanics are handled by:
 * - Client-side: MyModKeyBindings, ClientState, SitClientEvents, SitRenderHandler
 * - Network: NetworkHandler (SitPayload processing)
 * - Mixins: LocalPlayerMixin, RemotePlayerMixin, HumanoidModelMixin, etc.
 * - Visual: Various rendering and movement mixins
 * 
 * This separation keeps the sitting system modular while maintaining
 * the distributed nature of client/server functionality.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class SittingSystemModule implements FeatureModule {

    private static final String SIT_X_KEY = "bitsandbalance_sit_x";
    private static final String SIT_Y_KEY = "bitsandbalance_sit_y";
    private static final String SIT_Z_KEY = "bitsandbalance_sit_z";
    private static final String SIT_DIM_KEY = "bitsandbalance_sit_dim";
    
    @Override
    public String getFeatureName() {
        return "Sitting System";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.toggleSitting;
    }
    
    @Override
    public int getInitializationPriority() {
        return 200; // Medium priority - player interaction system
    }
    
    /**
     * Synchronizes sitting state when a player starts tracking another player.
     * This ensures that when players come into view of each other, they see
     * the correct sitting state.
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!Config.toggleSitting) return;
        
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!(event.getEntity() instanceof ServerPlayer tracker)) return;
        
        try {
            boolean sit = BitsAndBalance.SITTING_PLAYERS.contains(target.getUUID());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(tracker, 
                new SyncSitPayload(target.getUUID(), sit));
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Synchronizes sitting state when a player logs in.
     * This ensures that when players reconnect, they receive their current
     * sitting state and can see other sitting players correctly.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.toggleSitting) return;
        
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        
        try {
            boolean sit = BitsAndBalance.SITTING_PLAYERS.contains(sp.getUUID());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, 
                new SyncSitPayload(sp.getUUID(), sit));
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }

    /**
     * Server-side: if a player dismounts the seat (jump, knockback, etc.), clear sitting state.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.toggleSitting) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        try {
            var id = sp.getUUID();
            boolean sitting = BitsAndBalance.SITTING_PLAYERS.contains(id);

            if (!sitting) return;

            boolean valid = true;
            try { valid = Validation.testSit(sp); } catch (Throwable ignored) { valid = false; }

            // Anchor-based cancel: if you get moved (knockback, pushed, etc.), stop sitting.
            try {
                String dim = sp.getPersistentData().getString(SIT_DIM_KEY).orElse("");
                String currentDimStr;
                try {
                    currentDimStr = sp.level().dimension().identifier().toString();
                } catch (Throwable ignored) {
                    currentDimStr = "";
                }
                if (!dim.isEmpty() && !dim.equals(currentDimStr)) {
                    valid = false;
                }

                double anchorX = Double.longBitsToDouble(sp.getPersistentData().getLong(SIT_X_KEY).orElse(Double.doubleToRawLongBits(sp.getX())));
                double anchorY = Double.longBitsToDouble(sp.getPersistentData().getLong(SIT_Y_KEY).orElse(Double.doubleToRawLongBits(sp.getY())));
                double anchorZ = Double.longBitsToDouble(sp.getPersistentData().getLong(SIT_Z_KEY).orElse(Double.doubleToRawLongBits(sp.getZ())));

                double dx = sp.getX() - anchorX;
                double dy = sp.getY() - anchorY;
                double dz = sp.getZ() - anchorZ;

                double maxXZ = 0.15D;
                double maxY = 0.35D;
                if ((dx * dx + dz * dz) > (maxXZ * maxXZ) || Math.abs(dy) > maxY) {
                    valid = false;
                }
            } catch (Throwable ignored) {
            }

            if (!valid) {
                BitsAndBalance.SITTING_PLAYERS.remove(id);
                try {
                    sp.getPersistentData().remove(SIT_X_KEY);
                    sp.getPersistentData().remove(SIT_Y_KEY);
                    sp.getPersistentData().remove(SIT_Z_KEY);
                    sp.getPersistentData().remove(SIT_DIM_KEY);
                } catch (Throwable ignored) {
                }
                try { sp.refreshDimensions(); } catch (Throwable ignored) {}
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new SyncSitPayload(id, false));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(sp, new SyncSitPayload(id, false));
            }
        } catch (Throwable ignored) {
        }
    }
    
    @Override
    public void cleanup() {
        // Clear sitting state on shutdown
        BitsAndBalance.SITTING_PLAYERS.clear();
    }
}
