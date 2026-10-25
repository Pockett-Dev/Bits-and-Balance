package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    private static final Material SOUL_FIRE_MATERIAL_1 = new Material(
            TextureAtlas.LOCATION_BLOCKS,
            ResourceLocation.parse("block/soul_fire_1")
    );

    @Redirect(
            method = "renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/resources/model/ModelBakery;FIRE_1:Lnet/minecraft/client/resources/model/Material;"
            ),
            require = 0
    )
    private static Material bitsandbalance$swapFireMaterial1() {
        if (!FabricClientConfig.soulFireOverlayEnabled) return ModelBakery.FIRE_1;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return ModelBakery.FIRE_1;
        return SOUL_FIRE_MATERIAL_1;
    }
}
