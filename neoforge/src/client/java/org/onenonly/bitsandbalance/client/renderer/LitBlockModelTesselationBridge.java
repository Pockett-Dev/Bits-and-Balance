package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public final class LitBlockModelTesselationBridge {
    private static volatile ModelBlockRenderer renderer;

    private LitBlockModelTesselationBridge() {
    }

    public static void render(PoseStack.Pose pose,
                              VertexConsumer consumer,
                              Object level,
                              BlockPos blockPos,
                              BlockState sourceState,
                              List<BlockStateModelPart> parts,
                              int packedLight) {
        if (pose == null || consumer == null || blockPos == null || sourceState == null || parts == null || parts.isEmpty()) {
            return;
        }

        BlockAndTintGetter getter = level instanceof BlockAndTintGetter typed ? typed : BlockAndTintGetter.EMPTY;
        int resolvedPackedLight = VirtualRenderWorldLightBridge.capturePackedLight(level, packedLight);
        getRenderer().tesselateBlock(
                new VertexConsumerBlockQuadOutput(pose, consumer, resolvedPackedLight),
                0.0f,
                0.0f,
                0.0f,
                getter,
                blockPos,
                sourceState,
                new RuntimeBlockStateModel(parts),
                RandomSource.create(blockPos.asLong()).nextLong()
        );
    }

    private static ModelBlockRenderer getRenderer() {
        ModelBlockRenderer current = renderer;
        if (current == null) {
            current = new ModelBlockRenderer(true, false, Minecraft.getInstance().getBlockColors());
            renderer = current;
        }
        return current;
    }

    private record RuntimeBlockStateModel(List<BlockStateModelPart> parts) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource randomSource, List<BlockStateModelPart> out) {
            out.addAll(parts);
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return parts.getFirst().particleMaterial();
        }

        @Override
        public int materialFlags() {
            return parts.getFirst().materialFlags();
        }
    }

    private static final class VertexConsumerBlockQuadOutput implements BlockQuadOutput {
        private final PoseStack.Pose pose;
        private final VertexConsumer consumer;
        private final int packedLight;

        private VertexConsumerBlockQuadOutput(PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
            this.pose = pose;
            this.consumer = consumer;
            this.packedLight = packedLight;
        }

        @Override
        public void put(float x, float y, float z, BakedQuad quad, QuadInstance quadInstance) {
            Direction direction = quad.direction();
            Vector3fc unitNormal = direction != null ? direction.getUnitVec3f() : new Vector3f(0.0f, 1.0f, 0.0f);
            Vector3f transformedNormal = pose.transformNormal(unitNormal, new Vector3f());
            int lightEmission = quad.materialInfo().lightEmission();

            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = quad.position(vertex);
                Vector3f transformedPosition = pose.pose().transformPosition(
                        position.x() + x,
                        position.y() + y,
                        position.z() + z,
                        new Vector3f()
                );
                long packedUv = quad.packedUV(vertex);
                int resolvedLight = quadInstance.getLightCoordsWithEmission(vertex, lightEmission);
                if (resolvedLight == 0 && this.packedLight != 0) {
                    resolvedLight = this.packedLight;
                }

                consumer.addVertex(transformedPosition.x(), transformedPosition.y(), transformedPosition.z())
                        .setColor(quadInstance.getColor(vertex))
                        .setUv(UVPair.unpackU(packedUv), UVPair.unpackV(packedUv))
                        .setOverlay(quadInstance.overlayCoords())
                        .setLight(resolvedLight)
                        .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
            }
        }
    }
}