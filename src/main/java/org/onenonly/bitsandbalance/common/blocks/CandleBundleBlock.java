package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;

public class CandleBundleBlock extends CandleBlock implements EntityBlock {
    public CandleBundleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CandleBundleBlockEntity bundle) {
            int[] nonEmptyIndices = new int[4];
            int nonEmptyCount = 0;

            for (int i = 0; i < 4; i++) {
                ItemStack stack = bundle.getCandle(i);
                if (stack != null && !stack.isEmpty()) {
                    nonEmptyIndices[nonEmptyCount++] = i;
                }
            }

            if (nonEmptyCount > 0) {
                RandomSource random = (level instanceof Level l) ? l.getRandom() : RandomSource.create();
                int pickedIndex = nonEmptyIndices[random.nextInt(nonEmptyCount)];
                return bundle.getCandle(pickedIndex).copyWithCount(1);
            }
        }

        // Never return the bundle block/item from pick-block.
        return new ItemStack(Items.CANDLE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The model cannot express per-candle block types.
        // Rendering is delegated to the CandleBundleBlockEntityRenderer.
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bitsandbalance$rotatedShape(super.getShape(state, level, pos, context), bitsandbalance$facing(level, pos));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bitsandbalance$rotatedShape(super.getCollisionShape(state, level, pos, context), bitsandbalance$facing(level, pos));
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return bitsandbalance$rotatedShape(super.getInteractionShape(state, level, pos), bitsandbalance$facing(level, pos));
    }

    private static Direction bitsandbalance$facing(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CandleBundleBlockEntity bundleBe) {
            Direction facing = bundleBe.getFacing();
            if (facing != null && facing.getAxis().isHorizontal()) {
                return facing;
            }
        }
        return Direction.SOUTH;
    }

    /**
     * The bundle is rendered rotated based on its stored facing (see CandleBundleBlockEntityRenderer).
     * Vanilla candle hitboxes are asymmetric for multi-candle counts, so rotate the voxel shape too.
     */
    @SuppressWarnings("null")
    private static VoxelShape bitsandbalance$rotatedShape(VoxelShape baseSouthShape, Direction facing) {
        if (!facing.getAxis().isHorizontal() || facing == Direction.SOUTH) return baseSouthShape;

        VoxelShape[] out = new VoxelShape[] { Shapes.empty() };
        baseSouthShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // Rotate around block center. The renderer uses:
            // SOUTH=0, WEST=90, NORTH=180, EAST=270 degrees.
            double rMinX;
            double rMaxX;
            double rMinZ;
            double rMaxZ;

            switch (facing) {
                case WEST -> {
                    // +90° CW (from above): (x,z) -> (z, 1 - x)
                    double aX = minZ;
                    double aZ = 1.0 - minX;
                    double bX = maxZ;
                    double bZ = 1.0 - maxX;
                    rMinX = Math.min(aX, bX);
                    rMaxX = Math.max(aX, bX);
                    rMinZ = Math.min(aZ, bZ);
                    rMaxZ = Math.max(aZ, bZ);
                }
                case NORTH -> {
                    // 180°: (x,z) -> (1 - x, 1 - z)
                    double aX = 1.0 - minX;
                    double aZ = 1.0 - minZ;
                    double bX = 1.0 - maxX;
                    double bZ = 1.0 - maxZ;
                    rMinX = Math.min(aX, bX);
                    rMaxX = Math.max(aX, bX);
                    rMinZ = Math.min(aZ, bZ);
                    rMaxZ = Math.max(aZ, bZ);
                }
                case EAST -> {
                    // +270° CW (or +90° CCW): (x,z) -> (1 - z, x)
                    double aX = 1.0 - minZ;
                    double aZ = minX;
                    double bX = 1.0 - maxZ;
                    double bZ = maxX;
                    rMinX = Math.min(aX, bX);
                    rMaxX = Math.max(aX, bX);
                    rMinZ = Math.min(aZ, bZ);
                    rMaxZ = Math.max(aZ, bZ);
                }
                default -> {
                    rMinX = minX;
                    rMaxX = maxX;
                    rMinZ = minZ;
                    rMaxZ = maxZ;
                }
            }

            VoxelShape rotatedBox = Block.box(rMinX * 16.0, minY * 16.0, rMinZ * 16.0, rMaxX * 16.0, maxY * 16.0, rMaxZ * 16.0);
            out[0] = Shapes.or(out[0], rotatedBox);
        });
        return out[0];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandleBundleBlockEntity(pos, state);
    }
}
