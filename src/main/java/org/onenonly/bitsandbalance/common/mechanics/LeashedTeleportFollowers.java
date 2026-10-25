package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared follower-teleport helper used by teleport-style potion effects.
 */
public final class LeashedTeleportFollowers {
    private LeashedTeleportFollowers() {
    }

    public static void queueFollowerTeleport(ServerPlayer player, Vec3 destinationPos) {
        if (player == null || destinationPos == null || !LeashedTeleportRuntime.enabled) {
            return;
        }

        teleportFollowers(player, findLeashedFollowers(player), destinationPos);
    }

    public static List<UUID> captureLeashedFollowerIds(ServerPlayer player) {
        if (player == null || !LeashedTeleportRuntime.enabled) {
            return List.of();
        }

        return List.copyOf(findLeashedFollowers(player));
    }

    public static void teleportCapturedFollowers(ServerPlayer player, List<UUID> followerIds, Vec3 destinationPos) {
        if (player == null || destinationPos == null || followerIds == null || followerIds.isEmpty() || !LeashedTeleportRuntime.enabled) {
            return;
        }

        teleportFollowers(player, followerIds, destinationPos);
    }

    public static Vec3 findSafePlacement(ServerLevel level, Vec3 playerPos, Mob mob) {
        double baseRadius = LeashedTeleportRuntime.baseRadius;
        double maxRadius = LeashedTeleportRuntime.maxRadius;
        int maxTries = LeashedTeleportRuntime.safePlacementTries;

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

    private static List<UUID> findLeashedFollowers(ServerPlayer player) {
        List<UUID> followerIds = new ArrayList<>();

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return followerIds;
        }

        AABB searchArea = player.getBoundingBox().inflate(LeashedTeleportRuntime.scanRadius);
        List<Mob> nearbyMobs = serverLevel.getEntitiesOfClass(Mob.class, searchArea);

        for (Mob mob : nearbyMobs) {
            if (!mob.isLeashed() || mob.getLeashHolder() != player) {
                continue;
            }
            if (LeashedTeleportRuntime.isBlacklisted(mob.getType())) {
                continue;
            }
            if (followerIds.size() >= LeashedTeleportRuntime.maxFollowers) {
                break;
            }
            followerIds.add(mob.getUUID());
        }

        return followerIds;
    }

    private static void teleportFollowers(ServerPlayer player, List<UUID> followerIds, Vec3 destinationPos) {
        try {
            if (!(player.level() instanceof ServerLevel serverLevel) || followerIds.isEmpty()) {
                return;
            }

            for (UUID followerId : followerIds) {
                try {
                    Entity entity = serverLevel.getEntity(followerId);
                    if (!(entity instanceof Mob mob)) {
                        continue;
                    }

                    Vec3 safePos = findSafePlacement(serverLevel, destinationPos, mob);
                    if (safePos == null) {
                        safePos = destinationPos;
                    }

                    mob.teleportTo(safePos.x, safePos.y, safePos.z);
                    if (LeashedTeleportRuntime.postTeleportLeash) {
                        mob.setLeashedTo(player, true);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSafePosition(ServerLevel level, BlockPos pos, Mob mob) {
        if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() < 0) {
            return false;
        }

        AABB mobAABB = mob.getBoundingBox().move(
            pos.getX() + 0.5D - mob.getX(),
            pos.getY() - mob.getY(),
            pos.getZ() + 0.5D - mob.getZ()
        );
        return level.noCollision(mobAABB);
    }
}