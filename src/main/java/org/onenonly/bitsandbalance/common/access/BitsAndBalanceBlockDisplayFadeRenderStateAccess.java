package org.onenonly.bitsandbalance.common.access;

import net.minecraft.core.BlockPos;

public interface BitsAndBalanceBlockDisplayFadeRenderStateAccess {
    void bitsandbalance$setBottleCloudFade(boolean value);

    boolean bitsandbalance$isBottleCloudFade();

    void bitsandbalance$setBottleCloudFadeAlpha(float alpha);

    float bitsandbalance$getBottleCloudFadeAlpha();

    default void bitsandbalance$setBottleCloudBlockPos(BlockPos pos) {
    }

    default BlockPos bitsandbalance$getBottleCloudBlockPos() {
        return BlockPos.ZERO;
    }
}
