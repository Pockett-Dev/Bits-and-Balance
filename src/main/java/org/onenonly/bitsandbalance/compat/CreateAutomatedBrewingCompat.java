package org.onenonly.bitsandbalance.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.potions.CustomBrewing;

public final class CreateAutomatedBrewingCompat {
    private CreateAutomatedBrewingCompat() {
    }

    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        if (!CreateCompat.isCreateFamilyLoaded()) {
            return;
        }

        if (CustomBrewing.rules().isEmpty()) {
            CustomBrewing.loadDefaults();
        }

        int registered = 0;
        for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
            Holder<Potion> inputPotion = bitsandbalance$potion(rule.inputPotion());
            Item reagent = bitsandbalance$item(rule.reagentItem());
            Holder<Potion> resultPotion = bitsandbalance$potion(rule.resultPotion());
            if (inputPotion == null || reagent == null || resultPotion == null) {
                BitsAndBalance.LOGGER.debug("[{}] Skipping Create automated brewing rule with missing registry entry: {}", BitsAndBalance.MODID, rule);
                continue;
            }

            event.getBuilder().addMix(inputPotion, reagent, resultPotion);
            registered++;
        }

        if (registered > 0) {
            BitsAndBalance.LOGGER.info("[{}] Registered {} custom potion mix(es) for Create automated brewing", BitsAndBalance.MODID, registered);
        }
    }

    private static Holder<Potion> bitsandbalance$potion(Identifier id) {
        return BuiltInRegistries.POTION.get(id).orElse(null);
    }

    private static Item bitsandbalance$item(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(null);
    }
}