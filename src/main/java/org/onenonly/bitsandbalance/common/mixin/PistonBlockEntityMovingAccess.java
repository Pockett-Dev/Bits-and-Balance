package org.onenonly.bitsandbalance.common.mixin;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public interface PistonBlockEntityMovingAccess {
    @Nullable CompoundTag bitsandbalance$getMovedBlockEntityTag();

    void bitsandbalance$setMovedBlockEntityTag(@Nullable CompoundTag tag);

    boolean bitsandbalance$hasAppliedMovedBlockEntityTag();

    void bitsandbalance$markMovedBlockEntityTagApplied();
}