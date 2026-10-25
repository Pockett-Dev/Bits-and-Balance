package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockRenderDispatcher.class)
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
        if (!FabricTweaksConfig.enableEnhancedSlabs || state == null) {
            return;
        }
        Object block = state.getBlock();
        if (!(block instanceof MixedSlabBlock)
                && !(block instanceof VerticalSlabBlock)
                && !(block instanceof StepBlock)
                && !(block instanceof VerticalStepBlock)
                && !(block instanceof QuadStepBlock)
                && !(block instanceof QuadVerticalStepBlock)) {
            return;
        }
        if (EnhancedSlabTerrainRenderHelper.render((BlockRenderDispatcher) (Object) this, level, pos, state, poseStack, buffer)) {
            ci.cancel();
        }
    }
}
