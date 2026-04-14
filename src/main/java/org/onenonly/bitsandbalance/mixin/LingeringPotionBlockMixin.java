package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Mixin to block the creation of lingering potions for Displacement, Returning, and Resurfacing effects.
 * This prevents players from brewing lingering versions of these potions while keeping regular and splash versions.
 */
@Mixin(BrewingStandBlockEntity.class)
public class LingeringPotionBlockMixin {
    
    private static final String[] BLOCKED_POTIONS = {
        "bitsandbalance:displacement",
        "bitsandbalance:returning", 
        "bitsandbalance:resurfacing"
    };
    
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$blockLingeringPotions(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        // Check if the brewing stand has dragon breath as the ingredient
        ItemStack ingredient = be.getItem(3);
        if (ingredient.getItem() != Items.DRAGON_BREATH) {
            return; // Not a lingering potion brewing recipe
        }
        
        // Check each bottle slot for splash potions that should be blocked
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = be.getItem(i);
            if (bottle.getItem() == Items.SPLASH_POTION) {
                PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    Optional<net.minecraft.core.Holder<Potion>> potionHolder = contents.potion();
                    if (potionHolder.isPresent()) {
                        Potion potion = potionHolder.get().value();
                        Identifier potionId = BuiltInRegistries.POTION.getKey(potion);
                        if (potionId != null) {
                            // Check if this potion should be blocked from becoming lingering
                            for (String blockedPotion : BLOCKED_POTIONS) {
                                if (potionId.toString().equals(blockedPotion)) {
                                    // Cancel the brewing process entirely - do nothing
                                    ci.cancel();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
