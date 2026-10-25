package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;

public class VerticalSlabBlockEntityRenderer implements BlockEntityRenderer<VerticalSlabBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;

    public VerticalSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public void render(VerticalSlabBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) {
            return;
        }
        if (blockEntity.getLevel() == null || blockEntity.getFacingSlab() == null) {
            return;
        }

        buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);

        Direction facing = Direction.NORTH;
        boolean isDouble = false;
        if (blockEntity.getBlockState().getBlock() instanceof VerticalSlabBlock) {
            facing = blockEntity.getBlockState().getValue(VerticalSlabBlock.FACING);
            isDouble = blockEntity.getBlockState().getValue(VerticalSlabBlock.DOUBLE);
        }

        if (isDouble && blockEntity.getOppositeSlab() != null) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(
                    blockEntity.getFacingSlab(),
                    blockEntity.getOppositeSlab()
            );
            if (fullBlockState != null) {
                LegacyEnhancedSlabRenderHelper.renderSourceModel(
                        this.blockRenderDispatcher,
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos(),
                        fullBlockState,
                        poseStack,
                        buffer,
                        packedOverlay
                );
                return;
            }
        }

        LegacyEnhancedSlabRenderHelper.renderVerticalSlab(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getFacingSlab(), facing, poseStack, buffer, packedLight, packedOverlay);
        if (isDouble && blockEntity.getOppositeSlab() != null) {
            LegacyEnhancedSlabRenderHelper.renderVerticalSlab(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getOppositeSlab(), facing.getOpposite(), poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
