package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leashed Teleport Module
 * 
 * Allows leashed mobs to follow players through any type of teleportation,
 * including commands, chorus fruit, ender pearls, portals, and modded teleports.
 * 
 * Features:
 * - Comprehensive teleport event coverage (pre and post events)
 * - Cross-dimension support with safe placement
 * - Configurable limits and safety parameters
 * - Fallback detection for exotic teleports
 * - Safe placement algorithm to prevent suffocation/void placement
 * - Automatic leash re-attachment after teleportation
 * 
 * How it works:
 * 1. Pre-teleport events capture leashed mobs around the player
 * 2. Mob UUIDs are stored in a data attachment with expiration
 * 3. Post-teleport events move the mobs to safe locations near the player
 * 4. Leashes are re-attached to maintain the connection
 * 5. Fallback system catches teleports that bypass standard events
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class LeashedTeleportModule implements FeatureModule {
    
    // Static initialization to ensure the class is loaded
    static {
        BitsAndBalance.LOGGER.info("[LeashedTeleport] Class loaded - checking configuration...");
    }
    
    // Reference to the registered data attachment
    private static final net.neoforged.neoforge.attachment.AttachmentType<PendingFollowers> PENDING_FOLLOWERS = 
        LeashedTeleportAttachments.PENDING_FOLLOWERS;
    
    // Per-player cooldown tracking to prevent spam processing
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    
    // Per-player last position tracking for fallback detection
    private static final Map<UUID, PlayerPositionSnapshot> LAST_POSITIONS = new ConcurrentHashMap<>();
    
    // Enhanced player tracking for exotic teleport detection
    private static final Map<UUID, PlayerTrackingData> PLAYER_TRACKING = new ConcurrentHashMap<>();
    
    // Teleport detection constants
    private static final double TELEPORT_THRESHOLD_DISTANCE = 32.0; // blocks - reduced for better detection
    
    @Override
    public String getFeatureName() {
        return "Leashed Teleport";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.enableLeashedTeleport;
    }
    
    @Override
    public int getInitializationPriority() {
        return 500; // Medium priority - pet enhancement feature
    }
    
    @Override
    public void initialize() {
        BitsAndBalance.LOGGER.info("[LeashedTeleport] Module initialized - enabled: {}", Config.enableLeashedTeleport);
    }
    
    /**
     * Test event handler to verify the module is working
     */
    @SubscribeEvent
    public static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        BitsAndBalance.LOGGER.info("[LeashedTeleport] Server starting - module is active! Enabled: {}", Config.enableLeashedTeleport);
    }
    
    @Override
    public void cleanup() {
        // Clear cached data on mod shutdown
        PLAYER_COOLDOWNS.clear();
        LAST_POSITIONS.clear();
        PLAYER_TRACKING.clear();
        BitsAndBalance.LOGGER.debug("[LeashedTeleport] Cleaned up cached data");
    }

    /**
     * Fallback hook for teleports that don't fire NeoForge teleport events.
     * Intended to be called from a mixin on ServerPlayer.teleportTo.
     */
    public static void onPlayerTeleportedByMixin(ServerPlayer player) {
        if (!Config.enableLeashedTeleport) return;

        snapshotLeashedFollowers(player, "serverplayer_teleport");

        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(2, () -> processPendingFollowers(player)));
        }
    }
    
    /**
     * Snapshot of player position for teleport detection
     */
    private record PlayerPositionSnapshot(
        ResourceKey<Level> dimension,
        Vec3 position,
        long tickTime
    ) {}
    
    /**
     * Enhanced player tracking data for exotic teleport detection
     */
    private record PlayerTrackingData(
        ResourceKey<Level> lastDimension,
        Vec3 lastPosition,
        long lastTickTime,
        List<UUID> lastLeashedFollowers,
        long lastFallbackProcessed
    ) {}
    
    // NeoForge 1.21.10: EntityTeleportEvent.ChorusFruit no longer exists in this mapping.
    // Chorus fruit teleports are currently handled via the generic teleport handler.
    
    /**
     * Pre-teleport event handler for ender pearl teleportation
     */
    @SubscribeEvent
    public static void onEnderPearlTeleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        snapshotLeashedFollowers(player, "ender_pearl");
        
        // Schedule post-teleport processing
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(2, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Pre-teleport event handler for teleport command
     */
    @SubscribeEvent
    public static void onTeleportCommand(net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        snapshotLeashedFollowers(player, "teleport_command");
        
        // Schedule post-teleport processing
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(2, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Pre-teleport event handler for spread players command
     */
    @SubscribeEvent
    public static void onSpreadPlayersCommand(net.neoforged.neoforge.event.entity.EntityTeleportEvent.SpreadPlayersCommand event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        snapshotLeashedFollowers(player, "spread_players_command");
        
        // Schedule post-teleport processing
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(2, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Pre-dimension travel event handler for portals and modded teleports
     */
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Config.leashedTeleportAllowCrossDimension) return;
        
        snapshotLeashedFollowers(player, "dimension_travel");
        
        // Schedule post-teleport processing for dimension travel
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(3, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Post-dimension change event handler
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        processPendingFollowers(player);
    }
    
    /**
     * Entity join level event handler for post-teleport processing
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Small delay to ensure the player is fully loaded
        var server = event.getLevel().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(1, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Generic teleport event handler for modded teleportation
     * This catches any teleportation that might not be covered by specific events
     */
    @SubscribeEvent
    public static void onGenericTeleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Skip if this is already handled by specific events
        if (event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl ||
            event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand ||
            event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.SpreadPlayersCommand) {
            return;
        }
        
        
        snapshotLeashedFollowers(player, "generic_teleport");
        
        // Schedule post-teleport processing
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(2, () -> {
                processPendingFollowers(player);
            }));
        }
    }
    
    /**
     * Player respawn event handler for respawn teleportation
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        
        // For respawn, we don't snapshot beforehand, but we can try to find existing followers
        // This is more of a "catch-up" mechanism for respawns
        var server = player.level().getServer();
        if (server != null) {
            server.execute(new net.minecraft.server.TickTask(5, () -> {
                // Try to find and teleport any leashed followers that might still be around
                List<UUID> followers = findLeashedFollowers(player);
                if (!followers.isEmpty()) {
                    
                    // Create a temporary pending followers for respawn
                    ResourceKey<Level> fromDim = player.level().dimension();
                    Vec3 prePos = player.position();
                    long currentTime = player.level().getGameTime();
                    long expiresAt = currentTime + 200; // 10 seconds TTL
                    long teleportNonce = currentTime + player.getUUID().hashCode();
                    
                    PendingFollowers pending = PendingFollowers.create(
                        followers, fromDim, expiresAt, prePos, teleportNonce
                    );
                    
                    teleportFollowers(player, pending);
                }
            }));
        }
    }
    
    /**
     * Enhanced fallback teleport detection using player tick events
     * This is our catch-all safety net for exotic teleports that bypass standard events
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.enableLeashedTeleport) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Update every tick for maximum responsiveness to exotic teleports
        try {
            updateEnhancedPlayerTracking(player);
            detectExoticTeleport(player);
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[LeashedTeleport] Error in exotic teleport detection: {}", t.getMessage());
        }
    }
    
    /**
     * Snapshots leashed followers around the player before teleportation
     */
    private static void snapshotLeashedFollowers(ServerPlayer player, String teleportType) {
        try {
            // Check cooldown
            UUID playerId = player.getUUID();
            long currentTime = player.level().getGameTime();
            Long lastProcessed = PLAYER_COOLDOWNS.get(playerId);
            if (lastProcessed != null && currentTime - lastProcessed < Config.leashedTeleportCooldownTicks) {
                return;
            }
            
            // Find leashed mobs around the player
            List<UUID> followerIds = findLeashedFollowers(player);
            if (followerIds.isEmpty()) {
                return;
            }
            
            // Create pending followers data
            ResourceKey<Level> fromDim = player.level().dimension();
            Vec3 prePos = player.position();
            long expiresAt = currentTime + 200; // 10 seconds TTL
            long teleportNonce = currentTime + playerId.hashCode();
            
            PendingFollowers pending = PendingFollowers.create(
                followerIds, fromDim, expiresAt, prePos, teleportNonce
            );
            
            // Attach to player
            player.setData(PENDING_FOLLOWERS, pending);
            
            // Update cooldown
            PLAYER_COOLDOWNS.put(playerId, currentTime);
            
                
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[LeashedTeleport] Failed to snapshot followers: {}", t.getMessage());
        }
    }
    
    /**
     * Finds leashed mobs around the player
     */
    private static List<UUID> findLeashedFollowers(ServerPlayer player) {
        List<UUID> followers = new ArrayList<>();
        
        try {
            AABB searchArea = player.getBoundingBox().inflate(Config.leashedTeleportScanRadius);
            List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, searchArea);
            
            
            for (Mob mob : nearbyMobs) {
                // Check if mob is leashed to this player
                if (mob.isLeashed() && mob.getLeashHolder() == player) {
                    // Check blacklist
                    if (Config.leashedTeleportBlacklistTypes.contains(mob.getType())) {
                        continue;
                    }
                    
                    // Check max followers limit
                    if (followers.size() >= Config.leashedTeleportMaxFollowers) {
                        break;
                    }
                    
                    followers.add(mob.getUUID());
                }
            }
            
                
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[LeashedTeleport] Failed to find leashed followers: {}", t.getMessage());
        }
        
        return followers;
    }
    
    /**
     * Processes pending followers after player teleportation
     */
    private static void processPendingFollowers(ServerPlayer player) {
        try {
            PendingFollowers pending = player.getData(PENDING_FOLLOWERS);
            if (pending == null) {
                return;
            }
            
            if (!pending.hasFollowers()) {
                return;
            }
            
            // Check if expired
            long currentTime = player.level().getGameTime();
            if (pending.isExpired(currentTime)) {
                player.removeData(PENDING_FOLLOWERS);
                return;
            }
            
            // Teleport followers
            teleportFollowers(player, pending);
            
            // Clear the attachment
            player.removeData(PENDING_FOLLOWERS);
            
        } catch (Throwable t) {
            BitsAndBalance.LOGGER.warn("[LeashedTeleport] Failed to process pending followers: {}", t.getMessage());
            t.printStackTrace();
        }
    }
    
    /**
     * Teleports followers to safe locations near the player
     */
    private static void teleportFollowers(ServerPlayer player, PendingFollowers pending) {
        ServerLevel destinationLevel = (ServerLevel) player.level();
        Vec3 playerPos = player.position();
        
        for (UUID followerId : pending.followerIds()) {
            try {
                // Find the entity
                Entity entity = findEntityByUUID(followerId, pending.fromDimension(), destinationLevel);
                if (entity == null) {
                    continue;
                }
                
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                
                // Find safe placement location
                Vec3 safePos = findSafePlacement(destinationLevel, playerPos, mob);
                if (safePos == null) {
                    // Last resort: place at player feet
                    safePos = playerPos;
                }
                
                // Teleport the mob
                if (pending.fromDimension().equals(destinationLevel.dimension())) {
                    // Same dimension teleport
                    mob.teleportTo(safePos.x, safePos.y, safePos.z);
                } else {
                    // Cross-dimension teleport
                    if (Config.leashedTeleportAllowCrossDimension) {
                        // For cross-dimension teleportation, we need to:
                        // 1. Remove the mob from the old dimension
                        // 2. Create a new mob in the new dimension
                        // 3. Teleport the new mob to the safe position
                        
                        try {
                            var mobType = mob.getType();

                            // Remove from old dimension
                            mob.remove(net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION);

                            // Create new mob in destination dimension
                            var newEntity = mobType.create(destinationLevel, net.minecraft.world.entity.EntitySpawnReason.DIMENSION_TRAVEL);
                            if (newEntity instanceof Mob newMob) {
                                // Copy data from the old mob using 1.21's ValueInput/ValueOutput-backed implementation
                                newMob.restoreFrom(mob);

                                // Teleport to safe position
                                newMob.teleportTo(safePos.x, safePos.y, safePos.z);

                                // Add to destination level
                                destinationLevel.addFreshEntity(newMob);

                                // Update the mob reference for leash re-attachment
                                mob = newMob;
                            } else {
                                continue;
                            }
                        } catch (Throwable t) {
                            BitsAndBalance.LOGGER.warn("[LeashedTeleport] Cross-dimension teleport failed: {}", t.getMessage());
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                
                // Re-attach leash if configured
                if (Config.leashedTeleportPostTeleportLeash) {
                    mob.setLeashedTo(player, true);
                }
                    
            } catch (Throwable t) {
                BitsAndBalance.LOGGER.warn("[LeashedTeleport] Failed to teleport follower {}: {}", 
                    followerId, t.getMessage());
                t.printStackTrace();
            }
        }
    }
    
    /**
     * Finds an entity by UUID across dimensions
     */
    private static Entity findEntityByUUID(UUID entityId, ResourceKey<Level> fromDim, ServerLevel destinationLevel) {
        // Try destination level first
        Entity entity = destinationLevel.getEntity(entityId);
        if (entity != null) {
            return entity;
        }
        
        // Try source level
        ServerLevel fromLevel = destinationLevel.getServer().getLevel(fromDim);
        if (fromLevel != null) {
            entity = fromLevel.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        
        return null;
    }
    
    /**
     * Finds a safe placement location for a mob near the player
     */
    public static Vec3 findSafePlacement(ServerLevel level, Vec3 playerPos, Mob mob) {
        double baseRadius = Config.leashedTeleportBaseRadius;
        double maxRadius = Config.leashedTeleportMaxRadius;
        int maxTries = Config.leashedTeleportSafePlacementTries;
        
        for (int attempt = 0; attempt < maxTries; attempt++) {
            // Calculate radius for this attempt
            double radius = baseRadius + (maxRadius - baseRadius) * (attempt / (double) maxTries);
            
            // Try different angles around the player
            double angle = (attempt * 2 * Math.PI) / maxTries;
            double x = playerPos.x + radius * Math.cos(angle);
            double z = playerPos.z + radius * Math.sin(angle);
            double y = playerPos.y;
            
            // Try different Y levels
            for (int yOffset = 0; yOffset <= 3; yOffset++) {
                double testY = y + yOffset;
                BlockPos testPos = BlockPos.containing(x, testY, z);
                
                // Check if position is safe
                if (isSafePosition(level, testPos, mob)) {
                    return new Vec3(x, testY, z);
                }
            }
        }
        
        return null; // No safe position found
    }
    
    /**
     * Checks if a position is safe for mob placement
     */
    private static boolean isSafePosition(ServerLevel level, BlockPos pos, Mob mob) {
        // Check world border
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        
        // Check for collision
        AABB mobAABB = mob.getBoundingBox().move(
            pos.getX() + 0.5 - mob.getX(),
            pos.getY() - mob.getY(),
            pos.getZ() + 0.5 - mob.getZ()
        );
        if (!level.noCollision(mobAABB)) {
            return false;
        }
        
        // Check for void (Y < 0)
        if (pos.getY() < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Updates enhanced player tracking data for exotic teleport detection
     */
    private static void updateEnhancedPlayerTracking(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ResourceKey<Level> currentDim = player.level().dimension();
        Vec3 currentPos = player.position();
        long currentTick = player.level().getGameTime();
        
        // Get current leashed followers
        List<UUID> currentFollowers = findLeashedFollowers(player);
        
        // Update tracking data
        PlayerTrackingData trackingData = new PlayerTrackingData(
            currentDim,
            currentPos,
            currentTick,
            currentFollowers,
            0L // Will be updated when we process
        );
        
        PLAYER_TRACKING.put(playerId, trackingData);
        
        // Also update the legacy position snapshot for backward compatibility
        LAST_POSITIONS.put(playerId, new PlayerPositionSnapshot(currentDim, currentPos, currentTick));
    }
    
    /**
     * Enhanced exotic teleport detection with improved heuristics
     * This is our catch-all safety net for mods that move players without firing standard events
     */
    private static void detectExoticTeleport(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerTrackingData current = PLAYER_TRACKING.get(playerId);
        if (current == null) {
            return;
        }
        
        ResourceKey<Level> currentDim = player.level().dimension();
        Vec3 currentPos = player.position();
        long currentTick = player.level().getGameTime();
        
        // Debounce check - prevent spam processing
        if (currentTick - current.lastFallbackProcessed() < 20) { // 1 second debounce
            return;
        }
        
        // Check for dimension change (highest priority - always a teleport)
        boolean dimensionChanged = !current.lastDimension().equals(currentDim);
        
        // Check for huge instantaneous displacement (> 64-96 blocks in one tick)
        double distance = currentPos.distanceTo(current.lastPosition());
        boolean hugeDisplacement = distance > 64.0; // Conservative threshold for exotic teleports
        
        // Check for large movement over multiple ticks (fallback for slower teleports)
        long tickDifference = currentTick - current.lastTickTime();
        boolean largeMovement = distance > TELEPORT_THRESHOLD_DISTANCE && tickDifference <= 3; // Within 3 ticks
        
        if (dimensionChanged || hugeDisplacement || largeMovement) {
            // Check if we have pending followers from a recent pre-event
            PendingFollowers pending = player.getData(PENDING_FOLLOWERS);
            boolean hasRecentPending = pending != null && pending.hasFollowers() && !pending.isExpired(currentTick);
            
            // Check if player had leashed mobs last tick
            boolean hadLeashedMobs = !current.lastLeashedFollowers().isEmpty();
            
            if (hasRecentPending || hadLeashedMobs) {
                
                if (!hasRecentPending && hadLeashedMobs) {
                    // Create pending followers for fallback (player had leashed mobs but no pending data)
                    List<UUID> followers = findLeashedFollowers(player);
                    if (!followers.isEmpty()) {
                        ResourceKey<Level> fromDim = current.lastDimension();
                        Vec3 prePos = current.lastPosition();
                        long expiresAt = currentTick + 200; // 10 seconds TTL
                        long teleportNonce = currentTick + playerId.hashCode();
                        
                        PendingFollowers fallbackPending = PendingFollowers.create(
                            followers, fromDim, expiresAt, prePos, teleportNonce
                        );
                        
                        player.setData(PENDING_FOLLOWERS, fallbackPending);
                    }
                }
                
                // Process pending followers
                processPendingFollowers(player);
                
                // Update debounce timestamp
                PlayerTrackingData updatedTracking = new PlayerTrackingData(
                    current.lastDimension(),
                    current.lastPosition(),
                    current.lastTickTime(),
                    current.lastLeashedFollowers(),
                    currentTick
                );
                PLAYER_TRACKING.put(playerId, updatedTracking);
            }
        }
    }
}
