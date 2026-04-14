package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.List;
import java.util.function.Predicate;

/**
 * Fabric model wrapper that applies Enhanced Slab visual Y offsets at the quad level.
 *
 * <p>This is primarily for Sodium/Indium, where vanilla's
 * {@code BlockRenderDispatcher.renderBatched} may not be used for terrain.
 */
public final class FabricEnhancedSlabModelOffsetPlugin {

    private FabricEnhancedSlabModelOffsetPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,
                        (model, context) -> {
                            BlockState state = context.state();
                            if (state != null && !EnhancedSlabHelper.shouldExcludeFromVisualOffset(state)) {
                                return new OffsetBlockStateModel(model);
                            }
                            return model;
                        }));
    }

    private static final class OffsetBlockStateModel implements BlockStateModel {
        private final BlockStateModel delegate;

        private OffsetBlockStateModel(BlockStateModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> parts) {
            delegate.collectParts(random, parts);
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        @Override
        public void emitQuads(QuadEmitter emitter,
                              BlockAndTintGetter world,
                              BlockPos pos,
                              BlockState state,
                              RandomSource random,
                              Predicate<Direction> faceFilter) {
            if (!FabricTweaksConfig.isAnyEnhancedSlabBehaviorEnabled()) {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
                return;
            }

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
                return;
            }

            final float yOffF = (float) yOff;
            emitter.pushTransform(new QuadTransform() {
                @Override
                public boolean transform(MutableQuadView quad) {
                    for (int i = 0; i < 4; i++) {
                        quad.pos(i, quad.x(i), quad.y(i) + yOffF, quad.z(i));
                    }
                    return true;
                }
            });

            try {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
            } finally {
                emitter.popTransform();
            }
        }

        @Override
        public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
            Object base = delegate.createGeometryKey(world, pos, state, random);
            if (!FabricTweaksConfig.isAnyEnhancedSlabBehaviorEnabled()) return base;

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) return base;

            int sign = yOff > 0.0 ? 1 : -1;
            return new OffsetKey(base, sign);
        }

        @Override
        public TextureAtlasSprite particleSprite(BlockAndTintGetter world, BlockPos pos, BlockState state) {
            return delegate.particleSprite(world, pos, state);
        }

        private record OffsetKey(Object baseKey, int yOffSign) {
        }
    }
}
