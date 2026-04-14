package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.onenonly.bitsandbalance.common.candle.CandleBundleTintCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.blockentity.PistonHeadRenderer.class)
@SuppressWarnings("null")
public class PistonHeadRendererForcedPosMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL")
    )
    private void bitsandbalance$forceTintPosToDestination(PistonMovingBlockEntity be, PistonHeadRenderState renderState,
                                                         float partialTick, net.minecraft.world.phys.Vec3 cameraPos,
                                                         net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling,
                                                         CallbackInfo ci) {
        if (be == null || renderState == null) return;

        BlockPos destPos = be.getBlockPos();
        if (destPos == null) return;
        long destLong = destPos.asLong();

        MovingBlockRenderState block = renderState.block;
        if (block instanceof CandleBundleTintCarrier carrier) {
            carrier.bitsandbalance$setForcedPosLong(destLong);
        }

        MovingBlockRenderState base = renderState.base;
        if (base instanceof CandleBundleTintCarrier carrier) {
            carrier.bitsandbalance$setForcedPosLong(destLong);
        }
    }
}
