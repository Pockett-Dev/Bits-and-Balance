package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Deferred renderer for the Bottle-of-Cloud display entity's alpha fade.
 * <p>
 * Display entities render BEFORE translucent terrain (water). If the cloud
 * renders during the entity pass it either:
 * <ul>
 *   <li>Blocks water (depth write on) — water vanishes behind cloud</li>
 *   <li>Gets overwritten by water (depth write off) — cloud invisible where water is</li>
 * </ul>
 * <p>
 * By deferring the draw to AFTER translucent terrain (via a level render event),
 * the cloud renders on top of water with proper alpha blending.
 */
public final class CloudFadeRenderer {

    // The deferred display path renders as a simple cube instead of going through
    // chunk block tesselation, which makes the cloud read slightly darker than the
    // regular translucent block. Blend the directional shade partway toward white
    // to better match the in-world block appearance without flattening the lighting.
    private static final float DISPLAY_SHADE_BRIGHTNESS_BIAS = 0.18F;

    private static final Identifier CLOUD_TEXTURE = Identifier.parse("bitsandbalance:block/cloud");
    private static final Material CLOUD_MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS, CLOUD_TEXTURE);

    /** Queued cloud renders for the current frame. Filled during entity pass, drawn after translucent terrain. */
    private static final List<QueuedCloud> QUEUE = new ArrayList<>();

    /** Snapshot of one cloud's rendering data. */
    private record QueuedCloud(Matrix4f pose, Matrix3f normal, int alpha, int packedLight) {}

    /**
     * Queue a cloud for deferred rendering. Called from the cloud fade mixin
     * during the entity rendering pass (instead of drawing immediately).
     */
    public static void queue(PoseStack.Pose currentPose, int alpha, int packedLight) {
        QUEUE.add(new QueuedCloud(
                new Matrix4f(currentPose.pose()),
                new Matrix3f(currentPose.normal()),
                alpha,
                packedLight
        ));
    }

    /**
     * Flush all queued clouds. Call this from a level render event that fires
     * AFTER translucent terrain (NeoForge: AfterTranslucentBlocks, Fabric: END_MAIN).
     */
    public static void renderAllDeferred() {
        if (QUEUE.isEmpty()) return;

        try {
            ClientLevel level = Minecraft.getInstance().level;
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(CLOUD_MATERIAL);
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();

            MultiBufferSource.BufferSource bufferSource =
                    Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType renderType = CloudRenderType.get();
            VertexConsumer vc = bufferSource.getBuffer(renderType);

            PoseStack tempStack = new PoseStack();

            for (QueuedCloud cloud : QUEUE) {
                tempStack.pushPose();
                tempStack.last().pose().set(cloud.pose);
                tempStack.last().normal().set(cloud.normal);
                PoseStack.Pose pose = tempStack.last();

                int a = cloud.alpha;
                int light = cloud.packedLight;
                int overlay = OverlayTexture.NO_OVERLAY;
                int southShade = shade(level, Direction.SOUTH);
                int northShade = shade(level, Direction.NORTH);
                int eastShade = shade(level, Direction.EAST);
                int westShade = shade(level, Direction.WEST);
                int upShade = shade(level, Direction.UP);
                int downShade = shade(level, Direction.DOWN);

                // +Z (south)
                v(pose, vc, 0, 0, 1, u0, v1, light, overlay, southShade, a, 0, 0, 1);
                v(pose, vc, 1, 0, 1, u1, v1, light, overlay, southShade, a, 0, 0, 1);
                v(pose, vc, 1, 1, 1, u1, v0, light, overlay, southShade, a, 0, 0, 1);
                v(pose, vc, 0, 1, 1, u0, v0, light, overlay, southShade, a, 0, 0, 1);

                // -Z (north)
                v(pose, vc, 1, 0, 0, u1, v1, light, overlay, northShade, a, 0, 0, -1);
                v(pose, vc, 0, 0, 0, u0, v1, light, overlay, northShade, a, 0, 0, -1);
                v(pose, vc, 0, 1, 0, u0, v0, light, overlay, northShade, a, 0, 0, -1);
                v(pose, vc, 1, 1, 0, u1, v0, light, overlay, northShade, a, 0, 0, -1);

                // +X (east)
                v(pose, vc, 1, 0, 1, u1, v1, light, overlay, eastShade, a, 1, 0, 0);
                v(pose, vc, 1, 0, 0, u0, v1, light, overlay, eastShade, a, 1, 0, 0);
                v(pose, vc, 1, 1, 0, u0, v0, light, overlay, eastShade, a, 1, 0, 0);
                v(pose, vc, 1, 1, 1, u1, v0, light, overlay, eastShade, a, 1, 0, 0);

                // -X (west)
                v(pose, vc, 0, 0, 0, u1, v1, light, overlay, westShade, a, -1, 0, 0);
                v(pose, vc, 0, 0, 1, u0, v1, light, overlay, westShade, a, -1, 0, 0);
                v(pose, vc, 0, 1, 1, u0, v0, light, overlay, westShade, a, -1, 0, 0);
                v(pose, vc, 0, 1, 0, u1, v0, light, overlay, westShade, a, -1, 0, 0);

                // +Y (top)
                v(pose, vc, 0, 1, 1, u0, v1, light, overlay, upShade, a, 0, 1, 0);
                v(pose, vc, 1, 1, 1, u1, v1, light, overlay, upShade, a, 0, 1, 0);
                v(pose, vc, 1, 1, 0, u1, v0, light, overlay, upShade, a, 0, 1, 0);
                v(pose, vc, 0, 1, 0, u0, v0, light, overlay, upShade, a, 0, 1, 0);

                // -Y (bottom)
                v(pose, vc, 0, 0, 0, u0, v1, light, overlay, downShade, a, 0, -1, 0);
                v(pose, vc, 1, 0, 0, u1, v1, light, overlay, downShade, a, 0, -1, 0);
                v(pose, vc, 1, 0, 1, u1, v0, light, overlay, downShade, a, 0, -1, 0);
                v(pose, vc, 0, 0, 1, u0, v0, light, overlay, downShade, a, 0, -1, 0);

                tempStack.popPose();
            }

            // Flush the cloud geometry to the framebuffer immediately
            bufferSource.endBatch(renderType);
        } finally {
            QUEUE.clear();
        }
    }

    private static void v(
            PoseStack.Pose pose, VertexConsumer vc,
            float x, float y, float z,
            float u, float v,
            int packedLight, int packedOverlay, int shade, int alpha,
            float nx, float ny, float nz
    ) {
        vc.addVertex(pose, x, y, z)
                .setColor(shade, shade, shade, alpha)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private static int shade(ClientLevel level, Direction direction) {
        float shade = level != null ? level.getShade(direction, true) : fallbackShade(direction);
        shade += (1.0F - shade) * DISPLAY_SHADE_BRIGHTNESS_BIAS;
        int channel = Math.round(Math.max(0.0F, Math.min(1.0F, shade)) * 255.0F);
        return Math.max(0, Math.min(255, channel));
    }

    private static float fallbackShade(Direction direction) {
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    private CloudFadeRenderer() {}
}
