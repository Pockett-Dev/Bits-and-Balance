package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.onenonly.bitsandbalance.common.inventory.AutomaticBlockRestock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.mixin.InventoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class AutomaticBlockRestockItemStackMixin {

    @Unique
    private ItemStack bitsandbalance$blockRestockOriginal;

    @Unique
    private int bitsandbalance$blockRestockSlot = -1;

    @Inject(method = "useOn", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureOriginalBeforeUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // Keep overhead minimal.
        if (!FabricTweaksConfig.enableAutomaticBlockRestock) {
            this.bitsandbalance$blockRestockOriginal = null;
            this.bitsandbalance$blockRestockSlot = -1;
            return;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            this.bitsandbalance$blockRestockOriginal = null;
            this.bitsandbalance$blockRestockSlot = -1;
            return;
        }

        ItemStack stack;
        try {
            stack = context.getItemInHand();
        } catch (Throwable ignored) {
            this.bitsandbalance$blockRestockOriginal = null;
            this.bitsandbalance$blockRestockSlot = -1;
            return;
        }

        if (stack.isEmpty()) {
            this.bitsandbalance$blockRestockOriginal = null;
            this.bitsandbalance$blockRestockSlot = -1;
            return;
        }

        try {
            int selected = ((InventoryAccessor) (Object) player.getInventory()).bitsandbalance$getSelected();
            if (selected < 0 || selected > 8) {
                this.bitsandbalance$blockRestockOriginal = null;
                this.bitsandbalance$blockRestockSlot = -1;
                return;
            }

            // Store the original stack and slot.
            this.bitsandbalance$blockRestockOriginal = stack.copy();
            this.bitsandbalance$blockRestockSlot = selected;
        } catch (Throwable ignored) {
            this.bitsandbalance$blockRestockOriginal = null;
            this.bitsandbalance$blockRestockSlot = -1;
        }
    }

    @Inject(method = "useOn", at = @At("RETURN"), require = 0)
    private void bitsandbalance$tryRestockAfterUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack original = this.bitsandbalance$blockRestockOriginal;
        int slot = this.bitsandbalance$blockRestockSlot;
        this.bitsandbalance$blockRestockOriginal = null;
        this.bitsandbalance$blockRestockSlot = -1;

        if (!FabricTweaksConfig.enableAutomaticBlockRestock) return;
        if (original == null || original.isEmpty()) return;
        if (slot < 0 || slot > 8) return;

        // Only restock if the placement was successful.
        InteractionResult result = cir.getReturnValue();
        if (result != InteractionResult.SUCCESS && result != InteractionResult.CONSUME) {
            return;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack current;
        try {
            current = player.getInventory().getItem(slot);
        } catch (Throwable ignored) {
            return;
        }

        // Only restock if the slot is now empty.
        if (current != null && !current.isEmpty()) return;

        AutomaticBlockRestock.tryRestockEmptyHotbarBlock(player, slot, original);
    }
}
