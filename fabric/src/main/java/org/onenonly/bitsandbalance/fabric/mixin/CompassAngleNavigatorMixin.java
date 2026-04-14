package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompassAngle.class)
public class CompassAngleNavigatorMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void bitsAndBalance$navigatorCompassAngle(ItemStack stack, ClientLevel level, ItemOwner owner, int seed, CallbackInfoReturnable<Float> cir) {
        if (level == null || owner == null) return;

        BlockPos navTarget = NavigatorCompassItemData.getTargetPos(stack);
        String navDim = NavigatorCompassItemData.getTargetDimensionId(stack);
        if (navTarget == null || navDim == null) return;

        String currentDim;
        try {
            currentDim = level.dimension().identifier().toString();
        } catch (Throwable t) {
            return;
        }

        if (!navDim.equals(currentDim)) {
            cir.setReturnValue(spinningAngle(level));
            return;
        }

        cir.setReturnValue(computeAngle(owner, navTarget));
    }

    private static float computeAngle(ItemOwner owner, BlockPos targetPos) {
        double dx = ((double) targetPos.getX() + 0.5D) - owner.position().x;
        double dz = ((double) targetPos.getZ() + 0.5D) - owner.position().z;
        double angle = Math.atan2(dz, dx) / (Math.PI * 2D);
        double rotation = (double) owner.getVisualRotationYInDegrees() / 360.0D;
        return (float) Mth.positiveModulo(0.5D - (rotation - 0.25D - angle), 1.0D);
    }

    private static float spinningAngle(ClientLevel level) {
        long t;
        try {
            t = level.getGameTime();
        } catch (Throwable ignored) {
            t = 0L;
        }
        return (float) Mth.positiveModulo((double) (t % 360L) / 360.0D, 1.0D);
    }
}
