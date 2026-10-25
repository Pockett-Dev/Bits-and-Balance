package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class UnbreakableTrialSpawnerBlockStateMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockTrialSpawnerDestroyProgress(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        BlockState state = (BlockState) (Object) this;
        if (isProtected(state) && player != null && !player.isCreative()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$ignoreTrialSpawnerExplosions(ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer, CallbackInfo ci) {
        BlockState state = (BlockState) (Object) this;
        if (isProtected(state)) {
            ci.cancel();
        }
    }

    private static boolean isProtected(BlockState state) {
        return (FabricTweaksConfig.enableUnbreakableTrialSpawners && state.is(Blocks.TRIAL_SPAWNER))
                || (FabricTweaksConfig.enableUnbreakableVaults && state.is(Blocks.VAULT));
    }
}