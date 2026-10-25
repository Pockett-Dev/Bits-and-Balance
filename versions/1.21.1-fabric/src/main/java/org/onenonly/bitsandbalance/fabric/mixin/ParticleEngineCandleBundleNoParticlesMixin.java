package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.registry.FabricCandleBundleContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCandleBundleNoParticlesMixin {

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$noHitParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state != null && state.is(FabricCandleBundleContent.BUNDLE_CANDLE)) {
            ci.cancel();
        }
    }
}