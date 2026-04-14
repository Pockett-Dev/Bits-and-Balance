package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModItems;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.compat.CreateCompat;

import java.util.Comparator;

/**
 * Creative tab (category) for all Bits and Balance content.
 * Includes Azalea wood blocks, ore variants, and potions.
 */
public class CreativeCategory {
    // Register the Bits and Balance tab via the main mod's CreativeModeTab deferred register
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BITS_AND_BALANCE_TAB = BitsAndBalance.CREATIVE_MODE_TABS.register(
            "bits_and_balance",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bitsandbalance.bits_and_balance"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> new ItemStack(ModItems.AZALEA_LOG.get()))
                    .displayItems((parameters, output) -> {
                        // ========================================
                        // AZALEA WOOD BLOCKS
                        // ========================================
                        // Only show azalea woodset if enabled in config
                        if (org.onenonly.bitsandbalance.WorldConfig.ENABLE_AZALEA_WOODSET.get()) {
                            output.accept(ModItems.AZALEA_LOG.get());
                            output.accept(ModItems.AZALEA_WOOD.get());
                            output.accept(ModItems.STRIPPED_AZALEA_LOG.get());
                            output.accept(ModItems.STRIPPED_AZALEA_WOOD.get());
                            output.accept(ModItems.AZALEA_PLANKS.get());
                            
                            // Azalea Wood Derivatives
                            output.accept(ModItems.AZALEA_SLAB.get());
                            output.accept(ModItems.AZALEA_STAIRS.get());
                            output.accept(ModItems.AZALEA_BUTTON.get());
                            output.accept(ModItems.AZALEA_PRESSURE_PLATE.get());
                            output.accept(ModItems.AZALEA_FENCE.get());
                            output.accept(ModItems.AZALEA_FENCE_GATE.get());
                            output.accept(ModItems.AZALEA_SHELF.get());
                            output.accept(ModItems.AZALEA_DOOR.get());
                            output.accept(ModItems.AZALEA_TRAPDOOR.get());
                            output.accept(ModItems.AZALEA_SIGN.get());
                            output.accept(ModItems.AZALEA_HANGING_SIGN.get());
                            
                            // Azalea Boats
                            output.accept(ModItems.AZALEA_BOAT.get());
                            output.accept(ModItems.AZALEA_CHEST_BOAT.get());
                        }
                        
                        // ========================================
                        // ORE VARIANTS
                        // ========================================
                        // Coal Ore Variants
                        output.accept(ModItems.ANDESITE_COAL_ORE.get());
                        output.accept(ModItems.DIORITE_COAL_ORE.get());
                        output.accept(ModItems.GRANITE_COAL_ORE.get());
                        output.accept(ModItems.TUFF_COAL_ORE.get());
                        
                        // Iron Ore Variants
                        output.accept(ModItems.ANDESITE_IRON_ORE.get());
                        output.accept(ModItems.DIORITE_IRON_ORE.get());
                        output.accept(ModItems.GRANITE_IRON_ORE.get());
                        output.accept(ModItems.TUFF_IRON_ORE.get());
                        
                        // Copper Ore Variants
                        output.accept(ModItems.ANDESITE_COPPER_ORE.get());
                        output.accept(ModItems.DIORITE_COPPER_ORE.get());
                        output.accept(ModItems.GRANITE_COPPER_ORE.get());
                        output.accept(ModItems.TUFF_COPPER_ORE.get());
                        
                        // Gold Ore Variants
                        output.accept(ModItems.ANDESITE_GOLD_ORE.get());
                        output.accept(ModItems.DIORITE_GOLD_ORE.get());
                        output.accept(ModItems.GRANITE_GOLD_ORE.get());
                        output.accept(ModItems.TUFF_GOLD_ORE.get());
                        
                        // Diamond Ore Variants
                        output.accept(ModItems.ANDESITE_DIAMOND_ORE.get());
                        output.accept(ModItems.DIORITE_DIAMOND_ORE.get());
                        output.accept(ModItems.GRANITE_DIAMOND_ORE.get());
                        output.accept(ModItems.TUFF_DIAMOND_ORE.get());
                        
                        // Emerald Ore Variants
                        output.accept(ModItems.ANDESITE_EMERALD_ORE.get());
                        output.accept(ModItems.DIORITE_EMERALD_ORE.get());
                        output.accept(ModItems.GRANITE_EMERALD_ORE.get());
                        output.accept(ModItems.TUFF_EMERALD_ORE.get());
                        
                        // Lapis Ore Variants
                        output.accept(ModItems.ANDESITE_LAPIS_ORE.get());
                        output.accept(ModItems.DIORITE_LAPIS_ORE.get());
                        output.accept(ModItems.GRANITE_LAPIS_ORE.get());
                        output.accept(ModItems.TUFF_LAPIS_ORE.get());
                        
                        // Redstone Ore Variants
                        output.accept(ModItems.ANDESITE_REDSTONE_ORE.get());
                        output.accept(ModItems.DIORITE_REDSTONE_ORE.get());
                        output.accept(ModItems.GRANITE_REDSTONE_ORE.get());
                        output.accept(ModItems.TUFF_REDSTONE_ORE.get());
                        
                        // Zinc Ore Variants (Create Mod)
                        if (CreateCompat.isZincContentAvailable()) {
                            output.accept(ModItems.ANDESITE_ZINC_ORE.get());
                            output.accept(ModItems.DIORITE_ZINC_ORE.get());
                            output.accept(ModItems.GRANITE_ZINC_ORE.get());
                            output.accept(ModItems.TUFF_ZINC_ORE.get());
                        }

                        // ========================================
                        // STORAGE BLOCKS
                        // ========================================
                        BuiltInRegistries.ITEM.get(Identifier.parse("bitsandbalance:raw_quartz_block"))
                            .ifPresent(holder -> output.accept(holder.value()));
                        BuiltInRegistries.ITEM.get(Identifier.parse("bitsandbalance:bottle_of_cloud"))
                            .ifPresent(holder -> output.accept(holder.value()));
                        if (Config.enableGlowGoo) {
                            BuiltInRegistries.ITEM.get(Identifier.parse("bitsandbalance:glow_goo"))
                                .ifPresent(holder -> output.accept(holder.value()));
                        }
                        if (Config.enableDogMusicDisc) {
                            BuiltInRegistries.ITEM.get(ModDogMusicDisc.MUSIC_DISC_DOG_ID)
                                .ifPresent(holder -> output.accept(holder.value()));
                        }

                        // ========================================
                        // ENHANCED SLABS (Vertical Slabs)
                        // ========================================
                        if (Config.enableEnhancedSlabs && Config.enhancedSlabsVerticalSlabs) {
                            addVerticalSlabItems(output);
                        }
                        
                        // ========================================
                        // POTIONS
                        // ========================================
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
                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.POTION, "glowing"));
                            output.accept(potionStack(Items.POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.POTION, "bioluminescence"));
                            output.accept(potionStack(Items.POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.POTION, "long_bioluminescence"));
                        }
                        
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
                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.SPLASH_POTION, "glowing"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.SPLASH_POTION, "bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_bioluminescence"));
                        }
                        
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
                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.LINGERING_POTION, "glowing"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.LINGERING_POTION, "bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_bioluminescence"));
                        }
                    })
                    .build()
    );
    private static void addVerticalSlabItems(CreativeModeTab.Output output) {
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> BitsAndBalance.MODID.equals(id.getNamespace()))
                .filter(id -> id.getPath().startsWith("vertical_"))
                .sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> BuiltInRegistries.ITEM.get(id).ifPresent(holder -> {
                    Item item = holder.value();
                    if (item != Items.AIR) {
                        output.accept(item);
                    }
                }));
    }

    private static Item itemOrAir(String path) {
        return BuiltInRegistries.ITEM.get(Identifier.parse(BitsAndBalance.MODID + ":" + path))
                .map(holder -> holder.value())
                .orElse(Items.AIR);
    }

    /**
     * Helper method to create a potion ItemStack with the specified potion type.
     */
    private static ItemStack potionStack(Item baseItem, String id) {
        var rl = Identifier.parse(BitsAndBalance.MODID + ":" + id);
        var holderOpt = BuiltInRegistries.POTION.get(rl);
        if (holderOpt.isEmpty()) {
            return new ItemStack(baseItem);
        }
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(baseItem, holderOpt.get());
    }

    /**
     * Ensures the class is initialized so that the tab is registered.
     */
    public static void register() {
        // no-op; referencing this method from BitsAndBalance guarantees class loading
    }
}

