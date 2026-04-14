package org.onenonly.bitsandbalance.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NeoForge baked-model wrapper that applies Enhanced Slab visual Y offsets at the quad level.
 *
 * <p>This complements the pose-stack render offset mixin for renderer paths that bypass
 * {@code BlockRenderDispatcher.renderBatched} during terrain compilation and instead rely on
 * NeoForge's contextual {@code collectParts(level, pos, state, random, parts)} model hook.</p>
 */
public final class NeoForgeEnhancedSlabModelOffset {

    private NeoForgeEnhancedSlabModelOffset() {
    }

    public static void wrapAll(Map<BlockState, BlockStateModel> blockStateModels) {
        for (var entry : blockStateModels.entrySet()) {
            BlockState state = entry.getKey();
            BlockStateModel model = entry.getValue();
            if (state == null || model == null) continue;
            if (EnhancedSlabHelper.shouldExcludeFromVisualOffset(state)) continue;
            if (model instanceof OffsetBlockStateModel) continue;
            entry.setValue(new OffsetBlockStateModel(model));
        }
    }

    private static final class OffsetBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
        private OffsetBlockStateModel(BlockStateModel delegate) {
            super(delegate);
        }

        @Override
        public void collectParts(BlockAndTintGetter world,
                                 BlockPos pos,
                                 BlockState state,
                                 RandomSource random,
                                 List<BlockModelPart> parts) {
            if (!Config.enableEnhancedSlabs) {
                delegate.collectParts(world, pos, state, random, parts);
                return;
            }

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) {
                delegate.collectParts(world, pos, state, random, parts);
                return;
            }

            List<BlockModelPart> baseParts = new ArrayList<>();
            delegate.collectParts(world, pos, state, random, baseParts);
            float yOffF = (float) yOff;
            for (BlockModelPart part : baseParts) {
                parts.add(new YOffsetBlockModelPart(part, yOffF));
            }
        }

        @Override
        public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
            Object base = delegate.createGeometryKey(world, pos, state, random);
            if (!Config.enableEnhancedSlabs) return base;

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) return base;

            int halfSteps = (int) Math.round(yOff * 2.0);
            return new OffsetKey(base, halfSteps);
        }

        @Override
        public TextureAtlasSprite particleIcon(BlockAndTintGetter world, BlockPos pos, BlockState state) {
            return delegate.particleIcon(world, pos, state);
        }

        private record OffsetKey(Object baseKey, int yOffHalfSteps) {
        }
    }

    private static final class YOffsetBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final float yOff;

        private YOffsetBlockModelPart(BlockModelPart delegate, float yOff) {
            this.delegate = delegate;
            this.yOff = yOff;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            List<BakedQuad> quads = delegate.getQuads(direction);
            if (quads.isEmpty()) return quads;

            ArrayList<BakedQuad> shifted = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                shifted.add(translateQuadY(quad, yOff));
            }
            return shifted;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }
    }

    private static BakedQuad translateQuadY(BakedQuad quad, float yOff) {
        if (yOff == 0.0f) return quad;

        Vector3fc[] moved = new Vector3fc[4];
        for (int v = 0; v < 4; v++) {
            Vector3fc pos = quad.position(v);
            moved[v] = new Vector3f(pos.x(), pos.y() + yOff, pos.z());
        }

        return new BakedQuad(
                moved[0], moved[1], moved[2], moved[3],
                quad.packedUV0(), quad.packedUV1(),
                quad.packedUV2(), quad.packedUV3(),
                quad.tintIndex(),
                quad.direction(),
                quad.sprite(), quad.shade(),
                quad.lightEmission()
        );
    }
}