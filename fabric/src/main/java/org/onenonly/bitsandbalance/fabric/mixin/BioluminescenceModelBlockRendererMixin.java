package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(ModelBlockRenderer.class)
public abstract class BioluminescenceModelBlockRendererMixin {
    @Redirect(
            method = "putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/minecraft/client/renderer/block/ModelBlockRenderer$CommonRenderStorage;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"
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
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            VertexConsumer ignoredVertexConsumer,
            PoseStack.Pose ignoredPose,
            BakedQuad ignoredQuad,
            @Coerce Object storage,
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
                bitsandbalance$withDynamicLight(pos, lightmap),
                packedOverlay
        );
    }

    @Redirect(
            method = "tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer$Cache;getLightColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
            ),
            require = 0
    )
    private int bitsandbalance$applyBioluminescenceDynamicLightDuringTesselation(
            @Coerce Object cache,
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        return bitsandbalance$applyBioluminescenceDynamicLight(state, level, pos);
    }

    @Redirect(
            method = "renderModelFaceFlat(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;IIZLcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/client/renderer/block/ModelBlockRenderer$CommonRenderStorage;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer$Cache;getLightColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
            ),
            require = 0
    )
    private int bitsandbalance$applyBioluminescenceDynamicLightDuringFlatFaceRender(
            @Coerce Object cache,
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        return bitsandbalance$applyBioluminescenceDynamicLight(state, level, pos);
    }

    private static int bitsandbalance$applyBioluminescenceDynamicLight(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        int lightmap = LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, level, state, pos);
        return BioluminescenceLightManager.getLightmapWithDynamicLight(pos, lightmap);
    }

    private static int[] bitsandbalance$withDynamicLight(BlockPos pos, int[] lightmap) {
        if (lightmap == null || lightmap.length == 0) {
            return lightmap;
        }

        int[] adjusted = Arrays.copyOf(lightmap, lightmap.length);
        for (int index = 0; index < adjusted.length; index++) {
            adjusted[index] = BioluminescenceLightManager.getLightmapWithDynamicLight(pos, adjusted[index]);
        }
        return adjusted;
    }
}