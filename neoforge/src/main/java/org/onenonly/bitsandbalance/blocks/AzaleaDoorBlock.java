package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Custom door block that properly handles drops and placement.
 */
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
        
        // Get the placement details
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        
        // Check for adjacent doors to determine hinge side
        // This mimics vanilla behavior but works with our custom door
        Direction rightSide = facing.getClockWise();
        Direction leftSide = facing.getCounterClockWise();
        
        BlockPos rightPos = pos.relative(rightSide);
        BlockPos leftPos = pos.relative(leftSide);
        
        BlockState rightState = level.getBlockState(rightPos);
        BlockState leftState = level.getBlockState(leftPos);
        
        // Check if there's a door on either side
        boolean rightIsDoor = rightState.getBlock() instanceof DoorBlock;
        boolean leftIsDoor = leftState.getBlock() instanceof DoorBlock;
        
        // Determine hinge side based on adjacent doors
        // Vanilla behavior: when placing next to another door facing the same direction,
        // we want hinges on the outside and handles in the middle
        DoorHingeSide hinge = DoorHingeSide.LEFT; // Default
        
        if (leftIsDoor) {
            // There's a door on our left side
            if (leftState.getValue(DoorBlock.FACING) == facing) {
                // Same direction - we should mirror the adjacent door's hinge
                // If left door has LEFT hinge, we get RIGHT hinge (handles meet in middle)
                // If left door has RIGHT hinge, we get LEFT hinge
                DoorHingeSide leftDoorHinge = leftState.getValue(DoorBlock.HINGE);
                hinge = (leftDoorHinge == DoorHingeSide.LEFT) ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
            }
        } else if (rightIsDoor) {
            // There's a door on our right side
            if (rightState.getValue(DoorBlock.FACING) == facing) {
                // Same direction - we should mirror the adjacent door's hinge
                // If right door has RIGHT hinge, we get LEFT hinge (handles meet in middle)
                // If right door has LEFT hinge, we get RIGHT hinge
                DoorHingeSide rightDoorHinge = rightState.getValue(DoorBlock.HINGE);
                hinge = (rightDoorHinge == DoorHingeSide.RIGHT) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            }
        }
        
        // Apply the hinge to the state
        return state.setValue(DoorBlock.HINGE, hinge);
    }
}
