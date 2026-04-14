package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Data attachment for tracking pending follower teleports.
 * 
 * This record stores information about leashed mobs that need to be teleported
 * when a player teleports. It includes the follower UUIDs, source dimension,
 * expiration time, and source position for safe placement calculations.
 */
public record PendingFollowers(
    List<UUID> followerIds,
    ResourceKey<Level> fromDimension,
    long expiresAtGameTime,
    Vec3 prePosition,
    long teleportNonce
) {
    
    /**
     * Creates a new PendingFollowers instance with the given parameters.
     * 
     * @param followerIds List of UUIDs of leashed mobs to teleport
     * @param fromDimension The dimension the player is teleporting from
     * @param expiresAtGameTime Game time when this pending teleport expires (in ticks)
     * @param prePosition The player's position before teleportation
     * @param teleportNonce Unique identifier for this teleport event
     * @return New PendingFollowers instance
     */
    public static PendingFollowers create(List<UUID> followerIds, ResourceKey<Level> fromDimension, 
                                        long expiresAtGameTime, Vec3 prePosition, long teleportNonce) {
        return new PendingFollowers(followerIds, fromDimension, expiresAtGameTime, prePosition, teleportNonce);
    }
    
    /**
     * Checks if this pending teleport has expired.
     * 
     * @param currentGameTime Current game time in ticks
     * @return true if expired, false otherwise
     */
    public boolean isExpired(long currentGameTime) {
        return currentGameTime > expiresAtGameTime;
    }
    
    /**
     * Gets the number of followers pending teleport.
     * 
     * @return Number of followers
     */
    public int getFollowerCount() {
        return followerIds.size();
    }
    
    /**
     * Checks if this pending teleport has any followers.
     * 
     * @return true if there are followers to teleport, false otherwise
     */
    public boolean hasFollowers() {
        return !followerIds.isEmpty();
    }
}
