package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the live bell renderer apply the top-slab offset instead of BBE's idle chunk mesh. */
@Mixin(value = BlockEntity.class, priority = 500)
public abstract class BetterBlockEntitiesBellCompatMixin {
    @ModifyReturnValue(method = "bbe$isSupportedBlockEntity", at = @At("RETURN"), remap = false)
    private boolean bitsandbalance$renderOffsetBellLive(boolean supported) {
        if (!supported || !FabricTweaksConfig.enableEnhancedSlabs) return supported;

        BlockEntity self = (BlockEntity) (Object) this;
        if (!(self instanceof BellBlockEntity)) return supported;

        Level level = self.getLevel();
        if (level == null) return supported;

        BlockState state = self.getBlockState();
        if (!(state.getBlock() instanceof BellBlock)
                || state.getValue(BellBlock.ATTACHMENT) != BellAttachType.CEILING) {
            return supported;
        }

        return EnhancedSlabHelper.getVisualYOffset(level, self.getBlockPos(), state) == 0.0D;
    }
}
