package org.onenonly.bitsandbalance.enchantments;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Resource keys for Bits and Balance enchantments.
 *
 * Note: In 1.21+, enchantments are data-driven (JSON). This class only provides keys for lookups.
 */
public final class ModEnchantments {
    private ModEnchantments() {}

    public static final ResourceKey<Enchantment> AERODYNAMIC = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "aerodynamic")
    );
}
