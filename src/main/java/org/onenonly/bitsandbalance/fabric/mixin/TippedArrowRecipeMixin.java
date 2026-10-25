package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingInput;
//? if mc == 26.3
import net.minecraft.world.item.crafting.ImbueRecipe;
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.world.item.crafting.TippedArrowRecipe;
//? endif
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

//? if mc == 26.3
@Mixin(ImbueRecipe.class)
//? endif
//? if mc == 26.1 || mc == 26.2
@Mixin(targets = "net.minecraft.world.item.crafting.TippedArrowRecipe")
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
@Mixin(TippedArrowRecipe.class)
//? endif
public abstract class TippedArrowRecipeMixin {

    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true
    )
    private void bitsandbalance$blockForOurPotions(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        int arrowCount = 0;
        ItemStack potionStack = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.ARROW) {
                arrowCount++;
            } else if (stack.getItem() == Items.LINGERING_POTION) {
                potionStack = stack;
            }
        }

        if (arrowCount != 8 || potionStack.isEmpty()) return;

        PotionContents contents = potionStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;
        Optional<Holder<Potion>> holderOpt = contents.potion();
        if (holderOpt.isEmpty()) return;

        TagKey<Potion> singleArrowOnly = TagKey.create(Registries.POTION,
//? if mc == 1.21.1
                ResourceLocation.fromNamespaceAndPath("bitsandbalance", "single_arrow_only")
//? else
                Identifier.fromNamespaceAndPath("bitsandbalance", "single_arrow_only")
//? endif
        );
        if (holderOpt.get().is(singleArrowOnly)) {
            cir.setReturnValue(false);
        }
    }
}
