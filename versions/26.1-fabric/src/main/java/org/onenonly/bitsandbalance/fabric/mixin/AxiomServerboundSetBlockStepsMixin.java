package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.moulberry.axiom.packets.AxiomServerboundSetBlock")
public abstract class AxiomServerboundSetBlockStepsMixin {

    @Shadow
    private boolean breaking;

    @Shadow
    private BlockHitResult blockHit;

    @Shadow
    private InteractionHand hand;

    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/context/BlockPlaceContext;<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/BlockHitResult;)V"
            ),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$handleStepsThroughNormalPlacement(MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        if (this.breaking) return;
        if (!FabricTweaksConfig.isStepRuntimeEnabled()) return;

        ItemStack stack = player.getItemInHand(this.hand);
        if (!bitsandbalance$isStepItem(stack)) return;

        InteractionResult result = stack.useOn(new UseOnContext(player, this.hand, this.blockHit));
        if (result != InteractionResult.PASS) {
            ci.cancel();
        }
    }

    private static boolean bitsandbalance$isStepItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        if (blockItem.getBlock() instanceof FixedStepBlock || blockItem.getBlock() instanceof FixedVerticalStepBlock) {
            return true;
        }

        return StepDynamicRegistry.getSlabForStep(blockItem.getBlock()) != null
                || VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(blockItem.getBlock()) != null;
    }
}