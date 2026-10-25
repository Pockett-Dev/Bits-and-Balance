package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.registry.FabricCandleBundleContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelCandleBundleNoParticlesMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$noDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (state != null && state.is(FabricCandleBundleContent.BUNDLE_CANDLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$noHitParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        BlockState state = level.getBlockState(pos);
        if (state != null && state.is(FabricCandleBundleContent.BUNDLE_CANDLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$noBreakProgressCracks(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        if (progress < 0) {
            return;
        }

        ClientLevel level = (ClientLevel) (Object) this;
        BlockState state = level.getBlockState(pos);
        if (state != null && state.is(FabricCandleBundleContent.BUNDLE_CANDLE)) {
            ci.cancel();
        }
    }
}
