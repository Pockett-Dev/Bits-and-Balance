package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;

final class LegacyEnhancedSlabRenderHelper {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final int ALL_DIRECTIONS = directionMask(Direction.values());
    private static final int HORIZONTAL_DIRECTIONS = directionMask(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private LegacyEnhancedSlabRenderHelper() {
    }

    public static Direction mixedBottomHiddenFace(BlockRenderDispatcher dispatcher, BlockState bottomSlabState, BlockState topSlabState) {
        if (bottomSlabState == null || topSlabState == null) {
            return null;
        }
        return sourceModelHeight(dispatcher, horizontalSlabRenderState(bottomSlabState, SlabType.BOTTOM)) >= 7.999F ? Direction.UP : null;
    }

    public static void renderHorizontalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, SlabType half, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderHorizontalSlab(dispatcher, level, pos, slabState, half, null, poseStack, buffer, packedLight, packedOverlay);
    }

    public static void renderHorizontalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, SlabType half, Direction hiddenFace, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(slabState.getBlock())) {
            BlockState source = horizontalSlabRenderState(slabState, half);
            dispatcher.getModelRenderer().tesselateBlock(level,
                    createHorizontalSlabModel(dispatcher, slabState, half, hiddenFace), source, pos,
                    poseStack, buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(source)),
                    false, seededRandom(), source.getSeed(pos), packedOverlay);
            return;
        }
        float yMin;
        float yMax;
        if (half == SlabType.TOP) {
            yMin = 8.0F;
            yMax = 16.0F;
        } else if (half == SlabType.DOUBLE) {
            yMin = 0.0F;
            yMax = 16.0F;
        } else {
            yMin = 0.0F;
            yMax = 8.0F;
        }

        renderPrism(dispatcher, level, pos, horizontalSlabRenderState(slabState, half), new Vector3f(0.0F, yMin, 0.0F), new Vector3f(16.0F, yMax, 16.0F), hiddenFace, poseStack, buffer, packedOverlay);
    }

    public static void renderHorizontalSlabToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, SlabType half, Direction hiddenFace, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(slabState.getBlock())) {
            BlockState source = horizontalSlabRenderState(slabState, half);
            dispatcher.getModelRenderer().tesselateBlock(level,
                    createHorizontalSlabModel(dispatcher, slabState, half, hiddenFace), source, pos,
                    poseStack, vertexConsumer, false, seededRandom(), source.getSeed(pos), packedOverlay);
            return;
        }
        float yMin;
        float yMax;
        if (half == SlabType.TOP) {
            yMin = 8.0F;
            yMax = 16.0F;
        } else if (half == SlabType.DOUBLE) {
            yMin = 0.0F;
            yMax = 16.0F;
        } else {
            yMin = 0.0F;
            yMax = 8.0F;
        }

        renderPrismToBuffer(dispatcher, level, pos, horizontalSlabRenderState(slabState, half), new Vector3f(0.0F, yMin, 0.0F), new Vector3f(16.0F, yMax, 16.0F), hiddenFace, poseStack, vertexConsumer, packedOverlay);
    }

    public static void renderStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, boolean top, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderStep(dispatcher, level, pos, slabState, facing, top, poseStack, buffer, packedLight, packedOverlay, ALL_DIRECTIONS);
    }

    public static void renderStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, boolean top, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int visibleMask) {
        if (renderPreservedSourceModel(dispatcher, level, pos, slabState, ModelType.STEP, stepPartState(facing, top), poseStack, buffer, packedOverlay)) {
            return;
        }
        float xMin;
        float xMax;
        float zMin;
        float zMax;

        switch (facing) {
            case WEST -> {
                xMin = 0.0F;
                xMax = 8.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            case SOUTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 8.0F;
                zMax = 16.0F;
            }
            case NORTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 8.0F;
            }
            case EAST -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            default -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
        }

        float yMin = top ? 8.0F : 0.0F;
        float yMax = top ? 16.0F : 8.0F;
        Vector3f from = new Vector3f(xMin, yMin, zMin);
        Vector3f to = new Vector3f(xMax, yMax, zMax);
        BlockState renderState = stepRenderState(slabState, top);
        if (renderJoinedStepSidePrisms(dispatcher, level, pos, renderState, top, from, to, visibleMask, poseStack, buffer, packedOverlay)) {
            return;
        }
        renderPrism(dispatcher, level, pos, renderState, from, to, visibleMask, poseStack, buffer, packedOverlay);
    }

    public static void renderVerticalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction half, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return;
        }
        if (renderPreservedSourceModel(dispatcher, level, pos, slabState, ModelType.VERTICAL_SLAB, verticalSlabPartState(half), poseStack, buffer, packedOverlay)) {
            return;
        }

        Vector3f from = switch (half) {
            case SOUTH -> new Vector3f(0.0F, 0.0F, 8.0F);
            case EAST -> new Vector3f(8.0F, 0.0F, 0.0F);
            case NORTH, WEST -> new Vector3f(0.0F, 0.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 0.0F);
        };
        Vector3f to = switch (half) {
            case SOUTH, EAST -> new Vector3f(16.0F, 16.0F, 16.0F);
            case NORTH -> new Vector3f(16.0F, 16.0F, 8.0F);
            case WEST -> new Vector3f(8.0F, 16.0F, 16.0F);
            default -> new Vector3f(16.0F, 16.0F, 16.0F);
        };

        renderPrismExact(dispatcher, level, pos, verticalPrismRenderState(slabState), from, to, ALL_DIRECTIONS, true, poseStack, buffer, packedOverlay);
    }

    public static void renderVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, Direction side, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderVerticalStep(dispatcher, level, pos, slabState, facing, side, poseStack, buffer, packedLight, packedOverlay, ALL_DIRECTIONS);
    }

    public static void renderVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, Direction side, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int visibleMask) {
        if (renderPreservedSourceModel(dispatcher, level, pos, slabState, ModelType.VERTICAL_STEP, verticalStepPartState(facing, side), poseStack, buffer, packedOverlay)) {
            return;
        }
        float xMin = side == Direction.EAST ? 8.0F : 0.0F;
        float xMax = side == Direction.EAST ? 16.0F : 8.0F;
        float zMin = facing == Direction.SOUTH ? 8.0F : 0.0F;
        float zMax = facing == Direction.SOUTH ? 16.0F : 8.0F;

        renderPrismExact(dispatcher, level, pos, verticalPrismRenderState(slabState), new Vector3f(xMin, 0.0F, zMin), new Vector3f(xMax, 16.0F, zMax), visibleMask, true, poseStack, buffer, packedOverlay);
    }

    private static boolean renderPreservedSourceModel(BlockRenderDispatcher dispatcher,
                                                       BlockAndTintGetter level,
                                                       BlockPos pos,
                                                       BlockState slabState,
                                                       ModelType modelType,
                                                       BlockState partState,
                                                       PoseStack poseStack,
                                                       MultiBufferSource buffer,
                                                       int packedOverlay) {
        BlockState sourceState = DynamicVariantMaterialHelper.normalizeSlabState(slabState);
        if (sourceState == null || !SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(sourceState.getBlock())) {
            return false;
        }

        BlockState geometryState = SlabFamilyModelSourceSelector.thatchGeometrySourceState(sourceState, modelType);
        BakedModel sourceModel = dispatcher.getBlockModel(geometryState);
        BakedModel transformed = EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true, partState,
                EnhancedSlabBlockBakedModel.topStepSourceModel(sourceState, modelType, true));
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                transformed,
                sourceState,
                pos,
                poseStack,
                buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(sourceState)),
                false,
                seededRandom(),
                sourceState.getSeed(pos),
                packedOverlay
        );
        return true;
    }

    private static boolean renderPreservedSourceModelToBuffer(BlockRenderDispatcher dispatcher,
                                                               BlockAndTintGetter level,
                                                               BlockPos pos,
                                                               BlockState slabState,
                                                               ModelType modelType,
                                                               BlockState partState,
                                                               PoseStack poseStack,
                                                               VertexConsumer vertexConsumer,
                                                               int packedOverlay) {
        BlockState sourceState = DynamicVariantMaterialHelper.normalizeSlabState(slabState);
        if (sourceState == null || !SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(sourceState.getBlock())) {
            return false;
        }

        BlockState geometryState = SlabFamilyModelSourceSelector.thatchGeometrySourceState(sourceState, modelType);
        BakedModel sourceModel = dispatcher.getBlockModel(geometryState);
        BakedModel transformed = EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true, partState,
                EnhancedSlabBlockBakedModel.topStepSourceModel(sourceState, modelType, true));
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                transformed,
                sourceState,
                pos,
                poseStack,
                vertexConsumer,
                false,
                seededRandom(),
                sourceState.getSeed(pos),
                packedOverlay
        );
        return true;
    }

    private static BlockState stepPartState(Direction facing, boolean top) {
        BlockState state = CommonBlocks.STEP.defaultBlockState();
        return state.setValue(StepBlock.FACING, facing).setValue(StepBlock.TOP, top);
    }

    private static BlockState verticalSlabPartState(Direction facing) {
        BlockState state = CommonBlocks.VERTICAL_SLAB.defaultBlockState();
        return state.setValue(VerticalSlabBlock.FACING, facing).setValue(VerticalSlabBlock.DOUBLE, false);
    }

    private static BlockState verticalStepPartState(Direction facing, Direction side) {
        BlockState state = CommonBlocks.VERTICAL_STEP.defaultBlockState();
        return state.setValue(VerticalStepBlock.FACING, facing).setValue(VerticalStepBlock.SIDE, side);
    }

    public static void renderSourceModelToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState sourceState, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (level == null || pos == null || sourceState == null) {
            return;
        }

        BakedModel model = dispatcher.getBlockModel(sourceState);
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                model,
                sourceState,
                pos,
                poseStack,
                vertexConsumer,
                false,
                seededRandom(),
                sourceState.getSeed(pos),
                packedOverlay
        );
    }

    public static void renderThatchHalfBlockToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level,
                                                      BlockPos pos, BlockState fullState, Direction half,
                                                      PoseStack poseStack, VertexConsumer consumer, int packedOverlay) {
        BakedModel model = ThatchHalfBlockBakedModel.wrap(dispatcher.getBlockModel(fullState), fullState, half);
        dispatcher.getModelRenderer().tesselateBlock(level, model, fullState, pos, poseStack, consumer,
                false, seededRandom(), fullState.getSeed(pos), packedOverlay);
    }

    public static void renderThatchHalfBlock(BlockRenderDispatcher dispatcher, BlockAndTintGetter level,
                                              BlockPos pos, BlockState fullState, Direction half,
                                              PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        BakedModel model = ThatchHalfBlockBakedModel.wrap(dispatcher.getBlockModel(fullState), fullState, half);
        dispatcher.getModelRenderer().tesselateBlock(level, model, fullState, pos, poseStack,
                buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(fullState)), false,
                seededRandom(), fullState.getSeed(pos), packedOverlay);
    }

    public static void renderSourceModel(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState sourceState, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (level == null || pos == null || sourceState == null) {
            return;
        }

        BakedModel model = dispatcher.getBlockModel(sourceState);
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                model,
                sourceState,
                pos,
                poseStack,
                buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(sourceState)),
                false,
                seededRandom(),
                sourceState.getSeed(pos),
                packedOverlay
        );
    }

    public static void renderStepToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, boolean top, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        renderStepToBuffer(dispatcher, level, pos, slabState, facing, top, poseStack, vertexConsumer, packedOverlay, ALL_DIRECTIONS);
    }

    public static void renderStepToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, boolean top, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay, int visibleMask) {
        if (renderPreservedSourceModelToBuffer(dispatcher, level, pos, slabState, ModelType.STEP, stepPartState(facing, top), poseStack, vertexConsumer, packedOverlay)) {
            return;
        }
        float xMin;
        float xMax;
        float zMin;
        float zMax;

        switch (facing) {
            case WEST -> {
                xMin = 0.0F;
                xMax = 8.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            case SOUTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 8.0F;
                zMax = 16.0F;
            }
            case NORTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 8.0F;
            }
            case EAST -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            default -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
        }

        float yMin = top ? 8.0F : 0.0F;
        float yMax = top ? 16.0F : 8.0F;
        Vector3f from = new Vector3f(xMin, yMin, zMin);
        Vector3f to = new Vector3f(xMax, yMax, zMax);
        BlockState renderState = stepRenderState(slabState, top);
        if (renderJoinedStepSidePrismsToBuffer(dispatcher, level, pos, renderState, top, from, to, visibleMask, poseStack, vertexConsumer, packedOverlay)) {
            return;
        }
        renderPrismToBuffer(dispatcher, level, pos, renderState, from, to, visibleMask, poseStack, vertexConsumer, packedOverlay);
    }

    public static void renderVerticalSlabToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction half, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return;
        }
        if (renderPreservedSourceModelToBuffer(dispatcher, level, pos, slabState, ModelType.VERTICAL_SLAB, verticalSlabPartState(half), poseStack, vertexConsumer, packedOverlay)) {
            return;
        }

        Vector3f from = switch (half) {
            case SOUTH -> new Vector3f(0.0F, 0.0F, 8.0F);
            case EAST -> new Vector3f(8.0F, 0.0F, 0.0F);
            case NORTH, WEST -> new Vector3f(0.0F, 0.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 0.0F);
        };
        Vector3f to = switch (half) {
            case SOUTH, EAST -> new Vector3f(16.0F, 16.0F, 16.0F);
            case NORTH -> new Vector3f(16.0F, 16.0F, 8.0F);
            case WEST -> new Vector3f(8.0F, 16.0F, 16.0F);
            default -> new Vector3f(16.0F, 16.0F, 16.0F);
        };

        renderPrismExactToBuffer(dispatcher, level, pos, verticalPrismRenderState(slabState), from, to, ALL_DIRECTIONS, true, poseStack, vertexConsumer, packedOverlay);
    }

    public static void renderVerticalStepToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, Direction side, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        renderVerticalStepToBuffer(dispatcher, level, pos, slabState, facing, side, poseStack, vertexConsumer, packedOverlay, ALL_DIRECTIONS);
    }

    public static void renderVerticalStepToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Direction facing, Direction side, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay, int visibleMask) {
        if (renderPreservedSourceModelToBuffer(dispatcher, level, pos, slabState, ModelType.VERTICAL_STEP, verticalStepPartState(facing, side), poseStack, vertexConsumer, packedOverlay)) {
            return;
        }
        float xMin = side == Direction.EAST ? 8.0F : 0.0F;
        float xMax = side == Direction.EAST ? 16.0F : 8.0F;
        float zMin = facing == Direction.SOUTH ? 8.0F : 0.0F;
        float zMax = facing == Direction.SOUTH ? 16.0F : 8.0F;

        renderPrismExactToBuffer(dispatcher, level, pos, verticalPrismRenderState(slabState), new Vector3f(xMin, 0.0F, zMin), new Vector3f(xMax, 16.0F, zMax), visibleMask, true, poseStack, vertexConsumer, packedOverlay);
    }

    static BakedModel createHorizontalSlabModel(BlockRenderDispatcher dispatcher, BlockState slabState, SlabType half, Direction hiddenFace) {
        if (SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(slabState.getBlock())) {
            BlockState source = horizontalSlabRenderState(slabState, half);
            return ThatchHalfBlockBakedModel.wrapSlab(dispatcher.getBlockModel(source), source, hiddenFace);
        }
        float yMin;
        float yMax;
        if (half == SlabType.TOP) {
            yMin = 8.0F;
            yMax = 16.0F;
        } else if (half == SlabType.DOUBLE) {
            yMin = 0.0F;
            yMax = 16.0F;
        } else {
            yMin = 0.0F;
            yMax = 8.0F;
        }

        return createPrismModel(dispatcher, horizontalSlabRenderState(slabState, half), new Vector3f(0.0F, yMin, 0.0F), new Vector3f(16.0F, yMax, 16.0F), without(ALL_DIRECTIONS, hiddenFace));
    }

    static BakedModel createStepModel(BlockRenderDispatcher dispatcher, BlockState slabState, Direction facing, boolean top, int visibleMask) {
        float xMin;
        float xMax;
        float zMin;
        float zMax;

        switch (facing) {
            case WEST -> {
                xMin = 0.0F;
                xMax = 8.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            case SOUTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 8.0F;
                zMax = 16.0F;
            }
            case NORTH -> {
                xMin = 0.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 8.0F;
            }
            case EAST -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
            default -> {
                xMin = 8.0F;
                xMax = 16.0F;
                zMin = 0.0F;
                zMax = 16.0F;
            }
        }

        float yMin = top ? 8.0F : 0.0F;
        float yMax = top ? 16.0F : 8.0F;
        return createPrismModel(dispatcher, stepRenderState(slabState, top), new Vector3f(xMin, yMin, zMin), new Vector3f(xMax, yMax, zMax), visibleMask);
    }

    static BakedModel createVerticalSlabModel(BlockRenderDispatcher dispatcher, BlockState slabState, Direction half, int visibleMask) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return null;
        }

        Vector3f from = switch (half) {
            case SOUTH -> new Vector3f(0.0F, 0.0F, 8.0F);
            case EAST -> new Vector3f(8.0F, 0.0F, 0.0F);
            case NORTH, WEST -> new Vector3f(0.0F, 0.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 0.0F);
        };
        Vector3f to = switch (half) {
            case SOUTH, EAST -> new Vector3f(16.0F, 16.0F, 16.0F);
            case NORTH -> new Vector3f(16.0F, 16.0F, 8.0F);
            case WEST -> new Vector3f(8.0F, 16.0F, 16.0F);
            default -> new Vector3f(16.0F, 16.0F, 16.0F);
        };

        return createPrismModel(dispatcher, verticalPrismRenderState(slabState), from, to, visibleMask, false);
    }

    static BakedModel createVerticalStepModel(BlockRenderDispatcher dispatcher, BlockState slabState, Direction facing, Direction side, int visibleMask) {
        float xMin = side == Direction.EAST ? 8.0F : 0.0F;
        float xMax = side == Direction.EAST ? 16.0F : 8.0F;
        float zMin = facing == Direction.SOUTH ? 8.0F : 0.0F;
        float zMax = facing == Direction.SOUTH ? 16.0F : 8.0F;

        return createPrismModel(dispatcher, verticalPrismRenderState(slabState), new Vector3f(xMin, 0.0F, zMin), new Vector3f(xMax, 16.0F, zMax), visibleMask, false);
    }

    private static BakedModel createPrismModel(BlockRenderDispatcher dispatcher, BlockState slabState, Vector3f from, Vector3f to, int visibleMask) {
        return createPrismModel(dispatcher, slabState, from, to, visibleMask, true);
    }

    private static BakedModel createPrismModel(BlockRenderDispatcher dispatcher, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, boolean preserveSourceSideUv) {
        if (dispatcher == null || slabState == null) {
            return null;
        }

        BlockState renderState = prepareSourceState(slabState);
        BakedModel sourceModel = dispatcher.getBlockModel(renderState);
        return sourceModel == null ? null : PrismBakedModel.create(sourceModel, renderState, from, to, visibleMask, true, preserveSourceSideUv);
    }

    private static void renderPrism(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        renderPrism(dispatcher, level, pos, slabState, from, to, null, poseStack, buffer, packedOverlay);
    }

    private static void renderPrism(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, Direction hiddenFace, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        renderPrism(dispatcher, level, pos, slabState, from, to, without(ALL_DIRECTIONS, hiddenFace), poseStack, buffer, packedOverlay);
    }

    private static void renderPrism(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (level == null || pos == null || slabState == null) {
            return;
        }

        BlockState renderState = prepareSourceState(slabState);
        BakedModel sourceModel = dispatcher.getBlockModel(renderState);
        BakedModel prismModel = PrismBakedModel.create(sourceModel, renderState, from, to, visibleMask);
        if (prismModel == null) {
            return;
        }

        RenderType renderType = ItemBlockRenderTypes.getMovingBlockRenderType(renderState);
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                prismModel,
                renderState,
                pos,
                poseStack,
                buffer.getBuffer(renderType),
                false,
                seededRandom(),
                renderState.getSeed(pos),
                packedOverlay
        );
    }

    private static void renderPrismExact(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        renderPrismExact(dispatcher, level, pos, slabState, from, to, visibleMask, false, poseStack, buffer, packedOverlay);
    }

    private static void renderPrismExact(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, boolean preserveSourceSideUv, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (level == null || pos == null || slabState == null) {
            return;
        }

        BlockState renderState = prepareSourceState(slabState);
        BakedModel sourceModel = dispatcher.getBlockModel(renderState);
        BakedModel prismModel = PrismBakedModel.createExact(sourceModel, renderState, from, to, visibleMask, preserveSourceSideUv);
        if (prismModel == null) {
            return;
        }

        RenderType renderType = ItemBlockRenderTypes.getMovingBlockRenderType(renderState);
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                prismModel,
                renderState,
                pos,
                poseStack,
                buffer.getBuffer(renderType),
                false,
                seededRandom(),
                renderState.getSeed(pos),
                packedOverlay
        );
    }

    private static void renderPrismToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        renderPrismToBuffer(dispatcher, level, pos, slabState, from, to, null, poseStack, vertexConsumer, packedOverlay);
    }

    private static void renderPrismToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, Direction hiddenFace, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        renderPrismToBuffer(dispatcher, level, pos, slabState, from, to, without(ALL_DIRECTIONS, hiddenFace), poseStack, vertexConsumer, packedOverlay);
    }

    private static void renderPrismToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (level == null || pos == null || slabState == null) {
            return;
        }

        BlockState renderState = prepareSourceState(slabState);
        BakedModel sourceModel = dispatcher.getBlockModel(renderState);
        BakedModel prismModel = PrismBakedModel.create(sourceModel, renderState, from, to, visibleMask);
        if (prismModel == null) {
            return;
        }

        dispatcher.getModelRenderer().tesselateBlock(
                level,
                prismModel,
                renderState,
                pos,
                poseStack,
                vertexConsumer,
                false,
                seededRandom(),
                renderState.getSeed(pos),
                packedOverlay
        );
    }

    private static void renderPrismExactToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        renderPrismExactToBuffer(dispatcher, level, pos, slabState, from, to, visibleMask, false, poseStack, vertexConsumer, packedOverlay);
    }

    private static void renderPrismExactToBuffer(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState slabState, Vector3f from, Vector3f to, int visibleMask, boolean preserveSourceSideUv, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (level == null || pos == null || slabState == null) {
            return;
        }

        BlockState renderState = prepareSourceState(slabState);
        BakedModel sourceModel = dispatcher.getBlockModel(renderState);
        BakedModel prismModel = PrismBakedModel.createExact(sourceModel, renderState, from, to, visibleMask, preserveSourceSideUv);
        if (prismModel == null) {
            return;
        }

        dispatcher.getModelRenderer().tesselateBlock(
                level,
                prismModel,
                renderState,
                pos,
                poseStack,
                vertexConsumer,
                false,
                seededRandom(),
                renderState.getSeed(pos),
                packedOverlay
        );
    }

    private static boolean renderJoinedStepSidePrisms(BlockRenderDispatcher dispatcher,
                                                      BlockAndTintGetter level,
                                                      BlockPos pos,
                                                      BlockState slabState,
                                                      boolean top,
                                                      Vector3f from,
                                                      Vector3f to,
                                                      int visibleMask,
                                                      PoseStack poseStack,
                                                      MultiBufferSource buffer,
                                                      int packedOverlay) {
        JoinedStepSide side = joinedStepSide(dispatcher, slabState, top, from, to, visibleMask);
        if (side == null) {
            return false;
        }
        if (side.bodyMask() != 0) {
            renderPrism(dispatcher, level, pos, slabState, from, to, side.bodyMask(), poseStack, buffer, packedOverlay);
        }
        renderPrismExact(dispatcher, level, pos, side.renderState(), side.from(), side.to(), side.sideMask(), poseStack, buffer, packedOverlay);
        return true;
    }

    private static boolean renderJoinedStepSidePrismsToBuffer(BlockRenderDispatcher dispatcher,
                                                              BlockAndTintGetter level,
                                                              BlockPos pos,
                                                              BlockState slabState,
                                                              boolean top,
                                                              Vector3f from,
                                                              Vector3f to,
                                                              int visibleMask,
                                                              PoseStack poseStack,
                                                              VertexConsumer vertexConsumer,
                                                              int packedOverlay) {
        JoinedStepSide side = joinedStepSide(dispatcher, slabState, top, from, to, visibleMask);
        if (side == null) {
            return false;
        }
        if (side.bodyMask() != 0) {
            renderPrismToBuffer(dispatcher, level, pos, slabState, from, to, side.bodyMask(), poseStack, vertexConsumer, packedOverlay);
        }
        renderPrismExactToBuffer(dispatcher, level, pos, side.renderState(), side.from(), side.to(), side.sideMask(), poseStack, vertexConsumer, packedOverlay);
        return true;
    }

    private static JoinedStepSide joinedStepSide(BlockRenderDispatcher dispatcher, BlockState slabState, boolean top, Vector3f from, Vector3f to, int visibleMask) {
        if (!hasJoinedVerticalNeighbor(top, visibleMask)) {
            return null;
        }

        int sideMask = visibleMask & HORIZONTAL_DIRECTIONS;
        if (sideMask == 0) {
            return null;
        }

        BlockState renderState = fullBlockStateForSlab(slabState);
        if (renderState == null) {
            renderState = prepareSourceState(slabState);
        }

        float sourceHeight = sourceModelHeight(dispatcher, prepareSourceState(slabState));
        float splitY = 8.0F;
        float sideMinY = top ? splitY : 0.0F;
        float sideMaxY = top ? splitY + sourceHeight : splitY;
        if (sourceHeight <= 0.001F || sideMaxY <= sideMinY + 0.001F) {
            return null;
        }

        Vector3f sideFrom = new Vector3f(from.x(), sideMinY, from.z());
        Vector3f sideTo = new Vector3f(to.x(), sideMaxY, to.z());
        return new JoinedStepSide(renderState, sideFrom, sideTo, sideMask, visibleMask & ~HORIZONTAL_DIRECTIONS);
    }

    private static boolean hasJoinedVerticalNeighbor(boolean top, int visibleMask) {
        Direction hidden = top ? Direction.DOWN : Direction.UP;
        return (visibleMask & (1 << hidden.ordinal())) == 0;
    }

    private static float sourceModelHeight(BlockRenderDispatcher dispatcher, BlockState state) {
        if (dispatcher == null || state == null) {
            return 8.0F;
        }
        try {
            BakedModel model = dispatcher.getBlockModel(state);
            SourceGeometry geometry = extractSourceGeometry(model, state, model.getParticleIcon());
            return geometry == null ? 8.0F : geometry.bounds().height();
        } catch (Throwable ignored) {
            return 8.0F;
        }
    }

    private static BlockState fullBlockStateForSlab(BlockState slabState) {
        if (slabState == null) {
            return null;
        }
        BlockState sourceState = prepareSourceState(slabState);
        if (sourceState == null) {
            return null;
        }
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(sourceState);
    }

    private static SourceGeometry extractSourceGeometry(BakedModel sourceModel, BlockState state, TextureAtlasSprite particleIcon) {
        EnumMap<Direction, List<FaceData>> faceData = new EnumMap<>(Direction.class);
        IdentityHashMap<BakedQuad, Boolean> seenQuads = new IdentityHashMap<>();
        SourceBounds bounds = new SourceBounds();

        List<BakedQuad> unculled = sourceModel.getQuads(state, null, seededRandom());
        FaceData fallback = addFaceData(faceData, unculled, null, null);
        addBounds(bounds, seenQuads, unculled);

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = sourceModel.getQuads(state, direction, seededRandom());
            fallback = addFaceData(faceData, quads, direction, fallback);
            addBounds(bounds, seenQuads, quads);
        }

        if (fallback == null && particleIcon != null) {
            fallback = new FaceData(particleIcon, -1, true, null);
        }
        if (fallback == null) {
            return new SourceGeometry(faceData, null, bounds);
        }

        for (Direction direction : Direction.values()) {
            faceData.computeIfAbsent(direction, unused -> new ArrayList<>());
        }
        return new SourceGeometry(faceData, fallback, bounds);
    }

    private static FaceData addFaceData(EnumMap<Direction, List<FaceData>> faceData, List<BakedQuad> quads, Direction requestedDirection, FaceData fallback) {
        if (quads == null) {
            return fallback;
        }

        FaceData resolvedFallback = fallback;
        for (BakedQuad quad : quads) {
            if (quad == null || quad.getSprite() == null) {
                continue;
            }

            FaceData data = new FaceData(quad.getSprite(), quad.getTintIndex(), quad.isShade(), extractFaceUv(quad));
            if (resolvedFallback == null) {
                resolvedFallback = data;
            }

            Direction direction = requestedDirection != null ? requestedDirection : classifyQuadDirection(quad);
            if (direction != null) {
                faceData.computeIfAbsent(direction, unused -> new ArrayList<>()).add(data);
            }
        }
        return resolvedFallback;
    }

    private static Direction classifyQuadDirection(BakedQuad quad) {
        if (quad == null) {
            return null;
        }

        int[] vertices = quad.getVertices();
        if (vertices == null || vertices.length < 12) {
            return quad.getDirection();
        }

        int stride = vertices.length / 4;
        float x0 = Float.intBitsToFloat(vertices[0]);
        float y0 = Float.intBitsToFloat(vertices[1]);
        float z0 = Float.intBitsToFloat(vertices[2]);
        float x1 = Float.intBitsToFloat(vertices[stride]);
        float y1 = Float.intBitsToFloat(vertices[stride + 1]);
        float z1 = Float.intBitsToFloat(vertices[stride + 2]);
        float x2 = Float.intBitsToFloat(vertices[stride * 2]);
        float y2 = Float.intBitsToFloat(vertices[stride * 2 + 1]);
        float z2 = Float.intBitsToFloat(vertices[stride * 2 + 2]);

        float edgeAx = x1 - x0;
        float edgeAy = y1 - y0;
        float edgeAz = z1 - z0;
        float edgeBx = x2 - x0;
        float edgeBy = y2 - y0;
        float edgeBz = z2 - z0;

        float normalX = edgeAy * edgeBz - edgeAz * edgeBy;
        float normalY = edgeAz * edgeBx - edgeAx * edgeBz;
        float normalZ = edgeAx * edgeBy - edgeAy * edgeBx;

        float absX = Math.abs(normalX);
        float absY = Math.abs(normalY);
        float absZ = Math.abs(normalZ);
        if (absX < 0.0001F && absY < 0.0001F && absZ < 0.0001F) {
            return quad.getDirection();
        }

        if (absY >= absX && absY >= absZ) {
            return normalY >= 0.0F ? Direction.UP : Direction.DOWN;
        }
        if (absZ >= absX) {
            return normalZ >= 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
        if (absX >= 0.0001F) {
            return normalX >= 0.0F ? Direction.EAST : Direction.WEST;
        }
        return quad.getDirection();
    }

    private static void addBounds(SourceBounds bounds, IdentityHashMap<BakedQuad, Boolean> seenQuads, List<BakedQuad> quads) {
        if (quads == null) {
            return;
        }

        for (BakedQuad quad : quads) {
            if (quad == null || seenQuads.put(quad, Boolean.TRUE) != null) {
                continue;
            }

            int[] vertices = quad.getVertices();
            if (vertices == null || vertices.length < 12) {
                continue;
            }

            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int index = vertex * stride;
                float x = Float.intBitsToFloat(vertices[index]);
                float y = Float.intBitsToFloat(vertices[index + 1]);
                float z = Float.intBitsToFloat(vertices[index + 2]);
                bounds.include(x * 16.0F, y * 16.0F, z * 16.0F);
            }
        }
    }

    private static float[] extractFaceUv(BakedQuad quad) {
        if (quad == null || quad.getSprite() == null) {
            return null;
        }

        int[] vertices = quad.getVertices();
        if (vertices == null || vertices.length < 24) {
            return null;
        }

        int stride = vertices.length / 4;
        if (stride < 6) {
            return null;
        }

        TextureAtlasSprite sprite = quad.getSprite();
        float spriteU0 = sprite.getU0();
        float spriteU1 = sprite.getU1();
        float spriteV0 = sprite.getV0();
        float spriteV1 = sprite.getV1();
        float uScale = spriteU1 - spriteU0;
        float vScale = spriteV1 - spriteV0;
        if (Math.abs(uScale) < 0.000001F || Math.abs(vScale) < 0.000001F) {
            return null;
        }

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float atlasU = Float.intBitsToFloat(vertices[index + 4]);
            float atlasV = Float.intBitsToFloat(vertices[index + 5]);
            float localU = 16.0F * (atlasU - spriteU0) / uScale;
            float localV = 16.0F * (atlasV - spriteV0) / vScale;
            minU = Math.min(minU, localU);
            minV = Math.min(minV, localV);
            maxU = Math.max(maxU, localU);
            maxV = Math.max(maxV, localV);
        }

        if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
            return null;
        }
        return new float[]{minU, minV, maxU, maxV};
    }

    private static Vector3f adjustedFrom(Vector3f from, Vector3f to, SourceBounds bounds) {
        Vector3f adjusted = new Vector3f(from);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        if (isHalfBlock(ySize) && !isHalfBlock(xSize) && !isHalfBlock(zSize)) {
            return adjusted;
        }
        float xThickness = bounds.halfWidth();
        if (isHalfBlock(xSize) && shouldAdjustThickness(xThickness)) {
            adjusted.x = alignStart(from.x(), to.x(), xThickness);
        }
        float zThickness = bounds.halfDepth();
        if (isHalfBlock(zSize) && shouldAdjustThickness(zThickness)) {
            adjusted.z = alignStart(from.z(), to.z(), zThickness);
        }
        return adjusted;
    }

    private static Vector3f adjustedTo(Vector3f from, Vector3f to, SourceBounds bounds) {
        Vector3f adjusted = new Vector3f(to);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        float yThickness = bounds.height();
        if (shouldAdjustThickness(yThickness)) {
            adjusted.y = to.y() - (8.0F - yThickness);
        }
        float xThickness = bounds.halfWidth();
        if (isHalfBlock(xSize) && shouldAdjustThickness(xThickness)) {
            adjusted.x = alignEnd(from.x(), to.x(), xThickness);
        }
        float zThickness = bounds.halfDepth();
        if (isHalfBlock(zSize) && shouldAdjustThickness(zThickness)) {
            adjusted.z = alignEnd(from.z(), to.z(), zThickness);
        }
        return adjusted;
    }

    private static boolean isHalfBlock(float size) {
        return Math.abs(size - 8.0F) < 0.001F;
    }

    private static boolean shouldAdjustThickness(float thickness) {
        return thickness > 0.0F && Math.abs(thickness - 8.0F) >= 0.001F;
    }

    private static float alignStart(float from, float to, float thickness) {
        return from > 0.0F ? 16.0F - thickness : from;
    }

    private static float alignEnd(float from, float to, float thickness) {
        return from > 0.0F ? 16.0F : from + thickness;
    }

    private static float[] defaultUV(Direction direction, Vector3f from, Vector3f to) {
        float fx = from.x();
        float fy = from.y();
        float fz = from.z();
        float tx = to.x();
        float ty = to.y();
        float tz = to.z();
        return switch (direction) {
            case NORTH -> new float[]{16.0F - tx, 16.0F - ty, 16.0F - fx, 16.0F - fy};
            case SOUTH -> new float[]{fx, 16.0F - ty, tx, 16.0F - fy};
            case WEST -> new float[]{fz, 16.0F - ty, tz, 16.0F - fy};
            case EAST -> new float[]{16.0F - tz, 16.0F - ty, 16.0F - fz, 16.0F - fy};
            case UP -> new float[]{fx, fz, tx, tz};
            case DOWN -> new float[]{fx, 16.0F - tz, tx, 16.0F - fz};
        };
    }

    private static BakedQuad bakeFaceQuad(Direction direction, Vector3f from, Vector3f to, FaceData faceData) {
        return bakeFaceQuad(direction, from, to, faceData, true);
    }

    private static List<BakedQuad> bakeFaceQuads(Direction direction, Vector3f from, Vector3f to, FaceData faceData, boolean preserveSourceSideUv) {
        if (shouldRepeatSourceSideUv(direction, from, to, faceData, preserveSourceSideUv)) {
            float midY = from.y() + 8.0F;
            if (midY < to.y() - 0.001F) {
                ArrayList<BakedQuad> quads = new ArrayList<>(2);
                BakedQuad lower = bakeFaceQuad(direction, from, new Vector3f(to.x(), midY, to.z()), faceData, true);
                BakedQuad upper = bakeFaceQuad(direction, new Vector3f(from.x(), midY, from.z()), to, faceData, true);
                if (lower != null) {
                    quads.add(lower);
                }
                if (upper != null) {
                    quads.add(upper);
                }
                return quads.isEmpty() ? Collections.emptyList() : quads;
            }
        }

        BakedQuad quad = bakeFaceQuad(direction, from, to, faceData, preserveSourceSideUv);
        return quad == null ? Collections.emptyList() : Collections.singletonList(quad);
    }

    private static BakedQuad bakeFaceQuad(Direction direction, Vector3f from, Vector3f to, FaceData faceData, boolean preserveSourceSideUv) {
        if (faceData == null || faceData.sprite() == null) {
            return null;
        }

        float[] uv = preserveSourceSideUv ? mergedFaceUv(direction, from, to, faceData.uv()) : defaultUV(direction, from, to);
        BlockFaceUV blockFaceUV = new BlockFaceUV(new float[]{uv[0], uv[1], uv[2], uv[3]}, 0);
        BlockElementFace elementFace = new BlockElementFace(null, faceData.tintIndex(), "#texture", blockFaceUV);
        BakedQuad quad = FACE_BAKERY.bakeQuad(
                new Vector3f(from),
                new Vector3f(to),
                elementFace,
                faceData.sprite(),
                direction,
                BlockModelRotation.X0_Y0,
                null,
                faceData.shade()
        );
        return fixWinding(clampQuadUvToSprite(quad), direction);
    }

    private static BakedQuad clampQuadUvToSprite(BakedQuad quad) {
        if (quad == null || quad.getSprite() == null || quad.getVertices() == null) {
            return quad;
        }

        TextureAtlasSprite sprite = quad.getSprite();
        float uInset = Math.max(0.000001F, Math.abs(sprite.getU1() - sprite.getU0()) / 1024.0F);
        float vInset = Math.max(0.000001F, Math.abs(sprite.getV1() - sprite.getV0()) / 1024.0F);
        float minU = Math.min(sprite.getU0(), sprite.getU1()) + uInset;
        float maxU = Math.max(sprite.getU0(), sprite.getU1()) - uInset;
        float minV = Math.min(sprite.getV0(), sprite.getV1()) + vInset;
        float maxV = Math.max(sprite.getV0(), sprite.getV1()) - vInset;
        if (minU >= maxU || minV >= maxV) {
            return quad;
        }

        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        if (stride < 6) {
            return quad;
        }

        boolean changed = false;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float u = Float.intBitsToFloat(vertices[index + 4]);
            float v = Float.intBitsToFloat(vertices[index + 5]);
            float clampedU = clamp(u, minU, maxU);
            float clampedV = clamp(v, minV, maxV);
            if (clampedU != u || clampedV != v) {
                vertices[index + 4] = Float.floatToRawIntBits(clampedU);
                vertices[index + 5] = Float.floatToRawIntBits(clampedV);
                changed = true;
            }
        }

        return changed ? new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade()) : quad;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static BakedQuad fixWinding(BakedQuad quad, Direction direction) {
        if (quad == null || direction == null || quad.getVertices() == null) {
            return quad;
        }
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        if (!hasOppositeWinding(vertices, stride, direction)) {
            return quad;
        }

        int[] rewound = vertices.clone();
        reverseWinding(rewound, stride);
        return new BakedQuad(rewound, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
    }

    private static boolean hasOppositeWinding(int[] vertices, int stride, Direction direction) {
        if (direction == null || vertices.length < stride * 3 || stride < 3) {
            return false;
        }

        float x0 = Float.intBitsToFloat(vertices[0]);
        float y0 = Float.intBitsToFloat(vertices[1]);
        float z0 = Float.intBitsToFloat(vertices[2]);
        float x1 = Float.intBitsToFloat(vertices[stride]);
        float y1 = Float.intBitsToFloat(vertices[stride + 1]);
        float z1 = Float.intBitsToFloat(vertices[stride + 2]);
        float x2 = Float.intBitsToFloat(vertices[stride * 2]);
        float y2 = Float.intBitsToFloat(vertices[stride * 2 + 1]);
        float z2 = Float.intBitsToFloat(vertices[stride * 2 + 2]);

        float edgeAx = x1 - x0;
        float edgeAy = y1 - y0;
        float edgeAz = z1 - z0;
        float edgeBx = x2 - x0;
        float edgeBy = y2 - y0;
        float edgeBz = z2 - z0;

        float normalX = edgeAy * edgeBz - edgeAz * edgeBy;
        float normalY = edgeAz * edgeBx - edgeAx * edgeBz;
        float normalZ = edgeAx * edgeBy - edgeAy * edgeBx;
        float dot = normalX * direction.getStepX()
                + normalY * direction.getStepY()
                + normalZ * direction.getStepZ();
        return dot < -0.000001F;
    }

    private static void reverseWinding(int[] vertices, int stride) {
        int first = stride;
        int second = stride * 3;
        for (int i = 0; i < stride; i++) {
            int value = vertices[first + i];
            vertices[first + i] = vertices[second + i];
            vertices[second + i] = value;
        }
    }

    private static float[] mergedFaceUv(Direction direction, Vector3f from, Vector3f to, float[] sourceUv) {
        float[] uv = defaultUV(direction, from, to);
        if (sourceUv == null) {
            return uv;
        }

        if (direction.getAxis().isHorizontal() && shouldPreserveSourceSideUv(from, to)) {
            uv[1] = sourceUv[1];
            uv[3] = sourceUv[3];
        }
        return uv;
    }

    private static boolean shouldPreserveSourceSideUv(Vector3f from, Vector3f to) {
        return isHalfBlock(to.x() - from.x()) || isHalfBlock(to.y() - from.y()) || isHalfBlock(to.z() - from.z());
    }

    private static boolean shouldRepeatSourceSideUv(Direction direction, Vector3f from, Vector3f to, FaceData faceData, boolean preserveSourceSideUv) {
        return preserveSourceSideUv
                && direction != null
                && direction.getAxis().isHorizontal()
                && faceData != null
                && faceData.uv() != null
                && isHalfBlock(faceData.uv()[3] - faceData.uv()[1])
                && !isHalfBlock(to.y() - from.y())
                && (isHalfBlock(to.x() - from.x()) || isHalfBlock(to.z() - from.z()));
    }

    private static RandomSource seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }

    static int allDirectionMask() {
        return ALL_DIRECTIONS;
    }

    static int without(int mask, Direction direction) {
        return direction == null ? mask : mask & ~(1 << direction.ordinal());
    }

    private static int directionMask(Direction... directions) {
        int mask = 0;
        if (directions == null) {
            return mask;
        }
        for (Direction direction : directions) {
            if (direction != null) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    private static BlockState prepareSourceState(BlockState slabState) {
        BlockState renderState = slabState;
        if (renderState.hasProperty(SlabBlock.WATERLOGGED)) {
            renderState = renderState.setValue(SlabBlock.WATERLOGGED, false);
        }
        return renderState;
    }

    private static BlockState horizontalSlabRenderState(BlockState slabState, SlabType half) {
        if (slabState == null) {
            return null;
        }
        BlockState renderState = slabState;
        if (renderState.hasProperty(SlabBlock.TYPE)) {
            renderState = renderState.setValue(SlabBlock.TYPE, half);
        }
        return prepareSourceState(renderState);
    }

    private static BlockState stepRenderState(BlockState slabState, boolean top) {
        return horizontalSlabRenderState(slabState, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState verticalPrismRenderState(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(prepareSourceState(slabState));
    }

    private record FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, float[] uv) {
    }

    private record SourceGeometry(EnumMap<Direction, List<FaceData>> faceData,
                                  FaceData fallback,
                                  SourceBounds bounds) {
    }

    private record JoinedStepSide(BlockState renderState, Vector3f from, Vector3f to, int sideMask, int bodyMask) {
    }

    private static final class SourceBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void include(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private float height() {
            if (!Float.isFinite(this.minY) || !Float.isFinite(this.maxY)) {
                return 8.0F;
            }
            return Math.max(0.0F, this.maxY - this.minY);
        }

        private float halfWidth() {
            if (!Float.isFinite(this.minX) || !Float.isFinite(this.maxX)) {
                return 8.0F;
            }
            return Math.max(0.0F, (this.maxX - this.minX) * 0.5F);
        }

        private float halfDepth() {
            if (!Float.isFinite(this.minZ) || !Float.isFinite(this.maxZ)) {
                return 8.0F;
            }
            return Math.max(0.0F, (this.maxZ - this.minZ) * 0.5F);
        }
    }

    private static final class PrismBakedModel implements BakedModel {
        private final BakedModel sourceModel;
        private final List<BakedQuad> allQuads;
        private final EnumMap<Direction, List<BakedQuad>> byDirection;
        private final TextureAtlasSprite particleIcon;

        private PrismBakedModel(BakedModel sourceModel, List<BakedQuad> allQuads, EnumMap<Direction, List<BakedQuad>> byDirection, TextureAtlasSprite particleIcon) {
            this.sourceModel = sourceModel;
            this.allQuads = allQuads;
            this.byDirection = byDirection;
            this.particleIcon = particleIcon;
        }

        private static BakedModel create(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to) {
            return create(sourceModel, state, from, to, ALL_DIRECTIONS);
        }

        private static BakedModel create(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to, Direction hiddenFace) {
            return create(sourceModel, state, from, to, without(ALL_DIRECTIONS, hiddenFace));
        }

        private static BakedModel create(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to, int visibleMask) {
            return create(sourceModel, state, from, to, visibleMask, true, true);
        }

        private static BakedModel createExact(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to, int visibleMask) {
            return createExact(sourceModel, state, from, to, visibleMask, false);
        }

        private static BakedModel createExact(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to, int visibleMask, boolean preserveSourceSideUv) {
            return create(sourceModel, state, from, to, visibleMask, false, preserveSourceSideUv);
        }

        private static BakedModel create(BakedModel sourceModel, BlockState state, Vector3f from, Vector3f to, int visibleMask, boolean adjustToSourceBounds, boolean preserveSourceSideUv) {
            TextureAtlasSprite particleIcon = sourceModel.getParticleIcon();
            SourceGeometry sourceGeometry = extractSourceGeometry(sourceModel, state, particleIcon);
            if (sourceGeometry.faceData().isEmpty() || sourceGeometry.fallback() == null) {
                return null;
            }

            Vector3f adjustedFrom = adjustToSourceBounds ? adjustedFrom(from, to, sourceGeometry.bounds()) : new Vector3f(from);
            Vector3f adjustedTo = adjustToSourceBounds ? adjustedTo(from, to, sourceGeometry.bounds()) : new Vector3f(to);

            EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
            List<BakedQuad> allQuads = new ArrayList<>(12);
            for (Direction direction : Direction.values()) {
                if ((visibleMask & (1 << direction.ordinal())) == 0) {
                    continue;
                }
                List<FaceData> templates = sourceGeometry.faceData().get(direction);
                if (templates == null || templates.isEmpty()) {
                    templates = Collections.singletonList(sourceGeometry.fallback());
                }

                for (FaceData template : templates) {
                    List<BakedQuad> quads = bakeFaceQuads(direction, adjustedFrom, adjustedTo, template, preserveSourceSideUv);
                    if (quads.isEmpty()) {
                        continue;
                    }

                    allQuads.addAll(quads);
                    byDirection.computeIfAbsent(direction, unused -> new ArrayList<>()).addAll(quads);
                }
            }

            if (allQuads.isEmpty()) {
                return null;
            }
            return new PrismBakedModel(sourceModel, List.copyOf(allQuads), byDirection, particleIcon);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            if (side == null) {
                return this.allQuads;
            }
            return Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.sourceModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.sourceModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.sourceModel.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return this.sourceModel.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.particleIcon;
        }

        @Override
        public ItemTransforms getTransforms() {
            return this.sourceModel.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return this.sourceModel.getOverrides();
        }
    }
}
