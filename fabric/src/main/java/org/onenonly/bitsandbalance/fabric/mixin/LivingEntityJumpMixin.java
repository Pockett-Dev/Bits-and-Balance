package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricSittingState;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCoyoteTimeJump;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent jumping while sitting and allow coyote time jumps.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$handleJumpConditions(CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof Player p)) return;
            if (p.isPassenger()) return;

            // Check if sitting - cancel jump if sitting
            boolean sittingServer = FabricSittingState.isSitting(p.getUUID());
            boolean sittingClient = false;
            try {
                sittingClient = FabricClientState.sitting || FabricClientState.SYNCED_SITTING_PLAYERS.contains(p.getUUID());
            } catch (Throwable ignored) {
            }

            if (sittingServer || sittingClient) {
                ci.cancel();
                return;
            }

            // Check if player is not on ground but can use coyote time
            if (!p.onGround() && FabricCoyoteTimeJump.canUseCoyoteTime(p)) {
                // Allow the jump to proceed, but mark coyote time as used
                FabricCoyoteTimeJump.markCoyoteTimeUsed(p);
            }
        } catch (Throwable ignored) {
        }
    }
}
