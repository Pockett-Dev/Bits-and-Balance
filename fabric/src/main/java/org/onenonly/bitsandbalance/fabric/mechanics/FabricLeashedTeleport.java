package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricLeashedTeleport {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-leashed-teleport");

    // Matches NeoForge module's default detection threshold.
    private static final double TELEPORT_THRESHOLD_DISTANCE = 32.0D;

    private static final Map<UUID, TrackingData> TRACKING = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingTeleport> PENDING = new ConcurrentHashMap<>();

    private FabricLeashedTeleport() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMechanicsConfig.enableLeashedTeleport) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                onPlayerTick(player);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TRACKING.clear());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PENDING.clear());
    }

    /**
     * Called by mixins before a player teleport occurs.
     * This exists to reliably catch short-range teleports (chorus fruit, ender pearls, etc.)
     * that won't exceed the distance threshold used by the tick-based heuristic.
     */
    public static void capturePreTeleport(ServerPlayer player) {
        if (!FabricMechanicsConfig.enableLeashedTeleport) return;

        List<UUID> followers = findLeashedFollowers(player);
        if (followers.isEmpty()) return;

        ServerLevel fromLevel = (ServerLevel) player.level();
        PENDING.put(player.getUUID(), new PendingTeleport(fromLevel.dimension(), player.position(), fromLevel.getGameTime(), followers));
    }

    /**
     * Called by mixins after a player teleport occurs.
     */
    public static void processPostTeleport(ServerPlayer player) {
        if (!FabricMechanicsConfig.enableLeashedTeleport) return;

        PendingTeleport pending = PENDING.remove(player.getUUID());
        if (pending == null || pending.followerIds.isEmpty()) return;

        TrackingData snapshot = new TrackingData(pending.dimension, pending.position, pending.tick, pending.followerIds, pending.tick);
        processFollowers(player, snapshot);

        // Prevent the tick-based heuristic from immediately re-processing this teleport.
        ServerLevel currentLevel = (ServerLevel) player.level();
        TRACKING.compute(player.getUUID(), (id, prev) -> {
            long tick = currentLevel.getGameTime();
            return new TrackingData(currentLevel.dimension(), player.position(), tick, findLeashedFollowers(player), tick);
        });
    }

    private static void onPlayerTick(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ServerLevel currentLevel = (ServerLevel) player.level();
        ResourceKey<Level> currentDim = currentLevel.dimension();
        Vec3 currentPos = player.position();
        long tick = currentLevel.getGameTime();

        TrackingData previous = TRACKING.get(playerId);

        // If we detect a teleport, use the *previous* snapshot (pre-teleport).
        if (previous != null && shouldProcessTeleport(player, previous, currentDim, currentPos, tick)) {
            if (FabricMechanicsConfig.leashedTeleportDebug) {
                boolean dimChanged = !currentDim.equals(previous.dimension);
                double distSq = currentPos.distanceToSqr(previous.position);
                LOGGER.info("Teleport detected: player={} dimChanged={} distSq={} followers={}",
                    player.getName().getString(), dimChanged, String.format("%.2f", distSq),
                        previous.followerIds == null ? 0 : previous.followerIds.size());
            }
            processFollowers(player, previous);
            previous = previous.withLastProcessedTick(tick);
        }

        // Update snapshot for next tick.
        List<UUID> followers = findLeashedFollowers(player);
        long lastProcessedTick = previous != null ? previous.lastProcessedTick : Long.MIN_VALUE;
        TRACKING.put(playerId, new TrackingData(currentDim, currentPos, tick, followers, lastProcessedTick));
    }

    private static boolean shouldProcessTeleport(ServerPlayer player, TrackingData previous, ResourceKey<Level> currentDim, Vec3 currentPos, long tick) {
        if (tick - previous.lastProcessedTick < FabricMechanicsConfig.leashedTeleportCooldownTicks) return false;

        boolean dimensionChanged = !currentDim.equals(previous.dimension);
        if (dimensionChanged && !FabricMechanicsConfig.leashedTeleportAllowCrossDimension) return false;

        if (dimensionChanged) return true;

        double distSq = currentPos.distanceToSqr(previous.position);
        return distSq > (TELEPORT_THRESHOLD_DISTANCE * TELEPORT_THRESHOLD_DISTANCE);
    }

    private static List<UUID> findLeashedFollowers(ServerPlayer player) {
        List<UUID> out = new ArrayList<>();

        double r = FabricMechanicsConfig.leashedTeleportScanRadius;
        AABB search = player.getBoundingBox().inflate(r);

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, search)) {
            if (!mob.isLeashed()) continue;
            if (mob.getLeashHolder() != player) continue;
            if (mob.isPassenger()) continue;

            out.add(mob.getUUID());
            if (out.size() >= FabricMechanicsConfig.leashedTeleportMaxFollowers) break;
        }

        return out;
    }

    private static void processFollowers(ServerPlayer player, TrackingData pending) {
        if (pending.followerIds == null || pending.followerIds.isEmpty()) return;

        ServerLevel destinationLevel = (ServerLevel) player.level();
        boolean crossDim = !pending.dimension.equals(destinationLevel.dimension());
        if (crossDim && !FabricMechanicsConfig.leashedTeleportAllowCrossDimension) return;

        Vec3 playerPos = player.position();

        for (UUID followerId : pending.followerIds) {
            Entity entity = findEntityByUUID(player, followerId, pending.dimension, destinationLevel);
            if (!(entity instanceof Mob mob)) continue;

            Vec3 safePos = findSafePlacement(destinationLevel, playerPos, mob);
            if (safePos == null) safePos = playerPos;

            Mob teleportedMob = teleportFollower(mob, destinationLevel, safePos.x, safePos.y, safePos.z);
            if (teleportedMob == null) continue;

            if (FabricMechanicsConfig.leashedTeleportDebug) {
                LOGGER.info("Teleported leashed follower: mobType={} uuid={} to=({}, {}, {}) dim={}",
                        teleportedMob.getType().toString(), followerId,
                        String.format("%.2f", safePos.x), String.format("%.2f", safePos.y), String.format("%.2f", safePos.z),
                    destinationLevel.dimension().identifier());
            }

            if (FabricMechanicsConfig.leashedTeleportPostTeleportLeash) {
                teleportedMob.setLeashedTo(player, true);
            }
        }
    }

        private record PendingTeleport(
            ResourceKey<Level> dimension,
            Vec3 position,
            long tick,
            List<UUID> followerIds
        ) {
        }

    private static Entity findEntityByUUID(ServerPlayer player, UUID entityId, ResourceKey<Level> fromDim, ServerLevel destinationLevel) {
        Entity entity = destinationLevel.getEntity(entityId);
        if (entity != null) return entity;

        ServerLevel fromLevel = destinationLevel.getServer().getLevel(fromDim);
        if (fromLevel == null) return null;

        return fromLevel.getEntity(entityId);
    }

    private static Mob teleportFollower(Mob mob, ServerLevel destinationLevel, double x, double y, double z) {
        // Same-dimension is cheap and reliable.
        if (mob.level() == destinationLevel) {
            try {
                boolean ok = mob.teleportTo(destinationLevel, x, y, z, Set.of(), mob.getYRot(), mob.getXRot(), true);
                return ok ? mob : null;
            } catch (Throwable ignored) {
                mob.teleportTo(x, y, z);
                return mob;
            }
        }

        // Cross-dimension: vanilla-style dimension travel for non-player entities is not guaranteed to work via
        // teleportTo(level, ...). Recreate the mob in the destination dimension and copy state.
        try {
            var mobType = mob.getType();

            // Keep the original entity object around long enough to copy data.
            mob.remove(Entity.RemovalReason.CHANGED_DIMENSION);

            Entity created = mobType.create(destinationLevel, EntitySpawnReason.DIMENSION_TRAVEL);
            if (!(created instanceof Mob newMob)) return null;

            newMob.restoreFrom(mob);
            newMob.teleportTo(x, y, z);
            destinationLevel.addFreshEntity(newMob);
            return newMob;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Finds a safe placement location for a mob near the player.
     * Ported from NeoForge's LeashedTeleportModule with Fabric config values.
     */
    public static Vec3 findSafePlacement(ServerLevel level, Vec3 playerPos, Mob mob) {
        double baseRadius = FabricMechanicsConfig.leashedTeleportBaseRadius;
        double maxRadius = FabricMechanicsConfig.leashedTeleportMaxRadius;
        int maxTries = FabricMechanicsConfig.leashedTeleportSafePlacementTries;

        for (int attempt = 0; attempt < maxTries; attempt++) {
            double radius = baseRadius + (maxRadius - baseRadius) * (attempt / (double) maxTries);
            double angle = (attempt * 2 * Math.PI) / maxTries;
            double x = playerPos.x + radius * Math.cos(angle);
            double z = playerPos.z + radius * Math.sin(angle);
            double y = playerPos.y;

            for (int yOffset = 0; yOffset <= 3; yOffset++) {
                double testY = y + yOffset;
                BlockPos testPos = BlockPos.containing(x, testY, z);

                if (isSafePosition(level, testPos, mob)) {
                    return new Vec3(x, testY, z);
                }
            }
        }

        return null;
    }

    private static boolean isSafePosition(ServerLevel level, BlockPos pos, Mob mob) {
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;
        if (pos.getY() < 0) return false;

        AABB mobAABB = mob.getBoundingBox().move(
                pos.getX() + 0.5 - mob.getX(),
                pos.getY() - mob.getY(),
                pos.getZ() + 0.5 - mob.getZ()
        );

        return level.noCollision(mobAABB);
    }

    private static final class TrackingData {
        private final ResourceKey<Level> dimension;
        private final Vec3 position;
        private final long tick;
        private final List<UUID> followerIds;
        private final long lastProcessedTick;

        private TrackingData(ResourceKey<Level> dimension, Vec3 position, long tick, List<UUID> followerIds, long lastProcessedTick) {
            this.dimension = dimension;
            this.position = position;
            this.tick = tick;
            this.followerIds = followerIds;
            this.lastProcessedTick = lastProcessedTick;
        }

        private TrackingData withLastProcessedTick(long v) {
            return new TrackingData(this.dimension, this.position, this.tick, this.followerIds, v);
        }
    }
}
