package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
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

    public static Object[] wrapPartsInPlace(List<BlockStateModelPart> parts, float yOffset) {
        return wrapPartsInPlace(parts, yOffset, false);
    }

    public static Object[] wrapPartsInPlace(List<BlockStateModelPart> parts, float yOffset, boolean forceUnculled) {
        List<BlockStateModelPart> originals = new ArrayList<>(parts);
        for (int index = 0; index < parts.size(); index++) {
            parts.set(index, new OffsetBlockModelPart(parts.get(index), yOffset, forceUnculled));
        }
        return new Object[]{parts, originals};
    }

    public static boolean isWrappedPartList(List<BlockStateModelPart> parts) {
        return parts != null && !parts.isEmpty() && parts.get(0) instanceof OffsetBlockModelPart;
    }

    @SuppressWarnings("unchecked")
    public static void restoreParts(Object[] restoreState) {
        List<BlockStateModelPart> parts = (List<BlockStateModelPart>) restoreState[0];
        List<BlockStateModelPart> originals = (List<BlockStateModelPart>) restoreState[1];
        for (int index = 0; index < originals.size(); index++) {
            parts.set(index, originals.get(index));
        }
    }

    private static final class OffsetBlockModelPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final float yOffset;
        private final boolean forceUnculled;
        private final Map<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        private List<BakedQuad> unculled;

        private OffsetBlockModelPart(BlockStateModelPart delegate, float yOffset, boolean forceUnculled) {
            this.delegate = delegate;
            this.yOffset = yOffset;
            this.forceUnculled = forceUnculled;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction == null) {
                if (unculled == null) {
                    unculled = forceUnculled
                            ? translateAllQuads(delegate, yOffset)
                            : translateQuads(delegate.getQuads(null), yOffset);
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

    private static List<BakedQuad> translateAllQuads(BlockStateModelPart part, float yOffset) {
        ArrayList<BakedQuad> all = new ArrayList<>();
        all.addAll(translateQuads(part.getQuads(null), yOffset));
        for (Direction direction : Direction.values()) {
            all.addAll(translateQuads(part.getQuads(direction), yOffset));
        }
        return all.isEmpty() ? List.of() : List.copyOf(all);
    }

    private static Vector3fc translate(Vector3fc position, float yOffset) {
        return new Vector3f(position.x(), position.y() + yOffset, position.z());
    }
}
