package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.compat.ThatchLighting;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnhancedSlabMaterialSavedData.class)
public abstract class ThatchMaterialLightingMixin {
    @WrapMethod(method = "remember")
    private static void bitsandbalance$refreshThatchLighting(Level level, long position,
                                                             BlockState[] slots, Operation<Void> original) {
        boolean refresh = level != null && (ThatchLighting.containsThatch(slots)
                || ThatchLighting.containsThatch(EnhancedSlabMaterialSavedData.peek(level, position)));
        original.call(level, position, slots);
        if (refresh) {
            // Replacing materials can change opacity without changing the container BlockState.
            level.getChunkSource().getLightEngine().checkBlock(BlockPos.of(position));
        }
    }
}
