package org.onenonly.bitsandbalance.common.client;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface MixedSlabPistonHeadRenderStateAccess {

    @Nullable BlockState bitsandbalance$getBottomSlab();
    @Nullable BlockState bitsandbalance$getTopSlab();
    void bitsandbalance$setMixedSlabData(@Nullable BlockState bottom, @Nullable BlockState top);
}