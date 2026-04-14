package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.client.ClientState;
import org.onenonly.bitsandbalance.tweaks.CoyoteTimeJump;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent jumping while sitting.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void rebalance$cancelJumpWhenSitting(CallbackInfo ci) {
        try {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof Player p)) return;
            boolean sittingServer = BitsAndBalance.SITTING_PLAYERS.contains(p.getUUID());
            boolean sittingClient = false;
            try {
                sittingClient = ClientState.sitting || ClientState.SYNCED_SITTING_PLAYERS.contains(p.getUUID());
            } catch (Throwable ignored) {}
            if (sittingServer || sittingClient) {
                ci.cancel();
                return;
            }

            // If this is a mid-air jump triggered during our coyote-time window, consume it.
            if (!p.onGround() && CoyoteTimeJump.canUseCoyoteTime(p)) {
                CoyoteTimeJump.useCoyoteTime(p);
            }
        } catch (Throwable ignored) {}
    }
}
