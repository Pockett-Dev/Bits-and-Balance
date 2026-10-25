package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.onenonly.bitsandbalance.common.client.UsesForCursesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    private static final Identifier FIRE_SPRITE_0 = Identifier.parse("minecraft:block/fire_0");
    private static final Identifier FIRE_SPRITE_1 = Identifier.parse("minecraft:block/fire_1");
    private static final Identifier SOUL_FIRE_SPRITE_0 = Identifier.parse("minecraft:block/soul_fire_0");
    private static final Identifier SOUL_FIRE_SPRITE_1 = Identifier.parse("minecraft:block/soul_fire_1");

    @ModifyVariable(
            method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private static TextureAtlasSprite bitsandbalance$swapFireSprite(TextureAtlasSprite sprite) {
        if (!FabricClientConfig.soulFireOverlayEnabled) return sprite;

        boolean should = SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance());
        if (!should) return sprite;

        TextureAtlasSprite remapped = sprite;
        Identifier beforeName = null;
        try {
            beforeName = sprite != null ? sprite.contents().name() : null;
        } catch (Throwable ignored) {
        }

        // Fabric/Yarn: directly fetch the soul-fire sprites from the blocks atlas.
        // This avoids the reflective lookup in common code, which can fail across mappings.
        try {
            Identifier target = null;
            if (FIRE_SPRITE_0.equals(beforeName)) target = SOUL_FIRE_SPRITE_0;
            else if (FIRE_SPRITE_1.equals(beforeName)) target = SOUL_FIRE_SPRITE_1;

            if (target != null) {
                Object tex = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
                TextureAtlasSprite fetched = tex instanceof TextureAtlas atlas ? atlas.getSprite(target) : null;
                if (fetched != null) {
                    remapped = fetched;
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback: keep common behavior if the direct atlas fetch fails.
        if (remapped == sprite) {
            remapped = SoulFireOverlayClient.remapFireSprite(sprite);
        }

        return remapped;
    }

    @Inject(
            method = "getViewBlockingState(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$hideVanishingPumpkinViewBlockingState(
            Player player,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState state = cir.getReturnValue();
        if (UsesForCursesClient.shouldHidePumpkinOverlayFromHead(FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)
                && state != null
                && state.getBlock() == Blocks.CARVED_PUMPKIN) {
            cir.setReturnValue(null);
        }
    }
}
