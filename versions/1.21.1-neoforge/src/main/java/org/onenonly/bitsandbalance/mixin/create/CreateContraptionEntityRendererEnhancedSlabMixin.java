package org.onenonly.bitsandbalance.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer", remap = false)
public abstract class CreateContraptionEntityRendererEnhancedSlabMixin {
    @WrapOperation(
            method = "buildStructureBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"
            ),
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEnhancedSlabContraptionGeometry(ModelBlockRenderer renderer,
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
                                                                             ModelData modelData,
                                                                             RenderType renderType,
                                                                             Operation<Void> original) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        if (EnhancedSlabTerrainRenderHelper.renderCreateContraption(dispatcher, level, pos, state, poseStack, consumer, overlay)) {
            return;
        }
        original.call(renderer, level, model, state, pos, poseStack, consumer, checkSides, random, seed, overlay, modelData, renderType);
    }
}
