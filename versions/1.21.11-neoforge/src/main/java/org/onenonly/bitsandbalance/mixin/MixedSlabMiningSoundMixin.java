package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
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

    @Redirect(
        method = "continueDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"
        ),
        require = 0
    )
    private void bitsandbalance$spawnEnhancedHitParticles(ClientLevel level, BlockPos pos, Direction direction) {
        if (!Config.enableEnhancedSlabs) {
            level.addBreakingBlockEffect(pos, direction);
            return;
        }

        BlockState state = level.getBlockState(pos);
        Player player = minecraft.player;
        if (state == null || player == null || !bitsandbalance$isEnhancedInvisibleBreakState(state)) {
            level.addBreakingBlockEffect(pos, direction);
            return;
        }

        BlockHitResult hitResult = minecraft.hitResult instanceof BlockHitResult bhr ? bhr : null;
        if (!EnhancedSlabDestroyParticleHelper.spawnHitForState(level, player, hitResult, pos, direction, state)) {
            level.addBreakingBlockEffect(pos, direction);
        }
    }

    private static boolean bitsandbalance$isEnhancedInvisibleBreakState(BlockState state) {
        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }
}
