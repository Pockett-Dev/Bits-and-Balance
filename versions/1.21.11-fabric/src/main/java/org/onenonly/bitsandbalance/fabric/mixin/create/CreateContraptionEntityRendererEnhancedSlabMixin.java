package org.onenonly.bitsandbalance.fabric.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer", remap = false)
public abstract class CreateContraptionEntityRendererEnhancedSlabMixin {
    @WrapOperation(
            method = "buildStructureBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V"
            ),
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEnhancedSlabContraptionGeometry(ModelBlockRenderer renderer,
                                                                             BlockAndTintGetter level,
                                                                             List<BlockModelPart> parts,
                                                                             BlockState state,
                                                                             BlockPos pos,
                                                                             PoseStack poseStack,
                                                                             VertexConsumer consumer,
                                                                             boolean checkSides,
                                                                             int overlay,
                                                                             Operation<Void> original) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        if (EnhancedSlabTerrainRenderHelper.renderCreateContraption(dispatcher, level, pos, state, poseStack, consumer)) {
            return;
        }
        original.call(renderer, level, parts, state, pos, poseStack, consumer, checkSides, overlay);
    }
}
