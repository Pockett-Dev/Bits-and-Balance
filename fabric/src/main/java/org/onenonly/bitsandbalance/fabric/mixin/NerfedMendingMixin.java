package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Balance: Nerfed Mending
 *
 * Makes Mending require more XP for the same amount of repair.
 */
@Mixin(ExperienceOrb.class)
public abstract class NerfedMendingMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-nerfed-mending");

    @Inject(method = "repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I", at = @At("HEAD"))
    private void bitsandbalance$nerfedMending$logEntry(net.minecraft.server.level.ServerPlayer player, int xp, CallbackInfoReturnable<Integer> cir) {
        LOGGER.info("Nerfed Mending repairPlayerItems entered: player={} xp={} enabled={} multiplier={}",
                player.getScoreboardName(),
                xp,
                FabricBalanceConfig.enableNerfedMending,
                FabricBalanceConfig.nerfedMendingMultiplier);
    }

    @WrapOperation(
            method = "repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
        ),
        require = 1
    )
    private int bitsandbalance$nerfedMending$wrapRepair(ServerLevel level, ItemStack stack, int xp, Operation<Integer> original) {
        int durabilityToRepair = original.call(level, stack, xp);
    if (!FabricBalanceConfig.enableNerfedMending) {
        LOGGER.info("Nerfed Mending bypassed: enabled=false xp={} item={} damage={} vanillaRepair={} multiplier={}",
            xp,
            BuiltInRegistries.ITEM.getKey(stack.getItem()),
            stack.getDamageValue(),
            durabilityToRepair,
            FabricBalanceConfig.nerfedMendingMultiplier);
        return durabilityToRepair;
    }
    if (durabilityToRepair <= 0) {
        LOGGER.info("Nerfed Mending no-op: xp={} item={} damage={} vanillaRepair={} multiplier={}",
            xp,
            BuiltInRegistries.ITEM.getKey(stack.getItem()),
            stack.getDamageValue(),
            durabilityToRepair,
            FabricBalanceConfig.nerfedMendingMultiplier);
        return durabilityToRepair;
    }

        int multiplier = FabricBalanceConfig.nerfedMendingMultiplier;
        if (multiplier < 2) multiplier = 2;
        if (multiplier > 10) multiplier = 10;

        int scaled = durabilityToRepair / multiplier;
    int result = Math.max(1, scaled);
    LOGGER.info("Nerfed Mending applied: xp={} item={} damage={} vanillaRepair={} scaledRepair={} multiplier={}",
        xp,
        BuiltInRegistries.ITEM.getKey(stack.getItem()),
        stack.getDamageValue(),
        durabilityToRepair,
        result,
        multiplier);
    return result;
    }
}
