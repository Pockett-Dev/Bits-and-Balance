package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonMoveCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mixin.EnhancedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.PistonBlockEntityMovingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
@SuppressWarnings({"null", "UnusedMethod"})
public abstract class PistonMovingBlockEntityFinalizeMixin implements MixedSlabMovingPistonAccess, EnhancedSlabMovingPistonAccess, PistonBlockEntityMovingAccess {
    @Unique private static final String BITSANDBALANCE$MOVED_BLOCK_ENTITY_TAG = "bitsandbalanceMovedBlockEntity";

    @Unique private @Nullable BlockState bitsandbalance$bottomSlab;
    @Unique private @Nullable BlockState bitsandbalance$topSlab;
    @Unique private @Nullable EnhancedSlabPistonData bitsandbalance$enhancedSlabData;
    @Unique private @Nullable CompoundTag bitsandbalance$movedBlockEntityTag;
    @Unique private boolean bitsandbalance$appliedMovedBlockEntityTag;

    @Override
    public @Nullable BlockState bitsandbalance$getBottomSlab() {
        return bitsandbalance$bottomSlab;
    }

    @Override
    public @Nullable BlockState bitsandbalance$getTopSlab() {
        return bitsandbalance$topSlab;
    }

    @Override
    public void bitsandbalance$setMixedSlabData(BlockState bottom, BlockState top) {
        bitsandbalance$bottomSlab = bottom;
        bitsandbalance$topSlab = top;
    }

    @Override
    public @Nullable EnhancedSlabPistonData bitsandbalance$getEnhancedSlabPistonData() {
        return bitsandbalance$enhancedSlabData;
    }

    @Override
    public void bitsandbalance$setEnhancedSlabPistonData(EnhancedSlabPistonData data) {
        bitsandbalance$enhancedSlabData = data;
    }

    @Override
    public @Nullable CompoundTag bitsandbalance$getMovedBlockEntityTag() {
        return bitsandbalance$movedBlockEntityTag == null ? null : bitsandbalance$movedBlockEntityTag.copy();
    }

    @Override
    public void bitsandbalance$setMovedBlockEntityTag(@Nullable CompoundTag tag) {
        bitsandbalance$movedBlockEntityTag = tag == null ? null : tag.copy();
        bitsandbalance$appliedMovedBlockEntityTag = false;
    }

    @Override
    public boolean bitsandbalance$hasAppliedMovedBlockEntityTag() {
        return bitsandbalance$appliedMovedBlockEntityTag;
    }

    @Override
    public void bitsandbalance$markMovedBlockEntityTagApplied() {
        bitsandbalance$appliedMovedBlockEntityTag = true;
        bitsandbalance$movedBlockEntityTag = null;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void bitsandbalance$loadMovedBlockEntityTag(ValueInput input, CallbackInfo ci) {
        bitsandbalance$movedBlockEntityTag = input.read(BITSANDBALANCE$MOVED_BLOCK_ENTITY_TAG, CompoundTag.CODEC)
                .map(CompoundTag::copy)
                .orElse(null);
        bitsandbalance$appliedMovedBlockEntityTag = false;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void bitsandbalance$saveMovedBlockEntityTag(ValueOutput output, CallbackInfo ci) {
        if (bitsandbalance$movedBlockEntityTag != null && !bitsandbalance$movedBlockEntityTag.isEmpty()) {
            output.store(BITSANDBALANCE$MOVED_BLOCK_ENTITY_TAG, CompoundTag.CODEC, bitsandbalance$movedBlockEntityTag.copy());
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void bitsandbalance$applyDeferredPistonData(Level level,
                                                               BlockPos pos,
                                                               BlockState pistonState,
                                                               PistonMovingBlockEntity piston,
                                                               CallbackInfo ci) {
        bitsandbalance$applyDeferredData(level, pos, piston);
    }

    @Inject(method = "finalTick", at = @At("RETURN"))
    private void bitsandbalance$applyDeferredPistonDataAfterFinalTick(CallbackInfo ci) {
        PistonMovingBlockEntity piston = (PistonMovingBlockEntity) (Object) this;
        Level level = piston.getLevel();
        if (level == null) {
            return;
        }
        bitsandbalance$applyDeferredData(level, piston.getBlockPos(), piston);
    }

    @Unique
    private static void bitsandbalance$applyDeferredData(Level level, BlockPos pos, PistonMovingBlockEntity piston) {
        BlockState finalState = level.getBlockState(pos);
        long posLong = pos.asLong();

        if (finalState.getBlock() instanceof MixedSlabBlock mixedSlabBlock) {
            BlockState bottom = null;
            BlockState top = null;
            if (piston instanceof MixedSlabMovingPistonAccess access) {
                bottom = access.bitsandbalance$getBottomSlab();
                top = access.bitsandbalance$getTopSlab();
            }

            if (bottom == null || top == null) {
                Direction movementDirection = piston.getMovementDirection();
                if (level.isClientSide()) {
                    MixedSlabPistonRenderCache.Entry entry = bitsandbalance$takeRenderEntry(pos, posLong, movementDirection);
                    if (entry != null) {
                        bottom = entry.bottomSlab();
                        top = entry.topSlab();
                    }
                } else {
                    MixedSlabPistonMoveCache.Entry entry = bitsandbalance$takeMoveEntry(pos, posLong, movementDirection);
                    if (entry != null) {
                        bottom = entry.bottomSlab();
                        top = entry.topSlab();
                    }
                }
            }

            if (bottom != null && top != null) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof MixedSlabBlockEntity)) {
                    BlockEntity newBlockEntity = mixedSlabBlock.newBlockEntity(pos, finalState);
                    if (newBlockEntity != null) {
                        newBlockEntity.setLevel(level);
                        level.setBlockEntity(newBlockEntity);
                        blockEntity = newBlockEntity;
                    }
                }
                if (blockEntity instanceof MixedSlabBlockEntity mixedSlabBlockEntity) {
                    mixedSlabBlockEntity.setSlabs(bottom, top);
                }
            }
        }

        if (piston instanceof PistonBlockEntityMovingAccess access && !access.bitsandbalance$hasAppliedMovedBlockEntityTag()) {
            boolean applied = PistonBlockEntityMoveHooks.applyDeferred(
                    level,
                    pos,
                    finalState,
                    piston.getMovementDirection(),
                    access.bitsandbalance$getMovedBlockEntityTag()
            );
            if (applied) {
                access.bitsandbalance$markMovedBlockEntityTagApplied();
            }
        }

        EnhancedSlabPistonHooks.applyDeferred(
                level,
                pos,
                finalState,
                piston.getMovementDirection(),
                piston instanceof EnhancedSlabMovingPistonAccess access
                        ? access.bitsandbalance$getEnhancedSlabPistonData()
                        : null
        );
    }

    @Unique
    private static @Nullable MixedSlabPistonRenderCache.Entry bitsandbalance$takeRenderEntry(BlockPos pos,
                                                                                              long posLong,
                                                                                              @Nullable Direction movementDirection) {
        if (movementDirection != null) {
            MixedSlabPistonRenderCache.Entry entry = MixedSlabPistonRenderCache.takeForMove(
                    posLong,
                    pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) {
                return entry;
            }

            entry = MixedSlabPistonRenderCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) {
                return entry;
            }
        }

        return MixedSlabPistonRenderCache.takeForMove(posLong, posLong);
    }

    @Unique
    private static @Nullable MixedSlabPistonMoveCache.Entry bitsandbalance$takeMoveEntry(BlockPos pos,
                                                                                           long posLong,
                                                                                           @Nullable Direction movementDirection) {
        if (movementDirection != null) {
            MixedSlabPistonMoveCache.Entry entry = MixedSlabPistonMoveCache.takeForMove(
                    posLong,
                    pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) {
                return entry;
            }

            entry = MixedSlabPistonMoveCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) {
                return entry;
            }
        }

        return MixedSlabPistonMoveCache.takeForMove(posLong, posLong);
    }
}