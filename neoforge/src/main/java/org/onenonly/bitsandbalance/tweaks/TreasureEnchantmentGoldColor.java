package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.Holder;
import org.onenonly.bitsandbalance.Config;

/**
 * Treasure Enchantment Gold Color Tweak
 * 
 * Makes beneficial treasure enchantments display in gold color.
 * Treasure enchantments are those that cannot be obtained through
 * enchanting tables and must be found as loot or through other means.
 * 
 * Gold-colored treasure enchantments:
 * - Mending
 * - Frost Walker
 * - Soul Speed
 * - Swift Sneak
 * 
 * Note: Curses (Vanishing, Binding) remain red to indicate their negative nature.
 */
public class TreasureEnchantmentGoldColor {

    private static final TagKey<Enchantment> TREASURE_TAG = TagKey.create(
        Registries.ENCHANTMENT,
        Identifier.withDefaultNamespace("treasure")
    );

    private static final TagKey<Enchantment> CURSE_TAG = TagKey.create(
        Registries.ENCHANTMENT,
        Identifier.withDefaultNamespace("curse")
    );
    
    /**
     * Checks if the given enchantment is a treasure enchantment that should be colored gold.
     * Excludes curses, which maintain their red color.
     * @param enchantmentHolder The enchantment holder to check
     * @return true if the enchantment is a treasure enchantment that should be gold
     */
    public static boolean isTreasureEnchantment(Holder<Enchantment> enchantmentHolder) {
        if (!Config.enableTreasureEnchantmentGoldColor) {
            return false;
        }

        // Gold-color any treasure enchantment, excluding curses.
        // This automatically includes modded treasure enchants (e.g., Aerodynamic) as long as they are in the
        // minecraft:treasure enchantment tag.
        return enchantmentHolder.is(TREASURE_TAG) && !enchantmentHolder.is(CURSE_TAG);
    }
    
}
