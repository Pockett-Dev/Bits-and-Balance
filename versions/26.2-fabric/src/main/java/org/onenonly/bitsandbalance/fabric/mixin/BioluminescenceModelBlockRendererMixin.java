package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
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
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BakedQuad quad,
            QuadInstance quadInstance,
            BlockQuadOutput ignoredOutput,
            float ignoredX,
            float ignoredY,
            float ignoredZ,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            BakedQuad ignoredQuad
    ) {
        bitsandbalance$applyDynamicLight(level, pos, quad, quadInstance);
        output.put(x, y, z, quad, quadInstance);
    }

    private static void bitsandbalance$applyDynamicLight(BlockAndTintGetter level, BlockPos pos, BakedQuad quad, QuadInstance quadInstance) {
        if (quad == null || quadInstance == null) {
            return;
        }

        for (int vertex = 0; vertex < 4; vertex++) {
            int lightmap = quadInstance.getLightCoords(vertex);
            quadInstance.setLightCoords(vertex, bitsandbalance$getVertexLightmap(level, pos, quad, vertex, lightmap));
        }
    }

    private static int bitsandbalance$getVertexLightmap(BlockAndTintGetter level, BlockPos pos, BakedQuad quad, int vertexIndex, int lightmap) {
        Vector3fc vertex = quad.position(vertexIndex);
        if (vertex == null) {
            return BioluminescenceLightManager.getLightmapWithDynamicLight(level, pos, lightmap);
        }

        double sampleX = pos.getX() + vertex.x();
        double sampleY = pos.getY() + vertex.y();
        double sampleZ = pos.getZ() + vertex.z();
        Direction faceDirection = quad.direction();
        return BioluminescenceLightManager.getLightmapWithDynamicLight(level, sampleX, sampleY, sampleZ, faceDirection, lightmap);
    }
}
