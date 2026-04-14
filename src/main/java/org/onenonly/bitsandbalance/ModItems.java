package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.item.AzaleaBoatItem;

/**
 * Registers custom items for the Bits and Balance mod.
 */
public class ModItems {
        private static Item.Properties props(Identifier key) {
                return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key));
        }

    public static final DeferredHolder<Item, BlockItem> BUNDLE_CANDLE = BitsAndBalance.ITEMS.register("bundle_candle",
            (Identifier key) -> new BlockItem(ModBlocks.BUNDLE_CANDLE.get(), props(key)));

    // Azalea Wood Items
    public static final DeferredHolder<Item, BlockItem> AZALEA_LOG = BitsAndBalance.ITEMS.register("azalea_log",
                        (Identifier key) -> new BlockItem(ModBlocks.AZALEA_LOG.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_WOOD = BitsAndBalance.ITEMS.register("azalea_wood",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_WOOD.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> STRIPPED_AZALEA_LOG = BitsAndBalance.ITEMS.register("stripped_azalea_log",
            (Identifier key) -> new BlockItem(ModBlocks.STRIPPED_AZALEA_LOG.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> STRIPPED_AZALEA_WOOD = BitsAndBalance.ITEMS.register("stripped_azalea_wood",
            (Identifier key) -> new BlockItem(ModBlocks.STRIPPED_AZALEA_WOOD.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_PLANKS = BitsAndBalance.ITEMS.register("azalea_planks",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_PLANKS.get(), props(key)));

    // Azalea Wood Derivatives
    public static final DeferredHolder<Item, BlockItem> AZALEA_SLAB = BitsAndBalance.ITEMS.register("azalea_slab",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_SLAB.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_STAIRS = BitsAndBalance.ITEMS.register("azalea_stairs",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_STAIRS.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_BUTTON = BitsAndBalance.ITEMS.register("azalea_button",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_BUTTON.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_PRESSURE_PLATE = BitsAndBalance.ITEMS.register("azalea_pressure_plate",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_PRESSURE_PLATE.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_FENCE = BitsAndBalance.ITEMS.register("azalea_fence",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_FENCE.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_FENCE_GATE = BitsAndBalance.ITEMS.register("azalea_fence_gate",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_FENCE_GATE.get(), props(key)));

    public static final DeferredHolder<Item, BlockItem> AZALEA_SHELF = BitsAndBalance.ITEMS.register("azalea_shelf",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_SHELF.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_DOOR = BitsAndBalance.ITEMS.register("azalea_door",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_DOOR.get(), props(key)));
    
    public static final DeferredHolder<Item, BlockItem> AZALEA_TRAPDOOR = BitsAndBalance.ITEMS.register("azalea_trapdoor",
            (Identifier key) -> new BlockItem(ModBlocks.AZALEA_TRAPDOOR.get(), props(key)));
    
    public static final DeferredHolder<Item, SignItem> AZALEA_SIGN = BitsAndBalance.ITEMS.register("azalea_sign",
            (Identifier key) -> new SignItem(ModBlocks.AZALEA_SIGN.get(), ModBlocks.AZALEA_WALL_SIGN.get(), props(key).stacksTo(16)));
    
    public static final DeferredHolder<Item, HangingSignItem> AZALEA_HANGING_SIGN = BitsAndBalance.ITEMS.register("azalea_hanging_sign",
            (Identifier key) -> new HangingSignItem(ModBlocks.AZALEA_HANGING_SIGN.get(), ModBlocks.AZALEA_WALL_HANGING_SIGN.get(), props(key).stacksTo(16)));

    // Azalea Boats - Using custom entity types for proper rendering
    public static final DeferredHolder<Item, AzaleaBoatItem> AZALEA_BOAT = BitsAndBalance.ITEMS.register("azalea_boat",
            (Identifier key) -> new AzaleaBoatItem(false, props(key)));
    
    public static final DeferredHolder<Item, AzaleaBoatItem> AZALEA_CHEST_BOAT = BitsAndBalance.ITEMS.register("azalea_chest_boat",
            (Identifier key) -> new AzaleaBoatItem(true, props(key)));

    // ========================================
    // ORE VARIANT ITEMS
    // ========================================
    // Coal Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_COAL_ORE = BitsAndBalance.ITEMS.register("andesite_coal_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_COAL_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_COAL_ORE = BitsAndBalance.ITEMS.register("diorite_coal_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_COAL_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_COAL_ORE = BitsAndBalance.ITEMS.register("granite_coal_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_COAL_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_COAL_ORE = BitsAndBalance.ITEMS.register("tuff_coal_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_COAL_ORE.get(), props(key)));

    // Iron Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_IRON_ORE = BitsAndBalance.ITEMS.register("andesite_iron_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_IRON_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_IRON_ORE = BitsAndBalance.ITEMS.register("diorite_iron_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_IRON_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_IRON_ORE = BitsAndBalance.ITEMS.register("granite_iron_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_IRON_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_IRON_ORE = BitsAndBalance.ITEMS.register("tuff_iron_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_IRON_ORE.get(), props(key)));

    // Copper Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_COPPER_ORE = BitsAndBalance.ITEMS.register("andesite_copper_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_COPPER_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_COPPER_ORE = BitsAndBalance.ITEMS.register("diorite_copper_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_COPPER_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_COPPER_ORE = BitsAndBalance.ITEMS.register("granite_copper_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_COPPER_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_COPPER_ORE = BitsAndBalance.ITEMS.register("tuff_copper_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_COPPER_ORE.get(), props(key)));

    // Gold Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_GOLD_ORE = BitsAndBalance.ITEMS.register("andesite_gold_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_GOLD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_GOLD_ORE = BitsAndBalance.ITEMS.register("diorite_gold_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_GOLD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_GOLD_ORE = BitsAndBalance.ITEMS.register("granite_gold_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_GOLD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_GOLD_ORE = BitsAndBalance.ITEMS.register("tuff_gold_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_GOLD_ORE.get(), props(key)));

    // Diamond Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_DIAMOND_ORE = BitsAndBalance.ITEMS.register("andesite_diamond_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_DIAMOND_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_DIAMOND_ORE = BitsAndBalance.ITEMS.register("diorite_diamond_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_DIAMOND_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_DIAMOND_ORE = BitsAndBalance.ITEMS.register("granite_diamond_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_DIAMOND_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_DIAMOND_ORE = BitsAndBalance.ITEMS.register("tuff_diamond_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_DIAMOND_ORE.get(), props(key)));

    // Emerald Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_EMERALD_ORE = BitsAndBalance.ITEMS.register("andesite_emerald_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_EMERALD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_EMERALD_ORE = BitsAndBalance.ITEMS.register("diorite_emerald_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_EMERALD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_EMERALD_ORE = BitsAndBalance.ITEMS.register("granite_emerald_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_EMERALD_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_EMERALD_ORE = BitsAndBalance.ITEMS.register("tuff_emerald_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_EMERALD_ORE.get(), props(key)));

    // Lapis Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_LAPIS_ORE = BitsAndBalance.ITEMS.register("andesite_lapis_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_LAPIS_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_LAPIS_ORE = BitsAndBalance.ITEMS.register("diorite_lapis_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_LAPIS_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_LAPIS_ORE = BitsAndBalance.ITEMS.register("granite_lapis_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_LAPIS_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_LAPIS_ORE = BitsAndBalance.ITEMS.register("tuff_lapis_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_LAPIS_ORE.get(), props(key)));

    // Redstone Ore Variants
    public static final DeferredHolder<Item, BlockItem> ANDESITE_REDSTONE_ORE = BitsAndBalance.ITEMS.register("andesite_redstone_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_REDSTONE_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_REDSTONE_ORE = BitsAndBalance.ITEMS.register("diorite_redstone_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_REDSTONE_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_REDSTONE_ORE = BitsAndBalance.ITEMS.register("granite_redstone_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_REDSTONE_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_REDSTONE_ORE = BitsAndBalance.ITEMS.register("tuff_redstone_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_REDSTONE_ORE.get(), props(key)));

    // Zinc Ore Variants (Create Mod)
    public static final DeferredHolder<Item, BlockItem> ANDESITE_ZINC_ORE = BitsAndBalance.ITEMS.register("andesite_zinc_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.ANDESITE_ZINC_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> DIORITE_ZINC_ORE = BitsAndBalance.ITEMS.register("diorite_zinc_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.DIORITE_ZINC_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> GRANITE_ZINC_ORE = BitsAndBalance.ITEMS.register("granite_zinc_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.GRANITE_ZINC_ORE.get(), props(key)));
    public static final DeferredHolder<Item, BlockItem> TUFF_ZINC_ORE = BitsAndBalance.ITEMS.register("tuff_zinc_ore",
            (Identifier key) -> new BlockItem(org.onenonly.bitsandbalance.blocks.OreVariants.TUFF_ZINC_ORE.get(), props(key)));

    public static void register(IEventBus bus) {
        // Items are already registered via BitsAndBalance.ITEMS
    }
}