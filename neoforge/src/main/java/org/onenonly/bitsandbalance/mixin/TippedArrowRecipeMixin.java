package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(TippedArrowRecipe.class)
public abstract class TippedArrowRecipeMixin {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockForOurPotions(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is a tipped arrow recipe by looking for 8 arrows and 1 lingering potion
        int arrowCount = 0;
        ItemStack potionStack = ItemStack.EMPTY;
        
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty()) {
                if (s.is(Items.ARROW)) {
                    arrowCount++;
                } else if (s.is(Items.LINGERING_POTION)) {
                    potionStack = s;
                }
            }
        }
        
        // Only proceed if this looks like the vanilla 8->8 tipped arrow recipe
        if (arrowCount != 8 || potionStack.isEmpty()) return;

        // Read potion contents component
        PotionContents contents = potionStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;
        Optional<Holder<Potion>> holderOpt = contents.potion();
        if (holderOpt.isEmpty()) return;

        // If this potion is tagged as single_arrow_only, do not allow vanilla 8->8 to match
        TagKey<Potion> singleArrowOnly = TagKey.create(Registries.POTION, Identifier.fromNamespaceAndPath("bitsandbalance", "single_arrow_only"));
        if (holderOpt.get().is(singleArrowOnly)) {
            cir.setReturnValue(false);
        }
    }
}
