package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {

    @Accessor("activeLayerCount")
    int bitsandbalance$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] bitsandbalance$getLayers();

    @Accessor("displayContext")
    ItemDisplayContext bitsandbalance$getDisplayContext();
}
