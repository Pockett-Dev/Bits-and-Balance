package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class EnhancedSlabTerrainRenderHelper {
    private static final Logger SODIUM_LOGGER = LoggerFactory.getLogger("bitsandbalance-sodium-enhanced-slab-render");
    private static final ThreadLocal<Integer> NESTED_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<CreateModelContext> CREATE_MODEL_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> LITEMATICA_PREVIEW_RENDER = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<List<BlockState>> LITEMATICA_PREVIEW_RENDER_STATES = new ThreadLocal<>();
    private static final int FIXED_TERRAIN_CACHE_LIMIT = 8192;
    private static final Map<BlockState, List<RenderGroup>> FIXED_TERRAIN_CACHE = new ConcurrentHashMap<>();
    private static final int ALL_DIRECTIONS = FabricRuntimeModelCompat.allDirectionMask();
    private static final int HORIZONTAL_DIRECTIONS = bitsandbalance$directionMask(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    private static final Vector3fc MIXED_BOTTOM_FROM = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3fc MIXED_BOTTOM_TO = new Vector3f(16.0F, 8.0F, 16.0F);
    private static final Vector3fc MIXED_TOP_FROM = new Vector3f(0.0F, 8.0F, 0.0F);
    private static final Vector3fc MIXED_TOP_TO = new Vector3f(16.0F, 16.0F, 16.0F);
    private static final AtomicBoolean SODIUM_EMPTY_GROUPS_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean SODIUM_RENDERED_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean FIXED_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean MIXED_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean VERTICAL_SLAB_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean STEP_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean VERTICAL_STEP_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean QUAD_STEP_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean QUAD_VERTICAL_STEP_SECTION_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean LITEMATICA_PREVIEW_FAILURE_LOGGED = new AtomicBoolean(false);
    private static volatile Method litematicaTessellateMethod;

    private EnhancedSlabTerrainRenderHelper() {
    }

    public static void clearCaches() {
        FIXED_TERRAIN_CACHE.clear();
        EnhancedSlabFabricRendererModelBridge.clearCaches();
        ReflectiveLitBlockModelTesselationBridge.clearCaches();
        MovingBlockRenderTypeCompat.clearCaches();
        PistonMovingBlockEntityRenderHelper.clearCaches();
    }

    @FunctionalInterface
    public interface SodiumRenderInvoker {
        void render(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin);
    }

    public static boolean isEnabled() {
        return FabricTweaksConfig.enableEnhancedSlabs;
    }

    public static boolean handles(BlockState state) {
        if (!isEnabled() || state == null) {
            return false;
        }

        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    public static boolean renderFixedSection(ModelBlockRenderer renderer,
                                             BlockQuadOutput output,
                                             float x,
                                             float y,
                                             float z,
                                             BlockAndTintGetter level,
                                             BlockPos pos,
                                             BlockState state,
                                             long seed) {
        return bitsandbalance$isFixedEnhancedSlabState(state) && renderSection(renderer, output, x, y, z, level, pos, state, seed);
    }

    public static boolean renderSection(ModelBlockRenderer renderer,
                                        BlockQuadOutput output,
                                        float x,
                                        float y,
                                        float z,
                                        BlockAndTintGetter level,
                                        BlockPos pos,
                                        BlockState state,
                                        long seed) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || renderer == null || output == null || level == null || pos == null || state == null) {
            return false;
        }

        List<RenderGroup> groups = bitsandbalance$buildSectionGroups(level, pos, state);
        if (groups.isEmpty()) {
            return false;
        }

        float yOffset = (float) EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        boolean rendered = false;
        for (RenderGroup group : groups) {
            if (group == null || group.renderState() == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            try {
                BlockStateModel proxy = ReflectiveLitBlockModelTesselationBridge.createBlockStateModelProxy(group.parts());
                BlockColorRenderContext.setForcedPosLong(pos.asLong());
                try {
                    renderer.tesselateBlock(output, x, y + yOffset, z, level, pos, group.renderState(), proxy, seed);
                } finally {
                    BlockColorRenderContext.clear();
                }
                rendered = true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        if (rendered) {
            bitsandbalance$markSectionRendered(state);
        }
        return rendered;
    }

    public static boolean shouldSkipFixedBlockEntityRender(BlockState ownerState) {
        boolean skip = FabricTweaksConfig.enableEnhancedSlabs
                && bitsandbalance$isFixedEnhancedSlabState(ownerState)
                && !bitsandbalance$getOrBuildFixedGroups(ownerState).isEmpty();
        if (skip) {
            EnhancedSlabRenderDiagnostics.fixedFallbackSkipped();
        }
        return skip;
    }

    public static boolean shouldSkipBlockEntityRender(Object level, BlockPos pos, BlockState ownerState) {
        if (level == null || pos == null || ownerState == null || bitsandbalance$isFixedEnhancedSlabState(ownerState)) {
            return false;
        }
        if (!bitsandbalance$sectionRenderedFor(ownerState)) {
            return false;
        }
        boolean skip = !bitsandbalance$buildSectionGroups(level, pos, ownerState).isEmpty();
        if (skip) {
            EnhancedSlabRenderDiagnostics.dynamicFallbackSkipped();
        }
        return skip;
    }

    private static boolean bitsandbalance$isFixedEnhancedSlabState(BlockState state) {
        if (state == null) {
            return false;
        }
        return state.getBlock() instanceof FixedStepBlock
                || state.getBlock() instanceof FixedVerticalStepBlock
                || state.getBlock() instanceof FixedVerticalSlabBlock;
    }

    private static boolean bitsandbalance$isEnhancedSlabFamilyState(BlockState state) {
        if (state == null) {
            return false;
        }
        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    private static boolean bitsandbalance$sectionRenderedFor(BlockState state) {
        if (state.getBlock() instanceof MixedSlabBlock) return MIXED_SECTION_RENDERED.get();
        if (state.getBlock() instanceof VerticalSlabBlock) return VERTICAL_SLAB_SECTION_RENDERED.get();
        if (state.getBlock() instanceof StepBlock) return STEP_SECTION_RENDERED.get();
        if (state.getBlock() instanceof VerticalStepBlock) return VERTICAL_STEP_SECTION_RENDERED.get();
        if (state.getBlock() instanceof QuadStepBlock) return QUAD_STEP_SECTION_RENDERED.get();
        if (state.getBlock() instanceof QuadVerticalStepBlock) return QUAD_VERTICAL_STEP_SECTION_RENDERED.get();
        return false;
    }

    private static void bitsandbalance$markSectionRendered(BlockState state) {
        if (bitsandbalance$isFixedEnhancedSlabState(state)) {
            FIXED_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.fixedSection();
        } else if (state.getBlock() instanceof MixedSlabBlock) {
            MIXED_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.mixedSection();
        } else if (state.getBlock() instanceof VerticalSlabBlock) {
            VERTICAL_SLAB_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.verticalSlabSection();
        } else if (state.getBlock() instanceof StepBlock) {
            STEP_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.stepSection();
        } else if (state.getBlock() instanceof VerticalStepBlock) {
            VERTICAL_STEP_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.verticalStepSection();
        } else if (state.getBlock() instanceof QuadStepBlock) {
            QUAD_STEP_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.quadStepSection();
        } else if (state.getBlock() instanceof QuadVerticalStepBlock) {
            QUAD_VERTICAL_STEP_SECTION_RENDERED.set(true);
            EnhancedSlabRenderDiagnostics.quadVerticalStepSection();
        }
    }

    private static List<RenderGroup> bitsandbalance$buildSectionGroups(Object level, BlockPos pos, BlockState state) {
        if (bitsandbalance$isFixedEnhancedSlabState(state)) {
            return bitsandbalance$getOrBuildFixedGroups(state);
        }
        if (!bitsandbalance$isEnhancedSlabFamilyState(state)) {
            return List.of();
        }

        BlockEntity blockEntity = bitsandbalance$getBlockEntity(level, pos);
        Object materialSlots = bitsandbalance$materialSlots(level, pos);
        if (state.getBlock() instanceof MixedSlabBlock) {
            BlockState bottomSlab = bitsandbalance$slot(materialSlots, 0);
            BlockState topSlab = bitsandbalance$slot(materialSlots, 1);
            return bottomSlab == null && topSlab == null ? List.of() : bitsandbalance$buildMixedGroups(pos, bottomSlab, topSlab);
        }

        if (state.getBlock() instanceof VerticalSlabBlock) {
            BlockState facingSlab = blockEntity instanceof VerticalSlabBlockEntity verticalSlab
                    ? verticalSlab.getFacingSlab()
                    : bitsandbalance$slot(materialSlots, 0);
            BlockState oppositeSlab = blockEntity instanceof VerticalSlabBlockEntity verticalSlab
                    ? verticalSlab.getOppositeSlab()
                    : bitsandbalance$slot(materialSlots, 1);
            facingSlab = bitsandbalance$resolveVerticalSlabSourceState(facingSlab, state);
            oppositeSlab = bitsandbalance$resolveVerticalSlabSourceState(oppositeSlab, state);
            return bitsandbalance$buildVerticalSlabGroups(pos, state, facingSlab, oppositeSlab);
        }

        if (state.getBlock() instanceof StepBlock) {
            EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = level instanceof net.minecraft.world.level.BlockGetter blockGetter
                    ? EnhancedSlabPlacementPredictionCache.peekSingleStep(blockGetter, pos)
                    : null;
            if (singlePrediction != null && singlePrediction.slabState() != null) {
                return bitsandbalance$buildStepGroups(pos, singlePrediction.stepState(), singlePrediction.slabState());
            }
            BlockState storedSlab = bitsandbalance$slot(materialSlots, 0);
            BlockState slabState = blockEntity instanceof StepBlockEntity step
                    ? bitsandbalance$resolveStepSourceState(step.getSlabState(), state)
                    : bitsandbalance$resolveStepSourceState(storedSlab, state);
            return slabState == null ? List.of() : bitsandbalance$buildStepGroups(pos, state, slabState);
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = level instanceof net.minecraft.world.level.BlockGetter blockGetter
                    ? EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(blockGetter, pos)
                    : null;
            if (singlePrediction != null && singlePrediction.slabState() != null) {
                return bitsandbalance$buildVerticalStepGroups(pos, singlePrediction.stepState(), singlePrediction.slabState());
            }
            BlockState storedSlab = bitsandbalance$slot(materialSlots, 0);
            BlockState slabState = blockEntity instanceof VerticalStepBlockEntity verticalStep
                    ? bitsandbalance$resolveVerticalStepSourceState(verticalStep.getSlabState(), state)
                    : bitsandbalance$resolveVerticalStepSourceState(storedSlab, state);
            return slabState == null ? List.of() : bitsandbalance$buildVerticalStepGroups(pos, state, slabState);
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            Object slabs = blockEntity instanceof QuadStepBlockEntity ? blockEntity : materialSlots;
            return bitsandbalance$buildQuadStepGroups(pos, state, slabs);
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            Object slabs = blockEntity instanceof QuadVerticalStepBlockEntity ? blockEntity : materialSlots;
            return bitsandbalance$buildQuadVerticalStepGroups(pos, slabs);
        }

        return List.of();
    }

    private static BlockEntity bitsandbalance$getBlockEntity(Object level, BlockPos pos) {
        if (level instanceof BlockAndTintGetter blockAndTintGetter) {
            return blockAndTintGetter.getBlockEntity(pos);
        }
        if (level instanceof net.minecraft.world.level.Level realLevel) {
            return realLevel.getBlockEntity(pos);
        }
        return null;
    }

    private static Object bitsandbalance$materialSlots(Object level, BlockPos pos) {
        BlockState[] createSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        if (createSlots != null) {
            return createSlots;
        }
        if (level instanceof net.minecraft.world.level.Level actualLevel) {
            return EnhancedSlabMaterialSavedData.getClientView(actualLevel, pos.asLong());
        }
        return EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
    }

    private static BlockState bitsandbalance$slot(Object slots, int index) {
        if (slots instanceof EnhancedSlabMaterialSavedData.MaterialView materialView) {
            return materialView.slot(index);
        }
        if (slots instanceof BlockState[] materialStates) {
            return index >= 0 && index < materialStates.length ? materialStates[index] : null;
        }
        if (slots instanceof QuadStepBlockEntity quadStep) {
            return index >= 0 && index < 4 ? quadStep.getSlabAt(index) : null;
        }
        if (slots instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return index >= 0 && index < 4 ? quadVerticalStep.getSlabAt(index) : null;
        }
        return null;
    }

    public static boolean render(Object dispatcher,
                                 BlockAndTintGetter level,
                                 BlockPos pos,
                                 BlockState state,
                                 PoseStack poseStack,
                                 VertexConsumer consumer) {
        if (dispatcher == null || level == null || pos == null || poseStack == null || consumer == null) {
            return false;
        }
        if (NESTED_RENDER_DEPTH.get() > 0 || !handles(state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Object materialSlots = bitsandbalance$materialSlots(level, pos);
        if (state.getBlock() instanceof MixedSlabBlock) {
            boolean rendered = false;
            if (materialSlots != null) {
                rendered = bitsandbalance$renderMixedTerrain(dispatcher, level, pos, bitsandbalance$slot(materialSlots, 0), bitsandbalance$slot(materialSlots, 1), poseStack, consumer);
            }
            return rendered;
        }

        if (state.getBlock() instanceof VerticalSlabBlock) {
            if (state.getBlock() instanceof FixedVerticalSlabBlock) {
                return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$getOrBuildFixedGroups(state));
            }
            if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
                return bitsandbalance$renderVerticalSlabTerrain(dispatcher, level, pos, state, verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab(), poseStack, consumer);
            }
            if (materialSlots != null) {
                return bitsandbalance$renderVerticalSlabTerrain(dispatcher, level, pos, state, bitsandbalance$slot(materialSlots, 0), bitsandbalance$slot(materialSlots, 1), poseStack, consumer);
            }
            return false;
        }

        if (state.getBlock() instanceof StepBlock) {
            if (state.getBlock() instanceof FixedStepBlock) {
                return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$getOrBuildFixedGroups(state));
            }
            EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
            if (quadPrediction != null) {
                return bitsandbalance$renderQuadStepTerrain(dispatcher, level, pos, quadPrediction.quadState(), quadPrediction.slabs(), poseStack, consumer);
            }
            EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
            if (singlePrediction != null && singlePrediction.slabState() != null) {
                return bitsandbalance$renderStepTerrain(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, consumer);
            }
            if (blockEntity instanceof StepBlockEntity step && step.getSlabState() != null) {
                return bitsandbalance$renderStepTerrain(dispatcher, level, pos, state, step.getSlabState(), poseStack, consumer);
            }
            BlockState storedSlab = bitsandbalance$resolveStepSourceState(bitsandbalance$slot(materialSlots, 0), state);
            if (storedSlab != null) {
                return bitsandbalance$renderStepTerrain(dispatcher, level, pos, state, storedSlab, poseStack, consumer);
            }
            return false;
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            if (state.getBlock() instanceof FixedVerticalStepBlock) {
                return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$getOrBuildFixedGroups(state));
            }
            EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
            if (quadPrediction != null) {
                return bitsandbalance$renderQuadVerticalStepTerrain(dispatcher, level, pos, quadPrediction.slabs(), poseStack, consumer);
            }
            EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
            if (singlePrediction != null && singlePrediction.slabState() != null) {
                return bitsandbalance$renderVerticalStepTerrain(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, consumer);
            }
            if (blockEntity instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
                return bitsandbalance$renderVerticalStepTerrain(dispatcher, level, pos, state, verticalStep.getSlabState(), poseStack, consumer);
            }
            BlockState storedSlab = bitsandbalance$resolveVerticalStepSourceState(bitsandbalance$slot(materialSlots, 0), state);
            if (storedSlab != null) {
                return bitsandbalance$renderVerticalStepTerrain(dispatcher, level, pos, state, storedSlab, poseStack, consumer);
            }
            return false;
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            if (blockEntity instanceof QuadStepBlockEntity quadStep) {
                return bitsandbalance$renderQuadStepTerrain(dispatcher, level, pos, state, quadStep, poseStack, consumer);
            }
            if (materialSlots != null) {
                return bitsandbalance$renderQuadStepTerrain(dispatcher, level, pos, state, materialSlots, poseStack, consumer);
            }
            return false;
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
                return bitsandbalance$renderQuadVerticalStepTerrain(dispatcher, level, pos, quadVerticalStep, poseStack, consumer);
            }
            if (materialSlots != null) {
                return bitsandbalance$renderQuadVerticalStepTerrain(dispatcher, level, pos, materialSlots, poseStack, consumer);
            }
            return false;
        }

        return false;
    }

    public static boolean renderSodium(SodiumRenderInvoker invoker, BlockState state, BlockPos pos, BlockPos origin) {
        return renderSodium(invoker, null, state, pos, origin);
    }

    public static boolean renderSodium(SodiumRenderInvoker invoker, Object level, BlockState state, BlockPos pos, BlockPos origin) {
        if (invoker == null || state == null || pos == null || origin == null) {
            return false;
        }
        if (NESTED_RENDER_DEPTH.get() > 0 || !handles(state)) {
            return false;
        }

        List<RenderGroup> groups = bitsandbalance$buildSectionGroups(level, pos, state);
        if (groups.isEmpty()) {
            if (SODIUM_EMPTY_GROUPS_LOGGED.compareAndSet(false, true)) {
                SODIUM_LOGGER.warn("Sodium enhanced slab hook built no render groups for block={} pos={}", state.getBlock(), pos);
            }
            return false;
        }

        int previousDepth = NESTED_RENDER_DEPTH.get();
        NESTED_RENDER_DEPTH.set(previousDepth + 1);
        boolean rendered = false;
        try {
            for (RenderGroup group : groups) {
                if (group == null || group.renderState() == null || group.parts() == null || group.parts().isEmpty()) {
                    continue;
                }
                BlockStateModel proxy = ReflectiveLitBlockModelTesselationBridge.createBlockStateModelProxy(group.parts());
                BlockColorRenderContext.setForcedPosLong(pos.asLong());
                try {
                    invoker.render(proxy, group.renderState(), pos, origin);
                } finally {
                    BlockColorRenderContext.clear();
                }
                rendered = true;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return rendered;
        } finally {
            NESTED_RENDER_DEPTH.set(previousDepth);
        }

        if (rendered) {
            bitsandbalance$markSectionRendered(state);
            if (SODIUM_RENDERED_LOGGED.compareAndSet(false, true)) {
                SODIUM_LOGGER.info("Sodium enhanced slab hook emitted replacement terrain for block={} pos={} groups={}", state.getBlock(), pos, groups.size());
            }
        }
        return rendered;
    }

    public static boolean collectModelParts(BlockState state, List<Object> parts) {
        return collectModelParts(null, BlockPos.ZERO, state, parts);
    }

    public static void rememberCreateModelContext(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || !handles(state)) {
            CREATE_MODEL_CONTEXT.remove();
            return;
        }
        CREATE_MODEL_CONTEXT.set(new CreateModelContext(level, pos.immutable(), state));
    }

    public static boolean collectCreateModelParts(BlockState state, List<Object> parts) {
        CreateModelContext context = CREATE_MODEL_CONTEXT.get();
        CREATE_MODEL_CONTEXT.remove();
        if (context == null || state == null || parts == null || !handles(state) || !state.equals(context.state())) {
            return false;
        }

        boolean collected = false;
        for (RenderGroup group : bitsandbalance$buildSectionGroups(context.level(), context.pos(), state)) {
            if (group == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            parts.addAll(group.parts());
            collected = true;
        }
        if (collected) {
            bitsandbalance$markSectionRendered(state);
        }
        return collected;
    }

    /**
     * Litematica normally flattens all collected model parts and then shades
     * them using the owner block state. Enhanced slab-family parts can come
     * from several different material states, so that loses the state needed
     * for correct AO, tint, and light sampling. Tessellate each render group
     * separately while retaining its material state.
     *
     * @return the rendered result, or {@code null} when the normal Litematica
     * path should be used
     */
    public static Boolean tessellateLitematicaPreview(Object renderer,
                                                       BlockAndTintGetter level,
                                                       BlockState ownerState,
                                                       BlockPos pos,
                                                       Vec3 offset,
                                                       long seed,
                                                       Object output) {
        if (renderer == null
                || level == null
                || ownerState == null
                || pos == null
                || offset == null
                || output == null
                || !handles(ownerState)
                || LITEMATICA_PREVIEW_RENDER.get()) {
            return null;
        }

        List<RenderGroup> groups = bitsandbalance$buildSectionGroups(level, pos, ownerState);
        if (groups.isEmpty()) {
            return null;
        }

        LITEMATICA_PREVIEW_RENDER.set(true);
        try {
            Method tessellate = bitsandbalance$litematicaTessellateMethod(renderer.getClass());
            boolean rendered = false;
            ArrayList<BlockState> renderedStates = System.getProperty("bitsandbalance.fixtureRunner") != null
                    ? new ArrayList<>(groups.size())
                    : null;
            for (RenderGroup group : groups) {
                if (group == null || group.renderState() == null || group.parts() == null || group.parts().isEmpty()) {
                    continue;
                }
                BlockStateModel model = ReflectiveLitBlockModelTesselationBridge.createBlockStateModelProxy(group.parts());
                if (renderedStates != null) {
                    renderedStates.add(group.renderState());
                }
                Object result = tessellate.invoke(
                        renderer,
                        level,
                        group.renderState(),
                        pos,
                        offset,
                        model,
                        seed,
                        output
                );
                rendered |= Boolean.TRUE.equals(result);
            }
            if (renderedStates != null) {
                LITEMATICA_PREVIEW_RENDER_STATES.set(List.copyOf(renderedStates));
            }
            return rendered;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (LITEMATICA_PREVIEW_FAILURE_LOGGED.compareAndSet(false, true)) {
                SODIUM_LOGGER.warn("Failed to preserve enhanced slab material states in the Litematica preview", exception);
            }
            return null;
        } finally {
            LITEMATICA_PREVIEW_RENDER.remove();
        }
    }

    public static List<BlockState> takeLitematicaPreviewRenderStates() {
        List<BlockState> states = LITEMATICA_PREVIEW_RENDER_STATES.get();
        LITEMATICA_PREVIEW_RENDER_STATES.remove();
        return states == null ? List.of() : states;
    }

    private static Method bitsandbalance$litematicaTessellateMethod(Class<?> rendererClass) throws NoSuchMethodException {
        Method cached = litematicaTessellateMethod;
        if (cached != null && cached.getDeclaringClass().isAssignableFrom(rendererClass)) {
            return cached;
        }
        for (Method candidate : rendererClass.getMethods()) {
            if (candidate.getName().equals("tessellateBlock") && candidate.getParameterCount() == 7) {
                candidate.setAccessible(true);
                litematicaTessellateMethod = candidate;
                return candidate;
            }
        }
        throw new NoSuchMethodException(rendererClass.getName() + ".tessellateBlock");
    }

    public static Object geometryKey(Object level, BlockPos pos, BlockState state) {
        if (pos == null || !handles(state)) {
            return null;
        }
        if (bitsandbalance$isFixedEnhancedSlabState(state)) {
            return state;
        }

        BlockEntity blockEntity = bitsandbalance$getBlockEntity(level, pos);
        Object slots = bitsandbalance$materialSlots(level, pos);
        if (blockEntity instanceof QuadStepBlockEntity || blockEntity instanceof QuadVerticalStepBlockEntity) {
            slots = blockEntity;
        } else if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new MaterialGeometryKey(state, verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab(), null, null);
        } else if (blockEntity instanceof StepBlockEntity step) {
            return new MaterialGeometryKey(state, step.getSlabState(), null, null, null);
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            return new MaterialGeometryKey(state, verticalStep.getSlabState(), null, null, null);
        }
        // Slot occupancy and material edits do not necessarily change the container blockstate.
        return new MaterialGeometryKey(state, bitsandbalance$slot(slots, 0), bitsandbalance$slot(slots, 1),
                bitsandbalance$slot(slots, 2), bitsandbalance$slot(slots, 3));
    }

    private record MaterialGeometryKey(BlockState state, BlockState slot0, BlockState slot1,
                                       BlockState slot2, BlockState slot3) {
    }

    public static boolean collectModelParts(Object level, BlockPos pos, BlockState state, List<Object> parts) {
        if (state == null || parts == null || !handles(state)) {
            return false;
        }
        List<RenderGroup> groups = bitsandbalance$buildSectionGroups(level, pos == null ? BlockPos.ZERO : pos, state);
        if (groups.isEmpty()) {
            return false;
        }
        boolean collected = false;
        for (RenderGroup group : groups) {
            if (group == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            parts.addAll(group.parts());
            collected = true;
        }
        if (collected) {
            bitsandbalance$markSectionRendered(state);
        }
        return collected;
    }

    @SuppressWarnings("unchecked")
    public static boolean emitFabricRendererQuads(Object emitter, Object level, BlockPos pos, BlockState state, Object cullTest) {
        if (emitter == null || pos == null || state == null || !handles(state)) {
            return false;
        }

        List<RenderGroup> groups = bitsandbalance$buildSectionGroups(level, pos, state);
        if (groups.isEmpty()) {
            return false;
        }

        Predicate<Object> cullPredicate = cullTest instanceof Predicate<?> predicate
                ? (Predicate<Object>) predicate
                : ignored -> false;
        boolean emitted = false;
        for (RenderGroup group : groups) {
            if (group == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            for (BlockStateModelPart part : group.parts()) {
                if (part == null) {
                    continue;
                }
                emitted |= FabricRuntimeModelCompat.emitFabricPartQuads(part, emitter, cullPredicate);
            }
        }
        return emitted;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void renderNested(Object dispatcher,
                             BlockState renderState,
                             BlockPos pos,
                             BlockAndTintGetter level,
                             PoseStack poseStack,
                             VertexConsumer consumer,
                             List<?> parts) {
        renderNested(dispatcher, renderState, pos, level, poseStack, consumer, parts, true);
    }

    static void renderNested(Object dispatcher,
                             BlockState renderState,
                             BlockPos pos,
                             BlockAndTintGetter level,
                             PoseStack poseStack,
                             VertexConsumer consumer,
                             List<?> parts,
                             boolean checkSides) {
        if (dispatcher == null || renderState == null || pos == null || level == null || poseStack == null || consumer == null || parts == null || parts.isEmpty()) {
            return;
        }

        int previousDepth = NESTED_RENDER_DEPTH.get();
        NESTED_RENDER_DEPTH.set(previousDepth + 1);
        try {
            VirtualRenderWorldLightBridge.renderBatched(renderState, pos, level, poseStack, consumer, checkSides, parts, 0);
        } finally {
            NESTED_RENDER_DEPTH.set(previousDepth);
        }
    }

    private static boolean bitsandbalance$renderMixedTerrain(Object dispatcher,
                                                             BlockAndTintGetter level,
                                                             BlockPos pos,
                                                             BlockState bottomSlab,
                                                             BlockState topSlab,
                                                             PoseStack poseStack,
                                                             VertexConsumer consumer) {
        bottomSlab = bitsandbalance$horizontalSlabRenderState(bottomSlab, SlabType.BOTTOM);
        topSlab = bitsandbalance$horizontalSlabRenderState(topSlab, SlabType.TOP);
        boolean rendered = false;
        if (bottomSlab != null) {
            List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(bottomSlab, pos);
            if (parts != null && !parts.isEmpty()) {
                int mask = mixedBottomVisibleMask(pos, bottomSlab, topSlab);
                bitsandbalance$renderGroup(dispatcher, level, pos, poseStack, consumer, bottomSlab, FabricRuntimeModelCompat.filterPartList(parts, mask));
                rendered = true;
            }
        }
        if (topSlab != null) {
            List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(topSlab, pos);
            if (parts != null && !parts.isEmpty()) {
                int mask = bottomSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, Direction.DOWN) : ALL_DIRECTIONS;
                bitsandbalance$renderGroup(dispatcher, level, pos, poseStack, consumer, topSlab, FabricRuntimeModelCompat.filterPartList(parts, mask));
                rendered = true;
            }
        }
        return rendered;
    }

    private static boolean bitsandbalance$renderVerticalSlabTerrain(Object dispatcher,
                                                                    BlockAndTintGetter level,
                                                                    BlockPos pos,
                                                                    BlockState ownerState,
                                                                    BlockState facingSlab,
                                                                    BlockState oppositeSlab,
                                                                    PoseStack poseStack,
                                                                    VertexConsumer consumer) {
        Direction facing = ownerState.getValue(VerticalSlabBlock.FACING);
        List<RenderGroup> groups = new ArrayList<>(2);
        if (facingSlab != null) {
            BlockStateModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(facingSlab, pos, facing);
            if (part != null) {
                int mask = oppositeSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(facingSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        if (ownerState.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
            Direction oppositeFacing = facing.getOpposite();
            BlockStateModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(oppositeSlab, pos, oppositeFacing);
            if (part != null) {
                int mask = facingSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(oppositeSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$mergeGroups(groups));
    }

    private static boolean bitsandbalance$renderStepTerrain(Object dispatcher,
                                                            BlockAndTintGetter level,
                                                            BlockPos pos,
                                                            BlockState ownerState,
                                                            BlockState slabState,
                                                            PoseStack poseStack,
                                                            VertexConsumer consumer) {
        boolean top = ownerState.getValue(StepBlock.TOP);
        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        BlockStateModelPart part = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, ownerState.getValue(StepBlock.FACING), top);
        if (part == null) {
            return false;
        }
        return bitsandbalance$renderGroup(dispatcher, level, pos, poseStack, consumer, renderSlab, FabricRuntimeModelCompat.singletonPartList(part));
    }

    private static boolean bitsandbalance$renderVerticalStepTerrain(Object dispatcher,
                                                                    BlockAndTintGetter level,
                                                                    BlockPos pos,
                                                                    BlockState ownerState,
                                                                    BlockState slabState,
                                                                    PoseStack poseStack,
                                                                    VertexConsumer consumer) {
        BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slabState, pos, ownerState.getValue(VerticalStepBlock.FACING), ownerState.getValue(VerticalStepBlock.SIDE));
        if (part == null) {
            return false;
        }
        return bitsandbalance$renderGroup(dispatcher, level, pos, poseStack, consumer, slabState, FabricRuntimeModelCompat.singletonPartList(part));
    }

    private static boolean bitsandbalance$renderQuadStepTerrain(Object dispatcher,
                                                                BlockAndTintGetter level,
                                                                BlockPos pos,
                                                                BlockState ownerState,
                                                                Object slabs,
                                                                PoseStack poseStack,
                                                                VertexConsumer consumer) {
        BlockState fullBlockState = bitsandbalance$fullBlockStateForCompleteFamily(slabs);
        if (fullBlockState != null) {
            return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$buildFullBlockGroups(pos, fullBlockState));
        }

        Direction.Axis axis = ownerState.getValue(QuadStepBlock.AXIS);
        Map<BlockState, List<BlockStateModelPart>> groups = new LinkedHashMap<>(8);
        for (int i = 0; i < 4; i++) {
            BlockState slab = bitsandbalance$slot(slabs, i);
            if (slab == null) {
                continue;
            }
            boolean positive = i == 1 || i == 3;
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            boolean top = i >= 2;
            int mask = bitsandbalance$quadStepVisibleMask(axis, i, slabs);
            bitsandbalance$addStepParts(groups, pos, slab, facing, top, mask);
        }
        return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$toRenderGroups(groups));
    }

    private static boolean bitsandbalance$renderQuadVerticalStepTerrain(Object dispatcher,
                                                                        BlockAndTintGetter level,
                                                                        BlockPos pos,
                                                                        Object slabs,
                                                                        PoseStack poseStack,
                                                                        VertexConsumer consumer) {
        BlockState fullBlockState = bitsandbalance$fullBlockStateForCompleteFamily(slabs);
        if (fullBlockState != null) {
            return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$buildFullBlockGroups(pos, fullBlockState));
        }

        Map<BlockState, List<BlockStateModelPart>> groups = new LinkedHashMap<>(8);
        for (int i = 0; i < 4; i++) {
            BlockState slab = bitsandbalance$slot(slabs, i);
            if (slab == null) {
                continue;
            }
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slab, pos, facing, side);
            if (part == null) {
                continue;
            }
            int mask = bitsandbalance$quadVerticalStepVisibleMask(i, slabs);
            groups.computeIfAbsent(slab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(part, mask));
        }
        return bitsandbalance$renderGroups(dispatcher, level, pos, poseStack, consumer, bitsandbalance$toRenderGroups(groups));
    }

    private static List<RenderGroup> bitsandbalance$buildMixedGroups(BlockPos pos, BlockState bottomSlab, BlockState topSlab) {
        bottomSlab = bitsandbalance$horizontalSlabRenderState(bottomSlab, SlabType.BOTTOM);
        topSlab = bitsandbalance$horizontalSlabRenderState(topSlab, SlabType.TOP);
        ArrayList<RenderGroup> groups = new ArrayList<>(2);
        if (bottomSlab != null) {
            BlockStateModelPart part = bitsandbalance$buildCellPart(bottomSlab, pos, MIXED_BOTTOM_FROM, MIXED_BOTTOM_TO);
            if (part != null) {
                int mask = mixedBottomVisibleMask(pos, bottomSlab, topSlab);
                groups.add(new RenderGroup(bottomSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        if (topSlab != null) {
            BlockStateModelPart part = bitsandbalance$buildCellPart(topSlab, pos, MIXED_TOP_FROM, MIXED_TOP_TO);
            if (part != null) {
                int mask = bottomSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, Direction.DOWN) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(topSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        return bitsandbalance$mergeGroups(groups);
    }

    static int mixedBottomVisibleMask(BlockPos pos, BlockState bottomSlab, BlockState topSlab) {
        if (topSlab == null || bottomSlab == null) {
            return ALL_DIRECTIONS;
        }
        return FabricRuntimeModelCompat.sourceModelHeight(bottomSlab, pos) >= 7.999F
                ? bitsandbalance$without(ALL_DIRECTIONS, Direction.UP)
                : ALL_DIRECTIONS;
    }

    private static BlockStateModelPart bitsandbalance$buildCellPart(BlockState slabState,
                                                                    BlockPos pos,
                                                                    Vector3fc from,
                                                                    Vector3fc to) {
        Object part = FabricRuntimeModelCompat.buildCuboidPart(slabState, pos, from, to);
        return part instanceof BlockStateModelPart blockStateModelPart ? blockStateModelPart : null;
    }

    private static List<RenderGroup> bitsandbalance$buildFullBlockGroups(BlockPos pos, BlockState fullBlockState) {
        Object part = FabricRuntimeModelCompat.buildExactCuboidPart(
                fullBlockState,
                pos,
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 16.0F, 16.0F),
                ALL_DIRECTIONS
        );
        return part instanceof BlockStateModelPart blockStateModelPart
                ? List.of(new RenderGroup(fullBlockState, FabricRuntimeModelCompat.singletonPartList(blockStateModelPart)))
                : List.of();
    }

    private static BlockState bitsandbalance$fullBlockStateForCompleteFamily(Object slabs) {
        return SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(
                bitsandbalance$slot(slabs, 0),
                bitsandbalance$slot(slabs, 1),
                bitsandbalance$slot(slabs, 2),
                bitsandbalance$slot(slabs, 3)
        );
    }

    private static List<RenderGroup> bitsandbalance$buildVerticalSlabGroups(BlockPos pos,
                                                                            BlockState ownerState,
                                                                            BlockState facingSlab,
                                                                            BlockState oppositeSlab) {
        if (ownerState.getValue(VerticalSlabBlock.DOUBLE)) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab);
            if (fullBlockState != null) {
                return bitsandbalance$buildFullBlockGroups(pos, fullBlockState);
            }
        }

        Direction facing = ownerState.getValue(VerticalSlabBlock.FACING);
        ArrayList<RenderGroup> groups = new ArrayList<>(2);
        if (facingSlab != null) {
            BlockStateModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(facingSlab, pos, facing);
            if (part != null) {
                int mask = oppositeSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(facingSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        if (ownerState.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
            Direction oppositeFacing = facing.getOpposite();
            BlockStateModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(oppositeSlab, pos, oppositeFacing);
            if (part != null) {
                int mask = facingSlab != null ? bitsandbalance$without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(oppositeSlab, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(part, mask))));
            }
        }
        return bitsandbalance$mergeGroups(groups);
    }

    private static List<RenderGroup> bitsandbalance$buildStepGroups(BlockPos pos, BlockState ownerState, BlockState slabState) {
        boolean top = ownerState.getValue(StepBlock.TOP);
        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        BlockStateModelPart part = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, ownerState.getValue(StepBlock.FACING), top);
        return part == null ? List.of() : List.of(new RenderGroup(renderSlab, FabricRuntimeModelCompat.singletonPartList(part)));
    }

    private static List<RenderGroup> bitsandbalance$buildVerticalStepGroups(BlockPos pos, BlockState ownerState, BlockState slabState) {
        BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slabState, pos, ownerState.getValue(VerticalStepBlock.FACING), ownerState.getValue(VerticalStepBlock.SIDE));
        return part == null ? List.of() : List.of(new RenderGroup(slabState, FabricRuntimeModelCompat.singletonPartList(part)));
    }

    private static List<RenderGroup> bitsandbalance$buildQuadStepGroups(BlockPos pos, BlockState ownerState, Object slabs) {
        BlockState fullBlockState = bitsandbalance$fullBlockStateForCompleteFamily(slabs);
        if (fullBlockState != null) {
            return bitsandbalance$buildFullBlockGroups(pos, fullBlockState);
        }

        Direction.Axis axis = ownerState.getValue(QuadStepBlock.AXIS);
        Map<BlockState, List<BlockStateModelPart>> groups = new LinkedHashMap<>(8);
        for (int i = 0; i < 4; i++) {
            BlockState slab = bitsandbalance$slot(slabs, i);
            if (slab == null) {
                continue;
            }
            boolean positive = i == 1 || i == 3;
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            boolean top = i >= 2;
            int mask = bitsandbalance$quadStepVisibleMask(axis, i, slabs);
            bitsandbalance$addStepParts(groups, pos, slab, facing, top, mask);
        }
        return bitsandbalance$toRenderGroups(groups);
    }

    private static void bitsandbalance$addStepParts(Map<BlockState, List<BlockStateModelPart>> groups,
                                                    BlockPos pos,
                                                    BlockState slabState,
                                                    Direction facing,
                                                    boolean top,
                                                    int visibleMask) {
        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        if (!bitsandbalance$hasJoinedVerticalNeighbor(top, visibleMask) || (visibleMask & HORIZONTAL_DIRECTIONS) == 0) {
            BlockStateModelPart part = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, facing, top);
            if (part != null) {
                groups.computeIfAbsent(renderSlab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(part, visibleMask));
            }
            return;
        }

        int bodyMask = visibleMask & ~HORIZONTAL_DIRECTIONS;
        BlockStateModelPart bodyPart = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, facing, top);
        if (bodyPart != null && bodyMask != 0) {
            groups.computeIfAbsent(renderSlab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(bodyPart, bodyMask));
        }

        BlockState sideState = bitsandbalance$fullBlockStateForSlab(renderSlab);
        if (sideState == null) {
            sideState = renderSlab;
        }
        BlockStateModelPart sidePart = bitsandbalance$buildJoinedStepSidePart(sideState, renderSlab, pos, facing, top, visibleMask & HORIZONTAL_DIRECTIONS);
        if (sidePart != null) {
            groups.computeIfAbsent(sideState, ignored -> new ArrayList<>(4)).add(sidePart);
        }
    }

    private static BlockStateModelPart bitsandbalance$buildJoinedStepSidePart(BlockState sideState,
                                                                               BlockState sourceState,
                                                                               BlockPos pos,
                                                                               Direction facing,
                                                                               boolean top,
                                                                               int visibleMask) {
        if (visibleMask == 0) {
            return null;
        }

        float yMin = top ? 8.0f : 0.0f;
        float yMax = top ? Math.min(16.0f, 8.0f + FabricRuntimeModelCompat.sourceModelHeight(sourceState, pos)) : 8.0f;
        Object part = FabricRuntimeModelCompat.buildExactCuboidPart(
                sideState,
                pos,
                bitsandbalance$stepFrom(facing, yMin),
                bitsandbalance$stepTo(facing, yMax),
                visibleMask
        );
        return part instanceof BlockStateModelPart blockStateModelPart ? blockStateModelPart : null;
    }

    private static BlockState bitsandbalance$stepRenderState(BlockState slabState, boolean top) {
        return bitsandbalance$horizontalSlabRenderState(slabState, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState bitsandbalance$horizontalSlabRenderState(BlockState slabState, SlabType half) {
        if (slabState != null && slabState.hasProperty(SlabBlock.TYPE)) {
            slabState = slabState.setValue(SlabBlock.TYPE, half);
        }
        if (slabState != null && slabState.hasProperty(SlabBlock.WATERLOGGED)) {
            slabState = slabState.setValue(SlabBlock.WATERLOGGED, false);
        }
        return slabState;
    }

    private static boolean bitsandbalance$hasJoinedVerticalNeighbor(boolean top, int visibleMask) {
        Direction hidden = top ? Direction.DOWN : Direction.UP;
        return (visibleMask & (1 << hidden.ordinal())) == 0;
    }

    private static BlockState bitsandbalance$fullBlockStateForSlab(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(slabState);
    }

    private static Vector3fc bitsandbalance$stepFrom(Direction facing, float yMin) {
        return switch (facing) {
            case SOUTH -> new Vector3f(0.0F, yMin, 8.0F);
            case EAST -> new Vector3f(8.0F, yMin, 0.0F);
            default -> new Vector3f(0.0F, yMin, 0.0F);
        };
    }

    private static Vector3fc bitsandbalance$stepTo(Direction facing, float yMax) {
        return switch (facing) {
            case SOUTH, EAST -> new Vector3f(16.0F, yMax, 16.0F);
            case NORTH -> new Vector3f(16.0F, yMax, 8.0F);
            default -> new Vector3f(8.0F, yMax, 16.0F);
        };
    }

    private static List<RenderGroup> bitsandbalance$buildQuadVerticalStepGroups(BlockPos pos, Object slabs) {
        BlockState fullBlockState = bitsandbalance$fullBlockStateForCompleteFamily(slabs);
        if (fullBlockState != null) {
            return bitsandbalance$buildFullBlockGroups(pos, fullBlockState);
        }

        Map<BlockState, List<BlockStateModelPart>> groups = new LinkedHashMap<>(4);
        for (int i = 0; i < 4; i++) {
            BlockState slab = bitsandbalance$slot(slabs, i);
            if (slab == null) {
                continue;
            }
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slab, pos, facing, side);
            if (part == null) {
                continue;
            }
            int mask = bitsandbalance$quadVerticalStepVisibleMask(i, slabs);
            groups.computeIfAbsent(slab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(part, mask));
        }
        return bitsandbalance$toRenderGroups(groups);
    }

    private static boolean bitsandbalance$renderGroups(Object dispatcher,
                                                       BlockAndTintGetter level,
                                                       BlockPos pos,
                                                       PoseStack poseStack,
                                                       VertexConsumer consumer,
                                                       List<RenderGroup> groups) {
        boolean rendered = false;
        for (RenderGroup group : groups) {
            if (group == null || group.renderState() == null || group.parts().isEmpty()) {
                continue;
            }
            bitsandbalance$renderGroup(dispatcher, level, pos, poseStack, consumer, group.renderState(), group.parts());
            rendered = true;
        }
        return rendered;
    }

    private static boolean bitsandbalance$renderGroup(Object dispatcher,
                                                      BlockAndTintGetter level,
                                                      BlockPos pos,
                                                      PoseStack poseStack,
                                                      VertexConsumer consumer,
                                                      BlockState renderState,
                                                      List<BlockStateModelPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return false;
        }
        BlockColorRenderContext.setForcedPosLong(pos.asLong());
        try {
            renderNested(dispatcher, renderState, pos, level, poseStack, consumer, parts, true);
        } finally {
            BlockColorRenderContext.clear();
        }
        return true;
    }

    private static List<RenderGroup> bitsandbalance$toRenderGroups(Map<BlockState, List<BlockStateModelPart>> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        ArrayList<RenderGroup> renderGroups = new ArrayList<>(groups.size());
        for (Map.Entry<BlockState, List<BlockStateModelPart>> entry : groups.entrySet()) {
            renderGroups.add(new RenderGroup(entry.getKey(), FabricRuntimeModelCompat.partList(entry.getValue())));
        }
        return renderGroups;
    }

    private static List<RenderGroup> bitsandbalance$mergeGroups(List<RenderGroup> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        if (groups.size() == 1) {
            RenderGroup group = groups.getFirst();
            return group == null || group.renderState() == null || group.parts() == null || group.parts().isEmpty()
                    ? List.of()
                    : groups;
        }
        Map<BlockState, List<BlockStateModelPart>> merged = new LinkedHashMap<>(groups.size());
        for (RenderGroup group : groups) {
            if (group == null || group.renderState() == null || group.parts() == null) {
                continue;
            }
            merged.computeIfAbsent(group.renderState(), ignored -> new ArrayList<>()).addAll(group.parts());
        }
        return bitsandbalance$toRenderGroups(merged);
    }

    private static List<RenderGroup> bitsandbalance$getOrBuildFixedGroups(BlockState ownerState) {
        List<RenderGroup> cached = FIXED_TERRAIN_CACHE.get(ownerState);
        if (cached != null) {
            return cached;
        }

        List<RenderGroup> built = bitsandbalance$buildFixedGroups(ownerState);
        if (FIXED_TERRAIN_CACHE.size() >= FIXED_TERRAIN_CACHE_LIMIT) {
            FIXED_TERRAIN_CACHE.clear();
        }
        List<RenderGroup> raced = FIXED_TERRAIN_CACHE.putIfAbsent(ownerState, built);
        return raced != null ? raced : built;
    }

    private static List<RenderGroup> bitsandbalance$buildFixedGroups(BlockState ownerState) {
        if (ownerState.getBlock() instanceof FixedStepBlock fixedStep) {
            boolean top = ownerState.getValue(StepBlock.TOP);
            BlockState slabState = bitsandbalance$stepRenderState(fixedStep.getSourceSlab().defaultBlockState(), top);
            BlockStateModelPart part = StepBlockEntityRenderer.buildQuadrantPart(slabState, BlockPos.ZERO, ownerState.getValue(StepBlock.FACING), top);
            return part == null ? List.of() : List.of(new RenderGroup(slabState, FabricRuntimeModelCompat.singletonPartList(part)));
        }
        if (ownerState.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            BlockState slabState = bitsandbalance$resolveVerticalStepSourceState(fixedVerticalStep.getSourceVerticalSlab());
            BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slabState, BlockPos.ZERO, ownerState.getValue(VerticalStepBlock.FACING), ownerState.getValue(VerticalStepBlock.SIDE));
            return part == null ? List.of() : List.of(new RenderGroup(slabState, FabricRuntimeModelCompat.singletonPartList(part)));
        }
        if (ownerState.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            BlockState slabState = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            Direction facing = ownerState.getValue(VerticalSlabBlock.FACING);
            ArrayList<RenderGroup> groups = new ArrayList<>(2);
            BlockStateModelPart facingPart = VerticalSlabBlockEntityRenderer.buildTerrainPart(slabState, BlockPos.ZERO, facing);
            if (facingPart != null) {
                int mask = ownerState.getValue(VerticalSlabBlock.DOUBLE) ? bitsandbalance$without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
                groups.add(new RenderGroup(slabState, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(facingPart, mask))));
            }
            if (ownerState.getValue(VerticalSlabBlock.DOUBLE)) {
                BlockStateModelPart oppositePart = VerticalSlabBlockEntityRenderer.buildTerrainPart(slabState, BlockPos.ZERO, facing.getOpposite());
                if (oppositePart != null) {
                    groups.add(new RenderGroup(slabState, FabricRuntimeModelCompat.singletonPartList(FabricRuntimeModelCompat.filterDirections(oppositePart, bitsandbalance$without(ALL_DIRECTIONS, facing)))));
                }
            }
            return bitsandbalance$mergeGroups(groups);
        }
        return List.of();
    }

    private static BlockState bitsandbalance$resolveStepSourceState(BlockState candidate, BlockState ownerState) {
        BlockState inferred = null;
        if (ownerState != null) {
            if (ownerState.getBlock() instanceof FixedStepBlock fixed) {
                inferred = fixed.getSourceSlab().defaultBlockState();
            } else {
                net.minecraft.world.level.block.Block sourceSlab = StepDynamicRegistry.getSlabForStep(ownerState.getBlock());
                if (sourceSlab != null) {
                    inferred = sourceSlab.defaultBlockState();
                }
            }
        }
        return bitsandbalance$preferInferredSource(candidate, inferred);
    }

    private static BlockState bitsandbalance$resolveVerticalStepSourceState(BlockState candidate, BlockState ownerState) {
        BlockState inferred = null;
        if (ownerState != null) {
            net.minecraft.world.level.block.Block sourceVerticalSlab = null;
            if (ownerState.getBlock() instanceof FixedVerticalStepBlock fixed) {
                sourceVerticalSlab = fixed.getSourceVerticalSlab();
            } else {
                sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(ownerState.getBlock());
            }
            if (sourceVerticalSlab != null) {
                inferred = bitsandbalance$resolveVerticalStepSourceState(sourceVerticalSlab);
            }
        }
        return bitsandbalance$preferInferredSource(candidate, inferred);
    }

    private static BlockState bitsandbalance$resolveVerticalSlabSourceState(BlockState candidate, BlockState ownerState) {
        BlockState inferred = null;
        if (ownerState != null) {
            if (ownerState.getBlock() instanceof FixedVerticalSlabBlock fixed) {
                inferred = fixed.getSourceSlab().defaultBlockState();
            } else {
                net.minecraft.world.level.block.Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(ownerState.getBlock());
                if (sourceSlab != null) {
                    inferred = sourceSlab.defaultBlockState();
                }
            }
        }
        return bitsandbalance$preferInferredSource(candidate, inferred);
    }

    private static BlockState bitsandbalance$preferInferredSource(BlockState candidate, BlockState inferred) {
        if (inferred == null) {
            return candidate;
        }
        if (candidate == null || candidate.getBlock() != inferred.getBlock()) {
            return inferred;
        }
        return candidate;
    }

    private static BlockState bitsandbalance$resolveVerticalStepSourceState(net.minecraft.world.level.block.Block sourceVerticalSlab) {
        net.minecraft.world.level.block.Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return (sourceSlab != null ? sourceSlab : sourceVerticalSlab).defaultBlockState();
    }

    private static int bitsandbalance$quadStepVisibleMask(Direction.Axis axis, int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (bitsandbalance$sameRenderMaterial(bitsandbalance$slot(slabs, index), bitsandbalance$slot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = bitsandbalance$without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (bitsandbalance$sameRenderMaterial(bitsandbalance$slot(slabs, index), bitsandbalance$slot(slabs, verticalNeighbor))) {
            mask = bitsandbalance$without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int bitsandbalance$quadVerticalStepVisibleMask(int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (bitsandbalance$slot(slabs, eastWestNeighbor) != null) {
            mask = bitsandbalance$without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (bitsandbalance$slot(slabs, northSouthNeighbor) != null) {
            mask = bitsandbalance$without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static int bitsandbalance$without(int mask, Direction direction) {
        return mask & ~(1 << direction.ordinal());
    }

    private static boolean bitsandbalance$sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }

    private static int bitsandbalance$directionMask(Direction... directions) {
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

    private record CreateModelContext(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    }

    private record RenderGroup(BlockState renderState, List<BlockStateModelPart> parts) {
    }
}
