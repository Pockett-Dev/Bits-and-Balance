package org.onenonly.bitsandbalance.common.candle;

/**
 * Implemented by render-state objects (e.g. via mixin) to carry per-submission candle bundle
 * context through deferred rendering.
 */
public interface CandleBundleTintCarrier {
    long UNSET_POS_LONG = Long.MIN_VALUE;
    int UNSET_TINT_INDEX = Integer.MIN_VALUE;

    long bitsandbalance$getForcedPosLong();

    int bitsandbalance$getForcedTintIndex();

    void bitsandbalance$setForcedPosLong(long posLong);

    void bitsandbalance$setForcedTintIndex(int tintIndex);
}
