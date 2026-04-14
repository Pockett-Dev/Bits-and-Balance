package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Blocks creation of lingering potions for Displacement, Returning, and Resurfacing.
 */
@Mixin(BrewingStandBlockEntity.class)
public class LingeringPotionBlockMixin {

    private static final String[] BLOCKED_POTIONS = {
            "bitsandbalance:displacement",
            "bitsandbalance:returning",
            "bitsandbalance:resurfacing"
    };

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$blockLingeringPotions(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        ItemStack ingredient = be.getItem(3);
        if (ingredient.getItem() != Items.DRAGON_BREATH) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            ItemStack bottle = be.getItem(i);
            if (bottle.getItem() == Items.SPLASH_POTION) {
                PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    Optional<Holder<Potion>> potionHolder = contents.potion();
                    if (potionHolder.isPresent()) {
                        Potion potion = potionHolder.get().value();
                        Identifier potionId = BuiltInRegistries.POTION.getKey(potion);
                        if (potionId != null) {
                            for (String blockedPotion : BLOCKED_POTIONS) {
                                if (potionId.toString().equals(blockedPotion)) {
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
