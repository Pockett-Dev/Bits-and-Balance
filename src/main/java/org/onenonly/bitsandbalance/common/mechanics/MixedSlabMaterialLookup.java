package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;

public final class MixedSlabMaterialLookup {
    private MixedSlabMaterialLookup() {
    }

    public static @Nullable BlockState[] get(BlockGetter level, BlockPos pos) {
        if (level instanceof Level actualLevel) {
            BlockState[] saved = EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong());
            if (saved != null) {
                return saved;
            }
        }
        return EnhancedSlabCreateCompat.getRenderSlots(level, pos);
    }

    public static @Nullable BlockState bottom(@Nullable BlockState[] slots) {
        return slots != null && slots.length > 0 ? slots[0] : null;
    }

    public static @Nullable BlockState top(@Nullable BlockState[] slots) {
        return slots != null && slots.length > 1 ? slots[1] : null;
    }
}
