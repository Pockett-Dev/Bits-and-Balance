package org.onenonly.bitsandbalance.fabric.tweaks;

import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.compat.FabricCookingFuelRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port: Fuel Tweaks
 * Registers torches as fuel for furnaces.
 */
public final class FabricFuelTweaks {
    private FabricFuelTweaks() {
    }

    public static void init() {
        if (FabricTweaksConfig.enableTorchFuel) {
            FabricCookingFuelRegistry.register(Items.TORCH, FabricTweaksConfig.torchFuelBurnTime);
        }
    }
}
