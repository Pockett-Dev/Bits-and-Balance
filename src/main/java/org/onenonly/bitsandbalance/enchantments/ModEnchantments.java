package org.onenonly.bitsandbalance.enchantments;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
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
            //? if mc == 1.21.1
            ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "aerodynamic")
            //? else
            Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "aerodynamic")
            //? endif
    );
}
