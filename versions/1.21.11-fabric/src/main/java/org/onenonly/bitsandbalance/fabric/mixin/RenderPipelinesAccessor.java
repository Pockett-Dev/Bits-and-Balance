package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the private {@code register} method on {@link RenderPipelines}
 * so that Fabric can register custom render pipelines.
 */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {

    @Invoker("register")
    static RenderPipeline bitsandbalance$register(RenderPipeline pipeline) {
        throw new AssertionError("Mixin injection failed");
    }
}
