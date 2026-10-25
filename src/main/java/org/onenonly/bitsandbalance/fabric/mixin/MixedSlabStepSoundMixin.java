package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class MixedSlabStepSoundMixin {

    @ModifyExpressionValue(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType bitsandbalance$resolveMixedSlabStepSound(SoundType original, BlockPos pos, BlockState soundState) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, pos, soundState);
        return resolvedState == soundState ? original : resolvedState.getSoundType();
    }

    @ModifyExpressionValue(
            method = "playMuffledStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType bitsandbalance$resolveMixedSlabMuffledStepSound(SoundType original, BlockState soundState) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, null, soundState);
        return resolvedState == soundState ? original : resolvedState.getSoundType();
    }
}
