package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(ModelBlockRenderer.class)
public abstract class BioluminescenceModelBlockRendererMixin {
    @Redirect(
            method = "putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"
            ),
            require = 0
    )
        private void bitsandbalance$applyBioluminescenceDynamicLightDuringQuadUpload(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readExistingColor,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            VertexConsumer ignoredVertexConsumer,
            PoseStack.Pose ignoredPose,
            BakedQuad ignoredQuad,
            float ignoredRed,
            float ignoredGreen,
            float ignoredBlue,
            float ignoredAlpha,
            int ignoredLight0,
            int ignoredLight1,
            int ignoredLight2,
            int ignoredLight3,
            int ignoredPackedOverlay
    ) {
        vertexConsumer.putBulkData(
                pose,
                quad,
                brightness,
                red,
                green,
                blue,
                alpha,
                bitsandbalance$withDynamicLight(level, pos, quad, lightmap),
                packedOverlay,
                readExistingColor
        );
    }

    private static int[] bitsandbalance$withDynamicLight(BlockAndTintGetter level, BlockPos pos, BakedQuad quad, int[] lightmap) {
        if (lightmap == null || lightmap.length == 0) {
            return lightmap;
        }

        int[] adjusted = Arrays.copyOf(lightmap, lightmap.length);
        for (int index = 0; index < adjusted.length; index++) {
            adjusted[index] = bitsandbalance$getVertexLightmap(level, pos, quad, index, adjusted[index]);
        }
        return adjusted;
    }

    private static int bitsandbalance$getVertexLightmap(BlockAndTintGetter level, BlockPos pos, BakedQuad quad, int vertexIndex, int lightmap) {
        if (quad == null) {
            return BioluminescenceLightManager.getLightmapWithDynamicLight(level, pos, lightmap);
        }

        int[] vertices = quad.getVertices();
        if (vertices == null || vertices.length < 12) {
            return BioluminescenceLightManager.getLightmapWithDynamicLight(level, pos, lightmap);
        }

        int stride = vertices.length / 4;
        int base = vertexIndex * stride;
        if (base + 2 >= vertices.length) {
            return BioluminescenceLightManager.getLightmapWithDynamicLight(level, pos, lightmap);
        }

        double sampleX = pos.getX() + Float.intBitsToFloat(vertices[base]);
        double sampleY = pos.getY() + Float.intBitsToFloat(vertices[base + 1]);
        double sampleZ = pos.getZ() + Float.intBitsToFloat(vertices[base + 2]);
        Direction faceDirection = quad.getDirection();
        return BioluminescenceLightManager.getLightmapWithDynamicLight(level, sampleX, sampleY, sampleZ, faceDirection, lightmap);
    }
}
