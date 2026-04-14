package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class BioluminescenceLevelRendererMixin implements BioluminescenceLevelRendererAccess {
    @Inject(
            method = "getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$applyBioluminescenceDynamicLight(
            LevelRenderer.BrightnessGetter brightnessGetter,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(BioluminescenceLightManager.getLightmapWithDynamicLight(pos, cir.getReturnValue()));
    }

    @Invoker("setSectionDirty")
    protected abstract void bitsandbalance$invokeSetSectionDirty(int x, int y, int z, boolean important);

    @Override
    public void bitsandbalance$scheduleChunkRebuild(int x, int y, int z, boolean important) {
        bitsandbalance$invokeSetSectionDirty(x, y, z, important);
    }
}