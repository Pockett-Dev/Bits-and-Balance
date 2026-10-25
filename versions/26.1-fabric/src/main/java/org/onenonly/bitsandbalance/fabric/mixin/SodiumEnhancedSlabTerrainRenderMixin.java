package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class SodiumEnhancedSlabTerrainRenderMixin {
    @Unique
    private static final Logger bitsandbalance$logger = LoggerFactory.getLogger("bitsandbalance-sodium-enhanced-slab-render");

    @Unique
    private static final AtomicBoolean bitsandbalance$loggedHandledState = new AtomicBoolean(false);

    @Shadow(remap = false)
    public abstract void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin);

    @Inject(
            method = "renderModel",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$renderEnhancedSlabTerrainForSodium(BlockStateModel model,
                                                                   BlockState state,
                                                                   BlockPos pos,
                                                                   BlockPos origin,
                                                                   CallbackInfo ci) {
        if (EnhancedSlabTerrainRenderHelper.handles(state) && bitsandbalance$loggedHandledState.compareAndSet(false, true)) {
            bitsandbalance$logger.info(
                    "Sodium enhanced slab hook reached block={} pos={}",
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                    pos
            );
        }
        BlockAndTintGetter level = this instanceof SodiumAbstractBlockRenderContextAccessorMixin accessor
                ? accessor.bitsandbalance$getLevel()
                : null;
        if (EnhancedSlabTerrainRenderHelper.renderSodium(this::renderModel, level, state, pos, origin)) {
            ci.cancel();
        }
    }
}
