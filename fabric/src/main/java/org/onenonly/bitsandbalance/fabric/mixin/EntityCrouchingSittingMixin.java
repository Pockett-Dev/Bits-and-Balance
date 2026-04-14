package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client: suppress crouch animation for players marked as sitting.
 *
 * In 1.21.10, RemotePlayer does not declare its own isCrouching(); it inherits Entity.isCrouching().
 */
@Mixin(Entity.class)
public abstract class EntityCrouchingSittingMixin {

    @Inject(method = "isCrouching()Z", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$suppressCrouchWhenSitting(CallbackInfoReturnable<Boolean> cir) {
        try {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof Player player)) return;
            if (player.isPassenger()) return;

            Minecraft mc = Minecraft.getInstance();
            boolean isLocal = mc != null && mc.player != null && mc.player.getUUID().equals(player.getUUID());

            if (isLocal) {
                if (FabricClientState.sitting) {
                    cir.setReturnValue(false);
                }
                return;
            }

            if (FabricClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID())) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }
}
