package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface ItemStackRenderStateLayerAccessor {

    @Accessor("quads")
    List<?> bitsandbalance$getQuads();

    @Invoker("setItemTransform")
    void bitsandbalance$setItemTransform(ItemTransform transform);

    @Invoker("prepareQuadList")
    List<?> bitsandbalance$prepareQuadList();
}