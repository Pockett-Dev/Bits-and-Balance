package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
//? if mc != 1.21.1
import net.minecraft.server.level.ServerLevel;
//? endif
//? if mc == 1.21.1
import net.minecraft.world.level.Level;
//? endif
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Respawn Anchor Anywhere
 */
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {
    @Inject(method = "canSetSpawn", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$canSetSpawn(
//? if mc == 1.21.1
            Level level,
//? else
            ServerLevel level, BlockPos pos,
//? endif
            CallbackInfoReturnable<Boolean> cir) {
        if (FabricTweaksConfig.enableRespawnAnchorAnywhere) {
            cir.setReturnValue(true);
        }
    }
}
