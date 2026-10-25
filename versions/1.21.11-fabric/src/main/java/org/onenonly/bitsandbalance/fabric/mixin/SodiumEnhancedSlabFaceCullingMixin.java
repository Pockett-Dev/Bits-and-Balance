package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFaceCullingHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class SodiumEnhancedSlabFaceCullingMixin {
    @Shadow(remap = false)
    protected BlockAndTintGetter level;

    @Shadow(remap = false)
    protected BlockState state;

    @Shadow(remap = false)
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$forceFacesAroundEnhancedSlabs(Direction direction,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || this.level == null || this.pos == null || this.state == null || direction == null) {
            return;
        }

        BlockPos adjacentPos = this.pos.relative(direction);
        if (EnhancedSlabFaceCullingHelper.shouldForceRenderFace(this.level, this.pos, this.state, direction, adjacentPos)) {
            cir.setReturnValue(true);
        }
    }
}
