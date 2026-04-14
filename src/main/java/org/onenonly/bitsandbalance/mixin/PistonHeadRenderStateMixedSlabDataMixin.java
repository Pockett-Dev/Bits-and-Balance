package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.client.MixedSlabPistonHeadRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PistonHeadRenderState.class)
public class PistonHeadRenderStateMixedSlabDataMixin implements MixedSlabPistonHeadRenderStateAccess {
    @Unique private @Nullable BlockState bitsandbalance$bottomSlab;
    @Unique private @Nullable BlockState bitsandbalance$topSlab;

    @Override
    public @Nullable BlockState bitsandbalance$getBottomSlab() {
        return bitsandbalance$bottomSlab;
    }

    @Override
    public @Nullable BlockState bitsandbalance$getTopSlab() {
        return bitsandbalance$topSlab;
    }

    @Override
    public void bitsandbalance$setMixedSlabData(@Nullable BlockState bottom, @Nullable BlockState top) {
        bitsandbalance$bottomSlab = bottom;
        bitsandbalance$topSlab = top;
    }
}