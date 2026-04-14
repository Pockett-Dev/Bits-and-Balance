package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BellBlock.class)
public abstract class BellBlockInteractionOffsetMixin {

    @Shadow
    private boolean isProperHit(BlockState state, Direction direction, double relativeY) {
        throw new AssertionError();
    }

    @Redirect(
        method = "onHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BellBlock;isProperHit(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;D)Z"
        )
    )
    private boolean bitsandbalance$offsetBellHitCheck(
            BellBlock instance, BlockState state, Direction direction, double relativeY,
            Level level, BlockState blockState, BlockHitResult hitResult, Player player, boolean checkHitFromUse) {

        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return this.isProperHit(state, direction, relativeY);
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, hitResult.getBlockPos(), blockState);
        return this.isProperHit(state, direction, relativeY - yOff);
    }
}