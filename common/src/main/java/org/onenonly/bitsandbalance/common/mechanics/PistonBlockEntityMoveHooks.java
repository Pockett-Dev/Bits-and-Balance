package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

public final class PistonBlockEntityMoveHooks {
    private PistonBlockEntityMoveHooks() {
    }

    public static boolean supports(@Nullable BlockState state) {
        return PistonBlockEntityMoveRuntime.allows(state);
    }

    public static @Nullable CompoundTag capture(Level level, BlockPos sourcePos) {
        BlockEntity blockEntity = level.getBlockEntity(sourcePos);
        if (blockEntity == null) return null;
        return blockEntity.saveWithoutMetadata(level.registryAccess());
    }

    public static @Nullable CompoundTag captureAndDetach(Level level, BlockPos sourcePos) {
        BlockEntity blockEntity = level.getBlockEntity(sourcePos);
        if (blockEntity == null) return null;

        CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
        level.removeBlockEntity(sourcePos);
        return tag;
    }

    public static void stage(Level level, BlockPos sourcePos, BlockPos destPos, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;

        if (level.isClientSide()) {
            PistonBlockEntityRenderCache.putMove(sourcePos.asLong(), destPos.asLong(), tag);
        } else {
            PistonBlockEntityMoveCache.putMove(sourcePos.asLong(), destPos.asLong(), tag);
        }
    }

    public static boolean applyDeferred(Level level,
                                        BlockPos pos,
                                        BlockState finalState,
                                        @Nullable Direction movementDirection,
                                        @Nullable CompoundTag directTag) {
        if (!supports(finalState)) return false;

        CompoundTag tag = bitsandbalance$resolveDeferredTag(level, pos, movementDirection, directTag);
        if (tag == null || !(finalState.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !blockEntity.isValidBlockState(finalState)) {
            if (blockEntity != null) {
                level.removeBlockEntity(pos);
            }

            BlockEntity created = entityBlock.newBlockEntity(pos, finalState);
            if (created == null) {
                bitsandbalance$breakInvalidPlacement(level, pos, finalState, null);
                return true;
            }

            created.setLevel(level);
            created.clearRemoved();
            level.setBlockEntity(created);
            blockEntity = created;
        }

        blockEntity.clearRemoved();
        blockEntity.setLevel(level);
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag.copy()));
        blockEntity.setChanged();

        if (!finalState.canSurvive(level, pos)) {
            bitsandbalance$breakInvalidPlacement(level, pos, finalState, blockEntity);
            return true;
        }

        if (!level.isClientSide()) {
            level.sendBlockUpdated(pos, finalState, finalState, Block.UPDATE_ALL);
        }

        return true;
    }

    private static @Nullable CompoundTag bitsandbalance$resolveDeferredTag(Level level,
                                                                           BlockPos pos,
                                                                           @Nullable Direction movementDirection,
                                                                           @Nullable CompoundTag directTag) {
        if (directTag != null && !directTag.isEmpty()) {
            bitsandbalance$discardDeferredEntry(level, pos, movementDirection, pos.asLong());
            return directTag;
        }

        long posLong = pos.asLong();
        if (level.isClientSide()) {
            PistonBlockEntityRenderCache.Entry entry = bitsandbalance$peekRenderEntry(pos, movementDirection, posLong);
            return entry != null ? entry.tag() : null;
        }

        PistonBlockEntityMoveCache.Entry entry = bitsandbalance$takeMoveEntry(pos, movementDirection, posLong);
        return entry != null ? entry.tag() : null;
    }

    private static void bitsandbalance$discardDeferredEntry(Level level,
                                                            BlockPos pos,
                                                            @Nullable Direction movementDirection,
                                                            long posLong) {
        if (level.isClientSide()) {
            bitsandbalance$takeRenderEntry(pos, movementDirection, posLong);
            return;
        }

        bitsandbalance$takeMoveEntry(pos, movementDirection, posLong);
    }

    private static @Nullable PistonBlockEntityRenderCache.Entry bitsandbalance$takeRenderEntry(BlockPos pos,
                                                                                                 @Nullable Direction movementDirection,
                                                                                                 long posLong) {
        if (movementDirection != null) {
            PistonBlockEntityRenderCache.Entry entry = PistonBlockEntityRenderCache.takeForMove(
                    posLong,
                    pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) {
                return entry;
            }

            entry = PistonBlockEntityRenderCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) {
                return entry;
            }
        }

        return PistonBlockEntityRenderCache.takeForMove(posLong, posLong);
    }

    private static @Nullable PistonBlockEntityRenderCache.Entry bitsandbalance$peekRenderEntry(BlockPos pos,
                                                                                                 @Nullable Direction movementDirection,
                                                                                                 long posLong) {
        if (movementDirection != null) {
            PistonBlockEntityRenderCache.Entry entry = PistonBlockEntityRenderCache.peekForMove(
                    posLong,
                    pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) {
                PistonBlockEntityRenderCache.lingerForMove(posLong, pos.relative(movementDirection.getOpposite()).asLong());
                return entry;
            }

            entry = PistonBlockEntityRenderCache.peekForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) {
                PistonBlockEntityRenderCache.lingerForMove(posLong, pos.relative(movementDirection).asLong());
                return entry;
            }
        }

        PistonBlockEntityRenderCache.Entry entry = PistonBlockEntityRenderCache.peekForMove(posLong, posLong);
        if (entry != null) {
            PistonBlockEntityRenderCache.lingerForMove(posLong, posLong);
        }
        return entry;
    }

    private static @Nullable PistonBlockEntityMoveCache.Entry bitsandbalance$takeMoveEntry(BlockPos pos,
                                                                                             @Nullable Direction movementDirection,
                                                                                             long posLong) {
        if (movementDirection != null) {
            PistonBlockEntityMoveCache.Entry entry = PistonBlockEntityMoveCache.takeForMove(
                    posLong,
                    pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) return entry;

            entry = PistonBlockEntityMoveCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) return entry;
        }

        return PistonBlockEntityMoveCache.takeForMove(posLong, posLong);
    }

    private static void bitsandbalance$breakInvalidPlacement(Level level,
                                                             BlockPos pos,
                                                             BlockState finalState,
                                                             @Nullable BlockEntity blockEntity) {
        if (!level.isClientSide()) {
            if (blockEntity != null) {
                blockEntity.preRemoveSideEffects(pos, finalState);
            }
            Block.dropResources(finalState, level, pos, blockEntity);
        }

        if (blockEntity != null) {
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean bitsandbalance$isIntrinsicExclusion(Block block) {
        return block instanceof MovingPistonBlock
                || block instanceof MixedSlabBlock
                || block instanceof VerticalSlabBlock
                || block instanceof StepBlock
                || block instanceof VerticalStepBlock
                || block instanceof QuadStepBlock
                || block instanceof QuadVerticalStepBlock;
    }
}