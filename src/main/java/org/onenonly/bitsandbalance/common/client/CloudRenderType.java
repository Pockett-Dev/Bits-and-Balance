package org.onenonly.bitsandbalance.common.client;

//? if mc == 1.21.1
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
//? else
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//? endif

/**
 * Custom {@link RenderType} for the Bottle-of-Cloud display entity.
 * <p>
 * Uses the block-atlas texture with translucent alpha blending and the
 * current version's moving-block render path so it sorts like vanilla
 * translucent block-display geometry.
 */
public final class CloudRenderType {
    //? if mc == 1.21.1
    private static volatile RenderType instance;

    public static RenderType get() {
        if (instance == null) {
            synchronized (CloudRenderType.class) {
                if (instance == null) {
                    RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER)
                        .setTextureState(RenderStateShard.BLOCK_SHEET)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                        .createCompositeState(true);
                    instance = RenderType.create(
                        "bitsandbalance_cloud_no_depth",
                        DefaultVertexFormat.BLOCK,
                        VertexFormat.Mode.QUADS,
                        256,
                        true,
                        true,
                        state
                    );
                }
            }
        }
        return instance;
    }
    //? else
    public static RenderType get() {
        return RenderTypes.translucentMovingBlock();
    }
    //? endif

    private CloudRenderType() {
    }
}
