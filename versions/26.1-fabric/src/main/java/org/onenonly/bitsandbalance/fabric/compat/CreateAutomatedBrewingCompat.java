package org.onenonly.bitsandbalance.fabric.compat;

import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.compat.CreateCompat;
import org.onenonly.bitsandbalance.fabric.potions.FabricBrewingRecipeRegistrar;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateAutomatedBrewingCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitsAndBalanceCommon.MOD_ID);

    private CreateAutomatedBrewingCompat() {
    }

    public static void init() {
        if (!CreateCompat.isCreateFamilyLoaded()) {
            return;
        }

        if (FabricCustomBrewing.rules().isEmpty()) {
            FabricCustomBrewing.loadDefaults();
        }

        FabricBrewingRecipeRegistrar.registerWithMinecraftBrewing();
        LOGGER.info("[{}] Requested custom potion mix registration for Create automated brewing", BitsAndBalanceCommon.MOD_ID);
    }
}