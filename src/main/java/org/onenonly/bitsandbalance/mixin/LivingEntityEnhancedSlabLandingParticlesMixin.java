package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabLandingParticleHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEnhancedSlabLandingParticlesMixin {

    @ModifyArg(
            method = "checkFallDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            index = 1,
            require = 0
    )
        private BlockState bitsandbalance$useEnhancedSlabLandingParticleState(BlockState state) {
        if (!Config.enableEnhancedSlabs) return state;
        if (state == null) return null;

                BlockPos pos = ((LivingEntity) (Object) this).blockPosition();

        boolean isVerticalSlab = state.getBlock() instanceof VerticalSlabBlock;
        boolean isStep = state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock;
        boolean isVerticalStep = state.getBlock() instanceof VerticalStepBlock || state.getBlock() instanceof QuadVerticalStepBlock;

        if (isVerticalSlab && !Config.enhancedSlabsVerticalSlabs) return state;
        if ((isStep || isVerticalStep) && !Config.enhancedSlabsSteps) return state;

        return EnhancedSlabLandingParticleHelper.resolveLandingParticleState((LivingEntity) (Object) this, state, pos);
    }
}