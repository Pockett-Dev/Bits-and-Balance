package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives each Bits and Balance Step half of its source slab's Sable mass.
 *
 * <p>Sable normally rejects a block as massless before consulting its configured mass when its
 * memoized solidity probe sees an empty collision shape. Storage-backed Step shapes need their
 * level position and material slots, so that state-only probe can incorrectly reject one- to
 * three-slot Step combinations. Computing their mass from the stored source slabs also preserves
 * material-specific Sable mass overrides.</p>
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper", remap = false)
public abstract class SablePhysicsBlockPropertyStepMassMixin {
    @Unique
    private static final double STEP_TO_SLAB_MASS_RATIO = 0.5D;
    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$resolvingSourceMass =
            ThreadLocal.withInitial(() -> false);

    @Shadow
    public static double getMass(BlockGetter level, BlockPos pos, BlockState state) {
        throw new AssertionError();
    }

    @Inject(
            method = "getMass(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void bitsandbalance$useStepMass(BlockGetter level,
                                                    BlockPos pos,
                                                    BlockState state,
                                                    CallbackInfoReturnable<Double> cir) {
        if (Boolean.TRUE.equals(bitsandbalance$resolvingSourceMass.get())) {
            return;
        }

        BlockState[] sourceSlabs = bitsandbalance$sourceSlabs(level, pos, state);
        if (!EnhancedSlabCreateCompat.hasMaterialSlots(sourceSlabs)) {
            return;
        }

        double mass = 0.0D;
        bitsandbalance$resolvingSourceMass.set(true);
        try {
            for (BlockState sourceSlab : sourceSlabs) {
                if (sourceSlab != null && !sourceSlab.isAir()) {
                    mass += getMass(level, pos, sourceSlab) * STEP_TO_SLAB_MASS_RATIO;
                }
            }
        } finally {
            bitsandbalance$resolvingSourceMass.remove();
        }

        if (mass > 0.0D && Double.isFinite(mass)) {
            cir.setReturnValue(mass);
        }
    }

    private static @Nullable BlockState[] bitsandbalance$sourceSlabs(BlockGetter level,
                                                                     BlockPos pos,
                                                                     BlockState state) {
        if (state == null) {
            return null;
        }

        if (!(state.getBlock() instanceof StepBlock)
                && !(state.getBlock() instanceof VerticalStepBlock)
                && !(state.getBlock() instanceof QuadStepBlock)
                && !(state.getBlock() instanceof QuadVerticalStepBlock)) {
            return null;
        }

        BlockState[] storedSlabs = EnhancedSlabCreateCompat.getContraptionSlots(level, pos, state);
        if (EnhancedSlabCreateCompat.hasMaterialSlots(storedSlabs)) {
            return storedSlabs;
        }

        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return new BlockState[]{fixedStep.getSourceSlab().defaultBlockState()};
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            return new BlockState[]{(sourceSlab != null ? sourceSlab : sourceVerticalSlab).defaultBlockState()};
        }
        return null;
    }
}
