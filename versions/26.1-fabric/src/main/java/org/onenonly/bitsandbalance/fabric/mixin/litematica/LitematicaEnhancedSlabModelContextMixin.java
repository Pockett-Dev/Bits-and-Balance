package org.onenonly.bitsandbalance.fabric.mixin.litematica;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the world and position that Litematica drops when it asks a cached
 * block-state model for its parts. Enhanced slab-family models need that
 * position to select the materials stored for each schematic block.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic", remap = false)
public abstract class LitematicaEnhancedSlabModelContextMixin {
    @Inject(method = "tessellateBlock", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void bitsandbalance$rememberEnhancedSlabModelContext(BlockAndTintGetter level,
                                                                  BlockState state,
                                                                  BlockPos pos,
                                                                  Vec3 offset,
                                                                  BlockStateModel model,
                                                                  long seed,
                                                                  @Coerce Object output,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        Boolean rendered = EnhancedSlabTerrainRenderHelper.tessellateLitematicaPreview(
                this,
                level,
                state,
                pos,
                offset,
                seed,
                output
        );
        if (rendered != null) {
            cir.setReturnValue(rendered);
            return;
        }
        EnhancedSlabTerrainRenderHelper.rememberCreateModelContext(level, pos, state);
    }
}
