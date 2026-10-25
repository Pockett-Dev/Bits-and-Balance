package org.onenonly.bitsandbalance.fabric.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "com.simibubi.create.content.contraptions.render.ContraptionVisual$1",
        "com.zurrtum.create.client.content.contraptions.render.ContraptionVisual$1"
}, remap = false)
public abstract class CreateContraptionVisualEnhancedSlabModelContextMixin {
    @Inject(method = {"getBlockState", "method_8320"}, at = @At("RETURN"), remap = false, require = 0)
    private void bitsandbalance$rememberEnhancedSlabModelContext(BlockPos pos,
                                                                 CallbackInfoReturnable<BlockState> cir) {
        EnhancedSlabTerrainRenderHelper.rememberCreateModelContext(
                (BlockAndTintGetter) (Object) this,
                pos,
                cir.getReturnValue()
        );
    }
}
