package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

public class AzaleaDoorBlock extends DoorBlock {
    public AzaleaDoorBlock(BlockSetType blockSetType, BlockBehaviour.Properties properties) {
        super(blockSetType, properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();

        Direction rightSide = facing.getClockWise();
        Direction leftSide = facing.getCounterClockWise();

        BlockPos rightPos = pos.relative(rightSide);
        BlockPos leftPos = pos.relative(leftSide);

        BlockState rightState = level.getBlockState(rightPos);
        BlockState leftState = level.getBlockState(leftPos);

        boolean rightIsDoor = rightState.getBlock() instanceof DoorBlock;
        boolean leftIsDoor = leftState.getBlock() instanceof DoorBlock;

        DoorHingeSide hinge = DoorHingeSide.LEFT;

        if (leftIsDoor && leftState.getValue(DoorBlock.FACING) == facing) {
            DoorHingeSide leftDoorHinge = leftState.getValue(DoorBlock.HINGE);
            hinge = (leftDoorHinge == DoorHingeSide.LEFT) ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        } else if (rightIsDoor && rightState.getValue(DoorBlock.FACING) == facing) {
            DoorHingeSide rightDoorHinge = rightState.getValue(DoorBlock.HINGE);
            hinge = (rightDoorHinge == DoorHingeSide.RIGHT) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
        }

        return state.setValue(DoorBlock.HINGE, hinge);
    }
}
