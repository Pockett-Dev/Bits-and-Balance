package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class MixedSlabStepSoundMixin {

    @Redirect(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType bitsandbalance$resolveMixedSlabStepSound(BlockState soundState, BlockPos pos, BlockState state) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, pos, soundState);
        return resolvedState.getSoundType();
    }

    @Redirect(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/level/block/SoundType;"
            ),
            require = 0
    )
    private SoundType bitsandbalance$resolveMixedSlabStepSoundNeo(BlockState soundState,
                                                                  LevelReader levelReader,
                                                                  BlockPos pos,
                                                                  Entity entity) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, pos, soundState);
        return resolvedState.getSoundType(levelReader, pos, entity);
    }

    @Redirect(
            method = "playMuffledStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType bitsandbalance$resolveMixedSlabMuffledStepSound(BlockState soundState, BlockState state) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, null, soundState);
        return resolvedState.getSoundType();
    }

    @Redirect(
            method = "playMuffledStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/level/block/SoundType;"
            ),
            require = 0
    )
    private SoundType bitsandbalance$resolveMixedSlabMuffledStepSoundNeo(BlockState soundState,
                                                                         LevelReader levelReader,
                                                                         BlockPos pos,
                                                                         Entity entity) {
        BlockState resolvedState = EnhancedSlabHelper.resolveStepSoundState((Entity) (Object) this, pos, soundState);
        return resolvedState.getSoundType(levelReader, pos, entity);
    }
}