package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle block rendering with alpha support.
 * This targets the actual block rendering process to ensure alpha is applied correctly.
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRendererAlphaMixin {

    @Inject(
        method = "renderBatched",
        at = @At("HEAD"),
        require = 0
    )
    private void onBlockRenderStart(BlockState state, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.level.BlockAndTintGetter level,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack, 
                                  com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                  boolean checkSides, List<?> renderTypes,
                                  CallbackInfo ci) {
        // Intentionally empty (previously used for debug output)
    }

    @Inject(
        method = "renderBatched",
        at = @At("RETURN"),
        require = 0
    )
    private void onBlockRenderEnd(BlockState state, net.minecraft.core.BlockPos pos,
                                net.minecraft.world.level.BlockAndTintGetter level,
                                com.mojang.blaze3d.vertex.PoseStack poseStack, 
                                com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                boolean checkSides, List<?> renderTypes,
                                CallbackInfo ci) {
        // Intentionally empty (previously used for debug output)
    }
}

