package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public abstract class BioluminescenceModelBlockRendererMixin {
    @Redirect(
            method = "putQuadWithTint(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/geometry/BakedQuad;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
            ),
            require = 0
    )
    private void bitsandbalance$applyBioluminescenceDynamicLightDuringQuadOutput(
            BlockQuadOutput quadOutput,
            float offsetX,
            float offsetY,
            float offsetZ,
            BakedQuad quad,
            QuadInstance quadInstance,
            BlockQuadOutput ignoredQuadOutput,
            float ignoredOffsetX,
            float ignoredOffsetY,
            float ignoredOffsetZ,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            BakedQuad ignoredQuad
    ) {
        quadOutput.put(offsetX, offsetY, offsetZ, quad, bitsandbalance$withDynamicLight(pos, quadInstance));
    }

    private static QuadInstance bitsandbalance$withDynamicLight(BlockPos pos, QuadInstance quadInstance) {
        QuadInstance adjusted = new QuadInstance();
        for (int index = 0; index < 4; index++) {
            adjusted.setColor(index, quadInstance.getColor(index));
            adjusted.setLightCoords(index, BioluminescenceLightManager.getLightmapWithDynamicLight(pos, quadInstance.getLightCoords(index)));
        }
        adjusted.setOverlayCoords(quadInstance.overlayCoords());
        return adjusted;
    }
}