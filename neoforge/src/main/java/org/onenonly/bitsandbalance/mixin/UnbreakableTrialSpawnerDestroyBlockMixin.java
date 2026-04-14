package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class UnbreakableTrialSpawnerDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow @Final protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$preventTrialSpawnerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.isCreative()) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (isProtected(state)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isProtected(BlockState state) {
        return (Config.enableUnbreakableTrialSpawners && state.is(Blocks.TRIAL_SPAWNER))
                || (Config.enableUnbreakableVaults && state.is(Blocks.VAULT));
    }
}