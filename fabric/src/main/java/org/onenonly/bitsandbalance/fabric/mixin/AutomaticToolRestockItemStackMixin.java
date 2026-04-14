package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.inventory.AutomaticToolRestock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.mixin.InventoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class AutomaticToolRestockItemStackMixin {

    @Unique
    private ItemStack bitsandbalance$restockOriginal;

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureOriginalBeforeBreak(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        // Keep overhead minimal.
        if (!FabricTweaksConfig.enableAutomaticToolRestock) {
            this.bitsandbalance$restockOriginal = null;
            return;
        }

        if (!(entity instanceof ServerPlayer player)) {
            this.bitsandbalance$restockOriginal = null;
            return;
        }
        if (slot != EquipmentSlot.MAINHAND) {
            this.bitsandbalance$restockOriginal = null;
            return;
        }

        try {
            int selected = ((InventoryAccessor) (Object) player.getInventory()).bitsandbalance$getSelected();
            if (selected < 0 || selected > 8) {
                this.bitsandbalance$restockOriginal = null;
                return;
            }
        } catch (Throwable ignored) {
            this.bitsandbalance$restockOriginal = null;
            return;
        }

        try {
            ItemStack self = (ItemStack) (Object) this;
            if (self.isEmpty() || !self.isDamageableItem()) {
                this.bitsandbalance$restockOriginal = null;
                return;
            }
            this.bitsandbalance$restockOriginal = self.copy();
        } catch (Throwable ignored) {
            this.bitsandbalance$restockOriginal = null;
        }
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("TAIL"), require = 0)
    private void bitsandbalance$tryRestockAfterBreak(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        ItemStack original = this.bitsandbalance$restockOriginal;
        this.bitsandbalance$restockOriginal = null;

        if (!FabricTweaksConfig.enableAutomaticToolRestock) return;
        if (original == null || original.isEmpty()) return;

        if (!(entity instanceof ServerPlayer player)) return;
        if (slot != EquipmentSlot.MAINHAND) return;

        ItemStack self;
        try {
            self = (ItemStack) (Object) this;
        } catch (Throwable ignored) {
            return;
        }

        // Only run if the tool actually broke.
        if (self != null && !self.isEmpty()) return;

        int selected;
        try {
            selected = ((InventoryAccessor) (Object) player.getInventory()).bitsandbalance$getSelected();
        } catch (Throwable ignored) {
            return;
        }
        if (selected < 0 || selected > 8) return;

        AutomaticToolRestock.tryRestockBrokenHotbarTool(player, selected, original);
    }
}
