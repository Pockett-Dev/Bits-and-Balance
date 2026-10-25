package org.onenonly.bitsandbalance.fabric.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonMoveCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.onenonly.bitsandbalance.fabric.compat.FabricCarpetPistonCompat;

import java.util.HashMap;
import java.util.Map;

/** Carries resolved piston-move data to the moving-block-entity factory hook. */
public final class FabricPistonMoveCaptureContext {
    public record PendingMove(BlockState movedState,
                              @Nullable BlockState bottomSlab,
                              @Nullable BlockState topSlab,
                              @Nullable EnhancedSlabPistonData enhancedSlabData,
                              @Nullable CompoundTag blockEntityTag) {
        public PendingMove {
            blockEntityTag = blockEntityTag == null ? null : blockEntityTag.copy();
        }

        @Override
        public @Nullable CompoundTag blockEntityTag() {
            return blockEntityTag == null ? null : blockEntityTag.copy();
        }
    }

    private record ActiveMove(Level level, Map<Long, PendingMove> byDestination) {
    }

    private static final ThreadLocal<ActiveMove> ACTIVE = new ThreadLocal<>();

    private FabricPistonMoveCaptureContext() {
    }

    public static void begin(Level level) {
        ACTIVE.set(new ActiveMove(level, new HashMap<>()));
    }

    public static void stage(Level level, BlockPos sourcePos, BlockPos destPos, BlockState movedState) {
        ActiveMove active = ACTIVE.get();
        if (active == null || active.level() != level || movedState == null) {
            return;
        }

        EnhancedSlabPistonData enhancedData = EnhancedSlabPistonHooks.supports(movedState)
                ? EnhancedSlabPistonHooks.capture(level, sourcePos)
                : null;
        if (enhancedData != null) {
            EnhancedSlabPistonHooks.stage(level, sourcePos, destPos, enhancedData);
        }

        BlockState bottom = null;
        BlockState top = null;
        if (movedState.getBlock() instanceof MixedSlabBlock) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, sourcePos.asLong());
            bottom = stored != null && stored.length > 0 ? stored[0] : null;
            top = stored != null && stored.length > 1 ? stored[1] : null;
            if (bottom != null && top != null) {
                if (level.isClientSide()) {
                    MixedSlabPistonRenderCache.putMove(sourcePos.asLong(), destPos.asLong(), bottom, top);
                } else {
                    MixedSlabPistonMoveCache.putMove(sourcePos.asLong(), destPos.asLong(), bottom, top);
                }
            }
        }

        CompoundTag blockEntityTag = null;
        if (!FabricCarpetPistonCompat.ownsGenericBlockEntityMovement()
                && PistonBlockEntityMoveRuntime.enabled
                && PistonBlockEntityMoveHooks.supports(movedState)) {
            blockEntityTag = PistonBlockEntityMoveHooks.capture(level, sourcePos);
            if (blockEntityTag != null && !blockEntityTag.isEmpty()) {
                PistonBlockEntityMoveHooks.stage(level, sourcePos, destPos, blockEntityTag);
            }
        }

        if (bottom != null || top != null || enhancedData != null || blockEntityTag != null) {
            active.byDestination().put(
                    destPos.asLong(),
                    new PendingMove(movedState, bottom, top, enhancedData, blockEntityTag)
            );
        }
    }

    public static @Nullable Level level() {
        ActiveMove active = ACTIVE.get();
        return active == null ? null : active.level();
    }

    public static @Nullable PendingMove take(BlockPos destPos, BlockState movedState) {
        ActiveMove active = ACTIVE.get();
        if (active == null) {
            return null;
        }
        PendingMove pending = active.byDestination().remove(destPos.asLong());
        if (pending == null || pending.movedState().getBlock() != movedState.getBlock()) {
            return null;
        }
        return pending;
    }

    public static void clear() {
        ACTIVE.remove();
    }
}
