package org.onenonly.bitsandbalance.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final class OffsetBlockStateModel implements BlockStateModel {
        private final BlockStateModel delegate;

        private OffsetBlockStateModel(BlockStateModel delegate) {
            this.delegate = delegate;
        }

        @Override
        @Deprecated
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            delegate.collectParts(random, parts);
        }

        @Override
        @Deprecated
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        @Deprecated
        public int materialFlags() {
            return delegate.materialFlags();
        }

        @Override
        public void collectParts(BlockAndTintGetter world,
                                 BlockPos pos,
                                 BlockState state,
                                 RandomSource random,
                                 List<BlockStateModelPart> parts) {
            if (!Config.enableEnhancedSlabs) {
                delegate.collectParts(world, pos, state, random, parts);
                return;
            }

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) {
                delegate.collectParts(world, pos, state, random, parts);
                return;
            }

            List<BlockStateModelPart> baseParts = new ArrayList<>();
            delegate.collectParts(world, pos, state, random, baseParts);
            float yOffF = (float) yOff;
            for (BlockStateModelPart part : baseParts) {
                parts.add(new YOffsetBlockStateModelPart(part, yOffF));
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

        private record OffsetKey(Object baseKey, int yOffHalfSteps) {
        }
    }

    private static final class YOffsetBlockStateModelPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final float yOff;

        private YOffsetBlockStateModelPart(BlockStateModelPart delegate, float yOff) {
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
        @Deprecated
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
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
                quad.direction(),
                quad.materialInfo(),
                quad.bakedNormals(),
                quad.bakedColors()
        );
    }
}