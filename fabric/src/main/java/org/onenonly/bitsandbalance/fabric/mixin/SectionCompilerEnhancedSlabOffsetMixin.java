package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCompiler")
public class SectionCompilerEnhancedSlabOffsetMixin {

    @Unique
    private static final Logger bitsandbalance$logger = LoggerFactory.getLogger("bitsandbalance-enhanced-slab-render-offset");

    @Unique
    private static final AtomicBoolean bitsandbalance$logged = new AtomicBoolean(false);

        @WrapOperation(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"
            ),
            require = 0
    )
    private void bitsandbalance$tesselateBlockWithEnhancedSlabOffset(ModelBlockRenderer renderer,
                                                                     BlockQuadOutput output,
                                                                     float x,
                                                                     float y,
                                                                     float z,
                                                                     BlockAndTintGetter level,
                                                                     BlockPos pos,
                                                                     BlockState state,
                                                                     BlockStateModel model,
                                                                     long seed,
                                                                     Operation<Void> original) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || state == null || pos == null || model == null) {
            original.call(renderer, output, x, y, z, level, pos, state, model, seed);
            return;
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff == 0.0D) {
            original.call(renderer, output, x, y, z, level, pos, state, model, seed);
            return;
        }

        if (bitsandbalance$isTrackedBlock(state) && bitsandbalance$logged.compareAndSet(false, true)) {
            bitsandbalance$logger.info(
                    "Enhanced slab section compiler path block={} pos={} yOff={}",
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                    pos,
                    yOff
            );
        }

        original.call(
                renderer,
                output,
                x,
                y + (float) yOff,
                z,
                level,
                pos,
                state,
                model,
                seed
        );
    }

    @Unique
    private static boolean bitsandbalance$isTrackedBlock(BlockState state) {
        return state.getBlock() instanceof LanternBlock
                || state.getBlock() instanceof BellBlock
                || state.is(net.minecraft.tags.BlockTags.CEILING_HANGING_SIGNS)
                || state.is(net.minecraft.tags.BlockTags.WALL_HANGING_SIGNS);
    }
}