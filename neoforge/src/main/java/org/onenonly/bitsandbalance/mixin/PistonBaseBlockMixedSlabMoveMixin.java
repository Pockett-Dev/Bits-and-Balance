package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonMoveCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.onenonly.bitsandbalance.common.mixin.EnhancedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.PistonBlockEntityMovingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows pistons to move mixed slabs and transfers their block-entity slab-half data.
 *
 * <p>Slab data is captured inside a {@code @Redirect} on
 * {@link MovingPistonBlock#newMovingBlockEntity newMovingBlockEntity} and stored
 * <em>directly on the {@code PistonMovingBlockEntity} instance</em> via
 * {@link MixedSlabMovingPistonAccess}.  This avoids all HashMap / timing issues
 * because the data travels with the moving entity through the animation.</p>
 *
 * <p>A position-keyed cache is also staged (server + client) as a cross-cutting
 * fallback used by {@link MixedSlabBlockEntity#setLevel}.</p>
 */
@Mixin(PistonBaseBlock.class)
@SuppressWarnings({"null", "UnusedMethod"})
public abstract class PistonBaseBlockMixedSlabMoveMixin {

    /** Carries the current {@link Level} through {@code moveBlocks} for use inside the redirect. */
    @Unique
    private static final ThreadLocal<Level> BITSANDBALANCE$MOVE_LEVEL = new ThreadLocal<>();

    //  Allow mixed slabs to be moved (bypass hasBlockEntity check) 

    @Redirect(
            method = "isPushable",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z")
    )
    private static boolean bitsandbalance$allowMixedSlabsToMove(BlockState state) {
        if (state != null && state.getBlock() instanceof MixedSlabBlock) {
            return false; // pretend no BE so piston pushability check passes
        }
        if (EnhancedSlabPistonHooks.supports(state)) {
            return false;
        }
        if (PistonBlockEntityMoveRuntime.enabled && PistonBlockEntityMoveHooks.supports(state)) {
            return false;
        }
        return state != null && state.hasBlockEntity();
    }

    //  Capture level for use in the redirect 

    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private void bitsandbalance$captureLevel(Level level, BlockPos pos, Direction direction,
                                              boolean extending,
                                              CallbackInfoReturnable<Boolean> cir) {
        BITSANDBALANCE$MOVE_LEVEL.set(level);
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private void bitsandbalance$clearLevel(Level level, BlockPos pos, Direction direction,
                                            boolean extending,
                                            CallbackInfoReturnable<Boolean> cir) {
        BITSANDBALANCE$MOVE_LEVEL.remove();
    }

    //  Capture slab data when vanilla creates the PistonMovingBlockEntity 

    @Redirect(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;"
                           + "newMovingBlockEntity("
                           + "Lnet/minecraft/core/BlockPos;"
                           + "Lnet/minecraft/world/level/block/state/BlockState;"
                           + "Lnet/minecraft/world/level/block/state/BlockState;"
                           + "Lnet/minecraft/core/Direction;ZZ"
                           + ")Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private static BlockEntity bitsandbalance$captureMixedSlabOnBECreation(
            BlockPos destPos,
            BlockState movingPistonState,
            BlockState movedBlockState,
            Direction direction,
            boolean extending,
            boolean isHead) {

        BlockEntity result = MovingPistonBlock.newMovingBlockEntity(
                destPos, movingPistonState, movedBlockState, direction, extending, isHead);

        Level level = BITSANDBALANCE$MOVE_LEVEL.get();
        if (level == null || movedBlockState == null || isHead) {
            return result;
        }

        Direction moveDir = extending ? direction : direction.getOpposite();
        BlockPos sourcePos = destPos.relative(moveDir.getOpposite());

        if (EnhancedSlabPistonHooks.supports(movedBlockState)) {
            EnhancedSlabPistonData data = EnhancedSlabPistonHooks.capture(level, sourcePos);
            if (data != null) {
                if (result instanceof EnhancedSlabMovingPistonAccess access) {
                    access.bitsandbalance$setEnhancedSlabPistonData(data);
                }
                EnhancedSlabPistonHooks.stage(level, sourcePos, destPos, data);
            }
        }

        if (PistonBlockEntityMoveRuntime.enabled && PistonBlockEntityMoveHooks.supports(movedBlockState)) {
            CompoundTag tag = PistonBlockEntityMoveHooks.captureAndDetach(level, sourcePos);
            if (tag != null && !tag.isEmpty()) {
                if (result instanceof PistonBlockEntityMovingAccess access) {
                    access.bitsandbalance$setMovedBlockEntityTag(tag);
                }
                PistonBlockEntityMoveHooks.stage(level, sourcePos, destPos, tag);
            }
        }

        if (!(movedBlockState.getBlock() instanceof MixedSlabBlock)) {
            return result;
        }

        // sourcePos is the position the mixed slab is moving FROM.
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (!(sourceBe instanceof MixedSlabBlockEntity mixedBe)) {
            return result;
        }

        BlockState bottom = mixedBe.getBottomSlab();
        BlockState top    = mixedBe.getTopSlab();
        if (bottom == null || top == null) return result;

        // PRIMARY: store data on the PistonMovingBlockEntity instance itself.
        if (result instanceof MixedSlabMovingPistonAccess access) {
            access.bitsandbalance$setMixedSlabData(bottom, top);
        }

        // FALLBACK: also populate position-keyed caches.
        if (level.isClientSide()) {
            MixedSlabPistonRenderCache.putMove(sourcePos.asLong(), destPos.asLong(), bottom, top);
        } else {
            MixedSlabPistonMoveCache.putMove(sourcePos.asLong(), destPos.asLong(), bottom, top);
        }

        return result;
    }
}