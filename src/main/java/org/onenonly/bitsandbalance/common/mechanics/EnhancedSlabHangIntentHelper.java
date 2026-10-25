package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

public final class EnhancedSlabHangIntentHelper {
    private EnhancedSlabHangIntentHelper() {
    }

    public static BlockPos getPlacementTarget(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        return clickedState.canBeReplaced(context) ? clickedPos : clickedPos.relative(context.getClickedFace());
    }

    public static boolean isIntentionalHangPlacement(BlockPlaceContext context, BlockPos targetPos, BlockState placedState) {
        if (context == null || targetPos == null || placedState == null || placedState.isAir()) return false;
        if (!EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(placedState)) return false;
        Direction clickedFace = context.getClickedFace();
        boolean topTrapdoor = bitsandbalance$isTopTrapdoor(placedState);
        if (clickedFace != Direction.DOWN && !(topTrapdoor && clickedFace.getAxis().isHorizontal())) {
            return false;
        }

        BlockPos supportPos = targetPos.above();
        BlockGetter level = context.getLevel();
        BlockState supportState = level.getBlockState(supportPos);
        if (!EnhancedSlabHelper.hasHangBelowOffsetSurface(level, supportPos, supportState)) {
            return false;
        }

        if (clickedFace == Direction.DOWN) {
            return true;
        }

        return bitsandbalance$isIntentionalTopTrapdoorSideHang(context, supportPos);
    }

    public static boolean hasExplicitHangIntent(BlockGetter level, BlockPos pos, BlockState state) {
        if (pos == null || state == null || !EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)) return false;

        if (bitsandbalance$stateImpliesExplicitHangIntent(state)) {
            return true;
        }

        if (level instanceof ServerLevel serverLevel) {
            return EnhancedSlabHangIntentAccess.get(serverLevel).has(pos.asLong());
        }

        return EnhancedSlabHangIntentClientCache.has(pos);
    }

    private static boolean bitsandbalance$stateImpliesExplicitHangIntent(BlockState state) {
        if (state.getBlock() instanceof LanternBlock && state.hasProperty(BlockStateProperties.HANGING)) {
            return state.getValue(BlockStateProperties.HANGING);
        }

        if (state.getBlock() instanceof BellBlock && state.hasProperty(BellBlock.ATTACHMENT)) {
            return state.getValue(BellBlock.ATTACHMENT) == BellAttachType.CEILING;
        }

        if ((state.getBlock() instanceof LeverBlock || state.getBlock() instanceof ButtonBlock)
                && state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            return state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING;
        }

        if (state.getBlock() instanceof CeilingHangingSignBlock) {
            return true;
        }

        return false;
    }

    private static boolean bitsandbalance$isTopTrapdoor(BlockState state) {
        return EnhancedSlabHelper.isTrapdoorLike(state)
                && state.hasProperty(TrapDoorBlock.HALF)
                && state.getValue(TrapDoorBlock.HALF) == Half.TOP;
    }

    private static boolean bitsandbalance$isIntentionalTopTrapdoorSideHang(BlockPlaceContext context, BlockPos supportPos) {
        return bitsandbalance$isClickInBottomSlabSlotOf(context, supportPos);
    }

    private static boolean bitsandbalance$isClickInBottomSlabSlotOf(BlockPlaceContext context, BlockPos pos) {
        double localX = context.getClickLocation().x - pos.getX();
        double localY = context.getClickLocation().y - pos.getY();
        double localZ = context.getClickLocation().z - pos.getZ();
        return localX >= -1.0E-6D && localX <= 1.0D + 1.0E-6D
                && localY >= -1.0E-6D && localY < 0.5D + 1.0E-6D
                && localZ >= -1.0E-6D && localZ <= 1.0D + 1.0E-6D;
    }
}
