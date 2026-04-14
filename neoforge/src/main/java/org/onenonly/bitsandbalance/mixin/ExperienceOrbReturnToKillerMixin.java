package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbReturnToKillerMixin {
    private static final String KEY_KILLER_UUID = "bitsandbalance:return_to_killer_uuid";
    private static final String KEY_EXPIRES_AT = "bitsandbalance:return_to_killer_expires";

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$returnToKillerTick(CallbackInfo ci) {
        if (!Config.enableReturnToKiller) return;

        ExperienceOrb self = (ExperienceOrb) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

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

        Vec3 dir = toTarget.scale(1.0D / dist);
        double speed = Math.min(0.9D, 0.18D + dist * 0.05D);

        Vec3 newVel = self.getDeltaMovement().scale(0.85D).add(dir.scale(speed));
        self.setDeltaMovement(newVel);
    }
}
