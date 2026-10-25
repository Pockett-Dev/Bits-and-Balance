package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompassItemPropertyFunction.class)
public abstract class CompassAngleNavigatorMixin {
    @Shadow
    private float getRandomlySpinningRotation(int seed, long gameTime) {
        throw new AssertionError();
    }

    @Shadow
    private float getRotationTowardsCompassTarget(Entity entity, long gameTime, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "getCompassRotation", at = @At("HEAD"), cancellable = true)
    private void bitsAndBalance$navigatorCompassAngle(ItemStack stack, ClientLevel level, int seed, Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!FabricMechanicsConfig.enableNavigatorCompass) return;
        if (level == null || entity == null) return;

        BlockPos navTarget = NavigatorCompassItemData.getTargetPos(stack);
        String navDim = NavigatorCompassItemData.getTargetDimensionId(stack);
        if (navTarget == null || navDim == null) return;

        String currentDim;
        try {
            currentDim = level.dimension().location().toString();
        } catch (Throwable t) {
            return;
        }

        long gameTime = level.getGameTime();
        if (!navDim.equals(currentDim)) {
            cir.setReturnValue(getRandomlySpinningRotation(seed, gameTime));
            return;
        }

        cir.setReturnValue(getRotationTowardsCompassTarget(entity, gameTime, navTarget));
    }
}
