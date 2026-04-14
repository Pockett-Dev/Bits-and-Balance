package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricSittingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerDimensionsSittingMixin {

    @Inject(
            method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void bitsandbalance$shrinkWhenSitting(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            if (pose != Pose.STANDING && pose != Pose.SITTING) return;

            if (!((Object) this instanceof Player self)) return;
            if (self.isPassenger()) return;
            try {
                if (self.isSwimming()) return;
            } catch (Throwable ignored) {
            }

            boolean sittingServer = false;
            try {
                sittingServer = FabricSittingState.isSitting(self.getUUID());
            } catch (Throwable ignored) {
            }

            boolean sittingClient = false;
            try {
                sittingClient = FabricClientState.sitting || FabricClientState.SYNCED_SITTING_PLAYERS.contains(self.getUUID());
            } catch (Throwable ignored) {
            }

            if (!(sittingServer || sittingClient)) return;

            EntityDimensions original = cir.getReturnValue();
            float width = original.width();
            float height = Math.max(0.9F, original.height() - 0.5F);
            cir.setReturnValue(EntityDimensions.scalable(width, height));
        } catch (Throwable ignored) {
        }
    }
}
