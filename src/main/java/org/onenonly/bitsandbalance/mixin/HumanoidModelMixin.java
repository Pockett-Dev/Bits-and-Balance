package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.client.ClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client: apply sitting pose to player models
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void rebalance$applyVisualSitting(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;

        boolean sitting = ClientState.sitting || ClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
        if (!sitting) return;
        if (player.isPassenger()) return;
        // If we successfully set Pose.SITTING, vanilla already applies the correct seated pose.
        // Avoid double-applying our own leg rotations (which can make the model sink/clip).
        if (player.getPose() == Pose.SITTING) return;

        try {
            // Apply sitting pose - bend legs like when riding
            float xr = -1.4137167F; // ~-81 degrees
            float yr = 0.31415927F; // ~18 degrees
            float zr = 0.07853982F; // ~4.5 degrees

            this.rightLeg.xRot = xr;
            this.rightLeg.yRot = yr;
            this.rightLeg.zRot = zr;

            this.leftLeg.xRot = xr;
            this.leftLeg.yRot = -yr;
            this.leftLeg.zRot = -zr;
        } catch (Throwable ignored) {}
    }
}
