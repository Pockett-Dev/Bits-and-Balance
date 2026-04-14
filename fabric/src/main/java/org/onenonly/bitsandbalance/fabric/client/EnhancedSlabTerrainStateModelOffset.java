package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EnhancedSlabTerrainStateModelOffset {

    private EnhancedSlabTerrainStateModelOffset() {
    }

    public static BlockStateModel wrap(BlockStateModel model, float yOffset) {
        if (model == null || yOffset == 0.0F || model instanceof OffsetBlockStateModel) {
            return model;
        }
        return new OffsetBlockStateModel(model, yOffset);
    }

    private static final class OffsetBlockStateModel implements BlockStateModel {
        private final BlockStateModel delegate;
        private final float yOffset;

        private OffsetBlockStateModel(BlockStateModel delegate, float yOffset) {
            this.delegate = delegate;
            this.yOffset = yOffset;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            int originalSize = parts.size();
            delegate.collectParts(random, parts);
            for (int index = originalSize; index < parts.size(); index++) {
                BlockStateModelPart part = parts.get(index);
                if (!(part instanceof OffsetBlockStateModelPart)) {
                    parts.set(index, new OffsetBlockStateModelPart(part, yOffset));
                }
            }
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

    private static final class OffsetBlockStateModelPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final float yOffset;
        private final Map<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        private List<BakedQuad> unculled;

        private OffsetBlockStateModelPart(BlockStateModelPart delegate, float yOffset) {
            this.delegate = delegate;
            this.yOffset = yOffset;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction == null) {
                if (unculled == null) {
                    unculled = translateQuads(delegate.getQuads(null), yOffset);
                }
                return unculled;
            }
            return byDirection.computeIfAbsent(direction, key -> translateQuads(delegate.getQuads(key), yOffset));
        }

        @Override
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

    private static List<BakedQuad> translateQuads(List<BakedQuad> source, float yOffset) {
        if (source.isEmpty()) {
            return source;
        }

        ArrayList<BakedQuad> translated = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            translated.add(new BakedQuad(
                    translate(quad.position(0), yOffset),
                    translate(quad.position(1), yOffset),
                    translate(quad.position(2), yOffset),
                    translate(quad.position(3), yOffset),
                    quad.packedUV(0),
                    quad.packedUV(1),
                    quad.packedUV(2),
                    quad.packedUV(3),
                    quad.direction(),
                    quad.materialInfo()
            ));
        }
        return translated;
    }

    private static Vector3fc translate(Vector3fc position, float yOffset) {
        return new Vector3f(position.x(), position.y() + yOffset, position.z());
    }
}