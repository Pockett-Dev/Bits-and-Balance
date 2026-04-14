package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.entity.HomingMotionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityReturnToKillerMixin {
    private static final String KEY_KILLER_UUID = "bitsandbalance:return_to_killer_uuid";
    private static final String KEY_EXPIRES_AT = "bitsandbalance:return_to_killer_expires";
    private static final double RETURN_TO_KILLER_MIN_SPEED = 0.015D;
    private static final double RETURN_TO_KILLER_MAX_SPEED = 0.25D;
    private static final double RETURN_TO_KILLER_FULL_SPEED_DISTANCE = 8.0D;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$returnToKillerTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        if (bitsandbalance$stepDropHomeTick(self, level)) {
            return;
        }

        if (!Config.enableReturnToKiller) return;

        var data = self.getPersistentData();
        String killerUuidString = data.getString(KEY_KILLER_UUID).orElse("");
        if (killerUuidString.isEmpty()) return;

        // Float to the killer (no gravity) while tagged.
        self.setNoGravity(true);

        long expiresAt = data.getLong(KEY_EXPIRES_AT).orElse(0L);
        if (expiresAt > 0L && level.getGameTime() > expiresAt) {
            self.setNoGravity(false);
            data.remove(KEY_KILLER_UUID);
            data.remove(KEY_EXPIRES_AT);
            return;
        }

        UUID killerUuid;
        try {
            killerUuid = UUID.fromString(killerUuidString);
        } catch (IllegalArgumentException ignored) {
            self.setNoGravity(false);
            data.remove(KEY_KILLER_UUID);
            data.remove(KEY_EXPIRES_AT);
            return;
        }
        Player killer = level.getPlayerByUUID(killerUuid);
        if (killer == null || !killer.isAlive()) {
            self.setNoGravity(false);
            data.remove(KEY_KILLER_UUID);
            data.remove(KEY_EXPIRES_AT);
            return;
        }

        Vec3 target = killer.position().add(0.0D, 0.5D, 0.0D);
        Vec3 toTarget = target.subtract(self.position());
        double dist = toTarget.length();
        if (dist < 0.25D) return;

        Vec3 newVel = HomingMotionHelper.steerTowardTarget(
            self.getDeltaMovement(),
            toTarget,
            0.85D,
            RETURN_TO_KILLER_MIN_SPEED,
            RETURN_TO_KILLER_MAX_SPEED,
            RETURN_TO_KILLER_FULL_SPEED_DISTANCE);
        self.setDeltaMovement(newVel);
    }

    private static boolean bitsandbalance$stepDropHomeTick(ItemEntity self, ServerLevel level) {
        String killerUuidString = StepBlock.bitsandbalance$getStepDropHomeUuid(self);
        if (killerUuidString == null || killerUuidString.isEmpty()) {
            return false;
        }

        self.setNoGravity(true);
        StepBlock.bitsandbalance$setStepDropNoPhysics(self, true);

        long expiresAt = StepBlock.bitsandbalance$getStepDropHomeExpiresAt(self);
        if (expiresAt > 0L && level.getGameTime() > expiresAt) {
            self.setNoGravity(false);
            StepBlock.bitsandbalance$setStepDropNoPhysics(self, false);
            StepBlock.bitsandbalance$clearStepDropHoming(self);
            return true;
        }

        UUID killerUuid;
        try {
            killerUuid = UUID.fromString(killerUuidString);
        } catch (IllegalArgumentException ignored) {
            self.setNoGravity(false);
            StepBlock.bitsandbalance$setStepDropNoPhysics(self, false);
            StepBlock.bitsandbalance$clearStepDropHoming(self);
            return true;
        }

        Player killer = level.getPlayerByUUID(killerUuid);
        if (killer == null || !killer.isAlive()) {
            self.setNoGravity(false);
            StepBlock.bitsandbalance$setStepDropNoPhysics(self, false);
            StepBlock.bitsandbalance$clearStepDropHoming(self);
            return true;
        }

        Vec3 toTarget = new Vec3(
                killer.getX() - self.getX(),
                0.0D,
                killer.getZ() - self.getZ());
        double dist = toTarget.length();
        if (dist < 0.25D) {
            self.setDeltaMovement(self.getDeltaMovement().multiply(0.6D, 0.0D, 0.6D));
            return true;
        }

        Vec3 dir = toTarget.scale(1.0D / dist);
        double speed = Math.min(0.25D, 0.06D + dist * 0.015D);
        Vec3 current = self.getDeltaMovement();
        Vec3 newVel = new Vec3(
                current.x * 0.85D + dir.x * speed,
                Math.min(0.0D, current.y * 0.35D),
                current.z * 0.85D + dir.z * speed);
        self.setDeltaMovement(newVel);
        return true;
    }
}
