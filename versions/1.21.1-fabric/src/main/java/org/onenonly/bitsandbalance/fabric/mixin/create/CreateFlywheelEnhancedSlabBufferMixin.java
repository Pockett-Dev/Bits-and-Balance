package org.onenonly.bitsandbalance.fabric.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = {
        "dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer",
        "com.simibubi.create.client.flywheel.lib.model.baked.BakedModelBufferer",
        "com.zurrtum.create.client.flywheel.lib.model.baked.BakedModelBufferer"
}, remap = false)
public abstract class CreateFlywheelEnhancedSlabBufferMixin {
    @WrapOperation(
            method = {"bufferBlocks", "bufferModel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JI)V"
            ),
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEnhancedSlabFlywheelGeometry(ModelBlockRenderer renderer,
                                                                          BlockAndTintGetter level,
                                                                          BakedModel model,
                                                                          BlockState state,
                                                                          BlockPos pos,
                                                                          PoseStack poseStack,
                                                                          VertexConsumer consumer,
                                                                          boolean checkSides,
                                                                          RandomSource random,
                                                                          long seed,
                                                                          int overlay,
                                                                          Operation<Void> original) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        if (EnhancedSlabTerrainRenderHelper.renderCreateContraption(dispatcher, level, pos, state, poseStack, consumer, overlay)) {
            return;
        }
        original.call(renderer, level, model, state, pos, poseStack, consumer, checkSides, random, seed, overlay);
    }
}
