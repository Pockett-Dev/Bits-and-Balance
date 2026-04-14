package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * Custom {@link RenderType} for the Bottle-of-Cloud display entity.
 * <p>
 * Uses the block-atlas texture with translucent alpha blending but
 * <strong>does not write to the depth buffer</strong>. This prevents
 * the cloud overlay from hiding translucent world geometry (water,
 * stained glass, etc.) that sits behind it.
 */
public final class CloudRenderType {

    /**
     * Custom pipeline: translucent + no depth write, BLOCK vertex format.
     * Re-uses the same vertex/fragment shaders as the vanilla
     * {@code translucent_moving_block} pipeline.
     */
    public static final RenderPipeline CLOUD_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("bitsandbalance", "pipeline/cloud_no_depth"))
            .withVertexShader("core/rendertype_translucent_moving_block")
            .withFragmentShader("core/rendertype_translucent_moving_block")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
            .build();

    /**
     * The finished {@link RenderType}. Safe to call after pipelines have been
     * registered (i.e. after the render system has initialised).
     */
    private static volatile RenderType INSTANCE;

    public static RenderType get() {
        if (INSTANCE == null) {
            synchronized (CloudRenderType.class) {
                if (INSTANCE == null) {
                    INSTANCE = RenderType.create(
                            "bitsandbalance_cloud_no_depth",
                            RenderSetup.builder(CLOUD_PIPELINE)
                                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                                    .useLightmap()
                                    .sortOnUpload()
                                    .bufferSize(256)
                                    .createRenderSetup()
                    );
                }
            }
        }
        return INSTANCE;
    }

    private CloudRenderType() {}
}
