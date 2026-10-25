package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
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
        ItemStack ingredient = be.getItem(3);
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (ingredient.getItem() != Items.DRAGON_BREATH) {
        //? else
        if (!ingredient.is(Items.DRAGON_BREATH)) {
        //? endif
            return;
        }

        for (int i = 0; i < 3; i++) {
            ItemStack bottle = be.getItem(i);
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            if (bottle.getItem() == Items.SPLASH_POTION) {
            //? else
            if (bottle.is(Items.SPLASH_POTION)) {
            //? endif
                PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    Optional<net.minecraft.core.Holder<Potion>> potionHolder = contents.potion();
                    if (potionHolder.isPresent()) {
                        Potion potion = potionHolder.get().value();
                        //? if mc == 1.21.1
                        ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
                        //? else
                        Identifier potionId = BuiltInRegistries.POTION.getKey(potion);
                        //? endif
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
