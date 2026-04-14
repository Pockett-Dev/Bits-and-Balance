package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Balance: Nerfed Mending
 *
 * Makes Mending require more XP for the same amount of repair.
 */
@Mixin(ExperienceOrb.class)
public abstract class NerfedMendingMixin {

    @Redirect(
            method = "repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
            )
    )
    private int bitsandbalance$nerfedMending$modifyDurabilityToRepairFromXp(ServerLevel level, ItemStack stack, int xp) {
        int durabilityToRepair = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, stack, xp);
        if (!Config.enableNerfedMending) return durabilityToRepair;
        if (durabilityToRepair <= 0) return durabilityToRepair;

        int multiplier = Config.nerfedMendingMultiplier;
        if (multiplier < 2) multiplier = 2;
        if (multiplier > 10) multiplier = 10;

        // Divide repaired durability so the same repair requires N times more XP.
        // Clamp to 1 to avoid returning 0 and breaking the repair math.
        int scaled = durabilityToRepair / multiplier;
        return Math.max(1, scaled);
    }
}
