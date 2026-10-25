package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.tweaks.ReturnToKillerTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityReturnToKillerMixin {
    @Unique
    private static final double RETURN_TO_KILLER_MIN_SPEED = 0.06D;
    @Unique
    private static final double RETURN_TO_KILLER_MAX_SPEED = 0.5D;
    @Unique
    private static final double RETURN_TO_KILLER_FULL_SPEED_DISTANCE = 8.0D;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$returnToKillerTick(CallbackInfo ci) {
        if (!FabricTweaksConfig.enableReturnToKiller) return;

        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        String killerUuidString = ReturnToKillerTags.getKillerUuidString(self);
        if (killerUuidString == null || killerUuidString.isEmpty()) return;

        // Float to the killer (no gravity) while tagged.
        self.setNoGravity(true);

        long expiresAt = ReturnToKillerTags.getExpiresAt(self);
        if (expiresAt > 0L && level.getGameTime() > expiresAt) {
            self.setNoGravity(false);
            ReturnToKillerTags.clear(self);
            return;
        }

        UUID killerUuid;
        try {
            killerUuid = UUID.fromString(killerUuidString);
        } catch (IllegalArgumentException ignored) {
            self.setNoGravity(false);
            ReturnToKillerTags.clear(self);
            return;
        }
        Player killer = level.getPlayerByUUID(killerUuid);
        if (killer == null || !killer.isAlive()) {
            self.setNoGravity(false);
            ReturnToKillerTags.clear(self);
            return;
        }

        Vec3 target = killer.position().add(0.0D, 0.5D, 0.0D);
        Vec3 toTarget = target.subtract(self.position());
        double dist = toTarget.length();
        if (dist < 0.25D) return;

        Vec3 dir = toTarget.scale(1.0D / dist);
        double speed = bitsandbalance$distanceScaledSpeed(dist, RETURN_TO_KILLER_MIN_SPEED, RETURN_TO_KILLER_MAX_SPEED, RETURN_TO_KILLER_FULL_SPEED_DISTANCE);

        Vec3 newVel = bitsandbalance$steerTowardTarget(self.getDeltaMovement(), dir, speed);
        self.setDeltaMovement(newVel);
    }

    @Unique
    private static Vec3 bitsandbalance$steerTowardTarget(Vec3 currentVelocity, Vec3 direction, double speed) {
        Vec3 velocity = currentVelocity.scale(0.85D).add(direction.scale(speed));
        double speedLimitSqr = speed * speed;
        if (velocity.lengthSqr() > speedLimitSqr) {
            return velocity.normalize().scale(speed);
        }
        return velocity;
    }

    @Unique
    private static double bitsandbalance$distanceScaledSpeed(double distance, double minSpeed, double maxSpeed, double fullSpeedDistance) {
        double normalized = Math.max(0.0D, Math.min(1.0D, distance / Math.max(1.0E-4D, fullSpeedDistance)));
        double eased = 1.0D - ((1.0D - normalized) * (1.0D - normalized));
        return minSpeed + ((maxSpeed - minSpeed) * eased);
    }
}