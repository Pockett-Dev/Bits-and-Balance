package org.onenonly.bitsandbalance.fabric.tweaks;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
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
            ComposterBlock.COMPOSTABLES.put(Items.ROTTEN_FLESH, 0.3f);
        }
        if (FabricTweaksConfig.enableCompostablePoisonousPotato) {
            ComposterBlock.COMPOSTABLES.put(Items.POISONOUS_POTATO, 0.65f);
        }
    }
}
