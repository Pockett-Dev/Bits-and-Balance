package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.client.SoulFireFlameRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Objects;

@Mixin(FlameFeatureRenderer.class)
@SuppressWarnings({"null", "deprecation"})
public abstract class FlameFeatureRendererSoulFireFlamesMixin {
    private static final SpriteId BITSANDBALANCE$SOUL_FIRE_0 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/soul_fire_0")
    );

    private static final SpriteId BITSANDBALANCE$SOUL_FIRE_1 = new SpriteId(
            Objects.requireNonNull(TextureAtlas.LOCATION_BLOCKS),
            Identifier.fromNamespaceAndPath("minecraft", "block/soul_fire_1")
    );

    @ModifyArgs(
            method = "buildGroup(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Ljava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;prepare(Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer$Submit;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            )
    )
    private void bitsandbalance$swapSoulFireSprites(Args args) {
        FlameFeatureRenderer.Submit submit = args.get(0);
        if (!(submit.entityRenderState() instanceof SoulFireFlameRenderStateAccess access) || !access.bitsandbalance$isSoulFireFlame()) {
            return;
        }

        TextureAtlasSprite soulFire0 = Minecraft.getInstance().getAtlasManager().get(BITSANDBALANCE$SOUL_FIRE_0);
        TextureAtlasSprite soulFire1 = Minecraft.getInstance().getAtlasManager().get(BITSANDBALANCE$SOUL_FIRE_1);
        args.set(2, soulFire0);
        args.set(3, soulFire1);
    }
}
