package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeDreamMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostMining(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                                  Direction direction, int maxBuildHeight, int sequence,
                                                  CallbackInfo ci) {
        if (DreamProjection.isProjected(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (DreamProjection.isProjected(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostItem(ServerPlayer user, Level level, ItemStack stack,
                                                InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (DreamProjection.isProjected(user)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostBlockUse(ServerPlayer user, Level level, ItemStack stack,
                                                    InteractionHand hand, BlockHitResult hit,
                                                    CallbackInfoReturnable<InteractionResult> cir) {
        if (DreamProjection.isProjected(user)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
