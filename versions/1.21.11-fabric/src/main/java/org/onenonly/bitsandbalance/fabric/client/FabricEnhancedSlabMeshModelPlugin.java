package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabRendererCompat;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.List;
import java.util.function.Predicate;

public final class FabricEnhancedSlabMeshModelPlugin {
    private FabricEnhancedSlabMeshModelPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE,
                        (model, context) -> {
                            BlockState state = context.state();
                            if (state != null
                                    && (EnhancedSlabTerrainRenderHelper.rendersWithoutBlockEntity(state)
                                    || EnhancedSlabTerrainRenderHelper.handlesEnabledState(state))) {
                                return new EnhancedSlabBlockStateModel(model, state);
                            }
                            return model;
                        }));
    }

    private static final class EnhancedSlabBlockStateModel implements BlockStateModel {
        private final BlockStateModel delegate;
        private final BlockState ownerState;

        private EnhancedSlabBlockStateModel(BlockStateModel delegate, BlockState ownerState) {
            this.delegate = delegate;
            this.ownerState = ownerState;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> parts) {
            if (EnhancedSlabTerrainRenderHelper.rendersWithoutBlockEntity(ownerState)) {
                collectFixedParts(parts);
                return;
            }
            if (EnhancedSlabTerrainRenderHelper.collectCreateModelParts(ownerState, parts)) {
                return;
            }
            delegate.collectParts(random, parts);
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        @Override
        public TextureAtlasSprite particleSprite(BlockAndTintGetter world, BlockPos pos, BlockState state) {
            return delegate.particleSprite(world, pos, state);
        }

        @Override
        public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
            Object base = delegate.createGeometryKey(world, pos, state, random);
            if (state == null) {
                return base;
            }
            if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
                return EnhancedSlabTerrainRenderHelper.createFixedChunkGeometryKey(
                        state,
                        new VerticalSlabKey(base, state.getValue(VerticalSlabBlock.FACING), state.getValue(VerticalSlabBlock.DOUBLE), fixedVerticalSlab.getSourceSlab().defaultBlockState(), state.getValue(VerticalSlabBlock.DOUBLE) ? fixedVerticalSlab.getSourceSlab().defaultBlockState() : null)
                );
            }
            if (state.getBlock() instanceof FixedStepBlock fixedStep) {
                return EnhancedSlabTerrainRenderHelper.createFixedChunkGeometryKey(
                        state,
                        new StepKey(base, state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP), fixedStep.getSourceSlab().defaultBlockState())
                );
            }
            if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
                BlockState source = sourceRenderState(fixedVerticalStep);
                return EnhancedSlabTerrainRenderHelper.createFixedChunkGeometryKey(
                        state,
                        new VerticalStepKey(base, state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE), source)
                );
            }
            if (!FabricTweaksConfig.enableEnhancedSlabs || world == null || pos == null) {
                return base;
            }
            if (EnhancedSlabTerrainRenderHelper.handlesEnabledState(state)) {
                return EnhancedSlabTerrainRenderHelper.createChunkGeometryKey(world, pos, state, base);
            }
            return base;
        }

        @Override
        public void emitQuads(QuadEmitter emitter,
                              BlockAndTintGetter world,
                              BlockPos pos,
                              BlockState state,
                              RandomSource random,
                              Predicate<Direction> faceFilter) {
            if (EnhancedSlabTerrainRenderHelper.rendersWithoutBlockEntity(state)) {
                if (!emitFixedQuads(emitter, state)) {
                    delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
                }
                return;
            }
            if (!FabricTweaksConfig.enableEnhancedSlabs || !EnhancedSlabTerrainRenderHelper.handlesEnabledState(state)) {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
                return;
            }

            boolean[] emittedQuads = new boolean[1];
            boolean emittedDirect = EnhancedSlabTerrainRenderHelper.emitFabricRendererDirectQuads(world, pos, state, (renderState, quads) -> {
                var renderLayer = ItemBlockRenderTypes.getChunkRenderType(renderState);
                for (BakedQuad quad : quads) {
                    emitQuad(emitter, quad, renderLayer);
                    emittedQuads[0] = true;
                }
            });
            if (emittedDirect && emittedQuads[0]) {
                return;
            }

            boolean emitted = EnhancedSlabTerrainRenderHelper.emitFabricRendererQuads(world, pos, state, (renderState, parts) -> {
                var renderLayer = ItemBlockRenderTypes.getChunkRenderType(renderState);
                for (BlockModelPart part : parts) {
                    emittedQuads[0] |= emitPart(emitter, part, renderLayer);
                }
            });
            if (!emitted || !emittedQuads[0]) {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
            }
        }

        private void collectFixedParts(List<BlockModelPart> parts) {
            if (parts == null) {
                return;
            }
            for (EnhancedSlabTerrainRenderHelper.RenderGroup group :
                    EnhancedSlabTerrainRenderHelper.getOrBuildFixedRenderPlan(ownerState)) {
                if (group != null && group.parts() != null && !group.parts().isEmpty()) {
                    parts.addAll(group.parts());
                }
            }
        }

        private boolean emitFixedQuads(QuadEmitter emitter, BlockState state) {
            boolean emitted = false;
            for (EnhancedSlabTerrainRenderHelper.RenderGroup group :
                    EnhancedSlabTerrainRenderHelper.getOrBuildFixedRenderPlan(state)) {
                if (group == null || group.parts() == null || group.parts().isEmpty()) {
                    continue;
                }
                var renderLayer = ItemBlockRenderTypes.getChunkRenderType(group.renderState());
                for (BlockModelPart part : group.parts()) {
                    emitted |= emitPart(emitter, part, renderLayer);
                }
            }
            return emitted;
        }

        private static boolean emitPart(QuadEmitter emitter, BlockModelPart part, net.minecraft.client.renderer.chunk.ChunkSectionLayer renderLayer) {
            if (part == null) {
                return false;
            }

            List<BakedQuad> unculled = part.getQuads(null);
            if (unculled != null && !unculled.isEmpty()) {
                for (BakedQuad quad : unculled) {
                    emitQuad(emitter, quad, renderLayer);
                }
                return true;
            }

            boolean emitted = false;
            for (Direction direction : Direction.values()) {
                List<BakedQuad> quads = part.getQuads(direction);
                if (quads == null || quads.isEmpty()) {
                    continue;
                }
                for (BakedQuad quad : quads) {
                    emitQuad(emitter, quad, renderLayer);
                    emitted = true;
                }
            }
            return emitted;
        }

        private static void emitQuad(QuadEmitter emitter, BakedQuad quad, net.minecraft.client.renderer.chunk.ChunkSectionLayer renderLayer) {
            if (quad == null) {
                return;
            }
            emitter.fromBakedQuad(quad);
            emitter.renderLayer(renderLayer);
            emitter.emit();
        }

        private static BlockState sourceRenderState(FixedVerticalStepBlock fixedVerticalStep) {
            var sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            var slab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            return (slab != null ? slab : sourceVerticalSlab).defaultBlockState();
        }
    }

    private record MixedKey(Object base, BlockState bottom, BlockState top) {
    }

    private record VerticalSlabKey(Object base, Direction facing, boolean doubled, BlockState facingSlab, BlockState oppositeSlab) {
    }

    private record StepKey(Object base, Direction facing, boolean top, BlockState slab) {
    }

    private record VerticalStepKey(Object base, Direction facing, Direction side, BlockState slab) {
    }

    private record QuadKey(Object base, Object variant, BlockState[] slabs) {
        private QuadKey {
            slabs = slabs == null ? new BlockState[0] : slabs.clone();
        }
    }
}
