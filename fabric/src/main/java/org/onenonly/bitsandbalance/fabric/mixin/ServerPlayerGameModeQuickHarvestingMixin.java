package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricQuickHarvesting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerGameMode.class, priority = 1500)
public class ServerPlayerGameModeQuickHarvestingMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$handleQuickHarvesting(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricMechanicsConfig.enableQuickHarvesting) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!FabricQuickHarvesting.shouldHandleInteraction(stack, clickedState)) {
            return;
        }

        if (FabricQuickHarvesting.tryHandleInteraction(player, level, hand, clickedPos, clickedState)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}