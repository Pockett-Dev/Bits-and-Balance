package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface ItemStackRenderStateLayerAccessor {

    @Accessor("quads")
    List<BakedQuad> bitsandbalance$getQuads();

    @Mutable
    @Accessor("quads")
    void bitsandbalance$setQuads(List<BakedQuad> quads);

    @Accessor("transform")
    ItemTransform bitsandbalance$getTransform();

    @Mutable
    @Accessor("transform")
    void bitsandbalance$setTransform(ItemTransform transform);
}
