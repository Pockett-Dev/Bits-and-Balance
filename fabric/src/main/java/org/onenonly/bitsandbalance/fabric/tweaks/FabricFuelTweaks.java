package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port: Fuel Tweaks
 * Registers torches as fuel for furnaces.
 */
public final class FabricFuelTweaks {
    private FabricFuelTweaks() {
    }

    public static void init() {
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            if (FabricTweaksConfig.enableTorchFuel) {
                builder.add(Items.TORCH, FabricTweaksConfig.torchFuelBurnTime);
            }
        });
    }
}
