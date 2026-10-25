package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric port of NeoForge's Speedy Wolves mechanic.
 */
public final class FabricSpeedyWolves {
    private static final Identifier SPEEDY_WOLF_MOD_ID =
            Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "speedy_wolf_speed");

    private static final Map<UUID, PlayerMotionSample> LAST_PLAYER_MOTION = new ConcurrentHashMap<>();

    private static final double WALK_SPEED = 0.22D;
    private static final double SPRINT_SPEED = 0.28D;
    private static final double WOLF_SEARCH_RADIUS = 16.0D;
    private static final int UPDATE_INTERVAL = 5;

    private FabricSpeedyWolves() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMechanicsConfig.enableSpeedyWolves) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % UPDATE_INTERVAL != 0) continue;

                try {
                    updatePlayerMotionSample(player);
                    double horizSpeed = calculateHorizontalSpeed(player);
                    double speedBoostAmount = calculateSpeedBoostAmount(horizSpeed);
                    applySpeedBoostToNearbyWolves(player, speedBoostAmount);
                } catch (Throwable ignored) {
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> LAST_PLAYER_MOTION.clear());
    }

    private static void updatePlayerMotionSample(ServerPlayer player) {
        UUID id = player.getUUID();
        var prev = LAST_PLAYER_MOTION.get(id);
        if (prev == null) {
            LAST_PLAYER_MOTION.put(id, new PlayerMotionSample(player.getX(), player.getZ(), player.tickCount));
        }
    }

    private static double calculateHorizontalSpeed(ServerPlayer player) {
        double horizSpeed;

        try {
            var sample = LAST_PLAYER_MOTION.get(player.getUUID());
            if (sample != null) {
                int ticksDelta = player.tickCount - sample.tick();
                if (ticksDelta > 0) {
                    double dx = player.getX() - sample.x();
                    double dz = player.getZ() - sample.z();
                    double distanceTraveled = Math.sqrt(dx * dx + dz * dz);
                    horizSpeed = distanceTraveled / ticksDelta;
                } else {
                    Vec3 vel = player.getDeltaMovement();
                    horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                }
            } else {
                Vec3 vel = player.getDeltaMovement();
                horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            }
        } catch (Throwable t) {
            Vec3 vel = player.getDeltaMovement();
            horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        }

        LAST_PLAYER_MOTION.put(player.getUUID(), new PlayerMotionSample(player.getX(), player.getZ(), player.tickCount));
        return horizSpeed;
    }

    private static double calculateSpeedBoostAmount(double horizSpeed) {
        double maxMultiplier = FabricMechanicsConfig.speedyWolvesSpeedMultiplier;

        double speedRatio = Math.max(0.0D, (horizSpeed - WALK_SPEED) / (SPRINT_SPEED - WALK_SPEED));
        speedRatio = Math.min(1.0D, speedRatio);

        return speedRatio * (maxMultiplier - 1.0D);
    }

    private static void applySpeedBoostToNearbyWolves(ServerPlayer player, double speedBoostAmount) {
        double thresholdSqr = FabricMechanicsConfig.speedyWolvesDistanceThreshold * FabricMechanicsConfig.speedyWolvesDistanceThreshold;

        AABB area = player.getBoundingBox().inflate(WOLF_SEARCH_RADIUS);
        for (Entity e : player.level().getEntities(player, area)) {
            if (!(e instanceof Wolf wolf)) continue;
            if (!wolf.isTame() || !wolf.isOwnedBy(player)) continue;

            boolean shouldBoost = speedBoostAmount > 0.0D && wolf.distanceToSqr(player) > thresholdSqr;
            if (shouldBoost) {
                applySpeedyWolfBoost(wolf, speedBoostAmount);
            } else {
                removeSpeedyWolfBoost(wolf);
            }
        }
    }

    private static void applySpeedyWolfBoost(Wolf wolf, double amount) {
        AttributeInstance inst = wolf.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;

        var existing = inst.getModifier(SPEEDY_WOLF_MOD_ID);
        if (existing != null && Double.compare(existing.amount(), amount) == 0 && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }

        inst.addOrUpdateTransientModifier(new AttributeModifier(SPEEDY_WOLF_MOD_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeSpeedyWolfBoost(Wolf wolf) {
        AttributeInstance inst = wolf.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        inst.removeModifier(SPEEDY_WOLF_MOD_ID);
    }

    private record PlayerMotionSample(double x, double z, int tick) {
    }
}
