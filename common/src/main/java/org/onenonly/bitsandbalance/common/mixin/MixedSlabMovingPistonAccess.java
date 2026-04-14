package org.onenonly.bitsandbalance.common.mixin;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Accessor interface implemented by {@code PistonMovingBlockEntity} via Mixin.
 *
 * Carries mixed-slab half data through the piston animation so there is no dependency
 * on external caches or timing of block-entity lifecycle callbacks.
 */
public interface MixedSlabMovingPistonAccess {

    @Nullable BlockState bitsandbalance$getBottomSlab();
    @Nullable BlockState bitsandbalance$getTopSlab();
    void bitsandbalance$setMixedSlabData(BlockState bottom, BlockState top);
}