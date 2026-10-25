package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.block.BlockRenderDispatcher")
public abstract class EnhancedSlabTerrainRenderMixin {

    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$renderEnhancedSlabsInTerrain(BlockState state,
                                                             BlockPos pos,
                                                             BlockAndTintGetter level,
                                                             PoseStack poseStack,
                                                             VertexConsumer buffer,
                                                             boolean checkSides,
                                                             List<?> renderTypes,
                                                             CallbackInfo ci) {
        if (EnhancedSlabTerrainRenderHelper.render(this, level, pos, state, poseStack, buffer)) {
            ci.cancel();
        }
    }
}