package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes per-half mining hit sounds for mixed slabs on the client.
 *
 * Vanilla uses the block state's SoundType, but our block is a compound.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixedSlabMiningSoundMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
        method = "continueDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
        )
    )
    private SoundType bitsandbalance$mixedSlabHitSound(BlockState state, BlockPos pos, Direction direction) {
        ClientLevel level = minecraft.level;
        if (level == null) return state.getSoundType();

        BlockHitResult hitResult = minecraft.hitResult instanceof BlockHitResult bhr ? bhr : null;
        BlockState resolvedState = EnhancedSlabHelper.resolveMiningHitSoundState(level, minecraft.player, hitResult, pos, state);
        return resolvedState.getSoundType();
    }

    @Redirect(
        method = "continueDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/level/block/SoundType;"
        ),
        require = 0
    )
    private SoundType bitsandbalance$mixedSlabHitSoundNeo(BlockState state,
                                                          LevelReader levelReader,
                                                          BlockPos pos,
                                                          Entity entity) {
        ClientLevel level = minecraft.level;
        if (level == null) return state.getSoundType(levelReader, pos, entity);

        BlockHitResult hitResult = minecraft.hitResult instanceof BlockHitResult bhr ? bhr : null;
        BlockState resolvedState = EnhancedSlabHelper.resolveMiningHitSoundState(level, minecraft.player, hitResult, pos, state);
        return resolvedState.getSoundType(levelReader, pos, entity);
    }

}
