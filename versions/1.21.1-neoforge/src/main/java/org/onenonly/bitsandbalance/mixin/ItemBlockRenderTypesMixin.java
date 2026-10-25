package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kept as a compatibility shim for old dev runs that referenced this mixin.
 * Item Sharing should not replace vanilla block item render types; forcing all block
 * items onto the translucent item sheet makes ordinary full-block models vanish.
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Inject(
        method = "getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void bitsandbalance$overrideItemRenderType(ItemStack stack, boolean renderCutout, CallbackInfoReturnable<RenderType> cir) {
    }

    @Inject(
		method = "getRenderType(Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/client/renderer/RenderType;",
        at = @At("HEAD"),
        cancellable = true,
		require = 0
    )
    private static void bitsandbalance$overrideRenderType(BlockState state, boolean renderCutout, CallbackInfoReturnable<RenderType> cir) {
    }
}
