package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Vertex.class)
public interface ModelPartVertexAccessor {
    @Accessor("u")
    float bitsandbalance$getU();

    @Accessor("v")
    float bitsandbalance$getV();
}
