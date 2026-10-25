package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricSittingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Prevent horizontal movement while the player is sitting by zeroing out the
 * X/Z components of the travel vector. Gravity and vertical physics still apply.
 */
@Mixin(Player.class)
public abstract class PlayerTravelSittingMixin {

    @ModifyVariable(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3 bitsandbalance$disableHorizontalTravelWhenSitting(Vec3 original) {
        try {
            Player self = (Player) (Object) this;
            if (self.isPassenger()) return original;
            boolean sittingServer = FabricSittingState.isSitting(self.getUUID());
            boolean sittingClient = false;
            try {
                sittingClient = FabricClientState.sitting || FabricClientState.SYNCED_SITTING_PLAYERS.contains(self.getUUID());
            } catch (Throwable ignored) {
            }
            if (!(sittingServer || sittingClient)) return original;
            return new Vec3(0.0D, original.y, 0.0D);
        } catch (Throwable ignored) {
            return original;
        }
    }
}
