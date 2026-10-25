package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineEnhancedSlabHitParticlesMixin {

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$spawnEnhancedHitParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state == null) {
            return;
        }

        if (!bitsandbalance$isEnhancedSlabFamily(state)) {
            return;
        }

        BlockHitResult hitResult = minecraft.hitResult instanceof BlockHitResult bhr ? bhr : null;
        if (EnhancedSlabDestroyParticleHelper.spawnHitForState(level, player, hitResult, pos, direction, state)) {
            ci.cancel();
        }
    }

    private static boolean bitsandbalance$isEnhancedSlabFamily(BlockState state) {
        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }
}
