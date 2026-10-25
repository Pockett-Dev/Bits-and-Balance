package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.onenonly.bitsandbalance.common.config.BalanceFeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Balance: Nerfed Mending
 *
 * Makes Mending require more XP for the same amount of repair.
 */
@Mixin(EnchantmentHelper.class)
public abstract class NerfedMendingMixin {

    @Inject(
            method = "modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void bitsandbalance$nerfedMending$scaleRepair(
            ServerLevel level,
            ItemStack stack,
            int xp,
            CallbackInfoReturnable<Integer> cir
    ) {
        int durabilityToRepair = cir.getReturnValue();
        if (!BalanceFeatureFlags.nerfedMendingEnabled() || durabilityToRepair <= 0) return;

        int multiplier = BalanceFeatureFlags.nerfedMendingMultiplier();
        if (multiplier < 2) multiplier = 2;
        if (multiplier > 10) multiplier = 10;

        // Divide repaired durability so the same repair requires N times more XP.
        // Clamp to 1 to avoid returning 0 and breaking the repair math.
        int scaled = durabilityToRepair / multiplier;
        cir.setReturnValue(Math.max(1, scaled));
    }
}
