package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LightLayer;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
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
    private static final Identifier CLOUD_TEXTURE = Identifier.parse("bitsandbalance:block/cloud");
    private static final SpriteId CLOUD_SPRITE_ID = new SpriteId(TextureAtlas.LOCATION_BLOCKS, CLOUD_TEXTURE);
    private static final boolean IS_NEOFORGE = classExists("net.neoforged.fml.loading.FMLLoader");

    /** Queued cloud renders for the current frame. Filled during entity pass, drawn after translucent terrain. */
    private static final List<QueuedCloud> QUEUE = new ArrayList<>();

    /** Snapshot of one cloud's rendering data. */
    private record QueuedCloud(Matrix4f pose, Matrix3f normal, BlockPos blockPos, int alpha, int packedLight) {}

    /**
     * Queue a cloud for deferred rendering. Called from the cloud fade mixin
     * during the entity rendering pass (instead of drawing immediately).
     */
    public static void queue(PoseStack.Pose currentPose, BlockPos blockPos, int alpha, int packedLight) {
        QUEUE.add(new QueuedCloud(
                new Matrix4f(currentPose.pose()),
                new Matrix3f(currentPose.normal()),
                blockPos,
                alpha,
                packedLight
        ));
    }

    /**
     * Flush all queued clouds. Call this from a level render event that fires
     * AFTER translucent terrain (NeoForge: AfterTranslucentBlocks, Fabric: LevelRenderEvents.END_MAIN).
     */
    public static void renderAllDeferred() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || QUEUE.isEmpty()) return;

        try {
            TextureAtlasSprite sprite = bitsandbalance$getCloudSprite(minecraft);
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();

            MultiBufferSource.BufferSource bufferSource =
                    minecraft.renderBuffers().bufferSource();
            RenderType renderType = RenderTypes.translucentMovingBlock();
            VertexConsumer vc = bufferSource.getBuffer(renderType);

            PoseStack tempStack = new PoseStack();

            for (QueuedCloud cloud : QUEUE) {
                tempStack.pushPose();
                tempStack.last().pose().set(cloud.pose);
                tempStack.last().normal().set(cloud.normal);
                bitsandbalance$renderCloud(tempStack.last(), vc, level, cloud.blockPos, cloud.alpha, cloud.packedLight, u0, u1, v0, v1);

                tempStack.popPose();
            }

            // Flush the cloud geometry to the framebuffer immediately
            bufferSource.endBatch(renderType);
        } finally {
            QUEUE.clear();
        }
    }

    private static void bitsandbalance$renderCloud(
            PoseStack.Pose pose,
            VertexConsumer vc,
            ClientLevel level,
            BlockPos blockPos,
            int alpha,
            int packedLight,
            float u0,
            float u1,
            float v0,
            float v1
    ) {
        int overlay = OverlayTexture.NO_OVERLAY;
        int southShade = shade(Direction.SOUTH);
        int northShade = shade(Direction.NORTH);
        int eastShade = shade(Direction.EAST);
        int westShade = shade(Direction.WEST);
        int upShade = shade(Direction.UP);
        int downShade = shade(Direction.DOWN);
        int southLight = resolveFacePackedLight(level, blockPos, Direction.SOUTH, packedLight);
        int northLight = resolveFacePackedLight(level, blockPos, Direction.NORTH, packedLight);
        int eastLight = resolveFacePackedLight(level, blockPos, Direction.EAST, packedLight);
        int westLight = resolveFacePackedLight(level, blockPos, Direction.WEST, packedLight);
        int upLight = resolveFacePackedLight(level, blockPos, Direction.UP, packedLight);
        int downLight = resolveFacePackedLight(level, blockPos, Direction.DOWN, packedLight);
        boolean renderSouth = shouldRenderFace(level, blockPos, Direction.SOUTH);
        boolean renderNorth = shouldRenderFace(level, blockPos, Direction.NORTH);
        boolean renderEast = shouldRenderFace(level, blockPos, Direction.EAST);
        boolean renderWest = shouldRenderFace(level, blockPos, Direction.WEST);
        boolean renderUp = shouldRenderFace(level, blockPos, Direction.UP);
        boolean renderDown = shouldRenderFace(level, blockPos, Direction.DOWN);

        if (renderSouth) {
            v(pose, vc, 0, 0, 1, u0, v1, southLight, overlay, southShade, alpha, 0, 0, 1);
            v(pose, vc, 1, 0, 1, u1, v1, southLight, overlay, southShade, alpha, 0, 0, 1);
            v(pose, vc, 1, 1, 1, u1, v0, southLight, overlay, southShade, alpha, 0, 0, 1);
            v(pose, vc, 0, 1, 1, u0, v0, southLight, overlay, southShade, alpha, 0, 0, 1);
        }

        if (renderNorth) {
            v(pose, vc, 1, 0, 0, u1, v1, northLight, overlay, northShade, alpha, 0, 0, -1);
            v(pose, vc, 0, 0, 0, u0, v1, northLight, overlay, northShade, alpha, 0, 0, -1);
            v(pose, vc, 0, 1, 0, u0, v0, northLight, overlay, northShade, alpha, 0, 0, -1);
            v(pose, vc, 1, 1, 0, u1, v0, northLight, overlay, northShade, alpha, 0, 0, -1);
        }

        if (renderEast) {
            v(pose, vc, 1, 0, 1, u1, v1, eastLight, overlay, eastShade, alpha, 1, 0, 0);
            v(pose, vc, 1, 0, 0, u0, v1, eastLight, overlay, eastShade, alpha, 1, 0, 0);
            v(pose, vc, 1, 1, 0, u0, v0, eastLight, overlay, eastShade, alpha, 1, 0, 0);
            v(pose, vc, 1, 1, 1, u1, v0, eastLight, overlay, eastShade, alpha, 1, 0, 0);
        }

        if (renderWest) {
            v(pose, vc, 0, 0, 0, u1, v1, westLight, overlay, westShade, alpha, -1, 0, 0);
            v(pose, vc, 0, 0, 1, u0, v1, westLight, overlay, westShade, alpha, -1, 0, 0);
            v(pose, vc, 0, 1, 1, u0, v0, westLight, overlay, westShade, alpha, -1, 0, 0);
            v(pose, vc, 0, 1, 0, u1, v0, westLight, overlay, westShade, alpha, -1, 0, 0);
        }

        if (renderUp) {
            v(pose, vc, 0, 1, 1, u0, v1, upLight, overlay, upShade, alpha, 0, 1, 0);
            v(pose, vc, 1, 1, 1, u1, v1, upLight, overlay, upShade, alpha, 0, 1, 0);
            v(pose, vc, 1, 1, 0, u1, v0, upLight, overlay, upShade, alpha, 0, 1, 0);
            v(pose, vc, 0, 1, 0, u0, v0, upLight, overlay, upShade, alpha, 0, 1, 0);
        }

        if (renderDown) {
            v(pose, vc, 0, 0, 0, u0, v1, downLight, overlay, downShade, alpha, 0, -1, 0);
            v(pose, vc, 1, 0, 0, u1, v1, downLight, overlay, downShade, alpha, 0, -1, 0);
            v(pose, vc, 1, 0, 1, u1, v0, downLight, overlay, downShade, alpha, 0, -1, 0);
            v(pose, vc, 0, 0, 1, u0, v0, downLight, overlay, downShade, alpha, 0, -1, 0);
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

    private static int shade(Direction direction) {
        float shade = fallbackShade(direction);
        int channel = Math.round(Math.max(0.0F, Math.min(1.0F, shade)) * 255.0F);
        return Math.max(0, Math.min(255, channel));
    }

    private static float fallbackShade(Direction direction) {
        if (IS_NEOFORGE) {
            return switch (direction) {
                case DOWN -> 0.5F;
                case UP -> 0.99F;
                case NORTH, SOUTH -> 0.7F;
                case WEST, EAST -> 0.7F;
            };
        }
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    private static boolean shouldRenderFace(ClientLevel level, BlockPos pos, Direction direction) {
        if (level == null || pos == null) {
            return true;
        }
        return level.getBlockState(pos.relative(direction)).getBlock() != ModBottleOfCloud.cloudBlock();
    }

    private static int resolvePackedLight(ClientLevel level, BlockPos pos, int fallbackPackedLight) {
        if (level == null || pos == null) {
            return fallbackPackedLight;
        }
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return (block << 4) | (sky << 20);
    }

    private static int resolveFacePackedLight(ClientLevel level, BlockPos pos, Direction direction, int fallbackPackedLight) {
        if (level == null || pos == null) {
            return fallbackPackedLight;
        }
        return resolvePackedLight(level, pos.relative(direction), fallbackPackedLight);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, CloudFadeRenderer.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private CloudFadeRenderer() {}
    private static TextureAtlasSprite bitsandbalance$getCloudSprite(Minecraft minecraft) {
        Object atlasManager = bitsandbalance$getAtlasManager(minecraft);

        if (atlasManager == null) {
            Object texture = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            if (texture instanceof TextureAtlas textureAtlas) {
                return textureAtlas.getSprite(CLOUD_TEXTURE);
            }
            throw new IllegalStateException("Failed to resolve block atlas for cloud fade rendering");
        }

        try {
            Class<?> materialClass = Class.forName("net.minecraft.client.resources.model.Material");
            Object material = materialClass.getConstructor(Identifier.class, Identifier.class)
                    .newInstance(TextureAtlas.LOCATION_BLOCKS, CLOUD_TEXTURE);
            return (TextureAtlasSprite) atlasManager.getClass().getMethod("get", materialClass).invoke(atlasManager, material);
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return (TextureAtlasSprite) atlasManager.getClass().getMethod("get", SpriteId.class).invoke(atlasManager, CLOUD_SPRITE_ID);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to resolve cloud atlas sprite", exception);
        }
    }

    private static Object bitsandbalance$getAtlasManager(Minecraft minecraft) {
        try {
            return minecraft.getClass().getMethod("getAtlasManager").invoke(minecraft);
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Object modelManager = minecraft.getClass().getMethod("getModelManager").invoke(minecraft);
            if (modelManager != null) {
                for (java.lang.reflect.Field field : modelManager.getClass().getDeclaredFields()) {
                    if (!field.getType().getName().equals("net.minecraft.client.resources.model.sprite.AtlasManager")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object atlasManager = field.get(modelManager);
                    if (atlasManager != null) {
                        return atlasManager;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }
}
