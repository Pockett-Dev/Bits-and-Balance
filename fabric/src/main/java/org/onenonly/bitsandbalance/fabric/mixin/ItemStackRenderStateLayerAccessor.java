package org.onenonly.bitsandbalance.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface ItemStackRenderStateLayerAccessor {

    @Accessor("quads")
    List<?> bitsandbalance$getQuads();

    @Mutable
    @Accessor("quads")
    void bitsandbalance$setQuads(List<?> quads);

    @Accessor("itemTransform")
    Object bitsandbalance$getItemTransform();

    @Mutable
    @Accessor("itemTransform")
    void bitsandbalance$setItemTransform(Object transform);

    @Invoker("prepareQuadList")
    List<?> bitsandbalance$prepareQuadList();
}
