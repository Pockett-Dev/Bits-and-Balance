package org.onenonly.bitsandbalance.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
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
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer", remap = false)
public abstract class CreateFlywheelEnhancedSlabBufferMixin {
    @Inject(
            method = "bufferBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;iterator()Ljava/util/Iterator;"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEmptyEnhancedSlabFlywheelBlock(Iterator<BlockPos> posIterator,
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
                                                                            long seed,
                                                                            BakedModel model,
                                                                            ModelData modelData,
                                                                            ChunkRenderTypeSet renderTypes) {
        if (!bitsandbalance$shouldRenderFallback(model, state, modelData, renderTypes, seed)) {
            return;
        }

        CreateEnhancedSlabContraptionRenderBridge.renderFlywheelBlockFallback(
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

    @Inject(
            method = "bufferModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;iterator()Ljava/util/Iterator;"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            remap = false,
            require = 0
    )
    private static void bitsandbalance$renderEmptyEnhancedSlabFlywheelModel(BakedModel model,
                                                                            BlockPos pos,
                                                                            BlockAndTintGetter level,
                                                                            BlockState state,
                                                                            PoseStack poseStack,
                                                                            @Coerce Object blockMaterialFunction,
                                                                            CallbackInfoReturnable<?> cir,
                                                                            @Coerce Object objects,
                                                                            RandomSource random,
                                                                            @Coerce Object emitters,
                                                                            ModelBlockRenderer blockRenderer,
                                                                            long seed,
                                                                            ModelData modelData,
                                                                            ChunkRenderTypeSet renderTypes,
                                                                            boolean defaultAo,
                                                                            boolean aoEnabled) {
        if (!bitsandbalance$shouldRenderFallback(model, state, modelData, renderTypes, seed)) {
            return;
        }

        CreateEnhancedSlabContraptionRenderBridge.renderFlywheelModelFallback(
                emitters,
                Minecraft.getInstance().getBlockRenderer(),
                blockRenderer,
                level,
                poseStack,
                pos,
                state,
                aoEnabled
        );
    }

    @WrapOperation(
            method = {"bufferBlocks", "bufferModel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"
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
                                                                          ModelData modelData,
                                                                          RenderType renderType,
                                                                          Operation<Void> original) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        if (EnhancedSlabTerrainRenderHelper.renderCreateContraption(dispatcher, level, pos, state, poseStack, consumer, overlay)) {
            return;
        }
        original.call(renderer, level, model, state, pos, poseStack, consumer, checkSides, random, seed, overlay, modelData, renderType);
    }

    private static boolean bitsandbalance$shouldRenderFallback(BakedModel model,
                                                               BlockState state,
                                                               ModelData modelData,
                                                               ChunkRenderTypeSet renderTypes,
                                                               long seed) {
        if (model == null || !EnhancedSlabTerrainRenderHelper.handles(state)) {
            return false;
        }
        return !bitsandbalance$hasAnyUnculledQuads(model, state, modelData, renderTypes, seed);
    }

    private static boolean bitsandbalance$hasAnyUnculledQuads(BakedModel model,
                                                              BlockState state,
                                                              ModelData modelData,
                                                              ChunkRenderTypeSet renderTypes,
                                                              long seed) {
        if (renderTypes == null || renderTypes.isEmpty()) {
            return false;
        }

        for (RenderType renderType : renderTypes) {
            try {
                RandomSource random = RandomSource.create(seed);
                List<BakedQuad> quads = model.getQuads(state, null, random, modelData, renderType);
                if (quads != null && !quads.isEmpty()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return false;
    }
}
