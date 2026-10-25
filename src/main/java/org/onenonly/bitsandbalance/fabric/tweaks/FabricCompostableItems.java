package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port: Compostable Items
 * Registers Rotten Flesh and Poisonous Potatoes as compostable.
 */
public final class FabricCompostableItems {
    private FabricCompostableItems() {
    }

    public static void init() {
        if (!FabricTweaksConfig.enableCompostableItems) return;
        if (FabricTweaksConfig.enableCompostableRottenFlesh) {
            CompostingChanceRegistry.INSTANCE.add(Items.ROTTEN_FLESH, 0.3f);
        }
        if (FabricTweaksConfig.enableCompostablePoisonousPotato) {
            CompostingChanceRegistry.INSTANCE.add(Items.POISONOUS_POTATO, 0.65f);
        }
    }
}
