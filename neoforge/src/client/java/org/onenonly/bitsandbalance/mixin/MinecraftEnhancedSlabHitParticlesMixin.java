package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftEnhancedSlabHitParticlesMixin {

    @Shadow public ClientLevel level;
    @Shadow public LocalPlayer player;
    @Shadow public HitResult hitResult;

    private boolean bitsandbalance$skipVanillaBreakingEffect;

    @Redirect(
        method = "continueAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;continueDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
        ),
        require = 0
    )
    private boolean bitsandbalance$spawnEnhancedHitParticles(MultiPlayerGameMode gameMode,
                                                             BlockPos pos,
                                                             Direction direction) {
        boolean destroying = gameMode.continueDestroyBlock(pos, direction);
        bitsandbalance$skipVanillaBreakingEffect = false;
        if (!destroying || !Config.enableEnhancedSlabs || level == null || player == null) {
            return destroying;
        }

        BlockState state = level.getBlockState(pos);
        if (!bitsandbalance$isEnhancedInvisibleBreakState(state)) {
            return destroying;
        }

        BlockHitResult blockHit = hitResult instanceof BlockHitResult bhr ? bhr : null;
        bitsandbalance$skipVanillaBreakingEffect = EnhancedSlabDestroyParticleHelper.spawnHitForState(level, player, blockHit, pos, direction, state);
        return destroying;
    }

    @Redirect(
        method = "continueAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"
        ),
        require = 0
    )
    private void bitsandbalance$maybeSkipVanillaBreakingEffect(ClientLevel level, BlockPos pos, Direction direction) {
        if (bitsandbalance$skipVanillaBreakingEffect) {
            bitsandbalance$skipVanillaBreakingEffect = false;
            return;
        }

        if (level != null) {
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