package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import org.onenonly.bitsandbalance.common.candle.CandleBundleTintCarrier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MovingBlockRenderState.class)
public class MovingBlockRenderStateCandleBundleTintMixin implements CandleBundleTintCarrier {
    private long bitsandbalance$forcedPosLong = UNSET_POS_LONG;
    private int bitsandbalance$forcedTintIndex = UNSET_TINT_INDEX;

    @Override
    public long bitsandbalance$getForcedPosLong() {
        return bitsandbalance$forcedPosLong;
    }

    @Override
    public int bitsandbalance$getForcedTintIndex() {
        return bitsandbalance$forcedTintIndex;
    }

    @Override
    public void bitsandbalance$setForcedPosLong(long posLong) {
        this.bitsandbalance$forcedPosLong = posLong;
    }

    @Override
    public void bitsandbalance$setForcedTintIndex(int tintIndex) {
        this.bitsandbalance$forcedTintIndex = tintIndex;
    }
}
