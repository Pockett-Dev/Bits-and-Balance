package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Uses Blueprint's active vertical-thatch template rather than flattening its straw. */
final class ThatchModelGeometry {
    private static final ResourceLocation VERTICAL_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("blueprint", "models/block/template_thatch_vertical_slab.json");
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    private ThatchModelGeometry() {
    }

    static @Nullable List<BakedQuad> verticalSlabQuads(BakedModel sourceModel) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(VERTICAL_TEMPLATE);
        if (resource.isEmpty()) {
            return null;
        }
        TextureAtlasSprite thatch = sourceModel.getParticleIcon();
        TextureAtlasSprite extrudes = extrudeSprite(sourceModel, thatch);
        if (extrudes == null) {
            return null;
        }
        try (var reader = resource.get().openAsReader()) {
            BlockModel template = BlockModel.fromStream(reader);
            List<BakedQuad> quads = new ArrayList<>();
            for (var element : template.getElements()) {
                for (var entry : element.faces.entrySet()) {
                    var face = entry.getValue();
                    TextureAtlasSprite sprite = switch (face.texture()) {
                        case "#thatch" -> thatch;
                        case "#extrudes" -> extrudes;
                        default -> null;
                    };
                    // A resource pack may replace the template's texture contract entirely.
                    // In that case retain the source-model geometry fallback.
                    if (sprite == null) {
                        return null;
                    }
                    quads.add(FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite,
                            entry.getKey(), BlockModelRotation.X0_Y0, element.rotation, element.shade));
                }
            }
            return List.copyOf(quads);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static @Nullable TextureAtlasSprite extrudeSprite(BakedModel source, TextureAtlasSprite thatch) {
        List<Direction> sides = new ArrayList<>();
        sides.add(null);
        sides.addAll(List.of(Direction.values()));
        for (Direction side : sides) {
            for (BakedQuad quad : source.getQuads(null, side, RandomSource.create(42L))) {
                if (quad.getSprite() != thatch) {
                    return quad.getSprite();
                }
            }
        }
        return null;
    }
}
