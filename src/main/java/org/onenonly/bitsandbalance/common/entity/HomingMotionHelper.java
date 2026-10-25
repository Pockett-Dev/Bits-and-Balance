package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.world.phys.Vec3;

public final class HomingMotionHelper {
    private HomingMotionHelper() {
    }

    public static Vec3 steerTowardTarget(Vec3 currentVelocity, Vec3 toTarget, double drag, double minSpeed, double maxSpeed, double fullSpeedDistance) {
        double distance = toTarget.length();
        if (distance < 1.0E-4D) {
            return currentVelocity.scale(drag);
        }

        Vec3 direction = toTarget.scale(1.0D / distance);
        double speed = easedSpeed(distance, minSpeed, maxSpeed, fullSpeedDistance);
        return currentVelocity.scale(drag).add(direction.scale(speed));
    }

    public static double easedSpeed(double distance, double minSpeed, double maxSpeed, double fullSpeedDistance) {
        if (distance <= 0.0D) {
            return 0.0D;
        }

        double normalized = Math.max(0.0D, Math.min(1.0D, distance / Math.max(1.0E-4D, fullSpeedDistance)));
        double eased = 1.0D - ((1.0D - normalized) * (1.0D - normalized));
        return minSpeed + ((maxSpeed - minSpeed) * eased);
    }
}