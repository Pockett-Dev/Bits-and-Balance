package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(TippedArrowRecipe.class)
public abstract class TippedArrowResultMixin {

    @Inject(
            method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true
    )
        private void bitsandbalance$modifyTippedArrowResult(CraftingInput input, HolderLookup.Provider provider, CallbackInfoReturnable<ItemStack> cir) {
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

        if (arrowCount != 8 || potionStack.isEmpty()) return;

        PotionContents contents = potionStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;
        Optional<Holder<Potion>> holderOpt = contents.potion();
        if (holderOpt.isEmpty()) return;

        TagKey<Potion> singleArrowOnly = TagKey.create(Registries.POTION, Identifier.fromNamespaceAndPath("bitsandbalance", "single_arrow_only"));
        if (holderOpt.get().is(singleArrowOnly)) {
            ItemStack result = new ItemStack(Items.TIPPED_ARROW, 1);
            result.set(DataComponents.POTION_CONTENTS, contents);
            cir.setReturnValue(result);
        }
    }
}
