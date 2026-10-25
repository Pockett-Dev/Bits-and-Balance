package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class EnhancedSlabTerrainRenderHelper {
    private EnhancedSlabTerrainRenderHelper() {
    }

    public static boolean render(BlockRenderDispatcher dispatcher,
                                 BlockAndTintGetter level,
                                 BlockPos pos,
                                 BlockState state,
                                 PoseStack poseStack,
                                 VertexConsumer vertexConsumer,
                                 int packedOverlay) {
        return org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper.render(
                dispatcher,
                level,
                pos,
                state,
                poseStack,
                vertexConsumer,
                packedOverlay
        );
    }
}
