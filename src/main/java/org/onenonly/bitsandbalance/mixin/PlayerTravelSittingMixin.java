package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.client.ClientState;
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
    private Vec3 rebalance$disableHorizontalTravelWhenSitting(Vec3 original) {
        try {
            Player self = (Player) (Object) this;
            boolean sittingServer = BitsAndBalance.SITTING_PLAYERS.contains(self.getUUID());
            boolean sittingClient = false;
            try {
                // Client-only state; safe to reference on dedicated servers due to try/catch
                sittingClient = ClientState.sitting || ClientState.SYNCED_SITTING_PLAYERS.contains(self.getUUID());
            } catch (Throwable ignored) {}
            boolean sitting = sittingServer || sittingClient;
            if (!sitting) return original;
            // Zero horizontal input; keep Y to allow normal vertical physics handling within travel
            return new Vec3(0.0D, original.y, 0.0D);
        } catch (Throwable ignored) {
            return original;
        }
    }
}
