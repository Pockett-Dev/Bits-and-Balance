package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.client.SoulFireFlameRenderStateAccess;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(FlameFeatureRenderer.class)
@SuppressWarnings({"null", "deprecation"})
public abstract class FlameFeatureRendererSoulFireFlamesMixin {

    private static final SpriteId BITSANDBALANCE$FIRE_0 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/fire_0")
    );

    private static final SpriteId BITSANDBALANCE$FIRE_1 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/fire_1")
    );

    private static final SpriteId BITSANDBALANCE$SOUL_FIRE_0 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/soul_fire_0")
    );

    private static final SpriteId BITSANDBALANCE$SOUL_FIRE_1 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/soul_fire_1")
    );

    @Redirect(
            method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/resources/model/sprite/AtlasManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/sprite/AtlasManager;get(Lnet/minecraft/client/resources/model/sprite/SpriteId;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
            )
    )
    private TextureAtlasSprite bitsandbalance$swapFlameSprite(
            AtlasManager atlasManager,
                        SpriteId spriteId,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            EntityRenderState renderState,
            Quaternionf rotation,
            AtlasManager atlasManagerArg
    ) {
        if (renderState instanceof SoulFireFlameRenderStateAccess access && access.bitsandbalance$isSoulFireFlame()) {
                        if (BITSANDBALANCE$FIRE_0.equals(spriteId)) {
                                return atlasManager.get(BITSANDBALANCE$SOUL_FIRE_0);
            }
                        if (BITSANDBALANCE$FIRE_1.equals(spriteId)) {
                                return atlasManager.get(BITSANDBALANCE$SOUL_FIRE_1);
            }
        }

                return atlasManager.get(Objects.requireNonNull(spriteId));
    }
}
