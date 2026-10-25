package org.onenonly.bitsandbalance.fabric.mixin.create;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

@Pseudo
@Mixin(targets = {
        "com.zurrtum.create.client.flywheel.lib.model.baked.BakedModelBufferer",
        "com.simibubi.create.client.flywheel.lib.model.baked.BakedModelBufferer"
}, remap = false)
public abstract class CreateZurrtumFlywheelEnhancedSlabBufferMixin {
    @Inject(
            method = "bufferBlocks",
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEnhancedSlabFlywheelGeometry(Iterator<BlockPos> posIterator,
                                                                          BlockAndTintGetter level,
                                                                          PoseStack poseStack,
                                                                          boolean renderFluids,
                                                                          @Coerce Object blockMaterialFunction,
                                                                          CallbackInfoReturnable<?> cir,
                                                                          @Coerce Object objects,
                                                                          RandomSource random,
                                                                          @Coerce Object emitters,
                                                                          @Coerce Object transformingWrapper,
                                                                          BlockRenderDispatcher renderDispatcher,
                                                                          ModelBlockRenderer blockRenderer,
                                                                          boolean aoEnabled,
                                                                          BlockPos pos,
                                                                          BlockState state,
                                                                          BlockStateModel model,
                                                                          List<BlockModelPart> parts) {
        if (level == null || poseStack == null || emitters == null || renderDispatcher == null
                || blockRenderer == null || pos == null || state == null || parts == null || !parts.isEmpty()) {
            return;
        }

        CreateEnhancedSlabContraptionRenderBridge.renderFlywheel(
                emitters,
                renderDispatcher,
                blockRenderer,
                level,
                poseStack,
                pos,
                state,
                aoEnabled
        );
    }
}
