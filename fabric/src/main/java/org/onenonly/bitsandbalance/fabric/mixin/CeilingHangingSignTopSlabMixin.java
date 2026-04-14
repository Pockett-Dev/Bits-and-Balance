package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CeilingHangingSignBlock.class)
public abstract class CeilingHangingSignTopSlabMixin {

    @Redirect(
        method = "getStateForPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;isFaceFull(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/Direction;)Z"
        )
    )
    private boolean bitsandbalance$treatTopSlabsAsFullCeilings(
            VoxelShape shape, Direction direction, BlockPlaceContext context) {

        if (Block.isFaceFull(shape, direction)) {
            return true;
        }
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsHangBelow || direction != Direction.DOWN) {
            return false;
        }

        Level level = context.getLevel();
        BlockPos abovePos = context.getClickedPos().above();
        BlockState aboveState = level.getBlockState(abovePos);
        return EnhancedSlabHelper.hasFullBottomSurface(level, abovePos, aboveState);
    }
}