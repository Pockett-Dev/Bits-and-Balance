package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
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
        DefaultItemComponentEvents.MODIFY.register(context -> {
            if (FabricTweaksConfig.enableCompostableRottenFlesh) {
                context.modify(Items.ROTTEN_FLESH, builder -> builder.set(
                        DataComponents.COMPOSTABLE,
                        new Compostable(ContextIntProviders.COMPOSTABLE_LOW)
                ));
            }
            if (FabricTweaksConfig.enableCompostablePoisonousPotato) {
                context.modify(Items.POISONOUS_POTATO, builder -> builder.set(
                        DataComponents.COMPOSTABLE,
                        new Compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)
                ));
            }
        });
    }
}
