package org.onenonly.bitsandbalance.fabric.mixin.litematica;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Renders Litematica previews through the material-aware enhanced slab path. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.render.schematic.WorldRendererSchematic", remap = false)
public abstract class LitematicaEnhancedSlabModelContextMixin {
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void bitsandbalance$rememberEnhancedSlabModelContext(BlockAndTintGetter level,
                                                                  BlockState state,
                                                                  BlockPos pos,
                                                                  PoseStack poseStack,
                                                                  BufferBuilder buffer,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        if (EnhancedSlabTerrainRenderHelper.renderCreateContraption(
                Minecraft.getInstance().getBlockRenderer(),
                level,
                pos,
                state,
                poseStack,
                buffer
        )) {
            cir.setReturnValue(true);
        }
    }
}
