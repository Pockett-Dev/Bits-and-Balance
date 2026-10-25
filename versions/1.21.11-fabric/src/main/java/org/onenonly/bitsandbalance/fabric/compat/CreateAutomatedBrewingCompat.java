package org.onenonly.bitsandbalance.fabric.compat;

import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.compat.CreateCompat;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateAutomatedBrewingCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitsAndBalanceCommon.MOD_ID);

    private CreateAutomatedBrewingCompat() {
    }

    public static void init() {
        FabricBrewingRecipeRegistryBuilder.BUILD.register(CreateAutomatedBrewingCompat::onBuildBrewingRegistry);
    }

    private static void onBuildBrewingRegistry(PotionBrewing.Builder builder) {
        if (!CreateCompat.isCreateFamilyLoaded()) {
            return;
        }

        if (FabricCustomBrewing.rules().isEmpty()) {
            FabricCustomBrewing.loadDefaults();
        }

        int registered = 0;
        for (FabricCustomBrewing.Rule rule : FabricCustomBrewing.rules()) {
            Holder<Potion> inputPotion = bitsandbalance$potion(rule.inputPotion());
            Item reagent = bitsandbalance$item(rule.reagentItem());
            Holder<Potion> resultPotion = bitsandbalance$potion(rule.resultPotion());
            if (inputPotion == null || reagent == null || resultPotion == null) {
                LOGGER.debug("[{}] Skipping Create automated brewing rule with missing registry entry: {}", BitsAndBalanceCommon.MOD_ID, rule);
                continue;
            }

            builder.addMix(inputPotion, reagent, resultPotion);
            registered++;
        }

        if (registered > 0) {
            LOGGER.info("[{}] Registered {} custom potion mix(es) for Create automated brewing", BitsAndBalanceCommon.MOD_ID, registered);
        }
    }

    private static Holder<Potion> bitsandbalance$potion(Identifier id) {
        return BuiltInRegistries.POTION.get(id).orElse(null);
    }

    private static Item bitsandbalance$item(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(null);
    }
}