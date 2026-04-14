package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Leashed Teleport Module for testing
 * 
 * This is a minimal implementation to test basic functionality
 * without the complexity of data attachments.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class SimpleLeashedTeleportModule {
    
    // Simple tracking without data attachments
    private static final ConcurrentHashMap<UUID, Long> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, List<UUID>> PENDING_FOLLOWERS = new ConcurrentHashMap<>();
    
    static {
        BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Module loaded!");
    }
    
    /**
     * Test event handler to verify the module is working
     */
    @SubscribeEvent
    public static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Server starting - module is active! Enabled: {}", Config.enableLeashedTeleport);
    }
    
    /**
     * Simple teleport detection using player tick events
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Update every 20 ticks (1 second) for testing
        if (player.tickCount % 20 != 0) return;
        
        try {
            // Check for leashed mobs around the player
            List<UUID> followers = findLeashedFollowers(player);
            if (!followers.isEmpty()) {
                BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Player {} has {} leashed followers", 
                    player.getName().getString(), followers.size());
            }
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[SimpleLeashedTeleport] Error in player tick: {}", t.getMessage());
        }
    }
    
    /**
     * Test teleport command handler
     */
    @SubscribeEvent
    public static void onTeleportCommand(net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Teleport command detected for player: {}", player.getName().getString());
        
        try {
            List<UUID> followers = findLeashedFollowers(player);
            if (!followers.isEmpty()) {
                BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Found {} leashed followers for teleport", followers.size());
                PENDING_FOLLOWERS.put(player.getUUID(), followers);
            }
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[SimpleLeashedTeleport] Error in teleport command: {}", t.getMessage());
        }
    }
    
    /**
     * Post-teleport processing
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Player {} changed dimension", player.getName().getString());
        
        List<UUID> followers = PENDING_FOLLOWERS.remove(player.getUUID());
        if (followers != null && !followers.isEmpty()) {
            BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Processing {} pending followers for dimension change", followers.size());
            // For now, just log that we would teleport them
            for (UUID followerId : followers) {
                BitsAndBalance.LOGGER.info("[SimpleLeashedTeleport] Would teleport follower: {}", followerId);
            }
        }
    }
    
    /**
     * Finds leashed mobs around the player
     */
    private static List<UUID> findLeashedFollowers(ServerPlayer player) {
        List<UUID> followers = new java.util.ArrayList<>();
        
        try {
            AABB searchArea = player.getBoundingBox().inflate(Config.leashedTeleportScanRadius);
            List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, searchArea);
            
            for (Mob mob : nearbyMobs) {
                // Check if mob is leashed to this player
                if (mob.isLeashed() && mob.getLeashHolder() == player) {
                    followers.add(mob.getUUID());
                    BitsAndBalance.LOGGER.debug("[SimpleLeashedTeleport] Found leashed follower: {} at {}", 
                        mob.getType().getDescription().getString(), mob.position());
                }
            }
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[SimpleLeashedTeleport] Failed to find leashed followers: {}", t.getMessage());
        }
        
        return followers;
    }
}
