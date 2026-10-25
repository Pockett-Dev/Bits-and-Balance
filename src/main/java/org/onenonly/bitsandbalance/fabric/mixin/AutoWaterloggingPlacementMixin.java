package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(BlockItem.class)
public class AutoWaterloggingPlacementMixin {
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$prepareAutoWaterloggingPlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(AutoWaterloggingRuntime.preparePlacementState(
                    context.getLevel(),
                    context.getClickedPos(),
                    state
            ));
        }
    }

    @ModifyArgs(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private void bitsandbalance$prepareFinalAutoWaterloggingPlacementState(Args args) {
        BlockPlaceContext context = args.get(0);
        BlockState state = args.get(1);
        args.set(1, AutoWaterloggingRuntime.preparePlacementState(
                context.getLevel(),
                context.getClickedPos(),
                state
        ));
    }
}
