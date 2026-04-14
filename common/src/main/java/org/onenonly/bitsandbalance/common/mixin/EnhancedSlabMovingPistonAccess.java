package org.onenonly.bitsandbalance.common.mixin;

import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;

public interface EnhancedSlabMovingPistonAccess {

    @Nullable EnhancedSlabPistonData bitsandbalance$getEnhancedSlabPistonData();

    void bitsandbalance$setEnhancedSlabPistonData(EnhancedSlabPistonData data);
}