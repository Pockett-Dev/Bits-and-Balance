package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.UsesForCursesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorViewBlockingStateMixin {
    @Inject(
            method = "getViewBlockingState(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void bitsandbalance$hideVanishingPumpkinViewBlockingState(
            LocalPlayer player,
            Frustum frustum,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState state = cir.getReturnValue();
        if (UsesForCursesClient.shouldHidePumpkinOverlayFromHead(FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)
                && state != null
                && state.getBlock() == Blocks.CARVED_PUMPKIN) {
            cir.setReturnValue(null);
        }
    }
}
