package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to handle block rendering with alpha support.
 * (Currently no-op; kept for parity with the NeoForge mixin list.)
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRendererAlphaMixin {

    @Inject(
            method = "renderBatched",
            at = @At("HEAD"),
            require = 0
    )
    private void onBlockRenderStart(
            BlockState state,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.BlockAndTintGetter level,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            boolean checkSides,
            List<?> renderTypes,
            CallbackInfo ci
    ) {
        // Intentionally empty (previously used for debug output)
    }

    @Inject(
            method = "renderBatched",
            at = @At("RETURN"),
            require = 0
    )
    private void onBlockRenderEnd(
            BlockState state,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.BlockAndTintGetter level,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            boolean checkSides,
            List<?> renderTypes,
            CallbackInfo ci
    ) {
        // Intentionally empty (previously used for debug output)
    }
}
