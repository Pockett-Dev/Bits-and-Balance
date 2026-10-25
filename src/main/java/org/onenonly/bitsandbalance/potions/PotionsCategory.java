package org.onenonly.bitsandbalance.potions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Creative tab (category) for Potions-related content.
 */
public class PotionsCategory {
    // Register the Potions tab via the main mod's CreativeModeTab deferred register
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POTIONS_TAB = org.onenonly.bitsandbalance.BitsAndBalance.CREATIVE_MODE_TABS.register(
            "potions",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bitsandbalance.potions"))
                    .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
                    .icon(() -> potionStack(Items.POTION, "withering"))
                    .displayItems((parameters, output) -> {
                        // Regular potions
                        output.accept(potionStack(Items.POTION, "withering"));
                        output.accept(potionStack(Items.POTION, "long_withering"));
                        output.accept(potionStack(Items.POTION, "strong_withering"));
                        output.accept(potionStack(Items.POTION, "resurfacing"));
                        output.accept(potionStack(Items.POTION, "displacement"));
                        output.accept(potionStack(Items.POTION, "returning"));
                        output.accept(potionStack(Items.POTION, "levitation"));
                        output.accept(potionStack(Items.POTION, "long_levitation"));
                        output.accept(potionStack(Items.POTION, "strong_levitation"));
                        output.accept(potionStack(Items.POTION, "haste"));
                        output.accept(potionStack(Items.POTION, "long_haste"));
                        output.accept(potionStack(Items.POTION, "strong_haste"));
                        // Splash potions
                        output.accept(potionStack(Items.SPLASH_POTION, "withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "resurfacing"));
                        output.accept(potionStack(Items.SPLASH_POTION, "displacement"));
                        output.accept(potionStack(Items.SPLASH_POTION, "returning"));
                        output.accept(potionStack(Items.SPLASH_POTION, "levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "haste"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_haste"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_haste"));
                        // Lingering potions
                        output.accept(potionStack(Items.LINGERING_POTION, "withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "resurfacing"));
                        output.accept(potionStack(Items.LINGERING_POTION, "displacement"));
                        output.accept(potionStack(Items.LINGERING_POTION, "returning"));
                        output.accept(potionStack(Items.LINGERING_POTION, "levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "haste"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_haste"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_haste"));
                    })
                    .build()
    );

    private static ItemStack potionStack(Item baseItem, String id) {
        var rl = Identifier.parse(BitsAndBalance.MODID + ":" + id);
        var holder = BuiltInRegistries.POTION.get(rl).orElse(null);
        if (holder == null) {
            return new ItemStack(baseItem);
        }
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(baseItem, holder);
    }

    /**
     * Ensures the class is initialized so that the tab is registered.
     */
    public static void register() {
        // no-op; referencing this method from Rebalance guarantees class loading
    }
}
