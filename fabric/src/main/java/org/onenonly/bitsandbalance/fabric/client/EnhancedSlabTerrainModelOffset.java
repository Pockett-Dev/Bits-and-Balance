package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EnhancedSlabTerrainModelOffset {

    private EnhancedSlabTerrainModelOffset() {
    }

    public static Object[] wrapPartsInPlace(List<BlockModelPart> parts, float yOffset) {
        List<BlockModelPart> originals = new ArrayList<>(parts);
        for (int index = 0; index < parts.size(); index++) {
            parts.set(index, new OffsetBlockModelPart(parts.get(index), yOffset));
        }
        return new Object[]{parts, originals};
    }

    public static boolean isWrappedPartList(List<BlockModelPart> parts) {
        return parts != null && !parts.isEmpty() && parts.get(0) instanceof OffsetBlockModelPart;
    }

    @SuppressWarnings("unchecked")
    public static void restoreParts(Object[] restoreState) {
        List<BlockModelPart> parts = (List<BlockModelPart>) restoreState[0];
        List<BlockModelPart> originals = (List<BlockModelPart>) restoreState[1];
        for (int index = 0; index < originals.size(); index++) {
            parts.set(index, originals.get(index));
        }
    }

    private static final class OffsetBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final float yOffset;
        private final Map<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        private List<BakedQuad> unculled;

        private OffsetBlockModelPart(BlockModelPart delegate, float yOffset) {
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
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
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
                    quad.tintIndex(),
                    quad.direction(),
                    quad.sprite(),
                    quad.shade(),
                    quad.lightEmission()
            ));
        }
        return translated;
    }

    private static Vector3fc translate(Vector3fc position, float yOffset) {
        return new Vector3f(position.x(), position.y() + yOffset, position.z());
    }
}