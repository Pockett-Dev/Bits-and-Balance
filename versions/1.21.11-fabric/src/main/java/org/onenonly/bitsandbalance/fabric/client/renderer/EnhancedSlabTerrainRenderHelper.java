package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFaceCullingHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EnhancedSlabTerrainRenderHelper {
    private static final ThreadLocal<Integer> NESTED_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> CREATE_CONTRAPTION_RENDER = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<CreateModelContext> CREATE_MODEL_CONTEXT = new ThreadLocal<>();
    private static final AtomicBoolean MIXED_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean VERTICAL_SLAB_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean STEP_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean VERTICAL_STEP_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean QUAD_STEP_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final AtomicBoolean QUAD_VERTICAL_STEP_TERRAIN_RENDERED = new AtomicBoolean(false);
    private static final int ALL_DIRECTIONS = EnhancedSlabRenderModelCompat.allDirectionMask();
    private static final int HORIZONTAL_DIRECTIONS = directionMask(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    private static final int FULL_CELL_MASK = 0xFF;
    private static final int BOTTOM_HALF_CELL_MASK = cellMask(-1, 0, -1);
    private static final int TOP_HALF_CELL_MASK = cellMask(-1, 1, -1);
    private static final Vector3f MIXED_BOTTOM_FROM = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f MIXED_BOTTOM_TO = new Vector3f(16.0F, 8.0F, 16.0F);
    private static final Vector3f MIXED_TOP_FROM = new Vector3f(0.0F, 8.0F, 0.0F);
    private static final Vector3f MIXED_TOP_TO = new Vector3f(16.0F, 16.0F, 16.0F);
    private static final int RENDER_PLAN_CACHE_LIMIT = 8192;
    private static final int CELL_PART_CACHE_LIMIT = 32768;
    private static final int DIRECT_CELL_QUAD_CACHE_LIMIT = 32768;
    private static final Object RENDER_PLAN_CACHE_LOCK = new Object();
    private static final Object CELL_PART_CACHE_LOCK = new Object();
    private static final Object DIRECT_CELL_QUAD_CACHE_LOCK = new Object();
    private static final Map<RenderPlanKey, List<RenderGroup>> RENDER_PLAN_CACHE = createBoundedCache(RENDER_PLAN_CACHE_LIMIT);
    private static final Map<CellPartKey, List<BlockModelPart>> CELL_PART_CACHE = createBoundedCache(CELL_PART_CACHE_LIMIT);
    private static final Map<CellPartKey, List<BakedQuad>> DIRECT_CELL_QUAD_CACHE = createBoundedCache(DIRECT_CELL_QUAD_CACHE_LIMIT);

    private EnhancedSlabTerrainRenderHelper() {
    }

    public static void clearCaches() {
        synchronized (RENDER_PLAN_CACHE_LOCK) {
            RENDER_PLAN_CACHE.clear();
        }
        synchronized (CELL_PART_CACHE_LOCK) {
            CELL_PART_CACHE.clear();
        }
        synchronized (DIRECT_CELL_QUAD_CACHE_LOCK) {
            DIRECT_CELL_QUAD_CACHE.clear();
        }
    }

    public static boolean isEnabled() {
        return FabricTweaksConfig.enableEnhancedSlabs;
    }

    public static boolean handles(BlockState state) {
        if (!isEnabled() || state == null) {
            return false;
        }
        return handlesEnabledState(state);
    }

    public static boolean handlesEnabledState(BlockState state) {
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

    public static boolean render(BlockRenderDispatcher dispatcher,
                                 BlockAndTintGetter level,
                                 BlockPos pos,
                                 BlockState state,
                                 PoseStack poseStack,
                                 VertexConsumer consumer) {
        if (dispatcher == null || level == null || pos == null || poseStack == null || consumer == null) {
            return false;
        }
        if (rendersWithoutBlockEntity(state)) {
            return false;
        }
        if (!EnhancedSlabRendererCompat.preferVanillaTerrainPath() && !rendersWithoutBlockEntity(state)) {
            return false;
        }
        if (!handlesEnabledState(state) || NESTED_RENDER_DEPTH.get() > 0) {
            return false;
        }

        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
            if (rendered) {
                markTerrainRendered(state);
            }
            return rendered;
        }

        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
            if (rendered) {
                markTerrainRendered(state);
            }
            return rendered;
        }

        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
            if (rendered) {
                markTerrainRendered(state);
            }
            return rendered;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (state.getBlock() instanceof MixedSlabBlock) {
            if (hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        if (state.getBlock() instanceof VerticalSlabBlock) {
            if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab || hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        if (state.getBlock() instanceof StepBlock) {
            if (blockEntity instanceof StepBlockEntity step && step.getSlabState() != null || hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            if (blockEntity instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null || hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            if (blockEntity instanceof QuadStepBlockEntity quadStep || hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep || hasMaterialSlots(level, pos, state)) {
                boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, getOrBuildRenderPlan(level, pos, state));
                if (rendered) {
                    markTerrainRendered(state);
                }
                return rendered;
            }
            return false;
        }

        return false;
    }

    public static boolean renderCreateContraption(BlockRenderDispatcher dispatcher,
                                                  BlockAndTintGetter level,
                                                  BlockPos pos,
                                                  BlockState state,
                                                  PoseStack poseStack,
                                                  VertexConsumer consumer) {
        if (dispatcher == null || level == null || pos == null || state == null || poseStack == null || consumer == null
                || !handlesEnabledState(state) || NESTED_RENDER_DEPTH.get() > 0) {
            return false;
        }

        List<RenderGroup> groups = rendersWithoutBlockEntity(state)
                ? getOrBuildFixedRenderPlan(state)
                : getOrBuildCreateRenderPlan(level, pos, state);
        boolean rendered = renderPlan(dispatcher, level, pos, poseStack, consumer, groups);
        if (rendered) {
            markTerrainRendered(state);
        }
        return rendered;
    }

    public static void rememberCreateModelContext(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || !handlesEnabledState(state)) {
            CREATE_MODEL_CONTEXT.remove();
            return;
        }
        CREATE_MODEL_CONTEXT.set(new CreateModelContext(level, pos.immutable(), state));
    }

    public static boolean collectCreateModelParts(BlockState ownerState, List<BlockModelPart> parts) {
        CreateModelContext context = CREATE_MODEL_CONTEXT.get();
        CREATE_MODEL_CONTEXT.remove();
        if (context == null
                || ownerState == null
                || parts == null
                || !handlesEnabledState(ownerState)
                || !ownerState.equals(context.state())) {
            return false;
        }

        boolean added = false;
        for (RenderGroup group : getOrBuildCreateRenderPlan(context.level(), context.pos(), ownerState)) {
            if (group == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            parts.addAll(group.parts());
            added = true;
        }
        return added;
    }

    static void renderNested(BlockRenderDispatcher dispatcher,
                             BlockState renderState,
                             BlockPos pos,
                             BlockAndTintGetter level,
                             PoseStack poseStack,
                             VertexConsumer consumer,
                             List<BlockModelPart> parts) {
        if (dispatcher == null || renderState == null || pos == null || level == null || poseStack == null || consumer == null || parts == null || parts.isEmpty()) {
            return;
        }

        int previousDepth = NESTED_RENDER_DEPTH.get();
        NESTED_RENDER_DEPTH.set(previousDepth + 1);
        try {
            dispatcher.renderBatched(renderState, pos, level, poseStack, consumer, false, parts);
        } finally {
            NESTED_RENDER_DEPTH.set(previousDepth);
        }
    }

    public static boolean shouldSkipBlockEntityRender(BlockState ownerState) {
        return false;
    }

    public static boolean shouldSkipBlockEntityRender(BlockAndTintGetter level, BlockPos pos, BlockState ownerState) {
        if (rendersWithoutBlockEntity(ownerState)) {
            return true;
        }
        if (!EnhancedSlabRendererCompat.preferChunkRenderPath()
                || !isEnabled()
                || level == null
                || pos == null
                || ownerState == null
                || !handlesEnabledState(ownerState)) {
            return false;
        }
        return !getOrBuildRenderPlan(level, pos, ownerState).isEmpty();
    }

    public static boolean isTerrainRenderActive(BlockState ownerState) {
        if (ownerState == null) {
            return false;
        }
        if (ownerState.getBlock() instanceof MixedSlabBlock) {
            return MIXED_TERRAIN_RENDERED.get();
        }
        if (ownerState.getBlock() instanceof VerticalSlabBlock) {
            return VERTICAL_SLAB_TERRAIN_RENDERED.get();
        }
        if (ownerState.getBlock() instanceof StepBlock) {
            return STEP_TERRAIN_RENDERED.get();
        }
        if (ownerState.getBlock() instanceof VerticalStepBlock) {
            return VERTICAL_STEP_TERRAIN_RENDERED.get();
        }
        if (ownerState.getBlock() instanceof QuadStepBlock) {
            return QUAD_STEP_TERRAIN_RENDERED.get();
        }
        if (ownerState.getBlock() instanceof QuadVerticalStepBlock) {
            return QUAD_VERTICAL_STEP_TERRAIN_RENDERED.get();
        }
        return false;
    }

    public static int mixedBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static int verticalSlabBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static int stepBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static int verticalStepBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static int quadStepBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static int quadVerticalStepBlockEntityViewDistance() {
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
    }

    public static boolean rendersWithoutBlockEntity(BlockState state) {
        return state != null
                && (state.getBlock() instanceof FixedVerticalSlabBlock
                || state.getBlock() instanceof FixedStepBlock
                || state.getBlock() instanceof FixedVerticalStepBlock);
    }

    private static void markTerrainRendered(BlockState state) {
        if (state == null) {
            return;
        }
        if (state.getBlock() instanceof MixedSlabBlock) {
            EnhancedSlabRenderDiagnostics.mixedTerrain();
        } else if (state.getBlock() instanceof VerticalSlabBlock) {
            EnhancedSlabRenderDiagnostics.verticalSlabTerrain();
        } else if (state.getBlock() instanceof StepBlock) {
            EnhancedSlabRenderDiagnostics.stepTerrain();
        } else if (state.getBlock() instanceof VerticalStepBlock) {
            EnhancedSlabRenderDiagnostics.verticalStepTerrain();
        } else if (state.getBlock() instanceof QuadStepBlock) {
            EnhancedSlabRenderDiagnostics.quadStepTerrain();
        } else if (state.getBlock() instanceof QuadVerticalStepBlock) {
            EnhancedSlabRenderDiagnostics.quadVerticalStepTerrain();
        }
    }

    public static boolean emitFabricRendererQuads(BlockAndTintGetter level, BlockPos pos, BlockState state, RenderPlanConsumer consumer) {
        if (level == null || pos == null || state == null || consumer == null || !handlesEnabledState(state)
                || rendersWithoutBlockEntity(state)) {
            return false;
        }
        List<RenderGroup> groups = getOrBuildRenderPlan(level, pos, state);
        if (groups.isEmpty()) {
            return false;
        }
        markTerrainRendered(state);
        for (RenderGroup group : groups) {
            if (group.renderState != null && group.parts != null && !group.parts.isEmpty()) {
                consumer.accept(group.renderState, group.parts);
            }
        }
        return true;
    }

    public static boolean emitFabricRendererDirectQuads(BlockAndTintGetter level, BlockPos pos, BlockState state, DirectRenderPlanConsumer consumer) {
        if (level == null || pos == null || state == null || consumer == null || !handlesEnabledState(state)
                || rendersWithoutBlockEntity(state)) {
            return false;
        }
        List<DirectRenderGroup> groups = buildDirectRenderPlan(level, pos, state);
        if (groups.isEmpty()) {
            return false;
        }
        markTerrainRendered(state);
        for (DirectRenderGroup group : groups) {
            if (group.renderState != null && group.quads != null && !group.quads.isEmpty()) {
                consumer.accept(group.renderState, group.quads);
            }
        }
        return true;
    }

    public static Object createChunkGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, Object baseKey) {
        RenderPlanKey planKey = renderPlanKey(level, pos, state);
        return planKey == null ? baseKey : new ChunkGeometryKey(baseKey, planKey);
    }

    public static Object createFixedChunkGeometryKey(BlockState state, Object baseKey) {
        RenderPlanKey planKey = fixedRenderPlanKey(state);
        return planKey == null ? baseKey : new ChunkGeometryKey(baseKey, planKey);
    }

    public static List<RenderGroup> getOrBuildFixedRenderPlan(BlockState state) {
        RenderPlanKey key = fixedRenderPlanKey(state);
        if (key == null) {
            return Collections.emptyList();
        }
        synchronized (RENDER_PLAN_CACHE_LOCK) {
            List<RenderGroup> cached = RENDER_PLAN_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<RenderGroup> built = buildRenderPlan(null, BlockPos.ZERO, state);
        synchronized (RENDER_PLAN_CACHE_LOCK) {
            RENDER_PLAN_CACHE.put(key, built);
        }
        return built;
    }

    public static List<RenderGroup> getOrBuildRenderPlan(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        RenderPlanKey key = renderPlanKey(level, pos, state);
        if (key == null) {
            return Collections.emptyList();
        }
        synchronized (RENDER_PLAN_CACHE_LOCK) {
            List<RenderGroup> cached = RENDER_PLAN_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<RenderGroup> built = buildRenderPlan(level, pos, state);
        synchronized (RENDER_PLAN_CACHE_LOCK) {
            RENDER_PLAN_CACHE.put(key, built);
        }
        return built;
    }

    public static List<RenderGroup> getOrBuildCreateRenderPlan(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return Collections.emptyList();
        }

        boolean previous = CREATE_CONTRAPTION_RENDER.get();
        CREATE_CONTRAPTION_RENDER.set(true);
        try {
            return buildRenderPlan(level, pos, state);
        } finally {
            CREATE_CONTRAPTION_RENDER.set(previous);
        }
    }

    private static RenderPlanKey renderPlanKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return null;
        }

        BlockState s0 = null;
        BlockState s1 = null;
        BlockState s2 = null;
        BlockState s3 = null;
        int family;
        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            family = 2;
            s0 = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            s1 = state.getValue(VerticalSlabBlock.DOUBLE) ? s0 : null;
        } else if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            family = 3;
            s0 = fixedStep.getSourceSlab().defaultBlockState();
        } else if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            family = 4;
            s0 = sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
        } else {
            Object materials = materialSource(level, pos, state);
            boolean hasMaterials = materials != null;
            if (state.getBlock() instanceof MixedSlabBlock && hasMaterials) {
                family = 1;
                s0 = slot(materials, 0);
                s1 = slot(materials, 1);
            } else if (state.getBlock() instanceof VerticalSlabBlock && hasMaterials) {
                family = 2;
                s0 = slot(materials, 0);
                s1 = state.getValue(VerticalSlabBlock.DOUBLE) ? slot(materials, 1) : null;
            } else if (state.getBlock() instanceof StepBlock && hasMaterials) {
                family = 3;
                s0 = slot(materials, 0);
            } else if (state.getBlock() instanceof VerticalStepBlock && hasMaterials) {
                family = 4;
                s0 = slot(materials, 0);
            } else if (state.getBlock() instanceof QuadStepBlock && hasMaterials) {
                family = 5;
                s0 = slot(materials, 0);
                s1 = slot(materials, 1);
                s2 = slot(materials, 2);
                s3 = slot(materials, 3);
            } else if (state.getBlock() instanceof QuadVerticalStepBlock && hasMaterials) {
                family = 6;
                s0 = slot(materials, 0);
                s1 = slot(materials, 1);
                s2 = slot(materials, 2);
                s3 = slot(materials, 3);
            } else {
                return null;
            }
        }

        NeighborOcclusionKey west = neighborOcclusionKey(level, pos, Direction.WEST);
        NeighborOcclusionKey east = neighborOcclusionKey(level, pos, Direction.EAST);
        NeighborOcclusionKey down = neighborOcclusionKey(level, pos, Direction.DOWN);
        NeighborOcclusionKey up = neighborOcclusionKey(level, pos, Direction.UP);
        NeighborOcclusionKey north = neighborOcclusionKey(level, pos, Direction.NORTH);
        NeighborOcclusionKey south = neighborOcclusionKey(level, pos, Direction.SOUTH);
        return new RenderPlanKey(family, state, s0, s1, s2, s3, west, east, down, up, north, south);
    }

    private static RenderPlanKey fixedRenderPlanKey(BlockState state) {
        if (!rendersWithoutBlockEntity(state)) {
            return null;
        }

        BlockState s0;
        BlockState s1 = null;
        int family;
        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            family = 2;
            s0 = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            s1 = state.getValue(VerticalSlabBlock.DOUBLE) ? s0 : null;
        } else if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            family = 3;
            s0 = fixedStep.getSourceSlab().defaultBlockState();
        } else if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            family = 4;
            s0 = sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
        } else {
            return null;
        }
        return new RenderPlanKey(
                family,
                state,
                s0,
                s1,
                null,
                null,
                NeighborOcclusionKey.EMPTY,
                NeighborOcclusionKey.EMPTY,
                NeighborOcclusionKey.EMPTY,
                NeighborOcclusionKey.EMPTY,
                NeighborOcclusionKey.EMPTY,
                NeighborOcclusionKey.EMPTY
        );
    }

    private static NeighborOcclusionKey neighborOcclusionKey(BlockAndTintGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        int mask = occludingCellMask(level, neighborPos, neighborState);
        if (!usesMaterialAwareOcclusion(neighborState)) {
            return new NeighborOcclusionKey(mask, null, null, null, null, null);
        }

        BlockState s0 = null;
        BlockState s1 = null;
        BlockState s2 = null;
        BlockState s3 = null;
        if (neighborState.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            s0 = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            s1 = neighborState.getValue(VerticalSlabBlock.DOUBLE) ? s0 : null;
        } else if (neighborState.getBlock() instanceof FixedStepBlock fixedStep) {
            s0 = fixedStep.getSourceSlab().defaultBlockState();
        } else if (neighborState.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            s0 = sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
        } else {
            Object materials = materialSource(level, neighborPos, neighborState);
            s0 = slot(materials, 0);
            s1 = slot(materials, 1);
            s2 = slot(materials, 2);
            s3 = slot(materials, 3);
        }
        return new NeighborOcclusionKey(mask, neighborState, s0, s1, s2, s3);
    }

    private static Object materialSource(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return null;
        }

        BlockState[] createSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        if (createSlots != null) {
            return createSlots;
        }

        EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
        if (stored != null) {
            return stored;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] migrated = null;
        if (state.getBlock() instanceof VerticalSlabBlock && blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            migrated = new BlockState[]{
                    verticalSlab.getFacingSlab(),
                    state.getValue(VerticalSlabBlock.DOUBLE) ? verticalSlab.getOppositeSlab() : null,
                    null,
                    null
            };
        } else if (state.getBlock() instanceof StepBlock && blockEntity instanceof StepBlockEntity step) {
            migrated = new BlockState[]{step.getSlabState(), null, null, null};
        } else if (state.getBlock() instanceof VerticalStepBlock && blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            migrated = new BlockState[]{verticalStep.getSlabState(), null, null, null};
        } else if (state.getBlock() instanceof QuadStepBlock && blockEntity instanceof QuadStepBlockEntity quadStep) {
            migrated = quadStep.getAllSlabs();
        } else if (state.getBlock() instanceof QuadVerticalStepBlock && blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            migrated = quadVerticalStep.getAllSlabs();
        }

        if (migrated != null) {
            if (level instanceof Level actualLevel) {
                EnhancedSlabMaterialSavedData.rememberClient(actualLevel, pos.asLong(), migrated);
            } else {
                EnhancedSlabMaterialSavedData.rememberClient(pos.asLong(), migrated);
            }
        }
        return migrated;
    }

    private static boolean hasMaterialSlots(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return materialSource(level, pos, state) != null;
    }

    private static @Nullable EnhancedSlabMaterialSavedData.MaterialView storedMaterialView(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        return level instanceof Level actualLevel
                ? EnhancedSlabMaterialSavedData.getClientView(actualLevel, pos.asLong())
                : EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
    }

    private static BlockState slot(Object slots, int index) {
        if (slots instanceof EnhancedSlabMaterialSavedData.MaterialView view) {
            return view.slot(index);
        }
        BlockState[] array = slots instanceof BlockState[] states ? states : null;
        return array != null && index >= 0 && index < array.length ? array[index] : null;
    }

    private static BlockState fullBlockRenderState(Object slots) {
        return SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(
                slot(slots, 0),
                slot(slots, 1),
                slot(slots, 2),
                slot(slots, 3)
        );
    }

    private static List<RenderGroup> buildRenderPlan(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            BlockState source = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            return buildVerticalSlabPlan(
                    level,
                    pos,
                    source,
                    state.getValue(VerticalSlabBlock.DOUBLE) ? source : null,
                    state.getValue(VerticalSlabBlock.FACING),
                    state.getValue(VerticalSlabBlock.DOUBLE)
            );
        }
        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return buildStepPlan(level, pos, state, fixedStep.getSourceSlab().defaultBlockState());
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return buildVerticalStepPlan(level, pos, state, sourceRenderState(fixedVerticalStep.getSourceVerticalSlab()));
        }

        Object materials = materialSource(level, pos, state);
        if (state.getBlock() instanceof MixedSlabBlock && materials != null) {
            return buildMixedPlan(level, pos, slot(materials, 0), slot(materials, 1));
        }
        if (state.getBlock() instanceof VerticalSlabBlock && materials != null) {
            return buildVerticalSlabPlan(
                    level,
                    pos,
                    slot(materials, 0),
                    state.getValue(VerticalSlabBlock.DOUBLE) ? slot(materials, 1) : null,
                    state.getValue(VerticalSlabBlock.FACING),
                    state.getValue(VerticalSlabBlock.DOUBLE)
            );
        }
        if (state.getBlock() instanceof StepBlock && materials != null && slot(materials, 0) != null) {
            return buildStepPlan(level, pos, state, slot(materials, 0));
        }
        if (state.getBlock() instanceof VerticalStepBlock && materials != null && slot(materials, 0) != null) {
            return buildVerticalStepPlan(level, pos, state, slot(materials, 0));
        }
        if (state.getBlock() instanceof QuadStepBlock && materials != null) {
            return buildQuadStepPlan(level, pos, state.getValue(QuadStepBlock.AXIS), materials);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock && materials != null) {
            return buildQuadVerticalStepPlan(level, pos, materials);
        }
        return Collections.emptyList();
    }

    private static List<DirectRenderGroup> buildDirectRenderPlan(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return Collections.emptyList();
        }

        Object materials = materialSource(level, pos, state);
        if (state.getBlock() instanceof MixedSlabBlock && materials != null) {
            return buildDirectMixedPlan(level, pos, slot(materials, 0), slot(materials, 1));
        }
        if (state.getBlock() instanceof VerticalSlabBlock && materials != null) {
            return buildDirectVerticalSlabPlan(
                    level,
                    pos,
                    slot(materials, 0),
                    state.getValue(VerticalSlabBlock.DOUBLE) ? slot(materials, 1) : null,
                    state.getValue(VerticalSlabBlock.FACING),
                    state.getValue(VerticalSlabBlock.DOUBLE)
            );
        }
        if (state.getBlock() instanceof StepBlock && materials != null && slot(materials, 0) != null) {
            return buildDirectStepPlan(level, pos, state, slot(materials, 0));
        }
        if (state.getBlock() instanceof VerticalStepBlock && materials != null && slot(materials, 0) != null) {
            return buildDirectVerticalStepPlan(level, pos, state, slot(materials, 0));
        }
        if (state.getBlock() instanceof QuadStepBlock && materials != null) {
            return buildDirectQuadStepPlan(level, pos, state.getValue(QuadStepBlock.AXIS), materials);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock && materials != null) {
            return buildDirectQuadVerticalStepPlan(level, pos, materials);
        }
        return Collections.emptyList();
    }

    private static List<DirectRenderGroup> buildDirectMixedPlan(BlockAndTintGetter level, BlockPos pos, BlockState bottomSlabState, BlockState topSlabState) {
        bottomSlabState = horizontalSlabRenderState(bottomSlabState, SlabType.BOTTOM);
        topSlabState = horizontalSlabRenderState(topSlabState, SlabType.TOP);
        DirectGroupAccumulator groups = new DirectGroupAccumulator(2);
        if (bottomSlabState != null) {
            int visibleMask = mixedBottomVisibleMask(level, pos, bottomSlabState, topSlabState);
            visibleMask = cullExternalFaces(level, pos, bottomSlabState, BOTTOM_HALF_CELL_MASK, visibleMask);
            addDirectCell(groups, bottomSlabState, pos, MIXED_BOTTOM_FROM, MIXED_BOTTOM_TO, visibleMask);
        }
        if (topSlabState != null) {
            int visibleMask = bottomSlabState != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, Direction.DOWN) : ALL_DIRECTIONS;
            visibleMask = cullExternalFaces(level, pos, topSlabState, TOP_HALF_CELL_MASK, visibleMask);
            addDirectCell(groups, topSlabState, pos, MIXED_TOP_FROM, MIXED_TOP_TO, visibleMask);
        }
        return groups.toDirectRenderGroups();
    }

    private static List<DirectRenderGroup> buildDirectVerticalSlabPlan(BlockAndTintGetter level, BlockPos pos, BlockState facingSlab, BlockState oppositeSlab, Direction facing, boolean doubled) {
        if (doubled) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab);
            if (fullBlockState != null) {
                DirectGroupAccumulator fullGroups = new DirectGroupAccumulator(1);
                addDirectCell(fullGroups, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
                return fullGroups.toDirectRenderGroups();
            }
        }

        DirectGroupAccumulator groups = new DirectGroupAccumulator(2);
        if (facingSlab != null) {
            int mask = doubled && oppositeSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
            mask = cullExternalFaces(level, pos, facingSlab, verticalSlabCellMask(facing), mask);
            addDirectCell(groups, facingSlab, pos, verticalSlabFrom(facing), verticalSlabTo(facing), mask);
        }
        if (doubled && oppositeSlab != null) {
            Direction oppositeFacing = facing.getOpposite();
            int mask = facingSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
            mask = cullExternalFaces(level, pos, oppositeSlab, verticalSlabCellMask(oppositeFacing), mask);
            addDirectCell(groups, oppositeSlab, pos, verticalSlabFrom(oppositeFacing), verticalSlabTo(oppositeFacing), mask);
        }
        return groups.toDirectRenderGroups();
    }

    private static List<DirectRenderGroup> buildDirectStepPlan(BlockAndTintGetter level, BlockPos pos, BlockState ownerState, BlockState slabState) {
        Direction facing = ownerState.getValue(StepBlock.FACING);
        boolean top = ownerState.getValue(StepBlock.TOP);
        BlockState renderSlab = stepRenderState(slabState, top);
        int visibleMask = cullExternalFaces(level, pos, renderSlab, stepCellMask(facing, top), ALL_DIRECTIONS);
        DirectGroupAccumulator groups = new DirectGroupAccumulator(1);
        addDirectCell(groups, renderSlab, pos, stepFrom(facing, top), stepTo(facing, top), visibleMask);
        return groups.toDirectRenderGroups();
    }

    private static List<DirectRenderGroup> buildDirectVerticalStepPlan(BlockAndTintGetter level, BlockPos pos, BlockState ownerState, BlockState slabState) {
        Direction facing = ownerState.getValue(VerticalStepBlock.FACING);
        Direction side = ownerState.getValue(VerticalStepBlock.SIDE);
        int visibleMask = cullExternalFaces(level, pos, slabState, verticalStepCellMask(facing, side), ALL_DIRECTIONS);
        DirectGroupAccumulator groups = new DirectGroupAccumulator(1);
        addDirectCell(groups, slabState, pos, verticalStepFrom(facing, side), verticalStepTo(facing, side), visibleMask);
        return groups.toDirectRenderGroups();
    }

    private static List<DirectRenderGroup> buildDirectQuadStepPlan(BlockAndTintGetter level, BlockPos pos, Direction.Axis axis, Object slabs) {
        BlockState fullBlockState = fullBlockRenderState(slabs);
        if (fullBlockState != null) {
            DirectGroupAccumulator fullGroups = new DirectGroupAccumulator(1);
            addDirectCell(fullGroups, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
            return fullGroups.toDirectRenderGroups();
        }

        DirectGroupAccumulator groups = new DirectGroupAccumulator(8);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) continue;
            boolean positive = i == 1 || i == 3;
            Direction facing = axis == Direction.Axis.Z ? (positive ? Direction.SOUTH : Direction.NORTH) : (positive ? Direction.EAST : Direction.WEST);
            boolean top = i >= 2;
            BlockState renderSlab = stepRenderState(slab, top);
            int mask = quadStepVisibleMask(axis, i, slabs);
            boolean joinedVertical = hasJoinedStepVerticalNeighbor(top, mask);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, renderSlab, stepCellMask(facing, top), mask);
            }
            addDirectStepCell(groups, level, pos, renderSlab, top, stepFrom(facing, top), stepTo(facing, top), mask, joinedVertical);
        }
        return groups.toDirectRenderGroups();
    }

    private static List<DirectRenderGroup> buildDirectQuadVerticalStepPlan(BlockAndTintGetter level, BlockPos pos, Object slabs) {
        BlockState fullBlockState = fullBlockRenderState(slabs);
        if (fullBlockState != null) {
            DirectGroupAccumulator fullGroups = new DirectGroupAccumulator(1);
            addDirectCell(fullGroups, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
            return fullGroups.toDirectRenderGroups();
        }

        DirectGroupAccumulator groups = new DirectGroupAccumulator(4);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) continue;
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            int mask = quadVerticalStepVisibleMask(i, slabs);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, slab, verticalStepCellMask(facing, side), mask);
            }
            addDirectCell(groups, slab, pos, verticalStepFrom(facing, side), verticalStepTo(facing, side), mask);
        }
        return groups.toDirectRenderGroups();
    }

    private static void addDirectCell(DirectGroupAccumulator groups, BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (visibleMask == 0) {
            return;
        }

        List<BakedQuad> quads = getOrBuildDirectCellQuads(slabState, pos, from, to, visibleMask);
        if (!quads.isEmpty()) {
            groups.addAll(slabState, quads);
        }
    }

    private static void addDirectStepCell(DirectGroupAccumulator groups,
                                          BlockAndTintGetter level,
                                          BlockPos pos,
                                          BlockState slabState,
                                          boolean top,
                                          Vector3f from,
                                          Vector3f to,
                                          int visibleMask,
                                          boolean joinedVertical) {
        if (!joinedVertical || (visibleMask & HORIZONTAL_DIRECTIONS) == 0) {
            addDirectCell(groups, slabState, pos, from, to, visibleMask);
            return;
        }

        int bodyMask = visibleMask & ~HORIZONTAL_DIRECTIONS;
        addDirectCell(groups, slabState, pos, from, to, bodyMask);

        BlockState sideState = fullBlockStateForSlab(slabState);
        if (sideState == null) {
            sideState = slabState;
        }
        Vector3f sideFrom = joinedStepSideFrom(level, pos, slabState, top, from);
        Vector3f sideTo = joinedStepSideTo(level, pos, slabState, top, to);
        addDirectExactCell(groups, level, sideState, pos, sideFrom, sideTo, visibleMask & HORIZONTAL_DIRECTIONS);
    }

    private static void addDirectExactCell(DirectGroupAccumulator groups, BlockAndTintGetter level, BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (visibleMask == 0) {
            return;
        }

        List<BakedQuad> quads = StepBlockEntityRenderer.buildExactPrismQuads(level, pos, slabState, from, to, visibleMask);
        if (!quads.isEmpty()) {
            groups.addAll(slabState, quads);
        }
    }

    private static List<BakedQuad> getOrBuildDirectCellQuads(BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (slabState == null || from == null || to == null || visibleMask == 0) {
            return Collections.emptyList();
        }

        CellPartKey key = new CellPartKey(
                slabState,
                Float.floatToIntBits(from.x()),
                Float.floatToIntBits(from.y()),
                Float.floatToIntBits(from.z()),
                Float.floatToIntBits(to.x()),
                Float.floatToIntBits(to.y()),
                Float.floatToIntBits(to.z()),
                visibleMask
        );
        synchronized (DIRECT_CELL_QUAD_CACHE_LOCK) {
            List<BakedQuad> cached = DIRECT_CELL_QUAD_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<BakedQuad> built = StepBlockEntityRenderer.buildSimpleCachedPrismQuads(null, pos, slabState, from, to, visibleMask);
        synchronized (DIRECT_CELL_QUAD_CACHE_LOCK) {
            DIRECT_CELL_QUAD_CACHE.put(key, built);
        }
        return built;
    }

    private static List<RenderGroup> buildMixedPlan(BlockAndTintGetter level, BlockPos pos, BlockState bottomSlabState, BlockState topSlabState) {
        bottomSlabState = horizontalSlabRenderState(bottomSlabState, SlabType.BOTTOM);
        topSlabState = horizontalSlabRenderState(topSlabState, SlabType.TOP);
        GroupAccumulator groups = new GroupAccumulator(2);
        if (bottomSlabState != null) {
            int visibleMask = mixedBottomVisibleMask(level, pos, bottomSlabState, topSlabState);
            visibleMask = cullExternalFaces(level, pos, bottomSlabState, BOTTOM_HALF_CELL_MASK, visibleMask);
            addCell(groups, bottomSlabState, pos, MIXED_BOTTOM_FROM, MIXED_BOTTOM_TO, visibleMask);
        }
        if (topSlabState != null) {
            int visibleMask = bottomSlabState != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, Direction.DOWN) : ALL_DIRECTIONS;
            visibleMask = cullExternalFaces(level, pos, topSlabState, TOP_HALF_CELL_MASK, visibleMask);
            addCell(groups, topSlabState, pos, MIXED_TOP_FROM, MIXED_TOP_TO, visibleMask);
        }
        return groups.toRenderGroups();
    }

    static int mixedBottomVisibleMask(BlockAndTintGetter level, BlockPos pos, BlockState bottomSlabState, BlockState topSlabState) {
        if (topSlabState == null || bottomSlabState == null) {
            return ALL_DIRECTIONS;
        }
        return StepBlockEntityRenderer.sourceModelHeight(level, pos, bottomSlabState) >= 7.999F
                ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, Direction.UP)
                : ALL_DIRECTIONS;
    }

    private static void addCell(GroupAccumulator groups, BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (visibleMask == 0) {
            return;
        }

        List<BlockModelPart> parts = getOrBuildCellParts(slabState, pos, from, to, visibleMask);
        if (!parts.isEmpty()) {
            groups.addAll(slabState, parts);
        }
    }

    private static List<BlockModelPart> getOrBuildCellParts(BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (slabState == null || from == null || to == null || visibleMask == 0) {
            return Collections.emptyList();
        }

        CellPartKey key = new CellPartKey(
                slabState,
                Float.floatToIntBits(from.x()),
                Float.floatToIntBits(from.y()),
                Float.floatToIntBits(from.z()),
                Float.floatToIntBits(to.x()),
                Float.floatToIntBits(to.y()),
                Float.floatToIntBits(to.z()),
                visibleMask
        );
        synchronized (CELL_PART_CACHE_LOCK) {
            List<BlockModelPart> cached = CELL_PART_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<BlockModelPart> built = buildCellParts(slabState, pos, from, to, visibleMask);
        synchronized (CELL_PART_CACHE_LOCK) {
            CELL_PART_CACHE.put(key, built);
        }
        return built;
    }

    private static List<BlockModelPart> buildCellParts(BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        BlockModelPart part = StepBlockEntityRenderer.buildSimpleCachedPrismPart(null, pos, slabState, from, to);
        if (part != null) {
            return EnhancedSlabRenderModelCompat.singletonPartList(EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask));
        }

        List<BlockModelPart> rawParts = EnhancedSlabRenderModelCompat.collectModelParts(slabState, pos);
        if (!rawParts.isEmpty()) {
            return EnhancedSlabRenderModelCompat.filterPartList(rawParts, visibleMask);
        }
        return Collections.emptyList();
    }

    private static List<RenderGroup> buildVerticalSlabPlan(BlockAndTintGetter level, BlockPos pos, BlockState facingSlab, BlockState oppositeSlab, Direction facing, boolean doubled) {
        if (doubled) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab);
            if (fullBlockState != null) {
                GroupAccumulator fullGroups = new GroupAccumulator(1);
                addExactCell(fullGroups, level, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
                return fullGroups.toRenderGroups();
            }
        }

        GroupAccumulator groups = new GroupAccumulator(2);
        if (facingSlab != null) {
            int mask = doubled && oppositeSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
            mask = cullExternalFaces(level, pos, facingSlab, verticalSlabCellMask(facing), mask);
            addCell(groups, facingSlab, pos, verticalSlabFrom(facing), verticalSlabTo(facing), mask);
        }
        if (doubled && oppositeSlab != null) {
            Direction oppositeFacing = facing.getOpposite();
            int mask = facingSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
            mask = cullExternalFaces(level, pos, oppositeSlab, verticalSlabCellMask(oppositeFacing), mask);
            addCell(groups, oppositeSlab, pos, verticalSlabFrom(oppositeFacing), verticalSlabTo(oppositeFacing), mask);
        }
        return groups.toRenderGroups();
    }

    private static List<RenderGroup> buildStepPlan(BlockAndTintGetter level, BlockPos pos, BlockState ownerState, BlockState slabState) {
        Direction facing = ownerState.getValue(StepBlock.FACING);
        boolean top = ownerState.getValue(StepBlock.TOP);
        BlockState renderSlab = stepRenderState(slabState, top);
        int visibleMask = cullExternalFaces(level, pos, renderSlab, stepCellMask(facing, top), ALL_DIRECTIONS);
        GroupAccumulator groups = new GroupAccumulator(1);
        addCell(groups, renderSlab, pos, stepFrom(facing, top), stepTo(facing, top), visibleMask);
        return groups.toRenderGroups();
    }

    private static List<RenderGroup> buildVerticalStepPlan(BlockAndTintGetter level, BlockPos pos, BlockState ownerState, BlockState slabState) {
        Direction facing = ownerState.getValue(VerticalStepBlock.FACING);
        Direction side = ownerState.getValue(VerticalStepBlock.SIDE);
        int visibleMask = cullExternalFaces(level, pos, slabState, verticalStepCellMask(facing, side), ALL_DIRECTIONS);
        GroupAccumulator groups = new GroupAccumulator(1);
        addCell(groups, slabState, pos, verticalStepFrom(facing, side), verticalStepTo(facing, side), visibleMask);
        return groups.toRenderGroups();
    }

    private static List<RenderGroup> buildQuadStepPlan(BlockAndTintGetter level, BlockPos pos, Direction.Axis axis, Object slabs) {
        BlockState fullBlockState = fullBlockRenderState(slabs);
        if (fullBlockState != null) {
            GroupAccumulator fullGroups = new GroupAccumulator(1);
            addExactCell(fullGroups, level, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
            return fullGroups.toRenderGroups();
        }

        GroupAccumulator groups = new GroupAccumulator(8);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) continue;
            boolean positive = i == 1 || i == 3;
            Direction facing = axis == Direction.Axis.Z ? (positive ? Direction.SOUTH : Direction.NORTH) : (positive ? Direction.EAST : Direction.WEST);
            boolean top = i >= 2;
            BlockState renderSlab = stepRenderState(slab, top);
            int mask = quadStepVisibleMask(axis, i, slabs);
            boolean joinedVertical = hasJoinedStepVerticalNeighbor(top, mask);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, renderSlab, stepCellMask(facing, top), mask);
            }
            addStepCell(groups, level, pos, renderSlab, top, stepFrom(facing, top), stepTo(facing, top), mask, joinedVertical);
        }
        return groups.toRenderGroups();
    }

    private static void addStepCell(GroupAccumulator groups,
                                    BlockAndTintGetter level,
                                    BlockPos pos,
                                    BlockState slabState,
                                    boolean top,
                                    Vector3f from,
                                    Vector3f to,
                                    int visibleMask,
                                    boolean joinedVertical) {
        if (!joinedVertical || (visibleMask & HORIZONTAL_DIRECTIONS) == 0) {
            addCell(groups, slabState, pos, from, to, visibleMask);
            return;
        }

        int bodyMask = visibleMask & ~HORIZONTAL_DIRECTIONS;
        addCell(groups, slabState, pos, from, to, bodyMask);

        BlockState sideState = fullBlockStateForSlab(slabState);
        if (sideState == null) {
            sideState = slabState;
        }
        Vector3f sideFrom = joinedStepSideFrom(level, pos, slabState, top, from);
        Vector3f sideTo = joinedStepSideTo(level, pos, slabState, top, to);
        addExactCell(groups, level, sideState, pos, sideFrom, sideTo, visibleMask & HORIZONTAL_DIRECTIONS);
    }

    private static void addExactCell(GroupAccumulator groups, BlockAndTintGetter level, BlockState slabState, BlockPos pos, Vector3f from, Vector3f to, int visibleMask) {
        if (visibleMask == 0) {
            return;
        }

        BlockModelPart part = StepBlockEntityRenderer.buildExactPrismPart(level, pos, slabState, from, to, visibleMask);
        if (part != null) {
            groups.add(slabState, part);
        }
    }

    private static BlockState stepRenderState(BlockState slabState, boolean top) {
        return horizontalSlabRenderState(slabState, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState horizontalSlabRenderState(BlockState slabState, SlabType half) {
        if (slabState != null && slabState.hasProperty(SlabBlock.TYPE)) {
            slabState = slabState.setValue(SlabBlock.TYPE, half);
        }
        if (slabState != null && slabState.hasProperty(SlabBlock.WATERLOGGED)) {
            slabState = slabState.setValue(SlabBlock.WATERLOGGED, false);
        }
        return slabState;
    }

    private static boolean hasJoinedStepVerticalNeighbor(boolean top, int visibleMask) {
        Direction hidden = top ? Direction.DOWN : Direction.UP;
        return (visibleMask & (1 << hidden.ordinal())) == 0;
    }

    private static Vector3f joinedStepSideFrom(BlockAndTintGetter level, BlockPos pos, BlockState slabState, boolean top, Vector3f from) {
        return new Vector3f(from.x(), top ? 8.0F : 0.0F, from.z());
    }

    private static Vector3f joinedStepSideTo(BlockAndTintGetter level, BlockPos pos, BlockState slabState, boolean top, Vector3f to) {
        float sourceHeight = StepBlockEntityRenderer.sourceModelHeight(level, pos, slabState);
        float sideMaxY = top ? Math.min(16.0F, 8.0F + sourceHeight) : 8.0F;
        return new Vector3f(to.x(), sideMaxY, to.z());
    }

    private static BlockState fullBlockStateForSlab(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(slabState);
    }

    private static List<RenderGroup> buildQuadVerticalStepPlan(BlockAndTintGetter level, BlockPos pos, Object slabs) {
        BlockState fullBlockState = fullBlockRenderState(slabs);
        if (fullBlockState != null) {
            GroupAccumulator fullGroups = new GroupAccumulator(1);
            addExactCell(fullGroups, level, fullBlockState, pos, new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), ALL_DIRECTIONS);
            return fullGroups.toRenderGroups();
        }

        GroupAccumulator groups = new GroupAccumulator(4);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) continue;
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            int mask = quadVerticalStepVisibleMask(i, slabs);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, slab, verticalStepCellMask(facing, side), mask);
            }
            addCell(groups, slab, pos, verticalStepFrom(facing, side), verticalStepTo(facing, side), mask);
        }
        return groups.toRenderGroups();
    }

    private static Vector3f verticalSlabFrom(Direction half) {
        return switch (half) {
            case SOUTH -> new Vector3f(0.0F, 0.0F, 8.0F);
            case EAST -> new Vector3f(8.0F, 0.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 0.0F);
        };
    }

    private static Vector3f verticalSlabTo(Direction half) {
        return switch (half) {
            case SOUTH, EAST -> new Vector3f(16.0F, 16.0F, 16.0F);
            case NORTH -> new Vector3f(16.0F, 16.0F, 8.0F);
            default -> new Vector3f(8.0F, 16.0F, 16.0F);
        };
    }

    private static Vector3f stepFrom(Direction facing, boolean top) {
        float yMin = top ? 8.0F : 0.0F;
        return switch (facing) {
            case SOUTH -> new Vector3f(0.0F, yMin, 8.0F);
            case EAST -> new Vector3f(8.0F, yMin, 0.0F);
            default -> new Vector3f(0.0F, yMin, 0.0F);
        };
    }

    private static Vector3f stepTo(Direction facing, boolean top) {
        float yMax = top ? 16.0F : 8.0F;
        return switch (facing) {
            case SOUTH -> new Vector3f(16.0F, yMax, 16.0F);
            case EAST -> new Vector3f(16.0F, yMax, 16.0F);
            case NORTH -> new Vector3f(16.0F, yMax, 8.0F);
            default -> new Vector3f(8.0F, yMax, 16.0F);
        };
    }

    private static Vector3f verticalStepFrom(Direction facing, Direction side) {
        float xMin = side == Direction.EAST ? 8.0F : 0.0F;
        float zMin = facing == Direction.SOUTH ? 8.0F : 0.0F;
        return new Vector3f(xMin, 0.0F, zMin);
    }

    private static Vector3f verticalStepTo(Direction facing, Direction side) {
        float xMax = side == Direction.EAST ? 16.0F : 8.0F;
        float zMax = facing == Direction.SOUTH ? 16.0F : 8.0F;
        return new Vector3f(xMax, 16.0F, zMax);
    }

    private static boolean renderVerticalSlabTerrain(BlockRenderDispatcher dispatcher,
                                                     BlockAndTintGetter level,
                                                     BlockPos pos,
                                                     PoseStack poseStack,
                                                     VertexConsumer consumer,
                                                     BlockState facingSlab,
                                                     BlockState oppositeSlab,
                                                     Direction facing,
                                                     boolean doubled) {
        GroupAccumulator groups = new GroupAccumulator(2);
        if (facingSlab != null) {
            net.minecraft.client.renderer.block.model.BlockModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(facingSlab, pos, facing);
            if (part != null) {
                int mask = doubled && oppositeSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
                mask = cullExternalFaces(level, pos, facingSlab, verticalSlabCellMask(facing), mask);
                groups.add(
                        facingSlab,
                        EnhancedSlabRenderModelCompat.filterDirections(part, mask)
                );
            }
        }
        if (doubled && oppositeSlab != null) {
            Direction oppositeFacing = facing.getOpposite();
            net.minecraft.client.renderer.block.model.BlockModelPart part = VerticalSlabBlockEntityRenderer.buildTerrainPart(oppositeSlab, pos, oppositeFacing);
            if (part != null) {
                int mask = facingSlab != null ? EnhancedSlabRenderModelCompat.without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
                mask = cullExternalFaces(level, pos, oppositeSlab, verticalSlabCellMask(oppositeFacing), mask);
                groups.add(
                        oppositeSlab,
                        EnhancedSlabRenderModelCompat.filterDirections(part, mask)
                );
            }
        }
        return renderGroups(dispatcher, level, pos, poseStack, consumer, groups);
    }

    private static boolean renderPlan(BlockRenderDispatcher dispatcher,
                                      BlockAndTintGetter level,
                                      BlockPos pos,
                                      PoseStack poseStack,
                                      VertexConsumer consumer,
                                      List<RenderGroup> groups) {
        boolean rendered = false;
        for (RenderGroup group : groups) {
            if (group == null || group.renderState == null || group.parts == null || group.parts.isEmpty()) {
                continue;
            }
            renderGroup(dispatcher, level, pos, poseStack, consumer, group.renderState, group.parts);
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderStepTerrain(BlockRenderDispatcher dispatcher,
                                             BlockAndTintGetter level,
                                             BlockPos pos,
                                             BlockState ownerState,
                                             BlockState slabState,
                                             PoseStack poseStack,
                                             VertexConsumer consumer) {
        net.minecraft.client.renderer.block.model.BlockModelPart part = StepBlockEntityRenderer.buildQuadrantPart(
                slabState,
                pos,
                ownerState.getValue(StepBlock.FACING),
                ownerState.getValue(StepBlock.TOP)
        );
        int visibleMask = cullExternalFaces(
                level,
                pos,
                slabState,
                stepCellMask(ownerState.getValue(StepBlock.FACING), ownerState.getValue(StepBlock.TOP)),
                ALL_DIRECTIONS
        );
        return part != null && renderGroup(
                dispatcher,
                level,
                pos,
                poseStack,
                consumer,
                slabState,
                EnhancedSlabRenderModelCompat.singletonPartList(EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask))
        );
    }

    private static boolean renderVerticalStepTerrain(BlockRenderDispatcher dispatcher,
                                                     BlockAndTintGetter level,
                                                     BlockPos pos,
                                                     BlockState ownerState,
                                                     BlockState slabState,
                                                     PoseStack poseStack,
                                                     VertexConsumer consumer) {
        net.minecraft.client.renderer.block.model.BlockModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(
                slabState,
                pos,
                ownerState.getValue(VerticalStepBlock.FACING),
                ownerState.getValue(VerticalStepBlock.SIDE)
        );
        int visibleMask = cullExternalFaces(
                level,
                pos,
                slabState,
                verticalStepCellMask(ownerState.getValue(VerticalStepBlock.FACING), ownerState.getValue(VerticalStepBlock.SIDE)),
                ALL_DIRECTIONS
        );
        return part != null && renderGroup(
                dispatcher,
                level,
                pos,
                poseStack,
                consumer,
                slabState,
                EnhancedSlabRenderModelCompat.singletonPartList(EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask))
        );
    }

    private static boolean renderQuadStepTerrain(BlockRenderDispatcher dispatcher,
                                                 BlockAndTintGetter level,
                                                 BlockPos pos,
                                                 PoseStack poseStack,
                                                 VertexConsumer consumer,
                                                 Direction.Axis axis,
                                                 Object slabs) {
        GroupAccumulator groups = new GroupAccumulator(8);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) {
                continue;
            }
            boolean positive = i == 1 || i == 3;
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            boolean top = i >= 2;
            int mask = quadStepVisibleMask(axis, i, slabs);
            boolean joinedVertical = hasJoinedStepVerticalNeighbor(top, mask);
            BlockState renderSlab = stepRenderState(slab, top);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, renderSlab, stepCellMask(facing, top), mask);
            }
            addStepCell(groups, level, pos, renderSlab, top, stepFrom(facing, top), stepTo(facing, top), mask, joinedVertical);
        }
        return renderGroups(dispatcher, level, pos, poseStack, consumer, groups);
    }

    private static boolean renderQuadVerticalStepTerrain(BlockRenderDispatcher dispatcher,
                                                         BlockAndTintGetter level,
                                                         BlockPos pos,
                                                         PoseStack poseStack,
                                                         VertexConsumer consumer,
                                                         Object slabs) {
        GroupAccumulator groups = new GroupAccumulator(4);
        boolean cullExternal = canCullExternalQuad(slabs);
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) {
                continue;
            }
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            net.minecraft.client.renderer.block.model.BlockModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slab, pos, facing, side);
            if (part == null) {
                continue;
            }
            int mask = quadVerticalStepVisibleMask(i, slabs);
            if (cullExternal) {
                mask = cullExternalFaces(level, pos, slab, verticalStepCellMask(facing, side), mask);
            }
            groups.add(
                    slab,
                    EnhancedSlabRenderModelCompat.filterDirections(part, mask)
            );
        }
        return renderGroups(dispatcher, level, pos, poseStack, consumer, groups);
    }

    private static boolean renderGroups(BlockRenderDispatcher dispatcher,
                                        BlockAndTintGetter level,
                                        BlockPos pos,
                                        PoseStack poseStack,
                                        VertexConsumer consumer,
                                        GroupAccumulator groups) {
        boolean rendered = false;
        for (int i = 0; i < groups.size; i++) {
            BlockState renderState = groups.states[i];
            ArrayList<BlockModelPart> parts = groups.parts[i];
            if (renderState == null || parts == null || parts.isEmpty()) {
                continue;
            }
            renderGroup(dispatcher, level, pos, poseStack, consumer, renderState, EnhancedSlabRenderModelCompat.partList(parts));
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderGroup(BlockRenderDispatcher dispatcher,
                                       BlockAndTintGetter level,
                                       BlockPos pos,
                                       PoseStack poseStack,
                                       VertexConsumer consumer,
                                       BlockState renderState,
                                       List<BlockModelPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return false;
        }
        BlockColorRenderContext.setForcedPosLong(pos.asLong());
        try {
            renderNested(dispatcher, renderState, pos, level, poseStack, consumer, parts);
        } finally {
            BlockColorRenderContext.clear();
        }
        return true;
    }

    private static final class GroupAccumulator {
        private final BlockState[] states;
        private final ArrayList<BlockModelPart>[] parts;
        private int size;

        @SuppressWarnings("unchecked")
        private GroupAccumulator(int capacity) {
            this.states = new BlockState[capacity];
            this.parts = (ArrayList<BlockModelPart>[]) new ArrayList[capacity];
        }

        private void add(BlockState state, BlockModelPart part) {
            if (state == null || part == null) {
                return;
            }
            for (int i = 0; i < size; i++) {
                if (states[i] == state || states[i].equals(state)) {
                    parts[i].add(part);
                    return;
                }
            }
            if (size >= states.length) {
                return;
            }
            states[size] = state;
            parts[size] = new ArrayList<>(states.length);
            parts[size].add(part);
            size++;
        }

        private void addAll(BlockState state, List<BlockModelPart> newParts) {
            if (state == null || newParts == null || newParts.isEmpty()) {
                return;
            }
            for (BlockModelPart part : newParts) {
                add(state, part);
            }
        }

        private List<RenderGroup> toRenderGroups() {
            if (size == 0) {
                return Collections.emptyList();
            }
            ArrayList<RenderGroup> groups = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (states[i] != null && parts[i] != null && !parts[i].isEmpty()) {
                    groups.add(new RenderGroup(states[i], EnhancedSlabRenderModelCompat.partList(parts[i])));
                }
            }
            return groups.isEmpty() ? Collections.emptyList() : List.copyOf(groups);
        }
    }

    private static final class DirectGroupAccumulator {
        private final BlockState[] states;
        private final ArrayList<BakedQuad>[] quads;
        private int size;

        @SuppressWarnings("unchecked")
        private DirectGroupAccumulator(int capacity) {
            this.states = new BlockState[capacity];
            this.quads = (ArrayList<BakedQuad>[]) new ArrayList[capacity];
        }

        private void add(BlockState state, BakedQuad quad) {
            if (state == null || quad == null) {
                return;
            }
            for (int i = 0; i < size; i++) {
                if (states[i] == state || states[i].equals(state)) {
                    quads[i].add(quad);
                    return;
                }
            }
            if (size >= states.length) {
                return;
            }
            states[size] = state;
            quads[size] = new ArrayList<>(states.length * 6);
            quads[size].add(quad);
            size++;
        }

        private void addAll(BlockState state, List<BakedQuad> newQuads) {
            if (state == null || newQuads == null || newQuads.isEmpty()) {
                return;
            }
            for (BakedQuad quad : newQuads) {
                add(state, quad);
            }
        }

        private List<DirectRenderGroup> toDirectRenderGroups() {
            if (size == 0) {
                return Collections.emptyList();
            }
            ArrayList<DirectRenderGroup> groups = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (states[i] != null && quads[i] != null && !quads[i].isEmpty()) {
                    groups.add(new DirectRenderGroup(states[i], List.copyOf(quads[i])));
                }
            }
            return groups.isEmpty() ? Collections.emptyList() : List.copyOf(groups);
        }
    }

    public record RenderGroup(BlockState renderState, List<BlockModelPart> parts) {
    }

    public record DirectRenderGroup(BlockState renderState, List<BakedQuad> quads) {
    }

    @FunctionalInterface
    public interface RenderPlanConsumer {
        void accept(BlockState renderState, List<BlockModelPart> parts);
    }

    @FunctionalInterface
    public interface DirectRenderPlanConsumer {
        void accept(BlockState renderState, List<BakedQuad> quads);
    }

    private record RenderPlanKey(int family,
                                 BlockState ownerState,
                                 BlockState s0,
                                 BlockState s1,
                                 BlockState s2,
                                 BlockState s3,
                                 NeighborOcclusionKey west,
                                 NeighborOcclusionKey east,
                                 NeighborOcclusionKey down,
                                 NeighborOcclusionKey up,
                                 NeighborOcclusionKey north,
                                 NeighborOcclusionKey south) {
    }

    private record NeighborOcclusionKey(int mask,
                                        BlockState state,
                                        BlockState s0,
                                        BlockState s1,
                                        BlockState s2,
                                        BlockState s3) {
        private static final NeighborOcclusionKey EMPTY = new NeighborOcclusionKey(0, null, null, null, null, null);
    }

    private record CellPartKey(BlockState state,
                               int fromX,
                               int fromY,
                               int fromZ,
                               int toX,
                               int toY,
                               int toZ,
                               int visibleMask) {
    }

    private record ChunkGeometryKey(Object base, RenderPlanKey plan) {
    }

    private record CreateModelContext(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    }

    static int horizontalHalfCellMask(boolean top) {
        return top ? TOP_HALF_CELL_MASK : BOTTOM_HALF_CELL_MASK;
    }

    static int cullExternalFaces(BlockAndTintGetter level, BlockPos pos, BlockState renderState, int cellMask, int visibleMask) {
        if (CREATE_CONTRAPTION_RENDER.get()
                || level == null
                || pos == null
                || renderState == null
                || cellMask == 0
                || !canCullExternalFaces(renderState)) {
            return visibleMask;
        }
        int mask = visibleMask;
        for (Direction direction : Direction.values()) {
            if ((mask & (1 << direction.ordinal())) == 0) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (EnhancedSlabFaceCullingHelper.shouldForceRenderFace(level, pos, renderState, direction, neighborPos)) {
                continue;
            }
            int neighborMask = occludingCellMask(level, neighborPos, neighborState);
            if (neighborMask != 0 && faceCoveredByNeighbor(cellMask, direction, neighborMask)) {
                if (!usesMaterialAwareOcclusion(neighborState)
                        || faceCoveredByMatchingNeighborMaterial(level, neighborPos, neighborState, cellMask, direction, renderState)) {
                    mask = EnhancedSlabRenderModelCompat.without(mask, direction);
                }
            }
        }
        return mask;
    }

    private static boolean canCullExternalFaces(BlockState renderState) {
        if (renderState == null) {
            return false;
        }
        return switch (ItemBlockRenderTypes.getChunkRenderType(renderState)) {
            case TRANSLUCENT, TRIPWIRE -> false;
            default -> true;
        };
    }

    private static int occludingCellMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) {
            return 0;
        }
        int stateOnlyMask = stateOnlyOccludingCellMask(state);
        if (stateOnlyMask >= 0) {
            return stateOnlyMask;
        }
        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            BlockState source = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            if (!canCullExternalFaces(source)) {
                return 0;
            }
            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            int mask = verticalSlabCellMask(facing);
            if (state.getValue(VerticalSlabBlock.DOUBLE)) {
                mask |= verticalSlabCellMask(facing.getOpposite());
            }
            return mask;
        }
        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return canCullExternalFaces(fixedStep.getSourceSlab().defaultBlockState())
                    ? stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP))
                    : 0;
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return canCullExternalFaces(sourceRenderState(fixedVerticalStep.getSourceVerticalSlab()))
                    ? verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE))
                    : 0;
        }
        if (state.getBlock() instanceof MixedSlabBlock) {
            Object slots = materialSource(level, pos, state);
            if (slots != null) {
                int mask = 0;
                if (canCullExternalFaces(slot(slots, 0))) {
                    mask |= BOTTOM_HALF_CELL_MASK;
                }
                if (canCullExternalFaces(slot(slots, 1))) {
                    mask |= TOP_HALF_CELL_MASK;
                }
                return mask;
            }
            return 0;
        }
        if (state.getBlock() instanceof VerticalSlabBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
                int mask = 0;
                Direction facing = state.getValue(VerticalSlabBlock.FACING);
                if (canCullExternalFaces(verticalSlab.getFacingSlab())) {
                    mask |= verticalSlabCellMask(facing);
                }
                if (state.getValue(VerticalSlabBlock.DOUBLE) && canCullExternalFaces(verticalSlab.getOppositeSlab())) {
                    mask |= verticalSlabCellMask(facing.getOpposite());
                }
                return mask;
            }
            return 0;
        }
        if (state.getBlock() instanceof StepBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StepBlockEntity step && canCullExternalFaces(step.getSlabState())) {
                return stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP));
            }
            return 0;
        }
        if (state.getBlock() instanceof VerticalStepBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VerticalStepBlockEntity verticalStep && canCullExternalFaces(verticalStep.getSlabState())) {
                return verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE));
            }
            return 0;
        }
        if (state.getBlock() instanceof QuadStepBlock) {
            Object slabs = materialSource(level, pos, state);
            if (slabs != null) {
                Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
                int mask = 0;
                for (int i = 0; i < 4; i++) {
                    BlockState slab = slot(slabs, i);
                    if (!canCullExternalFaces(slab)) {
                        continue;
                    }
                    boolean positive = i == 1 || i == 3;
                    Direction facing = axis == Direction.Axis.Z
                            ? (positive ? Direction.SOUTH : Direction.NORTH)
                            : (positive ? Direction.EAST : Direction.WEST);
                    mask |= stepCellMask(facing, i >= 2);
                }
                return mask;
            }
            return 0;
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            Object slabs = materialSource(level, pos, state);
            if (slabs != null) {
                int mask = 0;
                for (int i = 0; i < 4; i++) {
                    if (!canCullExternalFaces(slot(slabs, i))) {
                        continue;
                    }
                    Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
                    Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
                    mask |= verticalStepCellMask(facing, side);
                }
                return mask;
            }
            return 0;
        }
        return state.isSolidRender() ? FULL_CELL_MASK : 0;
    }

    private static int stateOnlyOccludingCellMask(BlockState state) {
        if (state == null || state.isAir()) {
            return 0;
        }
        if (state.getBlock() instanceof MixedSlabBlock) {
            return FULL_CELL_MASK;
        }
        if (state.getBlock() instanceof VerticalSlabBlock) {
            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            int mask = verticalSlabCellMask(facing);
            if (state.getValue(VerticalSlabBlock.DOUBLE)) {
                mask |= verticalSlabCellMask(facing.getOpposite());
            }
            return mask;
        }
        if (state.getBlock() instanceof StepBlock) {
            return stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP));
        }
        if (state.getBlock() instanceof VerticalStepBlock) {
            return verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE));
        }
        if (state.getBlock() instanceof FixedVerticalSlabBlock) {
            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            int mask = verticalSlabCellMask(facing);
            if (state.getValue(VerticalSlabBlock.DOUBLE)) {
                mask |= verticalSlabCellMask(facing.getOpposite());
            }
            return mask;
        }
        if (state.getBlock() instanceof FixedStepBlock) {
            return stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP));
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock) {
            return verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE));
        }
        return -1;
    }

    private static BlockState sourceRenderState(Block sourceVerticalSlab) {
        Block slab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return (slab != null ? slab : sourceVerticalSlab).defaultBlockState();
    }

    private static boolean faceCoveredByNeighbor(int cellMask, Direction direction, int neighborMask) {
        boolean hasFaceCell = false;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int bit = cellBit(x, y, z);
                    if ((cellMask & bit) == 0 || !isOnFace(x, y, z, direction)) {
                        continue;
                    }
                    hasFaceCell = true;
                    int neighborBit = switch (direction) {
                        case WEST -> cellBit(1, y, z);
                        case EAST -> cellBit(0, y, z);
                        case DOWN -> cellBit(x, 1, z);
                        case UP -> cellBit(x, 0, z);
                        case NORTH -> cellBit(x, y, 1);
                        case SOUTH -> cellBit(x, y, 0);
                    };
                    if ((neighborMask & neighborBit) == 0) {
                        return false;
                    }
                }
            }
        }
        return hasFaceCell;
    }

    private static boolean faceCoveredByMatchingNeighborMaterial(BlockAndTintGetter level,
                                                                 BlockPos neighborPos,
                                                                 BlockState neighborState,
                                                                 int cellMask,
                                                                 Direction direction,
                                                                 BlockState renderState) {
        boolean hasFaceCell = false;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int bit = cellBit(x, y, z);
                    if ((cellMask & bit) == 0 || !isOnFace(x, y, z, direction)) {
                        continue;
                    }
                    hasFaceCell = true;
                    BlockState neighborMaterial = materialAtNeighborFaceCell(level, neighborPos, neighborState, x, y, z, direction);
                    if (!sameRenderMaterial(renderState, neighborMaterial)) {
                        return false;
                    }
                }
            }
        }
        return hasFaceCell;
    }

    private static BlockState materialAtNeighborFaceCell(BlockAndTintGetter level,
                                                         BlockPos neighborPos,
                                                         BlockState neighborState,
                                                         int x,
                                                         int y,
                                                         int z,
                                                         Direction direction) {
        return switch (direction) {
            case WEST -> materialAtCell(level, neighborPos, neighborState, 1, y, z);
            case EAST -> materialAtCell(level, neighborPos, neighborState, 0, y, z);
            case DOWN -> materialAtCell(level, neighborPos, neighborState, x, 1, z);
            case UP -> materialAtCell(level, neighborPos, neighborState, x, 0, z);
            case NORTH -> materialAtCell(level, neighborPos, neighborState, x, y, 1);
            case SOUTH -> materialAtCell(level, neighborPos, neighborState, x, y, 0);
        };
    }

    private static boolean usesMaterialAwareOcclusion(BlockState state) {
        return state != null && (handlesEnabledState(state) || rendersWithoutBlockEntity(state));
    }

    private static BlockState materialAtCell(BlockAndTintGetter level, BlockPos pos, BlockState state, int x, int y, int z) {
        if (state == null || state.isAir()) {
            return null;
        }
        int bit = cellBit(x, y, z);

        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            BlockState source = fixedVerticalSlab.getSourceSlab().defaultBlockState();
            if ((verticalSlabCellMask(facing) & bit) != 0) {
                return source;
            }
            return state.getValue(VerticalSlabBlock.DOUBLE) && (verticalSlabCellMask(facing.getOpposite()) & bit) != 0 ? source : null;
        }
        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return (stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP)) & bit) != 0
                    ? fixedStep.getSourceSlab().defaultBlockState()
                    : null;
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return (verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE)) & bit) != 0
                    ? sourceRenderState(fixedVerticalStep.getSourceVerticalSlab())
                    : null;
        }

        Object materials = materialSource(level, pos, state);
        if (state.getBlock() instanceof MixedSlabBlock && materials != null) {
            return slot(materials, y == 0 ? 0 : 1);
        }
        if (state.getBlock() instanceof VerticalSlabBlock && materials != null) {
            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            if ((verticalSlabCellMask(facing) & bit) != 0) {
                return slot(materials, 0);
            }
            return state.getValue(VerticalSlabBlock.DOUBLE) && (verticalSlabCellMask(facing.getOpposite()) & bit) != 0 ? slot(materials, 1) : null;
        }
        if (state.getBlock() instanceof StepBlock && materials != null) {
            return (stepCellMask(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP)) & bit) != 0 ? slot(materials, 0) : null;
        }
        if (state.getBlock() instanceof VerticalStepBlock && materials != null) {
            return (verticalStepCellMask(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE)) & bit) != 0 ? slot(materials, 0) : null;
        }
        if (state.getBlock() instanceof QuadStepBlock && materials != null) {
            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            for (int i = 0; i < 4; i++) {
                BlockState material = slot(materials, i);
                if (material == null) {
                    continue;
                }
                boolean positive = i == 1 || i == 3;
                Direction facing = axis == Direction.Axis.Z
                        ? (positive ? Direction.SOUTH : Direction.NORTH)
                        : (positive ? Direction.EAST : Direction.WEST);
                if ((stepCellMask(facing, i >= 2) & bit) != 0) {
                    return material;
                }
            }
            return null;
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock && materials != null) {
            for (int i = 0; i < 4; i++) {
                BlockState material = slot(materials, i);
                if (material == null) {
                    continue;
                }
                Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
                Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
                if ((verticalStepCellMask(facing, side) & bit) != 0) {
                    return material;
                }
            }
        }
        return null;
    }

    private static boolean isOnFace(int x, int y, int z, Direction direction) {
        return switch (direction) {
            case WEST -> x == 0;
            case EAST -> x == 1;
            case DOWN -> y == 0;
            case UP -> y == 1;
            case NORTH -> z == 0;
            case SOUTH -> z == 1;
        };
    }

    private static int verticalSlabCellMask(Direction half) {
        return switch (half) {
            case EAST -> cellMask(1, -1, -1);
            case WEST -> cellMask(0, -1, -1);
            case SOUTH -> cellMask(-1, -1, 1);
            case NORTH -> cellMask(-1, -1, 0);
            default -> FULL_CELL_MASK;
        };
    }

    private static int stepCellMask(Direction facing, boolean top) {
        return switch (facing) {
            case EAST -> cellMask(1, top ? 1 : 0, -1);
            case WEST -> cellMask(0, top ? 1 : 0, -1);
            case SOUTH -> cellMask(-1, top ? 1 : 0, 1);
            case NORTH -> cellMask(-1, top ? 1 : 0, 0);
            default -> cellMask(-1, top ? 1 : 0, -1);
        };
    }

    private static int verticalStepCellMask(Direction facing, Direction side) {
        int x = side == Direction.EAST ? 1 : 0;
        int z = facing == Direction.SOUTH ? 1 : 0;
        return cellMask(x, -1, z);
    }

    private static int cellMask(int fixedX, int fixedY, int fixedZ) {
        int mask = 0;
        for (int x = 0; x < 2; x++) {
            if (fixedX >= 0 && fixedX != x) {
                continue;
            }
            for (int y = 0; y < 2; y++) {
                if (fixedY >= 0 && fixedY != y) {
                    continue;
                }
                for (int z = 0; z < 2; z++) {
                    if (fixedZ >= 0 && fixedZ != z) {
                        continue;
                    }
                    mask |= cellBit(x, y, z);
                }
            }
        }
        return mask;
    }

    private static int cellBit(int x, int y, int z) {
        return 1 << (x | (y << 1) | (z << 2));
    }

    private static <K, V> Map<K, V> createBoundedCache(int limit) {
        return new LinkedHashMap<>(limit, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    private static int quadStepVisibleMask(Direction.Axis axis, int index, Object slabs) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = EnhancedSlabRenderModelCompat.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, verticalNeighbor))) {
            mask = EnhancedSlabRenderModelCompat.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int quadVerticalStepVisibleMask(int index, Object slabs) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, eastWestNeighbor))) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, northSouthNeighbor))) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static boolean canCullExternalQuad(Object slabs) {
        BlockState first = null;
        for (int i = 0; i < 4; i++) {
            BlockState slab = slot(slabs, i);
            if (slab == null) {
                continue;
            }
            if (first == null) {
                first = slab;
            } else if (!sameRenderMaterial(first, slab)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
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
}
